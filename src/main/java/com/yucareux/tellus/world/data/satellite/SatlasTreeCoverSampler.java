package com.yucareux.tellus.world.data.satellite;

import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class SatlasTreeCoverSampler {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final double MAX_WEB_MERCATOR_LAT = 85.05112878D;
	private static final int TREE_COVER_ZOOM = 7;
	private static final int MEMORY_CACHE_SIZE = 24;
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
	private static final String USER_AGENT = "Tellus/2.0.0 (Minecraft Mod)";
	private static final String DEFAULT_RELEASE =
			System.getProperty("tellus.satlas.treecover.release", "2024-07");
	private static final String URL_TEMPLATE =
			"https://storage.googleapis.com/satlas-explorer-public/outputs/tree-cover/%s/%d_%d.tif";

	private final HttpClient httpClient;
	private final Path cacheRoot;
	private final Executor executor;
	private final String release;
	private final Map<TileKey, BufferedImage> memoryCache;
	private final ConcurrentHashMap<TileKey, CompletableFuture<@Nullable BufferedImage>> inFlight;

	public SatlasTreeCoverSampler(final HttpClient httpClient, final Path cacheRoot) {
		this(httpClient, cacheRoot, ForkJoinPool.commonPool(), DEFAULT_RELEASE);
	}

	public SatlasTreeCoverSampler(
			final HttpClient httpClient,
			final Path cacheRoot,
			final Executor executor,
			final String release) {
		this.httpClient = httpClient;
		this.cacheRoot = cacheRoot;
		this.executor = executor;
		this.release = sanitizeRelease(release);
		this.inFlight = new ConcurrentHashMap<>();
		this.memoryCache = Collections.synchronizedMap(new LinkedHashMap<>(MEMORY_CACHE_SIZE + 1, 0.75F, true) {
			@Override
			protected boolean removeEldestEntry(final Map.Entry<TileKey, BufferedImage> eldest) {
				return size() > MEMORY_CACHE_SIZE;
			}
		});
	}

	public int sampleTreeCoverClass(final double latitude, final double longitude) {
		final TilePixelCoord sample = TilePixelCoord.fromLatLon(latitude, longitude, TREE_COVER_ZOOM);
		final BufferedImage tile = getTileBlocking(sample.key());
		return sampleTreeCoverClass(tile, sample);
	}

	public int sampleTreeCoverClassNonBlocking(final double latitude, final double longitude) {
		final TilePixelCoord sample = TilePixelCoord.fromLatLon(latitude, longitude, TREE_COVER_ZOOM);
		final BufferedImage tile = getTileNonBlocking(sample.key());
		return sampleTreeCoverClass(tile, sample);
	}

	public static double classToCanopyStrength(final int treeCoverClass) {
		return switch (treeCoverClass) {
			case 1 -> 0.0D;
			case 2 -> 0.30D;
			case 3 -> 0.55D;
			case 4 -> 0.78D;
			case 5 -> 1.0D;
			default -> -1.0D;
		};
	}

	private int sampleTreeCoverClass(final @Nullable BufferedImage tile, final TilePixelCoord sample) {
		if (tile == null) {
			return -1;
		}

		final int tileWidth = tile.getWidth();
		final int tileHeight = tile.getHeight();
		if (tileWidth <= 0 || tileHeight <= 0) {
			return -1;
		}

		final int pixelX = clamp((int) Math.floor(sample.fracX() * tileWidth), 0, tileWidth - 1);
		final int pixelY = clamp((int) Math.floor(sample.fracY() * tileHeight), 0, tileHeight - 1);

		final Raster raster = tile.getRaster();
		if (raster != null && raster.getNumBands() > 0) {
			final int rawValue = raster.getSample(pixelX, pixelY, 0);
			if (rawValue >= 0 && rawValue <= 5) {
				return rawValue;
			}
		}

		final int rgb = tile.getRGB(pixelX, pixelY);
		return decodeClassFromRgb(rgb);
	}

	private @Nullable BufferedImage getTileBlocking(final TileKey key) {
		final BufferedImage cached = memoryCache.get(key);
		if (cached != null) {
			return cached;
		}

		final CompletableFuture<@Nullable BufferedImage> future = inFlight.computeIfAbsent(
				key,
				tileKey -> CompletableFuture.supplyAsync(() -> loadTile(tileKey), executor));
		try {
			final BufferedImage image = future.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
			if (image != null) {
				memoryCache.put(key, image);
			}
			return image;
		} catch (final Exception e) {
			LOGGER.debug("SATLAS tree-cover tile fetch timed out for {}", key);
			return null;
		} finally {
			inFlight.remove(key, future);
		}
	}

	private @Nullable BufferedImage getTileNonBlocking(final TileKey key) {
		final BufferedImage cached = memoryCache.get(key);
		if (cached != null) {
			return cached;
		}

		inFlight.computeIfAbsent(key, tileKey -> CompletableFuture.supplyAsync(() -> {
			final BufferedImage image = loadTile(tileKey);
			if (image != null) {
				memoryCache.put(tileKey, image);
			}
			inFlight.remove(tileKey);
			return image;
		}, executor));
		return null;
	}

	private @Nullable BufferedImage loadTile(final TileKey key) {
		final Path path = tilePath(key);
		if (Files.exists(path)) {
			return readTile(path);
		}

		try {
			Files.createDirectories(path.getParent());
		} catch (final IOException e) {
			LOGGER.warn("Failed to create SATLAS cache directory {}", path.getParent(), e);
		}

		try {
			final String url = String.format(URL_TEMPLATE, release, key.x(), key.y());
			final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(REQUEST_TIMEOUT)
					.header("User-Agent", USER_AGENT)
					.GET()
					.build();
			final HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200 || response.body() == null || response.body().length == 0) {
				LOGGER.debug("SATLAS tree-cover request failed for {} with status {}", key, response.statusCode());
				return null;
			}

			final byte[] bytes = response.body();
			try {
				final Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");
				Files.write(tempPath, bytes);
				Files.move(tempPath, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException e) {
				LOGGER.debug("Failed to write SATLAS tree-cover cache {}", path, e);
			}

			return ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (final Exception e) {
			LOGGER.debug("Failed to download SATLAS tree-cover tile {}", key, e);
			return null;
		}
	}

	private @Nullable BufferedImage readTile(final Path path) {
		try {
			return ImageIO.read(path.toFile());
		} catch (final IOException e) {
			LOGGER.debug("Failed to read cached SATLAS tree-cover tile {}", path, e);
			return null;
		}
	}

	private Path tilePath(final TileKey key) {
		return cacheRoot.resolve(release)
				.resolve(Integer.toString(key.x()))
				.resolve(key.y() + ".tif");
	}

	private static int decodeClassFromRgb(final int rgb) {
		final int r = (rgb >> 16) & 0xFF;
		final int g = (rgb >> 8) & 0xFF;
		final int b = rgb & 0xFF;
		if (r == g && g == b) {
			return clamp(Math.round((r / 255.0F) * 5.0F), 0, 5);
		}
		final int max = Math.max(r, Math.max(g, b));
		return clamp(Math.round((max / 255.0F) * 5.0F), 0, 5);
	}

	private static String sanitizeRelease(final String release) {
		if (release == null || release.isBlank()) {
			return DEFAULT_RELEASE;
		}
		return release.trim();
	}

	public record TileKey(int x, int y, int zoom) {
	}

	private record TilePixelCoord(TileKey key, double fracX, double fracY) {
		private static TilePixelCoord fromLatLon(final double latitude, final double longitude, final int zoom) {
			final int n = 1 << zoom;
			final double clampedLat = Math.max(-MAX_WEB_MERCATOR_LAT, Math.min(MAX_WEB_MERCATOR_LAT, latitude));
			final double wrappedLon = wrapLongitude(longitude);

			final double x = (wrappedLon + 180.0D) / 360.0D * n;
			final double latRad = Math.toRadians(clampedLat);
			final double y = (1.0D - (Math.log(Math.tan(latRad) + (1.0D / Math.cos(latRad))) / Math.PI)) * 0.5D * n;

			final int tileX = clamp((int) Math.floor(x), 0, n - 1);
			final int tileY = clamp((int) Math.floor(y), 0, n - 1);
			final double fracX = clampUnit(x - tileX);
			final double fracY = clampUnit(y - tileY);
			return new TilePixelCoord(new TileKey(tileX, tileY, zoom), fracX, fracY);
		}
	}

	private static int clamp(final int value, final int min, final int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clampUnit(final double value) {
		if (value <= 0.0D) {
			return 0.0D;
		}
		if (value >= 1.0D) {
			return Math.nextDown(1.0D);
		}
		return value;
	}

	private static double wrapLongitude(final double longitude) {
		double wrapped = longitude;
		while (wrapped < -180.0D) {
			wrapped += 360.0D;
		}
		while (wrapped >= 180.0D) {
			wrapped -= 360.0D;
		}
		return wrapped;
	}
}
