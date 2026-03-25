package com.yucareux.tellus.client.widget.map;

import com.yucareux.tellus.Tellus;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;

public class SlippyMapTileCache {
	private static final int CACHE_SIZE = 1024;
	private static final String DEFAULT_CACHE_NAMESPACE = "map";
	private static final String DEFAULT_TEMPLATE = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";

	private final ExecutorService loadingService = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
			.setDaemon(true)
			.setNameFormat("tellus-map-load-%d")
			.build());

	private final Queue<InputStream> loadingStreams = new LinkedBlockingQueue<>();
	private final Path cacheRoot;
	private final String urlTemplate;
	private final HttpClient httpClient;
	private final LoadingCache<SlippyMapTilePos, SlippyMapTile> tileCache;

	public SlippyMapTileCache() {
		this(DEFAULT_CACHE_NAMESPACE, DEFAULT_TEMPLATE);
	}

	public SlippyMapTileCache(String cacheNamespace, String urlTemplate) {
		String resolvedNamespace = (cacheNamespace == null || cacheNamespace.isBlank())
				? DEFAULT_CACHE_NAMESPACE
				: cacheNamespace;
		this.cacheRoot = Minecraft.getInstance().gameDirectory.toPath().resolve("tellus/cache/" + resolvedNamespace);
		this.urlTemplate = (urlTemplate == null || urlTemplate.isBlank()) ? DEFAULT_TEMPLATE : urlTemplate;
		this.httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		this.tileCache = CacheBuilder.newBuilder()
				.maximumSize(CACHE_SIZE)
				.removalListener(notification -> {
					SlippyMapTile tile = (SlippyMapTile) notification.getValue();
					if (tile != null) {
						tile.delete();
					}
				})
				.build(new CacheLoader<>() {
					@Override
					public SlippyMapTile load(SlippyMapTilePos key) {
						SlippyMapTile tile = new SlippyMapTile(key);
						SlippyMapTileCache.this.loadingService
								.submit(() -> tile.supplyImage(SlippyMapTileCache.this.downloadImage(key)));
						return tile;
					}
				});
	}

	public SlippyMapTile getTile(SlippyMapTilePos pos) {
		try {
			return this.tileCache.get(pos);
		} catch (Exception e) {
			SlippyMapTile tile = new SlippyMapTile(pos);
			tile.supplyImage(this.createErrorImage());
			return tile;
		}
	}

	public void shutdown() {
		for (SlippyMapTile tile : this.tileCache.asMap().values()) {
			tile.delete();
		}

		this.tileCache.invalidateAll();
		this.loadingService.shutdown();

		while (!this.loadingStreams.isEmpty()) {
			try {
				InputStream poll = this.loadingStreams.poll();
				if (poll != null) {
					poll.close();
				}
			} catch (IOException e) {
				Tellus.LOGGER.warn("Failed to close loading map stream", e);
			}
		}
	}

	private NativeImage downloadImage(SlippyMapTilePos pos) {
		final Path cachePath = this.cacheRoot.resolve(pos.getCacheName());
		try (InputStream input = Objects.requireNonNull(this.getStream(pos), "tileStream")) {
			final byte[] data = input.readAllBytes();
			return this.decodeImageData(data);
		} catch (IOException first) {
			boolean removedCorruptCache = false;
			try {
				removedCorruptCache = Files.deleteIfExists(cachePath);
			} catch (IOException ignored) {
			}

			if (removedCorruptCache) {
				try (InputStream retry = Objects.requireNonNull(this.getStream(pos), "tileStreamRetry")) {
					final byte[] data = retry.readAllBytes();
					return this.decodeImageData(data);
				} catch (IOException second) {
					Tellus.LOGGER.error("Failed to load map tile after cache purge: {}", second.getMessage(), second);
				}
			} else {
				Tellus.LOGGER.error("Failed to load map tile: {}", first.getMessage(), first);
			}
		}
		return this.createErrorImage();
	}

	private NativeImage decodeImageData(final byte[] data) throws IOException {
		try {
			return NativeImage.read(new ByteArrayInputStream(data));
		} catch (IOException nativeDecodeFailure) {
			final BufferedImage buffered = ImageIO.read(new ByteArrayInputStream(data));
			if (buffered == null) {
				throw nativeDecodeFailure;
			}

			final int width = buffered.getWidth();
			final int height = buffered.getHeight();
			final NativeImage converted = new NativeImage(width, height, false);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					final int argb = buffered.getRGB(x, y);
					converted.setPixelABGR(x, y, argbToAbgr(argb));
				}
			}
			return converted;
		}
	}

	private static int argbToAbgr(final int argb) {
		final int a = (argb >>> 24) & 0xFF;
		final int r = (argb >>> 16) & 0xFF;
		final int g = (argb >>> 8) & 0xFF;
		final int b = argb & 0xFF;
		return (a << 24) | (b << 16) | (g << 8) | r;
	}

	private @NonNull InputStream getStream(SlippyMapTilePos pos) throws IOException {
		Path cachePath = this.cacheRoot.resolve(pos.getCacheName());
		if (Files.exists(cachePath)) {
			return new BufferedInputStream(Files.newInputStream(cachePath));
		}

		String url = this.buildTileUrl(pos);
		byte[] data = this.downloadBytes(url);

		if ((data == null || data.length == 0) && this.urlTemplate.contains("{x}") && this.urlTemplate.contains("{y}")) {
			String alternateUrl = this.urlTemplate
					.replace("{z}", Integer.toString(pos.getZoom()))
					.replace("{x}", Integer.toString(pos.getY()))
					.replace("{y}", Integer.toString(pos.getX()));
			data = this.downloadBytes(alternateUrl);
		}

		if (data == null || data.length == 0) {
			throw new IOException("Failed to download map tile: " + pos);
		}

		this.cacheData(cachePath, data);
		return new ByteArrayInputStream(data);
	}

	private String buildTileUrl(final SlippyMapTilePos pos) {
		if (this.urlTemplate.contains("{z}")
				|| this.urlTemplate.contains("{x}")
				|| this.urlTemplate.contains("{y}")) {
			return this.urlTemplate
					.replace("{z}", Integer.toString(pos.getZoom()))
					.replace("{x}", Integer.toString(pos.getX()))
					.replace("{y}", Integer.toString(pos.getY()));
		}

		return String.format(this.urlTemplate, pos.getZoom(), pos.getX(), pos.getY());
	}

	private byte[] downloadBytes(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofSeconds(8))
					.header("User-Agent", "Tellus/2.0.0 (Minecraft Mod)")
					.GET()
					.build();
			HttpResponse<byte[]> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() == HttpURLConnection.HTTP_OK && response.body() != null
					&& response.body().length > 0) {
				return response.body();
			}
		} catch (Exception ignored) {
			// caller handles fallback path and error image rendering
		}
		return null;
	}

	private void cacheData(Path cachePath, byte[] data) {
		try {
			Files.createDirectories(this.cacheRoot);
		} catch (IOException e) {
			Tellus.LOGGER.error("Failed to create cache root", e);
		}

		try (OutputStream output = Files.newOutputStream(cachePath)) {
			output.write(data);
		} catch (IOException e) {
			Tellus.LOGGER.error("Failed to cache map tile", e);
		}
	}

	private NativeImage createErrorImage() {
		NativeImage result = new NativeImage(SlippyMap.TILE_SIZE, SlippyMap.TILE_SIZE, false);
		for (int x = 0; x < SlippyMap.TILE_SIZE; x++) {
			for (int y = 0; y < SlippyMap.TILE_SIZE; y++) {
				result.setPixelABGR(x, y, 0xFF303030);
			}
		}
		return result;
	}
}
