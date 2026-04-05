package com.yucareux.tellus.world.data.overture;

import com.wdtinc.mapbox_vector_tile.adapt.jts.MvtReader;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TagKeyValueMapConverter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsLayer;
import com.wdtinc.mapbox_vector_tile.adapt.jts.model.JtsMvt;
import com.yucareux.tellus.Tellus;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.zip.GZIPInputStream;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public final class OvertureOverlaySampler {
	private static final double MAX_WEB_MERCATOR_LAT = 85.05112878D;
	private static final String DEFAULT_RELEASE =
			System.getProperty("tellus.overture.release", "2026-03-18.0");
	private static final String DEFAULT_BASE_URL =
			"https://tiles.overturemaps.org/" + DEFAULT_RELEASE + "/";
	private static final String DEFAULT_BUILDINGS_PM_TILES = "buildings.pmtiles";
	private static final String DEFAULT_TRANSPORTATION_PM_TILES = "transportation.pmtiles";
	private static final int TILE_MASK_SIZE = 256;
	private static final int MEMORY_CACHE_SIZE = intProperty("tellus.overture.cacheTiles", 96);
	private static final float ROAD_STROKE_MINOR = 1.1F;
	private static final float ROAD_STROKE_MEDIUM = 1.6F;
	private static final float ROAD_STROKE_MAJOR = 2.2F;
	private static final float ROAD_STROKE_WIDE = 3.0F;
	private static final Executor EXECUTOR = ForkJoinPool.commonPool();
	private static final TagKeyValueMapConverter TAG_CONVERTER = new TagKeyValueMapConverter(false);

	private final SourceSampler buildings;
	private final SourceSampler transportation;

	public OvertureOverlaySampler() {
		final String baseUrl = normalizeBaseUrl(System.getProperty("tellus.overture.baseUrl", DEFAULT_BASE_URL));
		final String buildingsUrl = System.getProperty(
				"tellus.overture.buildings.pmtiles",
				baseUrl + DEFAULT_BUILDINGS_PM_TILES);
		final String transportationUrl = System.getProperty(
				"tellus.overture.transportation.pmtiles",
				baseUrl + DEFAULT_TRANSPORTATION_PM_TILES);

		this.buildings = new SourceSampler("buildings", buildingsUrl, SourceType.BUILDINGS);
		this.transportation = new SourceSampler("transportation", transportationUrl, SourceType.TRANSPORTATION);
	}

	public void prefetchNonBlocking(final double latitude, final double longitude, final int desiredZoom) {
		if (this.transportation.available()) {
			final int zoom = this.transportation.clampZoom(desiredZoom);
			this.transportation.prefetchNonBlocking(TilePixelCoord.fromLatLon(latitude, longitude, zoom).key(), 1);
		}
		if (this.buildings.available()) {
			final int zoom = this.buildings.clampZoom(desiredZoom);
			this.buildings.prefetchNonBlocking(TilePixelCoord.fromLatLon(latitude, longitude, zoom).key(), 0);
		}
	}

	public OverlaySample sampleOverlayNonBlocking(
			final double latitude,
			final double longitude,
			final int desiredZoom) {
		boolean known = false;
		boolean road = false;
		boolean building = false;

		if (this.transportation.available()) {
			final int zoom = this.transportation.clampZoom(desiredZoom);
			final TilePixelCoord sample = TilePixelCoord.fromLatLon(latitude, longitude, zoom);
			final TileMask mask = this.transportation.getTileNonBlocking(sample.key());
			if (mask != null) {
				known = true;
				road = mask.sample(sample.fracX(), sample.fracY());
			}
		}

		if (this.buildings.available()) {
			final int zoom = this.buildings.clampZoom(desiredZoom);
			final TilePixelCoord sample = TilePixelCoord.fromLatLon(latitude, longitude, zoom);
			final TileMask mask = this.buildings.getTileNonBlocking(sample.key());
			if (mask != null) {
				known = true;
				building = mask.sample(sample.fracX(), sample.fracY());
			}
		}

		return new OverlaySample(known, road, building);
	}

	public record OverlaySample(boolean known, boolean road, boolean building) {
		public boolean hasAnyOverlay() {
			return this.road || this.building;
		}
	}

	private enum SourceType {
		BUILDINGS,
		TRANSPORTATION
	}

	private static final class SourceSampler {
		private final String name;
		private final PmTilesReader reader;
		private final SourceType sourceType;
		private final int minZoom;
		private final int maxZoom;
		private final int tileCompression;
		private final boolean available;
		private final Map<TileKey, TileMask> memoryCache;
		private final ConcurrentHashMap<TileKey, CompletableFuture<@Nullable TileMask>> inFlight;

		private SourceSampler(final String name, final String url, final SourceType sourceType) {
			this.name = name;
			this.reader = new PmTilesReader(url);
			this.sourceType = sourceType;

			int resolvedMinZoom = 0;
			int resolvedMaxZoom = 0;
			int resolvedCompression = 1;
			boolean resolvedAvailable = false;
			try {
				final PmTilesReader.PmTilesHeader header = this.reader.header();
				resolvedMinZoom = header.minZoom();
				resolvedMaxZoom = header.maxZoom();
				resolvedCompression = header.tileCompression();
				resolvedAvailable = true;
			} catch (final IOException e) {
				Tellus.LOGGER.warn("Overture {} PMTiles unavailable at {}", name, url, e);
			}

			this.minZoom = resolvedMinZoom;
			this.maxZoom = resolvedMaxZoom;
			this.tileCompression = resolvedCompression;
			this.available = resolvedAvailable;
			this.memoryCache = Collections.synchronizedMap(new LinkedHashMap<>(MEMORY_CACHE_SIZE + 1, 0.75F, true) {
				@Override
				protected boolean removeEldestEntry(final Map.Entry<TileKey, TileMask> eldest) {
					return size() > MEMORY_CACHE_SIZE;
				}
			});
			this.inFlight = new ConcurrentHashMap<>();
		}

		private boolean available() {
			return this.available;
		}

		private int clampZoom(final int desiredZoom) {
			if (!this.available) {
				return desiredZoom;
			}
			return Mth.clamp(desiredZoom, this.minZoom, this.maxZoom);
		}

		private void prefetchNonBlocking(final TileKey center, final int radius) {
			if (!this.available) {
				return;
			}
			final int zoom = center.zoom();
			final int tilesPerAxis = 1 << zoom;
			final int minX = Math.max(0, center.x() - radius);
			final int maxX = Math.min(tilesPerAxis - 1, center.x() + radius);
			final int minY = Math.max(0, center.y() - radius);
			final int maxY = Math.min(tilesPerAxis - 1, center.y() + radius);
			for (int y = minY; y <= maxY; y++) {
				for (int x = minX; x <= maxX; x++) {
					getTileNonBlocking(new TileKey(zoom, x, y));
				}
			}
		}

		private @Nullable TileMask getTileNonBlocking(final TileKey key) {
			if (!this.available) {
				return null;
			}
			final TileMask cached = this.memoryCache.get(key);
			if (cached != null) {
				return cached;
			}

			this.inFlight.computeIfAbsent(key, tileKey -> CompletableFuture.supplyAsync(() -> {
				final TileMask loaded = loadTile(tileKey);
				if (loaded != null) {
					this.memoryCache.put(tileKey, loaded);
				}
				this.inFlight.remove(tileKey);
				return loaded;
			}, EXECUTOR));
			return null;
		}

		private @Nullable TileMask loadTile(final TileKey key) {
			try {
				byte[] bytes = this.reader.getTileBytes(key.zoom(), key.x(), key.y());
				if (bytes == null || bytes.length == 0) {
					return TileMask.empty();
				}
				bytes = decodeTileBytes(bytes, this.tileCompression);
				return decodeTileMask(bytes, key.zoom(), this.sourceType);
			} catch (final Exception e) {
				Tellus.LOGGER.debug("Failed to decode Overture {} tile {}", this.name, key, e);
				return TileMask.empty();
			}
		}
	}

	private static TileMask decodeTileMask(final byte[] tileBytes, final int zoom, final SourceType sourceType)
			throws IOException {
		final JtsMvt tile = MvtReader.loadMvt(
				new ByteArrayInputStream(tileBytes),
				new org.locationtech.jts.geom.GeometryFactory(),
				TAG_CONVERTER);
		if (tile == null || tile.getLayers().isEmpty()) {
			return TileMask.empty();
		}

		final BufferedImage maskImage = new BufferedImage(TILE_MASK_SIZE, TILE_MASK_SIZE, BufferedImage.TYPE_BYTE_BINARY);
		final Graphics2D graphics = maskImage.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		int acceptedFeatureCount = 0;
		for (final JtsLayer layer : tile.getLayers()) {
			if (layer == null || layer.getGeometries() == null || layer.getGeometries().isEmpty()) {
				continue;
			}
			final String layerName = layer.getName() == null ? "" : layer.getName();
			final int extent = Math.max(1, layer.getExtent());
			final double scale = TILE_MASK_SIZE / (double) extent;

			for (final Geometry geometry : layer.getGeometries()) {
				if (geometry == null || geometry.isEmpty()) {
					continue;
				}
				if (sourceType == SourceType.BUILDINGS) {
					if (!isBuildingFeature(layerName, geometry, geometry.getUserData())) {
						continue;
					}
					acceptedFeatureCount++;
					drawFilledGeometry(graphics, geometry, 0.0D, 0.0D, scale, scale);
				} else {
					if (!isRoadFeature(layerName, geometry, geometry.getUserData())) {
						continue;
					}
					final float stroke = roadStrokeWidth(geometry.getUserData(), zoom);
					acceptedFeatureCount++;
					drawRoadGeometry(graphics, geometry, 0.0D, 0.0D, scale, scale, stroke);
				}
			}
		}
		graphics.dispose();

		if (acceptedFeatureCount <= 0) {
			return TileMask.empty();
		}

		if (!maskHasAnyWhite(maskImage)) {
			drawNormalizedFallback(maskImage, tile, sourceType, zoom);
		}

		if (!maskHasAnyWhite(maskImage)) {
			return TileMask.empty();
		}

		return TileMask.fromImage(maskImage);
	}

	private static boolean isBuildingFeature(final String layerName, final Geometry geometry, final Object userData) {
		if (!(geometry instanceof Polygon || geometry instanceof MultiPolygon || geometry.getDimension() >= 2)) {
			return false;
		}

		final String layer = layerName.toLowerCase(Locale.ROOT);
		if (layer.contains("building")) {
			return true;
		}

		final Map<String, Object> props = properties(userData);
		if (props.isEmpty()) {
			return true;
		}

		final String classText = propertyText(props, "class", "subtype", "type", "kind", "feature_type");
		if (containsAny(classText, "building", "structure", "roof")) {
			return true;
		}

		return props.containsKey("height")
				|| props.containsKey("num_floors")
				|| props.containsKey("roof_material")
				|| props.containsKey("roof_shape");
	}

	private static boolean isRoadFeature(final String layerName, final Geometry geometry, final Object userData) {
		if (geometry.getDimension() < 1) {
			return false;
		}
		if (!(geometry instanceof LineString
				|| geometry instanceof MultiLineString
				|| geometry instanceof Polygon
				|| geometry instanceof MultiPolygon
				|| geometry instanceof GeometryCollection)) {
			return false;
		}

		final String layer = layerName.toLowerCase(Locale.ROOT);
		if (containsAny(layer, "building", "water", "landuse", "boundary", "admin", "place")) {
			return false;
		}

		final Map<String, Object> props = properties(userData);
		final String typeText = propertyText(props, "class", "subclass", "subtype", "type", "kind", "feature_type");

		if (containsAny(typeText, "rail", "subway", "tram", "ferry", "water", "aeroway", "runway", "taxiway")) {
			return false;
		}

		if (containsAny(typeText,
				"road",
				"street",
				"motorway",
				"trunk",
				"primary",
				"secondary",
				"tertiary",
				"residential",
				"service",
				"living_street",
				"unclassified",
				"track",
				"path",
				"footway",
				"cycleway",
				"connector",
				"link",
				"segment")) {
			return true;
		}

		if (containsAny(layer, "road", "transport", "segment", "street", "highway")) {
			return true;
		}

		if (props.isEmpty()) {
			return true;
		}

		return props.containsKey("class")
				|| props.containsKey("subclass")
				|| props.containsKey("subtype")
				|| props.containsKey("kind")
				|| props.containsKey("type")
				|| props.containsKey("feature_type");
	}

	private static float roadStrokeWidth(final Object userData, final int zoom) {
		final Map<String, Object> props = properties(userData);
		final String text = propertyText(props, "class", "subclass", "subtype", "type", "kind", "feature_type");
		final float zoomScale = Mth.clamp(1.0F + ((14 - zoom) * 0.12F), 0.9F, 2.0F);

		if (containsAny(text, "motorway", "trunk", "freeway", "expressway")) {
			return ROAD_STROKE_WIDE * zoomScale;
		}
		if (containsAny(text, "primary", "secondary", "tertiary")) {
			return ROAD_STROKE_MAJOR * zoomScale;
		}
		if (containsAny(text, "residential", "street", "service", "living_street", "unclassified", "connector")) {
			return ROAD_STROKE_MEDIUM * zoomScale;
		}
		return ROAD_STROKE_MINOR * zoomScale;
	}

	private static boolean drawRoadGeometry(
			final Graphics2D graphics,
			final Geometry geometry,
			final double offsetX,
			final double offsetY,
			final double scaleX,
			final double scaleY,
			final float strokeWidth) {
		if (geometry instanceof LineString lineString) {
			final Path2D path = pathForLineString(lineString, offsetX, offsetY, scaleX, scaleY);
			if (path == null) {
				return false;
			}
			final java.awt.Stroke previous = graphics.getStroke();
			graphics.setStroke(new BasicStroke(Math.max(0.8F, strokeWidth), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.draw(path);
			graphics.setStroke(previous);
			return true;
		}
		if (geometry instanceof MultiLineString multiLineString) {
			boolean drawn = false;
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				drawn |= drawRoadGeometry(
						graphics,
						multiLineString.getGeometryN(i),
						offsetX,
						offsetY,
						scaleX,
						scaleY,
						strokeWidth);
			}
			return drawn;
		}
		if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
			return drawFilledGeometry(graphics, geometry, offsetX, offsetY, scaleX, scaleY);
		}
		if (geometry instanceof GeometryCollection collection) {
			boolean drawn = false;
			for (int i = 0; i < collection.getNumGeometries(); i++) {
				drawn |= drawRoadGeometry(
						graphics,
						collection.getGeometryN(i),
						offsetX,
						offsetY,
						scaleX,
						scaleY,
						strokeWidth);
			}
			return drawn;
		}
		return false;
	}

	private static boolean drawFilledGeometry(
			final Graphics2D graphics,
			final Geometry geometry,
			final double offsetX,
			final double offsetY,
			final double scaleX,
			final double scaleY) {
		if (geometry instanceof Polygon polygon) {
			final Path2D path = pathForPolygon(polygon, offsetX, offsetY, scaleX, scaleY);
			if (path == null) {
				return false;
			}
			graphics.fill(path);
			return true;
		}
		if (geometry instanceof MultiPolygon multiPolygon) {
			boolean drawn = false;
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				drawn |= drawFilledGeometry(graphics, multiPolygon.getGeometryN(i), offsetX, offsetY, scaleX, scaleY);
			}
			return drawn;
		}
		if (geometry instanceof GeometryCollection collection) {
			boolean drawn = false;
			for (int i = 0; i < collection.getNumGeometries(); i++) {
				drawn |= drawFilledGeometry(graphics, collection.getGeometryN(i), offsetX, offsetY, scaleX, scaleY);
			}
			return drawn;
		}
		return false;
	}

	private static @Nullable Path2D pathForLineString(
			final LineString lineString,
			final double offsetX,
			final double offsetY,
			final double scaleX,
			final double scaleY) {
		if (lineString.getNumPoints() < 2) {
			return null;
		}

		final Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
		path.moveTo(
				(lineString.getCoordinateN(0).x - offsetX) * scaleX,
				(lineString.getCoordinateN(0).y - offsetY) * scaleY);
		for (int i = 1; i < lineString.getNumPoints(); i++) {
			path.lineTo(
					(lineString.getCoordinateN(i).x - offsetX) * scaleX,
					(lineString.getCoordinateN(i).y - offsetY) * scaleY);
		}
		return path;
	}

	private static @Nullable Path2D pathForPolygon(
			final Polygon polygon,
			final double offsetX,
			final double offsetY,
			final double scaleX,
			final double scaleY) {
		if (polygon.isEmpty() || polygon.getNumPoints() < 3) {
			return null;
		}

		final Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
		appendRing(path, polygon.getExteriorRing().getCoordinates(), offsetX, offsetY, scaleX, scaleY);
		for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
			appendRing(path, polygon.getInteriorRingN(i).getCoordinates(), offsetX, offsetY, scaleX, scaleY);
		}
		return path;
	}

	private static void appendRing(
			final Path2D.Double path,
			final org.locationtech.jts.geom.Coordinate[] coordinates,
			final double offsetX,
			final double offsetY,
			final double scaleX,
			final double scaleY) {
		if (coordinates == null || coordinates.length == 0) {
			return;
		}
		path.moveTo((coordinates[0].x - offsetX) * scaleX, (coordinates[0].y - offsetY) * scaleY);
		for (int i = 1; i < coordinates.length; i++) {
			path.lineTo((coordinates[i].x - offsetX) * scaleX, (coordinates[i].y - offsetY) * scaleY);
		}
		path.closePath();
	}

	private static boolean maskHasAnyWhite(final BufferedImage image) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if ((image.getRGB(x, y) & 0x00FFFFFF) != 0) {
					return true;
				}
			}
		}
		return false;
	}

	private static void drawNormalizedFallback(
			final BufferedImage maskImage,
			final JtsMvt tile,
			final SourceType sourceType,
			final int zoom) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (final JtsLayer layer : tile.getLayers()) {
			if (layer == null || layer.getGeometries() == null || layer.getGeometries().isEmpty()) {
				continue;
			}
			final String layerName = layer.getName() == null ? "" : layer.getName();
			for (final Geometry geometry : layer.getGeometries()) {
				if (geometry == null || geometry.isEmpty()) {
					continue;
				}
				final boolean accepted = sourceType == SourceType.BUILDINGS
						? isBuildingFeature(layerName, geometry, geometry.getUserData())
						: isRoadFeature(layerName, geometry, geometry.getUserData());
				if (!accepted) {
					continue;
				}
				final Envelope env = geometry.getEnvelopeInternal();
				if (env == null || env.isNull()) {
					continue;
				}
				minX = Math.min(minX, env.getMinX());
				minY = Math.min(minY, env.getMinY());
				maxX = Math.max(maxX, env.getMaxX());
				maxY = Math.max(maxY, env.getMaxY());
			}
		}

		if (!Double.isFinite(minX)
				|| !Double.isFinite(minY)
				|| !Double.isFinite(maxX)
				|| !Double.isFinite(maxY)) {
			return;
		}

		final double spanX = Math.max(1.0E-9D, maxX - minX);
		final double spanY = Math.max(1.0E-9D, maxY - minY);
		final double scaleX = (TILE_MASK_SIZE - 1) / spanX;
		final double scaleY = (TILE_MASK_SIZE - 1) / spanY;

		final Graphics2D graphics = maskImage.createGraphics();
		graphics.setBackground(Color.BLACK);
		graphics.clearRect(0, 0, TILE_MASK_SIZE, TILE_MASK_SIZE);
		graphics.setColor(Color.WHITE);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		for (final JtsLayer layer : tile.getLayers()) {
			if (layer == null || layer.getGeometries() == null || layer.getGeometries().isEmpty()) {
				continue;
			}
			final String layerName = layer.getName() == null ? "" : layer.getName();
			for (final Geometry geometry : layer.getGeometries()) {
				if (geometry == null || geometry.isEmpty()) {
					continue;
				}
				if (sourceType == SourceType.BUILDINGS) {
					if (!isBuildingFeature(layerName, geometry, geometry.getUserData())) {
						continue;
					}
					drawFilledGeometry(graphics, geometry, minX, minY, scaleX, scaleY);
				} else {
					if (!isRoadFeature(layerName, geometry, geometry.getUserData())) {
						continue;
					}
					final float stroke = roadStrokeWidth(geometry.getUserData(), zoom);
					drawRoadGeometry(graphics, geometry, minX, minY, scaleX, scaleY, stroke);
				}
			}
		}

		graphics.dispose();
	}

	private static Map<String, Object> properties(final Object userData) {
		if (userData instanceof Map<?, ?> map && !map.isEmpty()) {
			final Map<String, Object> result = new LinkedHashMap<>();
			for (final Map.Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() == null) {
					continue;
				}
				result.put(entry.getKey().toString().toLowerCase(Locale.ROOT), entry.getValue());
			}
			return result;
		}
		return Map.of();
	}

	private static String propertyText(final Map<String, Object> props, final String... keys) {
		if (props.isEmpty()) {
			return "";
		}
		final StringBuilder builder = new StringBuilder();
		for (final String key : keys) {
			final Object value = props.get(key.toLowerCase(Locale.ROOT));
			if (value == null) {
				continue;
			}
			if (!builder.isEmpty()) {
				builder.append(' ');
			}
			builder.append(value.toString().toLowerCase(Locale.ROOT));
		}
		return builder.toString();
	}

	private static boolean containsAny(final String text, final String... needles) {
		if (text == null || text.isEmpty()) {
			return false;
		}
		for (final String needle : needles) {
			if (text.contains(needle)) {
				return true;
			}
		}
		return false;
	}

	private static byte[] decodeTileBytes(final byte[] bytes, final int compression) throws IOException {
		if (compression != 2) {
			return bytes;
		}
		try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes));
				 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			final byte[] buffer = new byte[8192];
			int read;
			while ((read = gzip.read(buffer)) != -1) {
				output.write(buffer, 0, read);
			}
			return output.toByteArray();
		}
	}

	private static String normalizeBaseUrl(final String baseUrl) {
		Objects.requireNonNull(baseUrl, "baseUrl");
		if (baseUrl.endsWith("/")) {
			return baseUrl;
		}
		return baseUrl + "/";
	}

	private static int intProperty(final String key, final int defaultValue) {
		final String value = System.getProperty(key);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		try {
			return Math.max(1, Integer.parseInt(value.trim()));
		} catch (final NumberFormatException ignored) {
			return defaultValue;
		}
	}

	private record TileKey(int zoom, int x, int y) {
	}

	private record TilePixelCoord(TileKey key, double fracX, double fracY) {
		private static TilePixelCoord fromLatLon(final double latitude, final double longitude, final int zoom) {
			final int n = 1 << zoom;
			final double clampedLat = Math.max(-MAX_WEB_MERCATOR_LAT, Math.min(MAX_WEB_MERCATOR_LAT, latitude));
			final double wrappedLon = wrapLongitude(longitude);

			final double x = (wrappedLon + 180.0D) / 360.0D * n;
			final double latRad = Math.toRadians(clampedLat);
			final double y = (1.0D - (Math.log(Math.tan(latRad) + (1.0D / Math.cos(latRad))) / Math.PI)) * 0.5D * n;

			final int tileX = Mth.clamp((int) Math.floor(x), 0, n - 1);
			final int tileY = Mth.clamp((int) Math.floor(y), 0, n - 1);
			final double fracX = clampUnit(x - tileX);
			final double fracY = clampUnit(y - tileY);
			return new TilePixelCoord(new TileKey(zoom, tileX, tileY), fracX, fracY);
		}
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

	private static double clampUnit(final double value) {
		if (value <= 0.0D) {
			return 0.0D;
		}
		if (value >= 1.0D) {
			return Math.nextDown(1.0D);
		}
		return value;
	}

	private static final class TileMask {
		private static final TileMask EMPTY = new TileMask(0, 0, new byte[0], true);

		private final int width;
		private final int height;
		private final byte[] mask;
		private final boolean empty;

		private TileMask(final int width, final int height, final byte[] mask, final boolean empty) {
			this.width = width;
			this.height = height;
			this.mask = mask;
			this.empty = empty;
		}

		private static TileMask empty() {
			return EMPTY;
		}

		private static TileMask fromImage(final BufferedImage image) {
			final int width = image.getWidth();
			final int height = image.getHeight();
			final byte[] bytes = new byte[width * height];
			for (int y = 0; y < height; y++) {
				final int row = y * width;
				for (int x = 0; x < width; x++) {
					bytes[row + x] = (byte) ((image.getRGB(x, y) & 0x00FFFFFF) == 0 ? 0 : 1);
				}
			}
			return new TileMask(width, height, bytes, false);
		}

		private boolean sample(final double fracX, final double fracY) {
			if (this.empty || this.mask.length == 0 || this.width <= 0 || this.height <= 0) {
				return false;
			}
			final int x = Mth.clamp((int) Math.floor(fracX * this.width), 0, this.width - 1);
			final int y = Mth.clamp((int) Math.floor(fracY * this.height), 0, this.height - 1);
			final int index = y * this.width + x;
			if (index < 0 || index >= this.mask.length) {
				return false;
			}
			return this.mask[index] != 0;
		}
	}
}
