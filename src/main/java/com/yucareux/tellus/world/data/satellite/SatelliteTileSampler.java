package com.yucareux.tellus.world.data.satellite;

import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
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

public final class SatelliteTileSampler {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final double MAX_WEB_MERCATOR_LAT = 85.05112878D;
	private static final int TILE_SIZE = 256;
	private static final int MEMORY_CACHE_SIZE = 512;
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
	private static final String USER_AGENT = "Tellus/2.0.0 (Minecraft Mod)";

	private final HttpClient httpClient;
	private final Path cacheRoot;
	private final Executor executor;
	private final Map<TileKey, BufferedImage> memoryCache;
	private final ConcurrentHashMap<TileKey, CompletableFuture<@Nullable BufferedImage>> inFlight;
	private final String urlTemplate;

	public SatelliteTileSampler(final HttpClient httpClient, final Path cacheRoot) {
		this(httpClient, cacheRoot, ForkJoinPool.commonPool(),
				"https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/%d/%d/%d");
	}

	public SatelliteTileSampler(
			final HttpClient httpClient,
			final Path cacheRoot,
			final Executor executor,
			final String urlTemplate) {
		this.httpClient = httpClient;
		this.cacheRoot = cacheRoot;
		this.executor = executor;
		this.urlTemplate = urlTemplate;
		this.inFlight = new ConcurrentHashMap<>();
		this.memoryCache = Collections.synchronizedMap(new LinkedHashMap<>(MEMORY_CACHE_SIZE + 1, 0.75F, true) {
			@Override
			protected boolean removeEldestEntry(final Map.Entry<TileKey, BufferedImage> eldest) {
				return size() > MEMORY_CACHE_SIZE;
			}
		});
	}

	public int sampleRgb(final double latitude, final double longitude, final int zoom) {
		final TilePixelCoord sample = TilePixelCoord.fromLatLon(latitude, longitude, zoom);
		final BufferedImage tile = getTileBlocking(sample.key());
		if (tile == null) {
			return -1;
		}
		if (sample.pixelX() < 0 || sample.pixelY() < 0
				|| sample.pixelX() >= tile.getWidth() || sample.pixelY() >= tile.getHeight()) {
			return -1;
		}
		return tile.getRGB(sample.pixelX(), sample.pixelY()) & 0x00FFFFFF;
	}

	public int sampleRgbNonBlocking(final double latitude, final double longitude, final int zoom) {
		final TilePixelCoord sample = TilePixelCoord.fromLatLon(latitude, longitude, zoom);
		final BufferedImage tile = getTileNonBlocking(sample.key());
		if (tile == null) {
			return -1;
		}
		if (sample.pixelX() < 0 || sample.pixelY() < 0
				|| sample.pixelX() >= tile.getWidth() || sample.pixelY() >= tile.getHeight()) {
			return -1;
		}
		return tile.getRGB(sample.pixelX(), sample.pixelY()) & 0x00FFFFFF;
	}

	public TileBounds tileBounds(final TileKey key) {
		return TileBounds.fromTile(key);
	}

	public TileKey tileKeyForLatLon(final double latitude, final double longitude, final int zoom) {
		return TilePixelCoord.fromLatLon(latitude, longitude, zoom).key();
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
			LOGGER.debug("Satellite tile fetch timed out for {}", key);
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

		final Path path = tilePath(key);
		if (Files.exists(path)) {
			final BufferedImage diskImage = readTile(path);
			if (diskImage != null) {
				memoryCache.put(key, diskImage);
			}
			return diskImage;
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
			LOGGER.warn("Failed to create satellite tile cache directory {}", path.getParent(), e);
		}

		try {
			final String url = String.format(urlTemplate, key.zoom(), key.y(), key.x());
			final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(REQUEST_TIMEOUT)
					.header("User-Agent", USER_AGENT)
					.GET()
					.build();
			final HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200 || response.body() == null || response.body().length == 0) {
				LOGGER.debug("Satellite tile request failed for {} with status {}", key, response.statusCode());
				return null;
			}

			final byte[] bytes = response.body();
			try {
				Files.write(path, bytes);
			} catch (final IOException e) {
				LOGGER.debug("Failed to write satellite tile cache {}", path, e);
			}

			return ImageIO.read(new ByteArrayInputStream(bytes));
		} catch (final Exception e) {
			LOGGER.debug("Failed to download satellite tile {}", key, e);
			return null;
		}
	}

	private @Nullable BufferedImage readTile(final Path path) {
		try {
			return ImageIO.read(path.toFile());
		} catch (final IOException e) {
			LOGGER.debug("Failed to read cached satellite tile {}", path, e);
			return null;
		}
	}

	private Path tilePath(final TileKey key) {
		return cacheRoot.resolve(Integer.toString(key.zoom()))
				.resolve(Integer.toString(key.x()))
				.resolve(key.y() + ".png");
	}

	public record TileKey(int x, int y, int zoom) {
	}

	public record TileBounds(double northLatitude, double southLatitude, double westLongitude, double eastLongitude) {
		private static TileBounds fromTile(final TileKey key) {
			final int n = 1 << key.zoom();
			final double west = (key.x() / (double) n) * 360.0D - 180.0D;
			final double east = ((key.x() + 1) / (double) n) * 360.0D - 180.0D;
			final double north = tileYToLatitude(key.y(), key.zoom());
			final double south = tileYToLatitude(key.y() + 1, key.zoom());
			return new TileBounds(north, south, west, east);
		}
	}

	private record TilePixelCoord(TileKey key, int pixelX, int pixelY) {
		private static TilePixelCoord fromLatLon(final double latitude, final double longitude, final int zoom) {
			final int n = 1 << zoom;
			final double clampedLat = Math.max(-MAX_WEB_MERCATOR_LAT, Math.min(MAX_WEB_MERCATOR_LAT, latitude));
			final double wrappedLon = wrapLongitude(longitude);

			final double x = (wrappedLon + 180.0D) / 360.0D * n;
			final double latRad = Math.toRadians(clampedLat);
			final double y = (1.0D - (Math.log(Math.tan(latRad) + (1.0D / Math.cos(latRad))) / Math.PI)) * 0.5D * n;

			int tileX = clamp((int) Math.floor(x), 0, n - 1);
			int tileY = clamp((int) Math.floor(y), 0, n - 1);
			int pixelX = clamp((int) Math.floor((x - tileX) * TILE_SIZE), 0, TILE_SIZE - 1);
			int pixelY = clamp((int) Math.floor((y - tileY) * TILE_SIZE), 0, TILE_SIZE - 1);

			return new TilePixelCoord(new TileKey(tileX, tileY, zoom), pixelX, pixelY);
		}
	}

	private static int clamp(final int value, final int min, final int max) {
		return Math.max(min, Math.min(max, value));
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

	private static double tileYToLatitude(final int y, final int zoom) {
		final int n = 1 << zoom;
		final double yNorm = y / (double) n;
		final double mercator = Math.PI * (1.0D - 2.0D * yNorm);
		return Math.toDegrees(Math.atan(Math.sinh(mercator)));
	}
}
