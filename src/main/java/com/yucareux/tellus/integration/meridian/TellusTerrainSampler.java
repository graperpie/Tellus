package com.yucareux.tellus.integration.meridian;

import com.leclowndu93150.meridian.api.terrain.ColumnSample;
import com.leclowndu93150.meridian.api.terrain.ColumnSampleBuffer;
import com.leclowndu93150.meridian.api.terrain.SamplerTables;
import com.leclowndu93150.meridian.api.terrain.TerrainGrid;
import com.leclowndu93150.meridian.api.terrain.TerrainSampler;
import com.yucareux.tellus.Tellus;
import com.yucareux.tellus.integration.meridian.TellusLodSurfaceClassifier.SurfaceSample;
import com.yucareux.tellus.integration.meridian.TellusLodSurfaceClassifier.TreePalette;
import com.yucareux.tellus.legacy.backend.GeoChunk;
import com.yucareux.tellus.legacy.backend.GeoView;
import com.yucareux.tellus.legacy.backend.earth.EarthAttachments;
import com.yucareux.tellus.legacy.backend.earth.EarthLayers;
import com.yucareux.tellus.legacy.backend.earth.EarthTiles;
import com.yucareux.tellus.legacy.backend.earth.cover.LegacyCover;
import com.yucareux.tellus.legacy.backend.loader.ConcurrencyLimiter;
import com.yucareux.tellus.legacy.backend.projection.Projection;
import com.yucareux.tellus.legacy.backend.projection.cylindrical.Equirectangular;
import com.yucareux.tellus.legacy.backend.raster.RasterShape;
import com.yucareux.tellus.legacy.backend.raster.ShortRaster;
import com.yucareux.tellus.legacy.backend.raster.EnumRaster;
import com.yucareux.tellus.legacy.backend.tile.GuavaTileCache;
import com.yucareux.tellus.world.data.osm.OsmBuildingFeature;
import com.yucareux.tellus.world.data.osm.OsmQueryMode;
import com.yucareux.tellus.world.data.osm.RoadFeature;
import com.yucareux.tellus.world.data.satellite.SatelliteTileSampler;
import com.yucareux.tellus.world.data.snow.SnowLineGrid;
import com.yucareux.tellus.world.data.satellite.TreeCanopyDetector;
import com.yucareux.tellus.worldgen.EarthCoordinateShift;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import com.yucareux.tellus.worldgen.EarthProjection;
import com.yucareux.tellus.worldgen.TellusWorldgenSources;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.HolderGetter;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Solves Meridian LOD columns from real world elevation and satellite imagery.
 *
 * <p>Meridian asks for a square grid of columns at a spacing of {@code 1 << level} blocks and turns
 * the result into a mesh itself, so all this has to do is fill in a {@link ColumnSample} per grid
 * point. That maps closely onto the Distant Horizons path in {@code LegacyLodGeneratorV2}, and the
 * data sources and classification thresholds are carried over from it.
 *
 * <p>Resolution is dropped when the data is fetched rather than afterwards. The elevation and land
 * cover pyramids pick a level from the requested output shape, the branded DEM picks a tile zoom
 * from the metres a column covers, and imagery zoom comes from the LOD level, so a distant tile
 * downloads a handful of coarse tiles instead of thousands of fine ones.
 *
 * <p>Roads and buildings are rasterized separately from vector OSM/Overture data ({@link
 * #computeOverlay}) rather than sampled like imagery, since a footprint is small relative to a
 * coarse LOD tile - there's no useful "coarser fetch" to ask for the way there is for satellite
 * data. Buildings pick a {@link MeridianBuildingBlueprints} archetype and vary height across their
 * footprint (gable ridge, tower setback); a bridge road renders as a raised deck instead of
 * vanishing into whatever it crosses. Both are still a single point per column, same as everything
 * else {@code ColumnSample} represents - there's no room for real walls or roof geometry.
 */
public final class TellusTerrainSampler implements TerrainSampler {
	public static final String NAME = "tellus";

	private static final int TREE_CENTER_SALT = 0x4F3A2C17;
	private static final int BRIDGE_CLEARANCE_BLOCKS_PER_LEVEL = 6;
	private static final int FETCH_ATTEMPTS = 3;
	private static final long[] FETCH_RETRY_DELAY_MS = {0L, 150L, 400L};

	private final EarthGeneratorSettings settings;
	private final Projection projection;
	private final EarthLayers earthLayers;
	private final SatelliteTileSampler satelliteSampler;
	private final SnowLineGrid snowLineGrid;
	private final MeridianSamplerTables tables;

	private final int spawnOffsetX;
	private final int spawnOffsetZ;
	private final int seaLevel;
	private final int minY;
	private final int maxY;
	private final float heightScale;

	private final int stoneIndex;
	private final int deepIndex;
	private final int fallbackFloorIndex;
	private final int fallbackBiomeIndex;
	private final int roadIndex;

	// -------------------------------------------------------------- diagnostics
	//
	// Temporary instrumentation for the "generation is far slower than the Distant Horizons path"
	// report. Rather than guess a third time at what is slow, this logs where every tile's time
	// actually goes and why a tile falls back to a flat plate, aggregated every few seconds so the
	// log stays readable. Remove once the bottleneck is confirmed and fixed.
	private final java.util.concurrent.atomic.AtomicLong statTiles = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicLong statFallbackChunkEmpty = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicLong statFallbackEarthEmpty = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicLong statFallbackException = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicLong statOpenTileNanos = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicLong statPrefetchNanos = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicLong statColumnsNanos = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicLong statLastLogNanos =
			new java.util.concurrent.atomic.AtomicLong(System.nanoTime());

	public TellusTerrainSampler(final EarthGeneratorSettings settings, final HolderGetter<Biome> biomes) {
		this.settings = settings;
		this.projection = new Equirectangular(settings.worldScale());
		this.tables = MeridianSamplerTables.build(biomes);

		this.spawnOffsetX = EarthCoordinateShift.spawnOffsetX(settings);
		this.spawnOffsetZ = EarthCoordinateShift.spawnOffsetZ(settings);
		this.seaLevel = settings.resolveSeaLevel();

		final EarthGeneratorSettings.HeightLimits limits = EarthGeneratorSettings.resolveHeightLimits(settings);
		this.minY = limits.minY();
		this.maxY = limits.minY() + limits.height();
		this.heightScale = (float) (settings.terrestrialHeightScale() / this.projection.idealMetersPerBlock());

		final EarthTiles.Config config = new EarthTiles.Config(
				HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build(),
				new ConcurrencyLimiter(16),
				Paths.get("tellus_cache", "legacy"),
				ForkJoinPool.commonPool(),
				ForkJoinPool.commonPool());
		final EarthTiles tiles = config.create(new GuavaTileCache(Duration.ofMinutes(5), 1000));
		this.earthLayers = EarthLayers.create(tiles, this.projection, ForkJoinPool.commonPool());

		final HttpClient satelliteHttp = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		this.satelliteSampler = new SatelliteTileSampler(satelliteHttp, Paths.get("tellus_cache", "satellite"));
		this.snowLineGrid = new SnowLineGrid();

		this.stoneIndex = this.tables.blockIndex(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
		this.deepIndex = this.tables.blockIndex(net.minecraft.world.level.block.Blocks.DEEPSLATE.defaultBlockState());
		this.fallbackFloorIndex = this.tables.blockIndex(
				TellusLodSurfaceClassifier.underwaterMaterial(LegacyCover.WATER));
		this.fallbackBiomeIndex = this.tables.biomeIndex("minecraft:ocean");
		this.roadIndex = this.tables.blockIndex(net.minecraft.world.level.block.Blocks.GRAY_CONCRETE.defaultBlockState());
	}

	@Override
	public String backendName() {
		return NAME;
	}

	@Override
	public SamplerTables tables() {
		return this.tables.tables();
	}

	@Override
	public void close() {
	}

	@Override
	public int surfaceY(final int blockX, final int blockZ) {
		return sample(blockX, blockZ).surfaceY();
	}

	@Override
	public ColumnSample sample(final int blockX, final int blockZ) {
		final TerrainGrid grid = new TerrainGrid(blockX, blockZ, 1, 1);
		final ColumnSampleBuffer buffer = ColumnSampleBuffer.allocate(1);
		sampleGrid(grid, buffer);
		return buffer.get(0);
	}

	@Override
	public void sampleGrid(final TerrainGrid grid, final ColumnSampleBuffer out) {
		final int count = grid.sampleCount();
		out.ensureCapacity(count);

		final long tStart = System.nanoTime();
		final Tile tile = openTile(grid);
		final long tAfterOpen = System.nanoTime();
		if (tile == null) {
			fillFallback(grid, out);
			recordTiming(tAfterOpen - tStart, 0L, 0L);
			return;
		}

		final int n = grid.samplesPerAxis();
		final int[] surfaceYs = new int[count];
		final int[] elevationValues = new int[count];

		for (int z = 0; z < n; z++) {
			for (int x = 0; x < n; x++) {
				final int index = z * n + x;

				final double elevation = sampleElevationBicubic(tile.elevation, x + 0.5D, z + 0.5D);
				final int surfaceY = surfaceFromElevation(elevation);

				elevationValues[index] = Mth.floor(elevation);
				surfaceYs[index] = surfaceY;
			}
		}

		prefetch(tile);
		final long tAfterPrefetch = System.nanoTime();

		final int[] satelliteRgb = new int[count];
		computeSatelliteRgbGrid(tile, n, satelliteRgb);

		final String[] biomeIds = new String[count];
		computeBiomeIdGrid(tile, n, surfaceYs, elevationValues, biomeIds);

		final Overlay overlay = computeOverlay(tile);

		for (int index = 0; index < count; index++) {
			if (out.prefilled(index)) {
				out.clearPrefilled(index);
				continue;
			}

			final int z = index / n;
			final int x = index % n;
			final LegacyCover columnCover = tile.landCover.get(x, z);
			final double columnLat = latitudeAt(tile.worldX(x), tile.worldZ(z));
			final double columnLon = longitudeAt(tile.worldX(x), tile.worldZ(z));
			final boolean aboveSnowLine = elevationValues[index] >= this.snowLineGrid.getSnowLineElevation(columnLat, columnLon);
			out.put(index, buildColumnBatched(
					tile, x, z, index,
					surfaceYs[index], elevationValues[index],
					columnCover,
					satelliteRgb[index],
					biomeIds[index],
					overlay,
					aboveSnowLine));
		}

		out.setCount(count);
		final long tEnd = System.nanoTime();
		recordTiming(tAfterOpen - tStart, tAfterPrefetch - tAfterOpen, tEnd - tAfterPrefetch);
	}

	private void computeSatelliteRgbGrid(final Tile tile, final int n, final int[] outRgb) {
		final int stride = tile.satelliteStride;
		final int maxCell = Math.max(0, (n - 1) / stride);
		final int[][] cellRgb = new int[maxCell + 1][maxCell + 1];

		final List<CompletableFuture<Void>> futures = new ArrayList<>();
		for (int cellZ = 0; cellZ <= maxCell; cellZ++) {
			for (int cellX = 0; cellX <= maxCell; cellX++) {
				final int fx = cellX;
				final int fz = cellZ;
				futures.add(CompletableFuture.runAsync(() -> {
					final int columnX = Math.min(n - 1, fx * stride + (stride >> 1));
					final int columnZ = Math.min(n - 1, fz * stride + (stride >> 1));
					final double lat = latitudeAt(tile.worldX(columnX), tile.worldZ(columnZ));
					final double lon = longitudeAt(tile.worldX(columnX), tile.worldZ(columnZ));
					final int zoom = MeridianLodPolicy.satelliteZoom(tile.level, lat, this.projection.idealMetersPerBlock());
					cellRgb[fz][fx] = this.satelliteSampler.sampleRgb(lat, lon, zoom);
				}, ForkJoinPool.commonPool()));
			}
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		final int[][] denoisedCellRgb = medianFilterRgbGrid(cellRgb, maxCell + 1, maxCell + 1);

		for (int z = 0; z < n; z++) {
			final double cellZf = z / (double) stride;
			final int cellZ0 = Mth.floor(cellZf);
			final double fracZ = cellZf - cellZ0;

			for (int x = 0; x < n; x++) {
				final double cellXf = x / (double) stride;
				final int cellX0 = Mth.floor(cellXf);
				final double fracX = cellXf - cellX0;

				final int c00 = denoisedCellRgb[Mth.clamp(cellZ0, 0, maxCell)][Mth.clamp(cellX0, 0, maxCell)];
				final int c10 = denoisedCellRgb[Mth.clamp(cellZ0, 0, maxCell)][Mth.clamp(cellX0 + 1, 0, maxCell)];
				final int c01 = denoisedCellRgb[Mth.clamp(cellZ0 + 1, 0, maxCell)][Mth.clamp(cellX0, 0, maxCell)];
				final int c11 = denoisedCellRgb[Mth.clamp(cellZ0 + 1, 0, maxCell)][Mth.clamp(cellX0 + 1, 0, maxCell)];

				if (c00 < 0 || c10 < 0 || c01 < 0 || c11 < 0) {
					outRgb[z * n + x] = c00 >= 0 ? c00 : (c10 >= 0 ? c10 : (c01 >= 0 ? c01 : c11));
					continue;
				}

				final double r00 = (c00 >> 16) & 0xFF;
				final double g00 = (c00 >> 8) & 0xFF;
				final double b00 = c00 & 0xFF;
				final double r10 = (c10 >> 16) & 0xFF;
				final double g10 = (c10 >> 8) & 0xFF;
				final double b10 = c10 & 0xFF;
				final double r01 = (c01 >> 16) & 0xFF;
				final double g01 = (c01 >> 8) & 0xFF;
				final double b01 = c01 & 0xFF;
				final double r11 = (c11 >> 16) & 0xFF;
				final double g11 = (c11 >> 8) & 0xFF;
				final double b11 = c11 & 0xFF;

				final double rTop = r00 + (r10 - r00) * fracX;
				final double gTop = g00 + (g10 - g00) * fracX;
				final double bTop = b00 + (b10 - b00) * fracX;
				final double rBot = r01 + (r11 - r01) * fracX;
				final double gBot = g01 + (g11 - g01) * fracX;
				final double bBot = b01 + (b11 - b01) * fracX;

				final double r = rTop + (rBot - rTop) * fracZ;
				final double g = gTop + (gBot - gTop) * fracZ;
				final double b = bTop + (bBot - bTop) * fracZ;

				outRgb[z * n + x] = (Mth.clamp(Mth.floor(r + 0.5D), 0, 255) << 16)
						| (Mth.clamp(Mth.floor(g + 0.5D), 0, 255) << 8)
						| Mth.clamp(Mth.floor(b + 0.5D), 0, 255);
			}
		}
	}

	/**
	 * Removes single-cell outliers (cloud cover, sun glint, JPEG blocking in the source imagery) from
	 * the sparse satellite sample grid before it gets bilinearly interpolated across the full column
	 * grid. At coarse LOD levels a single bad sample can dominate a wide stride's worth of columns
	 * once smeared by interpolation, which is what the blotchy, salt-and-pepper look on distant
	 * terrain comes from. A 3x3 per-channel median is the standard fix for exactly this kind of
	 * isolated-outlier noise, and unlike a blur it doesn't wash out real coastlines or biome edges.
	 *
	 * <p>Runs entirely on samples already fetched - no extra network round trips, so it doesn't touch
	 * generation speed.
	 */
	private static int[][] medianFilterRgbGrid(final int[][] cellRgb, final int width, final int height) {
		final int[][] filtered = new int[height][width];
		final int[] rSamples = new int[9];
		final int[] gSamples = new int[9];
		final int[] bSamples = new int[9];

		for (int z = 0; z < height; z++) {
			for (int x = 0; x < width; x++) {
				int count = 0;
				for (int dz = -1; dz <= 1; dz++) {
					final int nz = Mth.clamp(z + dz, 0, height - 1);
					for (int dx = -1; dx <= 1; dx++) {
						final int nx = Mth.clamp(x + dx, 0, width - 1);
						final int rgb = cellRgb[nz][nx];
						if (rgb < 0) {
							continue;
						}
						rSamples[count] = (rgb >> 16) & 0xFF;
						gSamples[count] = (rgb >> 8) & 0xFF;
						bSamples[count] = rgb & 0xFF;
						count++;
					}
				}

				filtered[z][x] = count == 0
						? cellRgb[z][x]
						: (medianOf(rSamples, count) << 16) | (medianOf(gSamples, count) << 8) | medianOf(bSamples, count);
			}
		}

		return filtered;
	}

	private static int medianOf(final int[] samples, final int count) {
		final int[] sorted = Arrays.copyOf(samples, count);
		Arrays.sort(sorted);
		return sorted[count / 2];
	}

	private void computeBiomeIdGrid(final Tile tile, final int n, final int[] surfaceYs, final int[] elevationValues, final String[] outBiomeIds) {
		final int stride = tile.biomeStride;
		final int maxCell = Math.max(0, (n - 1) / stride);
		final String[][] cellBiome = new String[maxCell + 1][maxCell + 1];

		for (int cellZ = 0; cellZ <= maxCell; cellZ++) {
			for (int cellX = 0; cellX <= maxCell; cellX++) {
				final int sampleX = Math.min(n - 1, cellX * stride + (stride >> 1));
				final int sampleZ = Math.min(n - 1, cellZ * stride + (stride >> 1));
				final int index = sampleZ * n + sampleX;
				final double latitude = latitudeAt(tile.worldX(sampleX), tile.worldZ(sampleZ));
				final LegacyCover cellCover = tile.landCover.get(sampleX, sampleZ);
				cellBiome[cellZ][cellX] = TellusLodSurfaceClassifier.classifyBiomeId(
						cellCover, surfaceYs[index], this.seaLevel, latitude, elevationValues[index]);
			}
		}

		for (int z = 0; z < n; z++) {
			final int cellZ = z / stride;
			for (int x = 0; x < n; x++) {
				final int cellX = x / stride;
				outBiomeIds[z * n + x] = cellBiome[Mth.clamp(cellZ, 0, maxCell)][Mth.clamp(cellX, 0, maxCell)];
			}
		}
	}

	// ------------------------------------------------------------------ roads & buildings

	/**
	 * Per-column road/building coverage for one grid, rasterized once from vector OSM/Overture
	 * features rather than sampled per cell like imagery. A road or building footprint is small
	 * relative to a coarse LOD tile, so there is no useful "coarser fetch" to ask for the way there is
	 * for satellite imagery - past {@link MeridianLodPolicy#overtureActive} the query is just skipped.
	 */
	private record Overlay(
			boolean[] road,
			int[] roadBridgeLevel,
			boolean[] building,
			int[] buildingHeight,
			int[] buildingWallIndex,
			int[] buildingRoofIndex) {
		private static Overlay empty(final int count) {
			return new Overlay(
					new boolean[count], new int[count], new boolean[count], new int[count], new int[count], new int[count]);
		}
	}

	private Overlay computeOverlay(final Tile tile) {
		final int n = tile.n;
		final int count = n * n;
		final boolean roadsEnabled = this.settings.enableRoads();
		final boolean buildingsEnabled = this.settings.enableBuildings();
		if (!MeridianLodPolicy.overtureActive(tile.level) || (!roadsEnabled && !buildingsEnabled)) {
			return Overlay.empty(count);
		}

		final double worldScale = this.settings.worldScale();
		if (!(worldScale > 0.0D)) {
			return Overlay.empty(count);
		}

		final int[] worldXs = new int[n];
		final int[] worldZs = new int[n];
		for (int i = 0; i < n; i++) {
			worldXs[i] = tile.worldX(i);
			worldZs[i] = tile.worldZ(i);
		}
		final int minWorldX = Math.min(worldXs[0], worldXs[n - 1]);
		final int maxWorldX = Math.max(worldXs[0], worldXs[n - 1]);
		final int minWorldZ = Math.min(worldZs[0], worldZs[n - 1]);
		final int maxWorldZ = Math.max(worldZs[0], worldZs[n - 1]);

		final boolean[] roadMask = new boolean[count];
		final int[] roadBridgeLevelMask = new int[count];
		final boolean[] buildingMask = new boolean[count];
		final int[] buildingHeightMask = new int[count];
		final int[] buildingWallIndexMask = new int[count];
		final int[] buildingRoofIndexMask = new int[count];

		if (roadsEnabled) {
			final List<RoadFeature> roads = TellusWorldgenSources.osmRoads()
					.roadsForAreaWithStatus(minWorldX, minWorldZ, maxWorldX, maxWorldZ, worldScale, 64, OsmQueryMode.BLOCKING)
					.features();
			if (!roads.isEmpty()) {
				rasterizeRoadCoverage(roads, worldXs, worldZs, n, n, tile.spacing, worldScale, roadMask, roadBridgeLevelMask);
			}
		}

		if (buildingsEnabled) {
			final int marginBlocks = Math.max(8, tile.spacing);
			final List<OsmBuildingFeature> buildings = TellusWorldgenSources.osmBuildings()
					.buildingsForAreaWithStatus(minWorldX, minWorldZ, maxWorldX, maxWorldZ, worldScale, marginBlocks, OsmQueryMode.BLOCKING)
					.features();
			if (!buildings.isEmpty()) {
				rasterizeBuildingCoverage(
						buildings, worldXs, worldZs, n, n, tile.spacing, worldScale,
						buildingMask, buildingHeightMask, buildingWallIndexMask, buildingRoofIndexMask);
			}
		}

		return new Overlay(roadMask, roadBridgeLevelMask, buildingMask, buildingHeightMask, buildingWallIndexMask, buildingRoofIndexMask);
	}

	/**
	 * Ported from {@code LegacyLodGeneratorV2.rasterizeRoadCoverage}; stamps a road's width along each
	 * segment, plus (new) the road's bridge level so a crossing can be rendered as a raised deck
	 * instead of vanishing into whatever it crosses.
	 */
	private static void rasterizeRoadCoverage(
			final List<RoadFeature> roads,
			final int[] worldXs,
			final int[] worldZs,
			final int width,
			final int height,
			final int cellSize,
			final double worldScale,
			final boolean[] roadMask,
			final int[] roadBridgeLevelMask) {
		if (roads.isEmpty() || width <= 0 || height <= 0 || cellSize <= 0) {
			return;
		}

		final double minWorldX = Math.min(worldXs[0], worldXs[width - 1]);
		final double maxWorldX = Math.max(worldXs[0], worldXs[width - 1]);
		final double minWorldZ = Math.min(worldZs[0], worldZs[height - 1]);
		final double maxWorldZ = Math.max(worldZs[0], worldZs[height - 1]);

		for (final RoadFeature road : roads) {
			final int points = road.pointCount();
			if (points < 2) {
				continue;
			}

			final int bridgeLevel = road.bridgeLevel();
			final int widthBlocks = roadWidthForScale(road.roadClass().baseWidth(), worldScale);
			final double halfWidth = Math.max(0.5, (widthBlocks - 1) * 0.5) + cellSize * 0.5;
			final double radiusSq = halfWidth * halfWidth + 1.0E-6;
			final double roadMinX = EarthProjection.lonToBlockX(road.minLon(), worldScale);
			final double roadMaxX = EarthProjection.lonToBlockX(road.maxLon(), worldScale);
			final double roadMinZ = EarthProjection.latToBlockZ(road.maxLat(), worldScale);
			final double roadMaxZ = EarthProjection.latToBlockZ(road.minLat(), worldScale);
			if (roadMaxX < minWorldX - halfWidth
					|| roadMinX > maxWorldX + halfWidth
					|| roadMaxZ < minWorldZ - halfWidth
					|| roadMinZ > maxWorldZ + halfWidth) {
				continue;
			}

			double x1 = EarthProjection.lonToBlockX(road.lonAt(0), worldScale);
			double z1 = EarthProjection.latToBlockZ(road.latAt(0), worldScale);

			for (int i = 1; i < points; i++) {
				final double x2 = EarthProjection.lonToBlockX(road.lonAt(i), worldScale);
				final double z2 = EarthProjection.latToBlockZ(road.latAt(i), worldScale);
				final double dx = x2 - x1;
				final double dz = z2 - z1;
				final double lenSq = dx * dx + dz * dz;
				if (lenSq <= 1.0E-6) {
					x1 = x2;
					z1 = z2;
					continue;
				}

				final int minGridX = Mth.clamp((int) Math.floor((Math.min(x1, x2) - halfWidth - minWorldX) / cellSize), 0, width - 1);
				final int maxGridX = Mth.clamp((int) Math.floor((Math.max(x1, x2) + halfWidth - minWorldX) / cellSize), 0, width - 1);
				final int minGridZ = Mth.clamp((int) Math.floor((Math.min(z1, z2) - halfWidth - minWorldZ) / cellSize), 0, height - 1);
				final int maxGridZ = Mth.clamp((int) Math.floor((Math.max(z1, z2) + halfWidth - minWorldZ) / cellSize), 0, height - 1);

				for (int gz = minGridZ; gz <= maxGridZ; gz++) {
					final double sampleZ = worldZs[gz];
					final int row = gz * width;
					for (int gx = minGridX; gx <= maxGridX; gx++) {
						final int index = row + gx;
						if (roadMask[index]) {
							continue;
						}

						final double sampleX = worldXs[gx];
						double t = ((sampleX - x1) * dx + (sampleZ - z1) * dz) / lenSq;
						t = Mth.clamp(t, 0.0D, 1.0D);
						final double px = x1 + t * dx;
						final double pz = z1 + t * dz;
						final double ddx = sampleX - px;
						final double ddz = sampleZ - pz;
						if (ddx * ddx + ddz * ddz <= radiusSq) {
							roadMask[index] = true;
							roadBridgeLevelMask[index] = bridgeLevel;
						}
					}
				}

				x1 = x2;
				z1 = z2;
			}
		}
	}

	/**
	 * Ported from {@code LegacyLodGeneratorV2.rasterizeBuildingCoverage}, extended to pick a
	 * {@link MeridianBuildingBlueprints} archetype per building and let a column's height vary across
	 * the footprint (gable ridge, tower setback) instead of every building being the same flat box.
	 */
	private static final int BUILDING_BLUEPRINT_SALT = 0x1B57A1E5;

	private void rasterizeBuildingCoverage(
			final List<OsmBuildingFeature> buildings,
			final int[] worldXs,
			final int[] worldZs,
			final int width,
			final int height,
			final int cellSize,
			final double worldScale,
			final boolean[] buildingMask,
			final int[] buildingHeightMask,
			final int[] buildingWallIndexMask,
			final int[] buildingRoofIndexMask) {
		if (buildings.isEmpty() || width <= 0 || height <= 0 || cellSize <= 0) {
			return;
		}

		final double minWorldX = Math.min(worldXs[0], worldXs[width - 1]);
		final double maxWorldX = Math.max(worldXs[0], worldXs[width - 1]);
		final double minWorldZ = Math.min(worldZs[0], worldZs[height - 1]);
		final double maxWorldZ = Math.max(worldZs[0], worldZs[height - 1]);

		for (final OsmBuildingFeature building : buildings) {
			final int heightBlocks = buildingHeightBlocks(building.heightMeters(), worldScale);
			final double featureMinX = building.minBlockXForScale(worldScale);
			final double featureMaxX = building.maxBlockXForScale(worldScale);
			final double featureMinZ = building.minBlockZ(worldScale);
			final double featureMaxZ = building.maxBlockZ(worldScale);
			if (featureMaxX < minWorldX || featureMinX > maxWorldX || featureMaxZ < minWorldZ || featureMinZ > maxWorldZ) {
				continue;
			}

			final double footprintWidthX = Math.max(1.0D, featureMaxX - featureMinX);
			final double footprintWidthZ = Math.max(1.0D, featureMaxZ - featureMinZ);
			final double footprintAreaSquareMeters = footprintWidthX * footprintWidthZ * worldScale * worldScale;
			final int hash = mixHash(
					(int) Math.round(building.minLon() * 1.0E6D),
					(int) Math.round(building.minLat() * 1.0E6D) ^ (int) Math.round(building.heightMeters() * 100.0D),
					BUILDING_BLUEPRINT_SALT);
			final MeridianBuildingBlueprints.Blueprint blueprint =
					MeridianBuildingBlueprints.select(heightBlocks, footprintAreaSquareMeters, hash);
			final int wallIndex = this.tables.blockIndex(blueprint.wall());
			final int roofIndex = this.tables.blockIndex(blueprint.roof());

			// Ridge runs along the footprint's longer axis; height falls off toward the eaves on the
			// shorter (perpendicular) axis, approximated from the bounding box since a real polygon's
			// principal axis isn't worth computing at LOD distance.
			final boolean ridgeAlongX = footprintWidthX >= footprintWidthZ;
			final double centerX = (featureMinX + featureMaxX) * 0.5D;
			final double centerZ = (featureMinZ + featureMaxZ) * 0.5D;
			final double halfSpanPerp = (ridgeAlongX ? footprintWidthZ : footprintWidthX) * 0.5D;
			final int gableMaxRise = Mth.clamp((int) Math.round(halfSpanPerp * 0.6D), 2, 8);

			// A tower core needs real margin on both axes to read as a setback rather than noise.
			final double setbackMargin = Mth.clamp(Math.min(footprintWidthX, footprintWidthZ) * 0.2D, 2.0D, 6.0D);
			final boolean setbackEligible =
					footprintWidthX > setbackMargin * 2.5D && footprintWidthZ > setbackMargin * 2.5D;
			final int setbackBaseDrop = (int) Math.round(heightBlocks * 0.3D);

			final int minGridX = Mth.clamp((int) Math.floor((featureMinX - minWorldX) / cellSize), 0, width - 1);
			final int maxGridX = Mth.clamp((int) Math.floor((featureMaxX - minWorldX) / cellSize), 0, width - 1);
			final int minGridZ = Mth.clamp((int) Math.floor((featureMinZ - minWorldZ) / cellSize), 0, height - 1);
			final int maxGridZ = Mth.clamp((int) Math.floor((featureMaxZ - minWorldZ) / cellSize), 0, height - 1);

			for (int gz = minGridZ; gz <= maxGridZ; gz++) {
				final double sampleZ = worldZs[gz];
				final int row = gz * width;
				for (int gx = minGridX; gx <= maxGridX; gx++) {
					final double sampleX = worldXs[gx];
					if (!building.containsWorld(sampleX, sampleZ, worldScale)) {
						continue;
					}

					int extraRise = 0;
					switch (blueprint.roofShape()) {
						case GABLE -> {
							final double perp = ridgeAlongX ? Math.abs(sampleZ - centerZ) : Math.abs(sampleX - centerX);
							final double frac = halfSpanPerp > 1.0E-6D ? Mth.clamp(perp / halfSpanPerp, 0.0D, 1.0D) : 0.0D;
							extraRise = (int) Math.round(gableMaxRise * (1.0D - frac));
						}
						case SETBACK -> {
							if (setbackEligible) {
								final boolean insideCore = sampleX >= featureMinX + setbackMargin
										&& sampleX <= featureMaxX - setbackMargin
										&& sampleZ >= featureMinZ + setbackMargin
										&& sampleZ <= featureMaxZ - setbackMargin;
								extraRise = insideCore ? 0 : -setbackBaseDrop;
							}
						}
						default -> {
						}
					}

					final int index = row + gx;
					final int columnHeight = Math.max(3, heightBlocks + extraRise);
					if (columnHeight >= buildingHeightMask[index]) {
						buildingHeightMask[index] = columnHeight;
						buildingWallIndexMask[index] = wallIndex;
						buildingRoofIndexMask[index] = roofIndex;
					}
					buildingMask[index] = true;
				}
			}
		}
	}

	private static int roadWidthForScale(final int baseWidth, final double worldScale) {
		return Math.max(1, (int) Math.round(baseWidth * roadWidthFactorForScale(worldScale)));
	}

	private static double roadWidthFactorForScale(final double worldScale) {
		if (!(worldScale > 0.0)) {
			return 0.25;
		}
		if (worldScale <= 1.0) {
			return 1.8;
		}
		if (worldScale <= 5.0) {
			final double t = (worldScale - 1.0) / 4.0;
			return Mth.lerp(Mth.clamp(t, 0.0D, 1.0D), 1.8D, 1.0D);
		}
		if (worldScale <= 10.0) {
			final double t = (worldScale - 5.0) / 5.0;
			return Mth.lerp(Mth.clamp(t, 0.0D, 1.0D), 1.0D, 0.5D);
		}
		return 0.25;
	}

	private static int buildingHeightBlocks(final double meters, final double worldScale) {
		if (!(worldScale > 0.0D)) {
			return 3;
		}
		return Math.max(3, (int) Math.round(meters / worldScale));
	}

	private ColumnSample buildColumnBatched(
			final Tile tile, final int x, final int z, final int index,
			final int surfaceY, final int elevationValue,
			final LegacyCover cover,
			final int satelliteRgb,
			final String biomeId,
			final Overlay overlay,
			final boolean aboveSnowLine) {
		final int worldX = tile.worldX(x);
		final int worldZ = tile.worldZ(z);

		final boolean isBuilding = overlay.building[index];
		final boolean isRoad = !isBuilding && overlay.road[index];
		final int bridgeLevel = isRoad ? overlay.roadBridgeLevel[index] : 0;

		if (bridgeLevel > 0) {
			// A deck raised clear of whatever it crosses - water, a valley, another road - rather than
			// classifying the column underneath at all. Meridian's ColumnSample is one point per
			// column, so there's no representing "water below, deck above" in the same sample; the
			// deck is what's visible from LOD distance anyway.
			final int clearance = BRIDGE_CLEARANCE_BLOCKS_PER_LEVEL * bridgeLevel;
			final int localBase = Math.max(surfaceY, this.seaLevel);
			final int deckY = Mth.clamp(localBase + clearance, this.minY, this.maxY - 1);
			return new ColumnSample(
					deckY, deckY, biomeIndex(biomeId), this.roadIndex, 0, 0, 0, 0,
					ColumnSample.FLUID_NONE, 0, 0, this.roadIndex, this.deepIndex,
					ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE,
					ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE);
		}

		final boolean isOcean = surfaceY < this.seaLevel;
		final boolean isUplandWater = cover == LegacyCover.WATER && !isOcean;

		if (isOcean || isUplandWater) {
			final BlockState floor = TellusLodSurfaceClassifier.underwaterMaterial(cover);
			final int floorIndex = this.tables.blockIndex(floor);
			final int terrainY = isOcean ? surfaceY : Math.max(this.minY, surfaceY - 1);
			final int fluidY = isOcean ? this.seaLevel : surfaceY;
			return new ColumnSample(
					terrainY, fluidY, biomeIndex(biomeId), floorIndex, 0, 0, 0, 0,
					ColumnSample.FLUID_WATER, 0, 0, floorIndex, this.deepIndex,
					ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE,
					ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE);
		}

		final SurfaceSample surface = TellusLodSurfaceClassifier.classifySatelliteSurface(
				satelliteRgb, biomeId, cover, aboveSnowLine);

		int flags = 0;
		if (!isBuilding && !isRoad) {
			if (surface.blockState().is(net.minecraft.world.level.block.Blocks.SNOW_BLOCK)) {
				flags |= ColumnSample.FLAG_SNOW;
			} else if (surface.blockState().is(net.minecraft.world.level.block.Blocks.PACKED_ICE)) {
				flags |= ColumnSample.FLAG_ICE;
			}
		}

		int treeKind = 0;
		int treeDensity = 0;
		int treeHeight = 0;
		if (!surface.forceExposeRock() && !isBuilding && !isRoad) {
			// Imagery-only canopy: trees only where satellite pixels are actually canopy.
			// No Satlas, no vegetationStrength double-count (that painted grass as forest).
			final double canopyStrength = TreeCanopyDetector.canopyStrength(satelliteRgb);
			final TreeCanopyDetector.CanopyBand canopyBand = TreeCanopyDetector.bandForStrength(canopyStrength);
			final int chancePercent = TreeCanopyDetector.chancePercent(canopyBand);
			final int centerHash = mixHash(worldX, worldZ, TREE_CENTER_SALT);

			if (chancePercent > 0 && canopyStrength > 0.0D) {
				final TreePalette palette = TellusLodSurfaceClassifier.treePaletteFromBiomeId(biomeId, centerHash);
				treeKind = this.tables.treeKind(palette);
				treeDensity = densityByte(chancePercent, canopyStrength);
				treeHeight = canopyHeight(canopyBand, centerHash);

				if (tile.spacing == 1 && Integer.remainderUnsigned(centerHash >>> 8, 100) < chancePercent) {
					flags |= ColumnSample.FLAG_TREE_HERE;
				}
			}
		}

		if (isBuilding) {
			// Still one point per column - Meridian's ColumnSample has no room for real walls - but the
			// height itself now varies across the footprint (gable ridge, tower setback), computed in
			// rasterizeBuildingCoverage, so the silhouette isn't a uniform box even though each column
			// is still a single flat top.
			final int buildingTopY = Mth.clamp(surfaceY + overlay.buildingHeight[index], this.minY, this.maxY - 1);
			final int roofIndex = overlay.buildingRoofIndex[index];
			final int wallIndex = overlay.buildingWallIndex[index];
			return new ColumnSample(
					buildingTopY, buildingTopY, biomeIndex(biomeId), roofIndex, 0,
					0, 0, 0,
					ColumnSample.FLUID_NONE, 0, 0, wallIndex, wallIndex,
					ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE,
					ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE);
		}

		final int topIndex = isRoad ? this.roadIndex : this.tables.blockIndex(surface.blockState());
		return new ColumnSample(
				surfaceY, surfaceY, biomeIndex(biomeId), topIndex, 0,
				treeKind, treeDensity, treeHeight,
				ColumnSample.FLUID_NONE, flags, 0, this.stoneIndex, this.deepIndex,
				ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE,
				ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE);
	}

	private int biomeIndex(final String biomeId) {
		return this.tables.biomeIndex(biomeId);
	}

	/** Aggregates per-tile timing and logs a summary every few seconds. See the fields above. */
	private void recordTiming(final long openTileNanos, final long prefetchNanos, final long columnsNanos) {
		this.statTiles.incrementAndGet();
		this.statOpenTileNanos.addAndGet(openTileNanos);
		this.statPrefetchNanos.addAndGet(prefetchNanos);
		this.statColumnsNanos.addAndGet(columnsNanos);

		final long now = System.nanoTime();
		final long last = this.statLastLogNanos.get();
		if (now - last < 5_000_000_000L) {
			return;
		}
		if (!this.statLastLogNanos.compareAndSet(last, now)) {
			return;
		}

		final long tiles = this.statTiles.getAndSet(0);
		final long openNs = this.statOpenTileNanos.getAndSet(0);
		final long prefetchNs = this.statPrefetchNanos.getAndSet(0);
		final long columnsNs = this.statColumnsNanos.getAndSet(0);
		final long chunkEmpty = this.statFallbackChunkEmpty.getAndSet(0);
		final long earthEmpty = this.statFallbackEarthEmpty.getAndSet(0);
		final long exceptions = this.statFallbackException.getAndSet(0);

		if (tiles == 0) {
			Tellus.LOGGER.info("[meridian-timing] 0 tiles in the last 5s window");
			return;
		}

		Tellus.LOGGER.info(
				"[meridian-timing] {} tiles/5s (avg {}ms: openTile={}ms prefetch={}ms columns={}ms) | fallback chunkEmpty={} earthEmpty={} exception={}",
				tiles,
				(openNs + prefetchNs + columnsNs) / tiles / 1_000_000L,
				openNs / tiles / 1_000_000L,
				prefetchNs / tiles / 1_000_000L,
				columnsNs / tiles / 1_000_000L,
				chunkEmpty,
				earthEmpty,
				exceptions);
	}

	// ------------------------------------------------------------------ column

	/**
	 * Meridian reads {@code treeDensity / 4} as trees per chunk, and treats 12 per chunk as fully
	 * forested, so a saturated canopy maps to 48.
	 */
	private static int densityByte(final int chancePercent, final double canopyStrength) {
		final double trees = (chancePercent / 100.0D) * Mth.clamp(canopyStrength, 0.0D, 1.0D) * 12.0D;
		return Mth.clamp((int) Math.round(trees * 4.0D), 0, 255);
	}

	private static int canopyHeight(final TreeCanopyDetector.CanopyBand band, final int centerHash) {
		final int base = TreeCanopyDetector.baseHeight(band);
		final int max = TreeCanopyDetector.maxHeight(band);
		final int span = Math.max(1, max - base);
		return Mth.clamp(base + Integer.remainderUnsigned(centerHash >>> 13, span), 2, 255);
	}

	private int surfaceFromElevation(final double elevationMeters) {
		return Mth.clamp(
				Mth.floor((elevationMeters * this.heightScale) + this.settings.heightOffset()), this.minY, this.maxY - 1);
	}

	/**
	 * A flat ocean plate for a tile whose data could not be resolved even after retrying.
	 *
	 * <p>This used to fill with {@code ColumnSample.UNKNOWN_INDEX} (65535), which is out of range of
	 * every real palette Meridian builds. {@code LodBlockPalette.block()} bounds-checks that and falls
	 * back to its own "missing" placeholder block - which is exactly the flat grey patches showing up
	 * in world gen. A real water column with a real biome index blends into the terrain around it
	 * instead of announcing itself as broken.
	 */
	private void fillFallback(final TerrainGrid grid, final ColumnSampleBuffer out) {
		final int count = grid.sampleCount();
		final ColumnSample flat = new ColumnSample(
				this.seaLevel, this.seaLevel, this.fallbackBiomeIndex, this.fallbackFloorIndex, 0, 0, 0, 0,
				ColumnSample.FLUID_WATER, 0, 0, this.fallbackFloorIndex, this.deepIndex,
				ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE,
				ColumnSample.SPAN_NONE, ColumnSample.SPAN_NONE);
		for (int i = 0; i < count; i++) {
			if (out.prefilled(i)) {
				out.clearPrefilled(i);
				continue;
			}
			out.put(i, flat);
		}
		out.setCount(count);
	}

	// -------------------------------------------------------------- tile setup

	/**
	 * Reports of "which patch of terrain stays a flat placeholder" changing between world creations
	 * pointed at a transient fetch failure, not a real coverage gap: a real gap would fail at the same
	 * coordinates every time. This retries the whole chunk fetch a couple of times with a short
	 * backoff before giving up, so a single dropped connection or slow tile doesn't permanently pin a
	 * tile to the fallback plate for the rest of the session.
	 */
	private Tile openTile(final TerrainGrid grid) {
		final int n = grid.samplesPerAxis();
		final int spacing = grid.spacingBlocks();
		final int level = Math.max(0, 31 - Integer.numberOfLeadingZeros(Math.max(1, spacing)));
		final int half = spacing >> 1;

		final int earthX0 = grid.originBlockX() + this.spawnOffsetX;
		final int earthZ0 = grid.originBlockZ() + this.spawnOffsetZ;
		final int sizeBlocks = n * spacing;
		final GeoView view = new GeoView(earthX0, earthZ0, earthX0 + sizeBlocks - 1, earthZ0 + sizeBlocks - 1);

		Optional<EarthAttachments> earth = Optional.empty();
		for (int attempt = 0; attempt < FETCH_ATTEMPTS; attempt++) {
			if (attempt > 0) {
				try {
					Thread.sleep(FETCH_RETRY_DELAY_MS[attempt]);
				} catch (final InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			final Optional<GeoChunk> chunk;
			try {
				chunk = this.earthLayers.get(view, new RasterShape(n, n)).join();
			} catch (final RuntimeException e) {
				this.statFallbackException.incrementAndGet();
				Tellus.LOGGER.warn("Tellus could not load Meridian LOD data for level {} at {},{} (attempt {}/{})",
						level, grid.originBlockX(), grid.originBlockZ(), attempt + 1, FETCH_ATTEMPTS, e);
				continue;
			}

			if (chunk.isEmpty()) {
				this.statFallbackChunkEmpty.incrementAndGet();
				continue;
			}

			earth = EarthAttachments.from(chunk.get());
			if (earth.isPresent()) {
				break;
			}
			this.statFallbackEarthEmpty.incrementAndGet();
		}

		if (earth.isEmpty()) {
			return null;
		}

		final ShortRaster original = earth.get().elevation();
		final EnumRaster<LegacyCover> landCover = earth.get().landCover();

		return new Tile(grid, n, spacing, half, level, original, landCover);
	}

	/**
	 * Fetches every satellite tile the grid will need, in parallel, and waits for them
	 * to land before the classification loop reads a single pixel.
	 *
	 * <p>Satellite imagery falls back to a blocking fetch on
	 * a cache miss, which is correct but serial: one 256x256 tile at a time, each a network round trip.
	 * Warming every distinct tile the grid touches up front, concurrently, turns what used to be
	 * dozens of sequential round trips - the actual cause of a LOD tile taking tens of seconds - into
	 * one wait for the slowest of a batch of parallel fetches.
	 */
	private void prefetch(final Tile tile) {
		final Set<SatelliteTileSampler.TileKey> satelliteKeys = new HashSet<>();
		final int satMaxCell = Math.max(0, (tile.n - 1) / tile.satelliteStride);
		for (int cellZ = 0; cellZ <= satMaxCell; cellZ++) {
			for (int cellX = 0; cellX <= satMaxCell; cellX++) {
				final int columnX = Math.min(tile.n - 1, cellX * tile.satelliteStride + (tile.satelliteStride >> 1));
				final int columnZ = Math.min(tile.n - 1, cellZ * tile.satelliteStride + (tile.satelliteStride >> 1));
				final double lat = latitudeAt(tile.worldX(columnX), tile.worldZ(columnZ));
				final double lon = longitudeAt(tile.worldX(columnX), tile.worldZ(columnZ));
				final int zoom = MeridianLodPolicy.satelliteZoom(tile.level, lat, this.projection.idealMetersPerBlock());
				satelliteKeys.add(this.satelliteSampler.tileKeyForLatLon(lat, lon, zoom));
			}
		}

		final CompletableFuture<?>[] futures = new CompletableFuture<?>[satelliteKeys.size()];
		int i = 0;
		for (final SatelliteTileSampler.TileKey key : satelliteKeys) {
			futures[i++] = this.satelliteSampler.requestTile(key);
		}

		try {
			CompletableFuture.allOf(futures).get(15, TimeUnit.SECONDS);
		} catch (final Exception e) {
			// A handful of tiles may still be in flight; the classification loop below will fall back
			// to a blocking fetch for those specific misses instead of failing the whole grid.
			Tellus.LOGGER.debug("Meridian imagery prefetch for level {} did not finish within the deadline", tile.level, e);
		}
	}

	private double latitudeAt(final int worldX, final int worldZ) {
		return this.projection.lat(worldX + this.spawnOffsetX, worldZ + this.spawnOffsetZ);
	}

	private double longitudeAt(final int worldX, final int worldZ) {
		return this.projection.lon(worldX + this.spawnOffsetX, worldZ + this.spawnOffsetZ);
	}

	// ------------------------------------------------------------- interpolation

	private static double sampleElevationBicubic(final ShortRaster raster, final double x, final double z) {
		final int baseX = Mth.floor(x);
		final int baseZ = Mth.floor(z);
		final double fracX = x - baseX;
		final double fracZ = z - baseZ;

		final double[] row = new double[4];
		for (int j = -1; j <= 2; j++) {
			final int sampleZ = Mth.clamp(baseZ + j, 0, raster.height() - 1);
			final double p0 = raster.getInt(Mth.clamp(baseX - 1, 0, raster.width() - 1), sampleZ);
			final double p1 = raster.getInt(Mth.clamp(baseX, 0, raster.width() - 1), sampleZ);
			final double p2 = raster.getInt(Mth.clamp(baseX + 1, 0, raster.width() - 1), sampleZ);
			final double p3 = raster.getInt(Mth.clamp(baseX + 2, 0, raster.width() - 1), sampleZ);
			row[j + 1] = catmullRom(p0, p1, p2, p3, fracX);
		}

		return catmullRom(row[0], row[1], row[2], row[3], fracZ);
	}

	static double catmullRom(final double p0, final double p1, final double p2, final double p3, final double t) {
		final double t2 = t * t;
		final double t3 = t2 * t;
		return 0.5D * ((2.0D * p1)
				+ (-p0 + p2) * t
				+ ((2.0D * p0) - (5.0D * p1) + (4.0D * p2) - p3) * t2
				+ (-p0 + (3.0D * p1) - (3.0D * p2) + p3) * t3);
	}

	private static int mixHash(final int x, final int z, final int salt) {
		int h = x * 0x27D4EB2D;
		h ^= (z * 0x165667B1);
		h ^= salt;
		h ^= (h >>> 15);
		h *= 0x2545F491;
		h ^= (h >>> 13);
		return h;
	}

	// --------------------------------------------------------------------- tile

	/**
	 * Per tile state: the fetched rasters plus the sparse imagery caches. One instance lives for the
	 * duration of a single {@link #sampleGrid} call, so the caches need no synchronisation.
	 */
	private final class Tile {
		private final TerrainGrid grid;
		private final int n;
		private final int spacing;
		private final int half;
		private final int level;
		private final ShortRaster elevation;
		private final EnumRaster<LegacyCover> landCover;
		private final int satelliteStride;
		private final int biomeStride;

		private Tile(
				final TerrainGrid grid,
				final int n,
				final int spacing,
				final int half,
				final int level,
				final ShortRaster elevation,
				final EnumRaster<LegacyCover> landCover) {
			this.grid = grid;
			this.n = n;
			this.spacing = spacing;
			this.half = half;
			this.level = level;
			this.elevation = elevation;
			this.landCover = landCover;
			this.satelliteStride = MeridianLodPolicy.satelliteStrideColumns(level);
			this.biomeStride = MeridianLodPolicy.biomeStrideColumns(level);
		}

		private int worldX(final int x) {
			return this.grid.blockX(Math.min(x, this.n - 1)) + this.half;
		}

		private int worldZ(final int z) {
			return this.grid.blockZ(Math.min(z, this.n - 1)) + this.half;
		}
	}
}
