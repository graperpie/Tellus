package com.yucareux.tellus.integration.distant_horizons;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.distanthorizons.api.objects.data.IDhApiFullDataSource;
import com.yucareux.tellus.Tellus;
import com.yucareux.tellus.integration.distant_horizons.TellusLodGenerator.CanopyColumn;
import com.yucareux.tellus.legacy.backend.GeoChunk;
import com.yucareux.tellus.legacy.backend.GeoView;
import com.yucareux.tellus.legacy.backend.earth.EarthAttachments;
import com.yucareux.tellus.legacy.backend.earth.EarthLayers;
import com.yucareux.tellus.legacy.backend.earth.EarthTiles;
import com.yucareux.tellus.legacy.backend.earth.cover.LegacyCover;
import com.yucareux.tellus.legacy.backend.loader.ConcurrencyLimiter;
import com.yucareux.tellus.legacy.backend.projection.Projection;
import com.yucareux.tellus.legacy.backend.projection.cylindrical.Equirectangular;
import com.yucareux.tellus.legacy.backend.raster.EnumRaster;
import com.yucareux.tellus.legacy.backend.raster.RasterShape;
import com.yucareux.tellus.legacy.backend.raster.ShortRaster;
import com.yucareux.tellus.legacy.backend.tile.GuavaTileCache;
import com.yucareux.tellus.world.data.overture.OvertureOverlaySampler;
import com.yucareux.tellus.world.data.satellite.SatlasTreeCoverSampler;
import com.yucareux.tellus.world.data.satellite.SatelliteTileSampler;
import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import com.yucareux.tellus.worldgen.EarthCoordinateShift;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class LegacyLodGeneratorV2 implements IDhApiWorldGenerator {
	private static final int PROFILE_LOG_EVERY_CHUNKS = 32;
	private static final AtomicInteger PROFILE_CHUNK_COUNT = new AtomicInteger();
	private static final AtomicLong PROFILE_TOTAL_NS = new AtomicLong();
	private static final AtomicLong PROFILE_PREFETCH_NS = new AtomicLong();
	private static final AtomicLong PROFILE_BIOME_NS = new AtomicLong();
	private static final AtomicLong PROFILE_ELEVATION_NS = new AtomicLong();
	private static final AtomicLong PROFILE_SATELLITE_NS = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_NS = new AtomicLong();
	private static final AtomicLong PROFILE_TREE_COVER_NS = new AtomicLong();
	private static final AtomicLong PROFILE_PREFETCH_SAMPLES = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_SAMPLES = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_KNOWN_COLUMNS = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_ROAD_COLUMNS = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_BUILDING_COLUMNS = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_ANY_COLUMNS = new AtomicLong();
	private static final AtomicLong PROFILE_TREE_COVER_SAMPLES = new AtomicLong();
	private static final AtomicLong PROFILE_BIOME_SAMPLES = new AtomicLong();
	private static final AtomicLong PROFILE_CANOPY_COLUMNS = new AtomicLong();
	private static final int V2_TREE_CENTER_SALT = 0x4F3A2C17;

	private final LegacyLodGenerator v1Fallback;
	private final IDhApiLevelWrapper levelWrapper;
	private final EarthLayers earthLayers;
	private final Projection projection;
	private final EarthGeneratorSettings settings;
	private final SatelliteTileSampler satelliteSampler;
	private final SatlasTreeCoverSampler satlasTreeCoverSampler;
	private final OvertureOverlaySampler overtureOverlaySampler;
	private final ThreadLocal<WrapperCache> wrapperCache;
	private final int spawnOriginOffsetX;
	private final int spawnOriginOffsetZ;

	public LegacyLodGeneratorV2(final IDhApiLevelWrapper levelWrapper, final EarthChunkGenerator generator) {
		this.levelWrapper = levelWrapper;
		this.v1Fallback = new LegacyLodGenerator(levelWrapper, generator);
		this.settings = generator.settings();
		this.projection = new Equirectangular(settings.worldScale());
		this.wrapperCache = ThreadLocal.withInitial(() -> new WrapperCache(levelWrapper));
		this.spawnOriginOffsetX = EarthCoordinateShift.spawnOffsetX(this.settings);
		this.spawnOriginOffsetZ = EarthCoordinateShift.spawnOffsetZ(this.settings);

		final EarthTiles.Config config = new EarthTiles.Config(
				HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build(),
				new ConcurrencyLimiter(16),
				Paths.get("tellus_cache", "legacy"),
				ForkJoinPool.commonPool(),
				ForkJoinPool.commonPool());
		final EarthTiles tiles = config.create(new GuavaTileCache(Duration.ofMinutes(5), 1000));
		this.earthLayers = EarthLayers.create(tiles, projection, ForkJoinPool.commonPool());

		final HttpClient satelliteHttp = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		this.satelliteSampler = new SatelliteTileSampler(
				satelliteHttp,
				Paths.get("tellus_cache", "satellite"));
		this.satlasTreeCoverSampler = new SatlasTreeCoverSampler(
				satelliteHttp,
				Paths.get("tellus_cache", "satlas_tree_cover"));
		this.overtureOverlaySampler = new OvertureOverlaySampler();
	}

	@Override
	public void preGeneratorTaskStart() {
		v1Fallback.preGeneratorTaskStart();
	}

	@Override
	public byte getLargestDataDetailLevel() {
		return v1Fallback.getLargestDataDetailLevel();
	}

	@Override
	public CompletableFuture<Void> generateLod(
			final int chunkPosMinX,
			final int chunkPosMinZ,
			final int lodPosX,
			final int lodPosZ,
			final byte detailLevel,
			final IDhApiFullDataSource pooledFullDataSource,
			final EDhApiDistantGeneratorMode generatorMode,
			final ExecutorService worldGeneratorThreadPool,
			final Consumer<IDhApiFullDataSource> resultConsumer) {
		final V2Policy policy = V2Policy.forDetailLevel(detailLevel);

		final int lodSizePoints = pooledFullDataSource.getWidthInDataColumns();
		final int lodSizeBlocks = lodSizePoints * (1 << detailLevel);

		final int x0 = SectionPos.sectionToBlockCoord(chunkPosMinX);
		final int z0 = SectionPos.sectionToBlockCoord(chunkPosMinZ);
		final int earthX0 = x0 + this.spawnOriginOffsetX;
		final int earthZ0 = z0 + this.spawnOriginOffsetZ;
		final int earthX1 = earthX0 + lodSizeBlocks - 1;
		final int earthZ1 = earthZ0 + lodSizeBlocks - 1;
		final GeoView blockSampleView = new GeoView(earthX0, earthZ0, earthX1, earthZ1);

		final RasterShape outputShape = new RasterShape(lodSizePoints, lodSizePoints);
		return earthLayers.get(blockSampleView, outputShape).thenAcceptAsync(
				geoChunk -> {
					if (geoChunk.isPresent()) {
						buildLod(policy, pooledFullDataSource, geoChunk.get(), x0, z0, detailLevel);
					}
					resultConsumer.accept(pooledFullDataSource);
				},
				worldGeneratorThreadPool);
	}

	private void buildLod(
			final V2Policy policy,
			final IDhApiFullDataSource output,
			final GeoChunk geoChunk,
			final int x0,
			final int z0,
			final byte detailLevel) {
		final long chunkStartNs = System.nanoTime();
		long biomeNs = 0L;
		long elevationNs = 0L;
		long satelliteNs = 0L;
		long overtureNs = 0L;
		long treeCoverNs = 0L;
		long prefetchNs = 0L;

		final WrapperCache wrappers = wrapperCache.get();
		final int minY = levelWrapper.getMinHeight();
		final int maxY = minY + levelWrapper.getMaxHeight();
		final int absoluteTop = levelWrapper.getMaxHeight();
		final VanillaSurfaceLodOutput lodOutput = new VanillaSurfaceLodOutput(output, wrappers, minY, absoluteTop);

		final Optional<EarthAttachments> earth = EarthAttachments.from(geoChunk);
		if (earth.isEmpty()) {
			return;
		}

		final ShortRaster elevation = earth.get().elevation();
		final EnumRaster<LegacyCover> landCover = earth.get().landCover();
		final int seaLevel = settings.resolveSeaLevel();
		final float heightScale = (float) (settings.terrestrialHeightScale() / projection.idealMetersPerBlock());
		final IDhApiBiomeWrapper defaultBiomeWrapper = wrappers.getBiome("minecraft:plains");
		final int lodBlockSpan = 1 << Math.max(0, detailLevel);
		final int halfSpan = Math.max(1, lodBlockSpan >> 1);
		final int satelliteStrideColumns = satelliteSampleStrideColumns(detailLevel, policy);
		final int overtureStrideColumns = overtureSampleStrideColumns(detailLevel, policy);
		final int treeCoverStrideColumns = treeCoverSampleStrideColumns(detailLevel, policy);
		final int biomeStrideColumns = biomeSampleStrideColumns(detailLevel, policy);
		final Map<Long, Integer> satelliteRgbCache = new HashMap<>();
		final Map<Long, OvertureOverlaySampler.OverlaySample> overtureOverlayCache = new HashMap<>();
		final Map<Long, Integer> satlasTreeCoverCache = new HashMap<>();
		final Map<Long, BiomeSample> biomeSampleCache = new HashMap<>();
		int biomeSamples = 0;
		int canopyColumns = 0;
		int overtureKnownColumns = 0;
		int overtureRoadColumns = 0;
		int overtureBuildingColumns = 0;
		int overtureAnyColumns = 0;

		final long prefetchStartNs = System.nanoTime();
		final int prefetchSatelliteSamples = prefetchSatelliteTiles(
				detailLevel,
				x0,
				z0,
				elevation.width(),
				elevation.height(),
				lodBlockSpan,
				halfSpan,
				satelliteStrideColumns);
		final int prefetchOvertureSamples = prefetchOvertureTiles(
				detailLevel,
				x0,
				z0,
				elevation.width(),
				elevation.height(),
				lodBlockSpan,
				halfSpan,
				overtureStrideColumns);
		final int prefetchTreeCoverSamples = prefetchSatlasTreeCoverTiles(
				x0,
				z0,
				elevation.width(),
				elevation.height(),
				lodBlockSpan,
				halfSpan,
				treeCoverStrideColumns);
		final int prefetchSamples = prefetchSatelliteSamples + prefetchOvertureSamples + prefetchTreeCoverSamples;
		prefetchNs += System.nanoTime() - prefetchStartNs;

		for (int z = 0; z < elevation.height(); z++) {
			for (int x = 0; x < elevation.width(); x++) {
				final int worldX = x0 + (x * lodBlockSpan) + halfSpan;
				final int worldZ = z0 + (z * lodBlockSpan) + halfSpan;

				final long biomeStartNs = System.nanoTime();
				final int biomeCellX = x / biomeStrideColumns;
				final int biomeCellZ = z / biomeStrideColumns;
				final BiomeSample biomeSample = getOrCreateBiomeSample(
						biomeSampleCache,
						biomeCellX,
						biomeCellZ,
						biomeStrideColumns,
						elevation,
						landCover,
						x0,
						z0,
						lodBlockSpan,
						halfSpan,
						seaLevel,
						heightScale,
						defaultBiomeWrapper,
						wrappers);
				final String biomeId = biomeSample.biomeId();
				final IDhApiBiomeWrapper biomeWrapper = biomeSample.wrapper();
				biomeNs += System.nanoTime() - biomeStartNs;
				lodOutput.beginColumn(x, z, biomeWrapper != null ? biomeWrapper : defaultBiomeWrapper);

				final long elevationStartNs = System.nanoTime();
				final int elevationValue = Mth.floor(sampleElevationBicubic(elevation, x + 0.5D, z + 0.5D));
				final int surfaceY = Mth.clamp(Mth.floor((elevationValue * heightScale) + settings.heightOffset()), minY,
						maxY);
				final LegacyCover cover = landCover.get(x, z);
				final boolean aboveSnowLine = false;
				elevationNs += System.nanoTime() - elevationStartNs;

				final boolean isOcean = surfaceY < seaLevel;
				final boolean isUplandWater = cover == LegacyCover.WATER && surfaceY >= seaLevel;

				if (isOcean) {
					lodOutput.addLayerUpTo(surfaceY, getLodUnderwaterMaterial(cover));
					lodOutput.addLayerUpTo(seaLevel, Blocks.WATER.defaultBlockState());
				} else if (isUplandWater) {
					final int waterTo = surfaceY;
					final int floorTo = Math.max(minY, waterTo - 20);
					lodOutput.addLayerUpTo(floorTo, getLodUnderwaterMaterial(cover));
					lodOutput.addLayerUpTo(waterTo, Blocks.WATER.defaultBlockState());
				} else {
					final int overtureCellX = x / overtureStrideColumns;
					final int overtureCellZ = z / overtureStrideColumns;
					final long overtureStartNs = System.nanoTime();
					final OvertureOverlaySampler.OverlaySample overlaySample = getOrCreateOvertureOverlaySample(
							overtureOverlayCache,
							overtureCellX,
							overtureCellZ,
							overtureStrideColumns,
							elevation.width(),
							elevation.height(),
							x0,
							z0,
							lodBlockSpan,
							halfSpan,
							detailLevel);
					overtureNs += System.nanoTime() - overtureStartNs;
					if (overlaySample.known()) {
						overtureKnownColumns++;
					}
					if (overlaySample.road()) {
						overtureRoadColumns++;
					}
					if (overlaySample.building()) {
						overtureBuildingColumns++;
					}
					if (overlaySample.hasAnyOverlay()) {
						overtureAnyColumns++;
					}

					final int sampleCellX = x / satelliteStrideColumns;
					final int sampleCellZ = z / satelliteStrideColumns;
					final double fracX = ((x % satelliteStrideColumns) + 0.5D) / satelliteStrideColumns;
					final double fracZ = ((z % satelliteStrideColumns) + 0.5D) / satelliteStrideColumns;

					final long satelliteStartNs = System.nanoTime();
					final int sampledRgb = interpolateSatelliteRgbCubic(
							satelliteRgbCache,
							sampleCellX,
							sampleCellZ,
							fracX,
							fracZ,
							satelliteStrideColumns,
							elevation.width(),
							elevation.height(),
							x0,
							z0,
							lodBlockSpan,
							halfSpan,
							detailLevel);

					final SatelliteSurface satelliteSurface = classifySatelliteSurface(
							sampledRgb,
							biomeId,
							cover,
							aboveSnowLine);
					satelliteNs += System.nanoTime() - satelliteStartNs;
					BlockState surfaceMaterial = satelliteSurface.blockState();
					if (overlaySample.road()) {
						surfaceMaterial = Blocks.BLACK_CONCRETE.defaultBlockState();
					} else if (overlaySample.building()) {
						surfaceMaterial = Blocks.GRAY_CONCRETE.defaultBlockState();
					}
					if (aboveSnowLine && !satelliteSurface.forceExposeRock()) {
						surfaceMaterial = Blocks.SNOW_BLOCK.defaultBlockState();
					}
					lodOutput.addLayerUpTo(surfaceY, surfaceMaterial);

					final int treeSampleCellX = x / treeCoverStrideColumns;
					final int treeSampleCellZ = z / treeCoverStrideColumns;
					final double treeFracX = ((x % treeCoverStrideColumns) + 0.5D) / treeCoverStrideColumns;
					final double treeFracZ = ((z % treeCoverStrideColumns) + 0.5D) / treeCoverStrideColumns;

					final long treeCoverStartNs = System.nanoTime();
					final double satlasCanopyStrength = interpolateSatlasTreeCoverStrengthBilinear(
							satlasTreeCoverCache,
							treeSampleCellX,
							treeSampleCellZ,
							treeFracX,
							treeFracZ,
							treeCoverStrideColumns,
							elevation.width(),
							elevation.height(),
							x0,
							z0,
							lodBlockSpan,
							halfSpan);
					treeCoverNs += System.nanoTime() - treeCoverStartNs;

					final double canopyStrength = resolveCanopyStrength(
							satlasCanopyStrength,
							satelliteSurface.vegetationStrength());
					if (canopyStrength > 0.0D
							&& !satelliteSurface.forceExposeRock()
							&& !aboveSnowLine
							&& !overlaySample.hasAnyOverlay()) {
						final CanopyColumn canopy = resolveMinecraftStyleCanopyColumn(
								biomeId,
								worldX,
								worldZ,
								lodBlockSpan,
								canopyStrength);
						if (canopy != null) {
							lodOutput.addCanopy(canopy);
							canopyColumns++;
						}
					}
				}

				lodOutput.endColumn();
			}
		}
		biomeSamples = biomeSampleCache.size();

		final long totalNs = System.nanoTime() - chunkStartNs;
		PROFILE_TOTAL_NS.addAndGet(totalNs);
		PROFILE_PREFETCH_NS.addAndGet(prefetchNs);
		PROFILE_BIOME_NS.addAndGet(biomeNs);
		PROFILE_ELEVATION_NS.addAndGet(elevationNs);
		PROFILE_SATELLITE_NS.addAndGet(satelliteNs);
		PROFILE_OVERTURE_NS.addAndGet(overtureNs);
		PROFILE_TREE_COVER_NS.addAndGet(treeCoverNs);
		PROFILE_PREFETCH_SAMPLES.addAndGet(prefetchSamples);
		PROFILE_OVERTURE_SAMPLES.addAndGet(prefetchOvertureSamples + overtureOverlayCache.size());
		PROFILE_OVERTURE_KNOWN_COLUMNS.addAndGet(overtureKnownColumns);
		PROFILE_OVERTURE_ROAD_COLUMNS.addAndGet(overtureRoadColumns);
		PROFILE_OVERTURE_BUILDING_COLUMNS.addAndGet(overtureBuildingColumns);
		PROFILE_OVERTURE_ANY_COLUMNS.addAndGet(overtureAnyColumns);
		PROFILE_TREE_COVER_SAMPLES.addAndGet(prefetchTreeCoverSamples + satlasTreeCoverCache.size());
		PROFILE_BIOME_SAMPLES.addAndGet(biomeSamples);
		PROFILE_CANOPY_COLUMNS.addAndGet(canopyColumns);
		final int chunks = PROFILE_CHUNK_COUNT.incrementAndGet();

		if (chunks % PROFILE_LOG_EVERY_CHUNKS == 0) {
			final double avgTotalMs = PROFILE_TOTAL_NS.get() / 1_000_000.0D / chunks;
			final double avgPrefetchMs = PROFILE_PREFETCH_NS.get() / 1_000_000.0D / chunks;
			final double avgBiomeMs = PROFILE_BIOME_NS.get() / 1_000_000.0D / chunks;
			final double avgElevationMs = PROFILE_ELEVATION_NS.get() / 1_000_000.0D / chunks;
			final double avgSatelliteMs = PROFILE_SATELLITE_NS.get() / 1_000_000.0D / chunks;
			final double avgOvertureMs = PROFILE_OVERTURE_NS.get() / 1_000_000.0D / chunks;
			final double avgTreeCoverMs = PROFILE_TREE_COVER_NS.get() / 1_000_000.0D / chunks;
			final double avgPrefetchSamples = PROFILE_PREFETCH_SAMPLES.get() / (double) chunks;
			final double avgOvertureSamples = PROFILE_OVERTURE_SAMPLES.get() / (double) chunks;
			final double avgOvertureKnownColumns = PROFILE_OVERTURE_KNOWN_COLUMNS.get() / (double) chunks;
			final double avgOvertureRoadColumns = PROFILE_OVERTURE_ROAD_COLUMNS.get() / (double) chunks;
			final double avgOvertureBuildingColumns = PROFILE_OVERTURE_BUILDING_COLUMNS.get() / (double) chunks;
			final double avgOvertureAnyColumns = PROFILE_OVERTURE_ANY_COLUMNS.get() / (double) chunks;
			final double avgTreeCoverSamples = PROFILE_TREE_COVER_SAMPLES.get() / (double) chunks;
			final double avgBiomeSamples = PROFILE_BIOME_SAMPLES.get() / (double) chunks;
			final double avgCanopyColumns = PROFILE_CANOPY_COLUMNS.get() / (double) chunks;
			Tellus.LOGGER.info(
					"Tellus V2 LOD profile avg ({} chunks): total={}ms prefetch={}ms biome={}ms elevation={}ms satellite={}ms overture={}ms treeCover={}ms prefetchSamples={} overtureSamples={} overtureKnown={} overtureAny={} overtureRoad={} overtureBuildings={} treeCoverSamples={} canopyColumns={} biomeSamples={} biomeSamplingEnabled={} satlasCanopyEnabled={}",
					chunks,
					String.format("%.2f", avgTotalMs),
					String.format("%.2f", avgPrefetchMs),
					String.format("%.2f", avgBiomeMs),
					String.format("%.2f", avgElevationMs),
					String.format("%.2f", avgSatelliteMs),
					String.format("%.2f", avgOvertureMs),
					String.format("%.2f", avgTreeCoverMs),
					String.format("%.1f", avgPrefetchSamples),
					String.format("%.1f", avgOvertureSamples),
					String.format("%.1f", avgOvertureKnownColumns),
					String.format("%.1f", avgOvertureAnyColumns),
					String.format("%.1f", avgOvertureRoadColumns),
					String.format("%.1f", avgOvertureBuildingColumns),
					String.format("%.1f", avgTreeCoverSamples),
					String.format("%.1f", avgCanopyColumns),
					String.format("%.1f", avgBiomeSamples),
					"false",
					"true");
		}
	}

	private int prefetchSatelliteTiles(
			final byte detailLevel,
			final int x0,
			final int z0,
			final int width,
			final int height,
			final int lodBlockSpan,
			final int halfSpan,
			final int strideColumns) {
		int samples = 0;
		for (int z = 0; z < height; z += strideColumns) {
			for (int x = 0; x < width; x += strideColumns) {
				final int worldX = x0 + (x * lodBlockSpan) + halfSpan;
				final int worldZ = z0 + (z * lodBlockSpan) + halfSpan;
				final double lat = projection.lat(worldX + this.spawnOriginOffsetX, worldZ + this.spawnOriginOffsetZ);
				final double lon = projection.lon(worldX + this.spawnOriginOffsetX, worldZ + this.spawnOriginOffsetZ);
				final int zoom = detailLevelSatelliteZoom(detailLevel, lat);
				satelliteSampler.sampleRgbNonBlocking(lat, lon, zoom);
				samples++;
			}
		}
		return samples;
	}

	private int prefetchOvertureTiles(
			final byte detailLevel,
			final int x0,
			final int z0,
			final int width,
			final int height,
			final int lodBlockSpan,
			final int halfSpan,
			final int strideColumns) {
		int samples = 0;
		for (int z = 0; z < height; z += strideColumns) {
			for (int x = 0; x < width; x += strideColumns) {
				final int worldX = x0 + (x * lodBlockSpan) + halfSpan;
				final int worldZ = z0 + (z * lodBlockSpan) + halfSpan;
				final double lat = projection.lat(worldX + this.spawnOriginOffsetX, worldZ + this.spawnOriginOffsetZ);
				final double lon = projection.lon(worldX + this.spawnOriginOffsetX, worldZ + this.spawnOriginOffsetZ);
				final int zoom = detailLevelOvertureZoom(detailLevel, lat);
				overtureOverlaySampler.prefetchNonBlocking(lat, lon, zoom);
				samples++;
			}
		}
		return samples;
	}

	private int prefetchSatlasTreeCoverTiles(
			final int x0,
			final int z0,
			final int width,
			final int height,
			final int lodBlockSpan,
			final int halfSpan,
			final int strideColumns) {
		int samples = 0;
		for (int z = 0; z < height; z += strideColumns) {
			for (int x = 0; x < width; x += strideColumns) {
				final int worldX = x0 + (x * lodBlockSpan) + halfSpan;
				final int worldZ = z0 + (z * lodBlockSpan) + halfSpan;
				final double lat = projection.lat(worldX + this.spawnOriginOffsetX, worldZ + this.spawnOriginOffsetZ);
				final double lon = projection.lon(worldX + this.spawnOriginOffsetX, worldZ + this.spawnOriginOffsetZ);
				satlasTreeCoverSampler.sampleTreeCoverClassNonBlocking(lat, lon);
				samples++;
			}
		}
		return samples;
	}

	private int getOrCreateSatlasTreeCoverClassSample(
			final Map<Long, Integer> satlasTreeCoverCache,
			final int cellX,
			final int cellZ,
			final int strideColumns,
			final int width,
			final int height,
			final int x0,
			final int z0,
			final int lodBlockSpan,
			final int halfSpan) {
		final int maxCellX = Math.max(0, (width - 1) / strideColumns);
		final int maxCellZ = Math.max(0, (height - 1) / strideColumns);
		final int clampedCellX = Mth.clamp(cellX, 0, maxCellX);
		final int clampedCellZ = Mth.clamp(cellZ, 0, maxCellZ);
		final long key = (((long) clampedCellX) << 32) | (clampedCellZ & 0xFFFFFFFFL);

		return satlasTreeCoverCache.computeIfAbsent(key, ignored -> {
			final int sampleColumnX = Math.min(width - 1, clampedCellX * strideColumns + (strideColumns >> 1));
			final int sampleColumnZ = Math.min(height - 1, clampedCellZ * strideColumns + (strideColumns >> 1));
			final int sampleWorldX = x0 + (sampleColumnX * lodBlockSpan) + halfSpan;
			final int sampleWorldZ = z0 + (sampleColumnZ * lodBlockSpan) + halfSpan;
			final double sampleLat = projection.lat(sampleWorldX + this.spawnOriginOffsetX, sampleWorldZ + this.spawnOriginOffsetZ);
			final double sampleLon = projection.lon(sampleWorldX + this.spawnOriginOffsetX, sampleWorldZ + this.spawnOriginOffsetZ);
			return satlasTreeCoverSampler.sampleTreeCoverClassNonBlocking(sampleLat, sampleLon);
		});
	}

	private OvertureOverlaySampler.OverlaySample getOrCreateOvertureOverlaySample(
			final Map<Long, OvertureOverlaySampler.OverlaySample> overlayCache,
			final int cellX,
			final int cellZ,
			final int strideColumns,
			final int width,
			final int height,
			final int x0,
			final int z0,
			final int lodBlockSpan,
			final int halfSpan,
			final byte detailLevel) {
		final int maxCellX = Math.max(0, (width - 1) / strideColumns);
		final int maxCellZ = Math.max(0, (height - 1) / strideColumns);
		final int clampedCellX = Mth.clamp(cellX, 0, maxCellX);
		final int clampedCellZ = Mth.clamp(cellZ, 0, maxCellZ);
		final long key = (((long) clampedCellX) << 32) | (clampedCellZ & 0xFFFFFFFFL);
		final OvertureOverlaySampler.OverlaySample cached = overlayCache.get(key);
		if (cached != null && cached.known()) {
			return cached;
		}

		final int sampleColumnX = Math.min(width - 1, clampedCellX * strideColumns + (strideColumns >> 1));
		final int sampleColumnZ = Math.min(height - 1, clampedCellZ * strideColumns + (strideColumns >> 1));
		final int sampleWorldX = x0 + (sampleColumnX * lodBlockSpan) + halfSpan;
		final int sampleWorldZ = z0 + (sampleColumnZ * lodBlockSpan) + halfSpan;
		final double sampleLat = projection.lat(sampleWorldX + this.spawnOriginOffsetX, sampleWorldZ + this.spawnOriginOffsetZ);
		final double sampleLon = projection.lon(sampleWorldX + this.spawnOriginOffsetX, sampleWorldZ + this.spawnOriginOffsetZ);
		final int zoom = detailLevelOvertureZoom(detailLevel, sampleLat);
		final OvertureOverlaySampler.OverlaySample sampled = overtureOverlaySampler.sampleOverlayNonBlocking(sampleLat, sampleLon, zoom);
		if (sampled.known()) {
			overlayCache.put(key, sampled);
		}
		return sampled;
	}

	private double interpolateSatlasTreeCoverStrengthBilinear(
			final Map<Long, Integer> satlasTreeCoverCache,
			final int cellX,
			final int cellZ,
			final double fracX,
			final double fracZ,
			final int strideColumns,
			final int width,
			final int height,
			final int x0,
			final int z0,
			final int lodBlockSpan,
			final int halfSpan) {
		final double fx = Mth.clamp(fracX, 0.0D, 1.0D);
		final double fz = Mth.clamp(fracZ, 0.0D, 1.0D);

		final int c00 = getOrCreateSatlasTreeCoverClassSample(
				satlasTreeCoverCache,
				cellX,
				cellZ,
				strideColumns,
				width,
				height,
				x0,
				z0,
				lodBlockSpan,
				halfSpan);
		final int c10 = getOrCreateSatlasTreeCoverClassSample(
				satlasTreeCoverCache,
				cellX + 1,
				cellZ,
				strideColumns,
				width,
				height,
				x0,
				z0,
				lodBlockSpan,
				halfSpan);
		final int c01 = getOrCreateSatlasTreeCoverClassSample(
				satlasTreeCoverCache,
				cellX,
				cellZ + 1,
				strideColumns,
				width,
				height,
				x0,
				z0,
				lodBlockSpan,
				halfSpan);
		final int c11 = getOrCreateSatlasTreeCoverClassSample(
				satlasTreeCoverCache,
				cellX + 1,
				cellZ + 1,
				strideColumns,
				width,
				height,
				x0,
				z0,
				lodBlockSpan,
				halfSpan);

		final double s00 = satlasClassToCanopyStrength(c00);
		final double s10 = satlasClassToCanopyStrength(c10);
		final double s01 = satlasClassToCanopyStrength(c01);
		final double s11 = satlasClassToCanopyStrength(c11);

		if (s00 < 0.0D || s10 < 0.0D || s01 < 0.0D || s11 < 0.0D) {
			return satlasClassToCanopyStrength(c00);
		}

		final double top = s00 + (s10 - s00) * fx;
		final double bottom = s01 + (s11 - s01) * fx;
		return Mth.clamp(top + (bottom - top) * fz, 0.0D, 1.0D);
	}

	private BiomeSample getOrCreateBiomeSample(
			final Map<Long, BiomeSample> biomeSampleCache,
			final int cellX,
			final int cellZ,
			final int strideColumns,
			final ShortRaster elevation,
			final EnumRaster<LegacyCover> landCover,
			final int x0,
			final int z0,
			final int lodBlockSpan,
			final int halfSpan,
			final int seaLevel,
			final float heightScale,
			final IDhApiBiomeWrapper defaultBiomeWrapper,
			final WrapperCache wrappers) {
		final int maxCellX = Math.max(0, (elevation.width() - 1) / strideColumns);
		final int maxCellZ = Math.max(0, (elevation.height() - 1) / strideColumns);
		final int clampedCellX = Mth.clamp(cellX, 0, maxCellX);
		final int clampedCellZ = Mth.clamp(cellZ, 0, maxCellZ);
		final long key = (((long) clampedCellX) << 32) | (clampedCellZ & 0xFFFFFFFFL);

		return biomeSampleCache.computeIfAbsent(key, ignored -> {
			final int sampleColumnX = Math.min(
					elevation.width() - 1,
					clampedCellX * strideColumns + (strideColumns >> 1));
			final int sampleColumnZ = Math.min(
					elevation.height() - 1,
					clampedCellZ * strideColumns + (strideColumns >> 1));
			final int sampleWorldX = x0 + (sampleColumnX * lodBlockSpan) + halfSpan;
			final int sampleWorldZ = z0 + (sampleColumnZ * lodBlockSpan) + halfSpan;
			final int elevationValue = elevation.getInt(sampleColumnX, sampleColumnZ);
			final int surfaceY = Mth.floor((elevationValue * heightScale) + settings.heightOffset());
			final LegacyCover cover = landCover.get(sampleColumnX, sampleColumnZ);
			final double latitude = projection.lat(
					sampleWorldX + this.spawnOriginOffsetX,
					sampleWorldZ + this.spawnOriginOffsetZ);

			final String biomeId = classifyBiomeIdFast(cover, surfaceY, seaLevel, latitude, elevationValue);
			final IDhApiBiomeWrapper wrapper = Optional.ofNullable(wrappers.getBiome(biomeId))
					.orElse(defaultBiomeWrapper);
			return new BiomeSample(biomeId, wrapper);
		});
	}

	private static String classifyBiomeIdFast(
			final LegacyCover cover,
			final int surfaceY,
			final int seaLevel,
			final double latitude,
			final int elevationValue) {
		final double absLat = Math.abs(latitude);

		if (cover == LegacyCover.WATER) {
			return surfaceY < seaLevel ? "minecraft:ocean" : "minecraft:river";
		}

		if (cover == LegacyCover.PERMANENT_SNOW || (absLat > 58.0D && elevationValue > 900)) {
			return elevationValue > 1700 ? "minecraft:frozen_peaks" : "minecraft:snowy_slopes";
		}

		if (isFloodedCover(cover)) {
			return absLat < 30.0D ? "minecraft:mangrove_swamp" : "minecraft:swamp";
		}

		if (elevationValue > 3200) {
			return "minecraft:jagged_peaks";
		}
		if (elevationValue > 2400) {
			return "minecraft:stony_peaks";
		}
		if (elevationValue > 1600 && absLat > 52.0D) {
			return "minecraft:windswept_hills";
		}

		if (isForestCover(cover)) {
			if (absLat >= 55.0D) {
				return "minecraft:taiga";
			}
			if (absLat <= 17.0D) {
				return "minecraft:jungle";
			}
			if (absLat <= 28.0D) {
				return "minecraft:savanna";
			}
			return "minecraft:forest";
		}

		if (isGrassOrCropCover(cover)) {
			if (absLat <= 18.0D) {
				return "minecraft:savanna";
			}
			if (absLat >= 55.0D) {
				return "minecraft:taiga";
			}
			return "minecraft:plains";
		}

		if (isBareCover(cover)) {
			if (absLat <= 23.0D) {
				return "minecraft:desert";
			}
			if (absLat <= 30.0D) {
				return "minecraft:badlands";
			}
			return "minecraft:plains";
		}

		return "minecraft:plains";
	}

	private static boolean isForestCover(final LegacyCover cover) {
		return switch (cover) {
			case TREE_OR_SHRUB_COVER,
					BROADLEAF_EVERGREEN,
					BROADLEAF_DECIDUOUS,
					BROADLEAF_DECIDUOUS_CLOSED,
					BROADLEAF_DECIDUOUS_OPEN,
					NEEDLE_LEAF_EVERGREEN,
					NEEDLE_LEAF_EVERGREEN_CLOSED,
					NEEDLE_LEAF_EVERGREEN_OPEN,
					NEEDLE_LEAF_DECIDUOUS,
					NEEDLE_LEAF_DECIDUOUS_CLOSED,
					NEEDLE_LEAF_DECIDUOUS_OPEN,
					MIXED_LEAF_TYPE,
					TREE_AND_SHRUB_WITH_HERBACEOUS_COVER,
					HERBACEOUS_COVER_WITH_TREE_AND_SHRUB,
					SHRUBLAND,
					SHRUBLAND_EVERGREEN,
					SHRUBLAND_DECIDUOUS,
					SPARSE_TREE -> true;
			default -> false;
		};
	}

	private static boolean isGrassOrCropCover(final LegacyCover cover) {
		return switch (cover) {
			case RAINFED_CROPLAND,
					IRRIGATED_CROPLAND,
					CROPLAND_WITH_VEGETATION,
					VEGETATION_WITH_CROPLAND,
					GRASSLAND,
					HERBACEOUS_COVER,
					SPARSE_VEGETATION,
					SPARSE_SHRUB,
					SPARSE_HERBACEOUS_COVER,
					LICHENS_AND_MOSSES -> true;
			default -> false;
		};
	}

	private static boolean isBareCover(final LegacyCover cover) {
		return switch (cover) {
			case BARE,
					BARE_CONSOLIDATED,
					BARE_UNCONSOLIDATED,
					URBAN -> true;
			default -> false;
		};
	}

	private static boolean isFloodedCover(final LegacyCover cover) {
		return switch (cover) {
			case FRESH_FLOODED_FOREST,
					SALINE_FLOODED_FOREST,
					FLOODED_VEGETATION -> true;
			default -> false;
		};
	}

	private int getOrCreateSatelliteRgbSample(
			final Map<Long, Integer> satelliteRgbCache,
			final int cellX,
			final int cellZ,
			final int strideColumns,
			final int width,
			final int height,
			final int x0,
			final int z0,
			final int lodBlockSpan,
			final int halfSpan,
			final byte detailLevel) {
		final int maxCellX = Math.max(0, (width - 1) / strideColumns);
		final int maxCellZ = Math.max(0, (height - 1) / strideColumns);
		final int clampedCellX = Mth.clamp(cellX, 0, maxCellX);
		final int clampedCellZ = Mth.clamp(cellZ, 0, maxCellZ);
		final long key = (((long) clampedCellX) << 32) | (clampedCellZ & 0xFFFFFFFFL);

		return satelliteRgbCache.computeIfAbsent(key, ignored -> {
			final int sampleColumnX = Math.min(width - 1, clampedCellX * strideColumns + (strideColumns >> 1));
			final int sampleColumnZ = Math.min(height - 1, clampedCellZ * strideColumns + (strideColumns >> 1));
			final int sampleWorldX = x0 + (sampleColumnX * lodBlockSpan) + halfSpan;
			final int sampleWorldZ = z0 + (sampleColumnZ * lodBlockSpan) + halfSpan;
			final double sampleLat = projection.lat(sampleWorldX + this.spawnOriginOffsetX, sampleWorldZ + this.spawnOriginOffsetZ);
			final double sampleLon = projection.lon(sampleWorldX + this.spawnOriginOffsetX, sampleWorldZ + this.spawnOriginOffsetZ);
			final int zoom = detailLevelSatelliteZoom(detailLevel, sampleLat);
			return satelliteSampler.sampleRgb(sampleLat, sampleLon, zoom);
		});
	}

	private int interpolateSatelliteRgbCubic(
			final Map<Long, Integer> satelliteRgbCache,
			final int cellX,
			final int cellZ,
			final double fracX,
			final double fracZ,
			final int strideColumns,
			final int width,
			final int height,
			final int x0,
			final int z0,
			final int lodBlockSpan,
			final int halfSpan,
			final byte detailLevel) {
		final double fx = Mth.clamp(fracX, 0.0D, 1.0D);
		final double fz = Mth.clamp(fracZ, 0.0D, 1.0D);
		final int[][] samples = new int[4][4];
		for (int j = -1; j <= 2; j++) {
			for (int i = -1; i <= 2; i++) {
				samples[j + 1][i + 1] = getOrCreateSatelliteRgbSample(
						satelliteRgbCache,
						cellX + i,
						cellZ + j,
						strideColumns,
						width,
						height,
						x0,
						z0,
						lodBlockSpan,
						halfSpan,
						detailLevel);
			}
		}

		final int centerRgb = samples[1][1];
		for (int j = 0; j < 4; j++) {
			for (int i = 0; i < 4; i++) {
				if (samples[j][i] < 0) {
					return centerRgb;
				}
			}
		}

		final double[] rRow = new double[4];
		final double[] gRow = new double[4];
		final double[] bRow = new double[4];
		for (int j = 0; j < 4; j++) {
			final int rgb0 = samples[j][0];
			final int rgb1 = samples[j][1];
			final int rgb2 = samples[j][2];
			final int rgb3 = samples[j][3];
			rRow[j] = catmullRom(
					(rgb0 >> 16) & 0xFF,
					(rgb1 >> 16) & 0xFF,
					(rgb2 >> 16) & 0xFF,
					(rgb3 >> 16) & 0xFF,
					fx);
			gRow[j] = catmullRom(
					(rgb0 >> 8) & 0xFF,
					(rgb1 >> 8) & 0xFF,
					(rgb2 >> 8) & 0xFF,
					(rgb3 >> 8) & 0xFF,
					fx);
			bRow[j] = catmullRom(
					rgb0 & 0xFF,
					rgb1 & 0xFF,
					rgb2 & 0xFF,
					rgb3 & 0xFF,
					fx);
		}

		final int r = Mth.clamp(Mth.floor(catmullRom(rRow[0], rRow[1], rRow[2], rRow[3], fz) + 0.5D), 0, 255);
		final int g = Mth.clamp(Mth.floor(catmullRom(gRow[0], gRow[1], gRow[2], gRow[3], fz) + 0.5D), 0, 255);
		final int b = Mth.clamp(Mth.floor(catmullRom(bRow[0], bRow[1], bRow[2], bRow[3], fz) + 0.5D), 0, 255);
		return (r << 16) | (g << 8) | b;
	}

	private SatelliteSurface classifySatelliteSurface(
			final int rgb,
			final String biomeId,
			final LegacyCover cover,
			final boolean aboveSnowLine) {
		if (rgb < 0) {
			return fallbackSurfaceFromCoverAndBiome(cover, biomeId, aboveSnowLine);
		}

		final double r = ((rgb >> 16) & 0xFF) / 255.0D;
		final double g = ((rgb >> 8) & 0xFF) / 255.0D;
		final double b = (rgb & 0xFF) / 255.0D;
		final double max = Math.max(r, Math.max(g, b));
		final double min = Math.min(r, Math.min(g, b));
		final double value = max;
		final double chroma = max - min;
		final double saturation = max <= 1.0E-5D ? 0.0D : chroma / max;
		final double lightness = (max + min) * 0.5D;
		final double hue = computeHueDegrees(r, g, b, max, chroma);
		final double blueDominance = b - Math.max(r, g);
		final double greenDominance = g - Math.max(r, b);
		final double vari = (g - r) / Math.max(0.12D, g + r - b);
		final double exg = (2.0D * g) - r - b;
		final double vegetationStrength = Mth.clamp(
				(vari * 0.9D) + (Math.max(0.0D, exg) * 0.55D) + (Math.max(0.0D, greenDominance) * 0.6D),
				0.0D,
				1.0D);
		final String biome = biomeId == null ? "" : biomeId.toLowerCase();
		final boolean biomeDesert = biome.contains("desert");
		final boolean biomeBadlands = biome.contains("badlands");
		final boolean biomeRocky = biome.contains("mountain")
				|| biome.contains("peak")
				|| biome.contains("stony")
				|| biome.contains("windswept");

		// Enhanced color classification based on satellite imagery color codes
		
		// 1. WATER DETECTION (Deep Blue → Medium Blue)
		if (blueDominance > 0.08D && value < 0.62D && saturation > 0.10D) {
			return new SatelliteSurface(Blocks.WATER.defaultBlockState(), 0.0D, false);
		}

		// 2. ICE DETECTION (Light Blue)
		if (blueDominance > 0.05D && value > 0.65D && saturation > 0.08D && saturation < 0.35D && hue >= 180.0D
				&& hue <= 250.0D) {
			return new SatelliteSurface(Blocks.PACKED_ICE.defaultBlockState(), 0.0D, false);
		}

		// 3. SNOW DETECTION (White / Very high brightness)
		final boolean nearWhite = Math.abs(r - g) < 0.045D && Math.abs(g - b) < 0.045D;
		final boolean isSnow = value > 0.90D && saturation < 0.10D && nearWhite;
		if (isSnow) {
			return new SatelliteSurface(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

		// Force snow above tree line
		if (aboveSnowLine && value > 0.66D && saturation < 0.15D) {
			return new SatelliteSurface(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

		// 4. GREEN VEGETATION DETECTION (Dark Green → Medium Green → Light Green)
		if (hue >= 58.0D && hue <= 165.0D && greenDominance > 0.03D) {
			// Dark Green (Dense Forest / Spruce Leaves)
			if (value < 0.42D && saturation > 0.20D) {
				return new SatelliteSurface(Blocks.SPRUCE_LEAVES.defaultBlockState(), 0.45D, false);
			}
			
			// Medium-Dark Green (Dense vegetation / Podzol forest)
			if (value < 0.56D && saturation > 0.14D) {
				if (vari > 0.10D || exg > 0.06D) {
					return new SatelliteSurface(Blocks.PODZOL.defaultBlockState(), 0.35D, false);
				}
			}
			
			// Medium Green (Grass / Oak Leaves / General Vegetation)
			if (value >= 0.42D && value <= 0.70D && saturation > 0.12D) {
				if (vegetationStrength > 0.14D && value > 0.45D) {
					return new SatelliteSurface(Blocks.GRASS_BLOCK.defaultBlockState(), vegetationStrength, false);
				}
			}
			
			// Light Green (Cropland / Lime area)
			if (value > 0.70D && saturation > 0.22D && saturation < 0.45D && hue >= 70.0D && hue <= 105.0D) {
				return new SatelliteSurface(Blocks.LIME_CONCRETE.defaultBlockState(), 0.25D, false);
			}

			// Moss covering (low saturation green)
			if (value > 0.35D && value < 0.58D && saturation < 0.13D && hue > 98.0D) {
				return new SatelliteSurface(Blocks.MOSS_BLOCK.defaultBlockState(), 0.20D, false);
			}
		}

		// 5. ALPINE ROCK DETECTION (prioritize mountain limestone/granite over dirt)
		if ((aboveSnowLine || biomeRocky)
				&& saturation < 0.30D
				&& hue >= 18.0D
				&& hue <= 60.0D
				&& value > 0.50D) {
			if (value > 0.78D) {
				return new SatelliteSurface(Blocks.SMOOTH_STONE.defaultBlockState(), 0.0D, true);
			}
			if (value > 0.62D) {
				return new SatelliteSurface(Blocks.ANDESITE.defaultBlockState(), 0.0D, true);
			}
			return new SatelliteSurface(Blocks.STONE.defaultBlockState(), 0.0D, true);
		}

		// 6. RED / ORANGE DETECTION (Rooftops, Terracotta, Red Sand, Orange Terracotta)
		if (hue >= 355.0D || hue <= 50.0D) {
			// Pure Red (Rooftops / Brick)
			if (hue >= 350.0D || hue <= 15.0D) {
				if (saturation > 0.50D && value > 0.40D && value < 0.68D) {
					return new SatelliteSurface(Blocks.RED_TERRACOTTA.defaultBlockState(), 0.0D, false);
				}
				if (saturation > 0.45D && r > 0.60D && value < 0.62D) {
					return new SatelliteSurface(Blocks.BRICKS.defaultBlockState(), 0.0D, true);
				}
			}
			
			// Orange (Orange Terracotta, transitioning to Red Sand)
			if (hue >= 15.0D && hue <= 38.0D && saturation > 0.40D) {
				if (value > 0.68D) {
					return new SatelliteSurface(Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 0.0D, false);
				} else if (value > 0.52D) {
					return new SatelliteSurface(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
				}
			}
		}

		// 7. YELLOW / SAND DETECTION
		if (hue >= 40.0D && hue <= 60.0D) {
			if (saturation > 0.32D && value > 0.68D) {
				// Bright Yellow → Sand
				return new SatelliteSurface(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
			if (saturation > 0.26D && value > 0.56D && value < 0.74D) {
				// Darker Yellow → Sand variant
				return new SatelliteSurface(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
		}

		// 8. BROWN DETECTION (Dirt, Podzol, Coarse Dirt)
		if (hue >= 15.0D && hue <= 38.0D && saturation > 0.16D && saturation < 0.42D) {
			if (value > 0.46D && value < 0.62D) {
				// Brown → Coarse Dirt / Regular Dirt
				return new SatelliteSurface(Blocks.COARSE_DIRT.defaultBlockState(), 0.0D, false);
			}
			if (value >= 0.32D && value <= 0.48D) {
				// Darker Brown → Podzol
				return new SatelliteSurface(Blocks.PODZOL.defaultBlockState(), 0.15D, false);
			}
		}

		// 9. SWAMPY GREEN-BROWN DETECTION (Mud / Mangrove Roots)
		// Mixture of green and brown with lowered saturation
		if (hue >= 50.0D && hue <= 95.0D && value < 0.50D && saturation > 0.10D && saturation < 0.34D) {
			if (g > r && (g - r) > 0.05D) {
				return new SatelliteSurface(Blocks.MUD.defaultBlockState(), 0.10D, false);
			}
		}

		// 10. GRAY DETECTION (Light Gray → Stone/Andesite, Dark Gray → Basalt/Blackstone)
		// Also urban areas and asphalt
		if (saturation < 0.17D && value > 0.22D) {
			if (isForestCover(cover) || isGrassOrCropCover(cover)) {
				if (value > 0.60D) {
					return new SatelliteSurface(Blocks.GRASS_BLOCK.defaultBlockState(), Math.max(0.16D, vegetationStrength), false);
				}
				if (value > 0.42D) {
					return new SatelliteSurface(Blocks.PODZOL.defaultBlockState(), Math.max(0.10D, vegetationStrength * 0.6D), false);
				}
				return new SatelliteSurface(Blocks.COARSE_DIRT.defaultBlockState(), Math.max(0.06D, vegetationStrength * 0.45D), false);
			}

			if (isFloodedCover(cover)) {
				return new SatelliteSurface(Blocks.MUD.defaultBlockState(), 0.10D, false);
			}

			if (cover == LegacyCover.URBAN) {
				if (value > 0.74D) {
					return new SatelliteSurface(Blocks.STONE_BRICKS.defaultBlockState(), 0.0D, true);
				}
				if (value >= 0.52D) {
					return new SatelliteSurface(Blocks.ANDESITE.defaultBlockState(), 0.0D, true);
				}
				if (value >= 0.38D) {
					return new SatelliteSurface(Blocks.STONE.defaultBlockState(), 0.0D, true);
				}
				if (value >= 0.24D) {
					return new SatelliteSurface(Blocks.BLACK_CONCRETE.defaultBlockState(), 0.0D, true);
				}
				return new SatelliteSurface(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 0.0D, true);
			}

			if (isBareCover(cover)) {
				if (biomeDesert) {
					return new SatelliteSurface(Blocks.SAND.defaultBlockState(), 0.0D, false);
				}
				if (biomeBadlands) {
					return new SatelliteSurface(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
				}
				if (biomeRocky || aboveSnowLine) {
					if (value > 0.65D) {
						return new SatelliteSurface(Blocks.ANDESITE.defaultBlockState(), 0.0D, true);
					}
					return new SatelliteSurface(Blocks.STONE.defaultBlockState(), 0.0D, true);
				}
				return new SatelliteSurface(Blocks.COARSE_DIRT.defaultBlockState(), 0.0D, false);
			}

			if (!isBareCover(cover)) {
				return fallbackSurfaceFromCoverAndBiome(cover, biomeId, aboveSnowLine);
			}

			// Light Gray (urban stone/concrete)
			if (value > 0.74D) {
				return new SatelliteSurface(Blocks.STONE_BRICKS.defaultBlockState(), 0.0D, true);
			}
			
			// Medium Gray (Andesite / generic stone)
			if (value >= 0.52D && value <= 0.74D) {
				return new SatelliteSurface(Blocks.ANDESITE.defaultBlockState(), 0.0D, true);
			}
			
			// Mid-Dark Gray (Stone)
			if (value >= 0.38D && value < 0.52D) {
				return new SatelliteSurface(Blocks.STONE.defaultBlockState(), 0.0D, true);
			}
			
			// Dark Gray (Asphalt / Black Concrete)
			if (value >= 0.24D && value < 0.38D) {
				if (r > 0.05D && g > 0.05D && b > 0.05D) {
					return new SatelliteSurface(Blocks.BLACK_CONCRETE.defaultBlockState(), 0.0D, true);
				}
				return new SatelliteSurface(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 0.0D, true);
			}
			
			// Very Dark Gray (Deepslate/Basalt)
			if (value < 0.24D) {
				return new SatelliteSurface(Blocks.BASALT.defaultBlockState(), 0.0D, true);
			}
		}

		// 10. FALLBACK: Biome-based safety net (V2-only)
		return fallbackSurfaceFromCoverAndBiome(cover, biomeId, aboveSnowLine);
	}

	private static SatelliteSurface fallbackSurfaceFromCoverAndBiome(
			final LegacyCover cover,
			final String biomeId,
			final boolean aboveSnowLine) {
		if (aboveSnowLine) {
			return new SatelliteSurface(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

		if (isFloodedCover(cover)) {
			return new SatelliteSurface(Blocks.MUD.defaultBlockState(), 0.10D, false);
		}

		if (isForestCover(cover)) {
			return new SatelliteSurface(Blocks.PODZOL.defaultBlockState(), 0.25D, false);
		}

		if (isGrassOrCropCover(cover)) {
			return new SatelliteSurface(Blocks.GRASS_BLOCK.defaultBlockState(), 0.12D, false);
		}

		if (cover == LegacyCover.URBAN) {
			return new SatelliteSurface(Blocks.STONE_BRICKS.defaultBlockState(), 0.0D, true);
		}

		if (isBareCover(cover)) {
			final String biome = biomeId == null ? "" : biomeId.toLowerCase();
			if (biome.contains("desert")) {
				return new SatelliteSurface(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
			if (biome.contains("badlands")) {
				return new SatelliteSurface(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
			}
			return new SatelliteSurface(Blocks.COARSE_DIRT.defaultBlockState(), 0.0D, false);
		}

		return fallbackSurfaceFromBiome(biomeId, aboveSnowLine);
	}

	private static SatelliteSurface fallbackSurfaceFromBiome(
			final String biomeId,
			final boolean aboveSnowLine) {
		if (aboveSnowLine) {
			return new SatelliteSurface(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

		final String biome = biomeId == null ? "" : biomeId.toLowerCase();

		if (biome.contains("desert")) {
			return new SatelliteSurface(Blocks.SAND.defaultBlockState(), 0.0D, false);
		}
		if (biome.contains("badlands")) {
			return new SatelliteSurface(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
		}
		if (biome.contains("mountain") || biome.contains("peak") || biome.contains("stony")) {
			return new SatelliteSurface(Blocks.STONE.defaultBlockState(), 0.0D, true);
		}
		if (biome.contains("taiga") || biome.contains("forest") || biome.contains("jungle")) {
			return new SatelliteSurface(Blocks.PODZOL.defaultBlockState(), 0.25D, false);
		}

		return new SatelliteSurface(Blocks.GRASS_BLOCK.defaultBlockState(), 0.10D, false);
	}

	private static double computeHueDegrees(
			final double r,
			final double g,
			final double b,
			final double max,
			final double chroma) {
		if (chroma <= 1.0E-6D) {
			return 0.0D;
		}

		double hue;
		if (max == r) {
			hue = ((g - b) / chroma) % 6.0D;
		} else if (max == g) {
			hue = ((b - r) / chroma) + 2.0D;
		} else {
			hue = ((r - g) / chroma) + 4.0D;
		}

		hue *= 60.0D;
		if (hue < 0.0D) {
			hue += 360.0D;
		}
		return hue;
	}

	private int detailLevelSatelliteZoom(final byte detailLevel, final double latitude) {
		final int level = Byte.toUnsignedInt(detailLevel);
		int zoom = switch (level) {
			case 0, 1 -> 15;
			case 2 -> 14;
			case 3 -> 13;
			case 4 -> 12;
			case 5 -> 11;
			case 6 -> 10;
			case 7 -> 9;
			case 8 -> 8;
			case 9 -> 7;
			default -> 6;
		};

		// Keep far LOD columns close to 1:1 with imagery pixel footprint.
		final int lodBlockSpan = 1 << Math.min(level, 24);
		final double metersPerColumn = lodBlockSpan * projection.idealMetersPerBlock();
		if (metersPerColumn >= 4096.0D) {
			zoom = Math.min(zoom, 6);
		} else if (metersPerColumn >= 2048.0D) {
			zoom = Math.min(zoom, 7);
		} else if (metersPerColumn >= 1024.0D) {
			zoom = Math.min(zoom, 8);
		}

		// High latitudes compress meters-per-pixel in Web Mercator.
		final double absLat = Math.abs(latitude);
		if (absLat >= 70.0D) {
			zoom -= 2;
		} else if (absLat >= 55.0D) {
			zoom -= 1;
		}

		return Mth.clamp(zoom, 5, 15);
	}

	private int detailLevelOvertureZoom(final byte detailLevel, final double latitude) {
		final int level = Byte.toUnsignedInt(detailLevel);
		int zoom = switch (level) {
			case 0, 1 -> 15;
			case 2 -> 14;
			case 3 -> 13;
			case 4 -> 12;
			case 5 -> 11;
			case 6 -> 10;
			case 7 -> 9;
			case 8 -> 8;
			default -> 7;
		};

		final int lodBlockSpan = 1 << Math.min(level, 24);
		final double metersPerColumn = lodBlockSpan * projection.idealMetersPerBlock();
		if (metersPerColumn >= 4096.0D) {
			zoom = Math.min(zoom, 7);
		} else if (metersPerColumn >= 2048.0D) {
			zoom = Math.min(zoom, 8);
		} else if (metersPerColumn >= 1024.0D) {
			zoom = Math.min(zoom, 9);
		}

		final double absLat = Math.abs(latitude);
		if (absLat >= 70.0D) {
			zoom -= 2;
		} else if (absLat >= 55.0D) {
			zoom -= 1;
		}

		return Mth.clamp(zoom, 6, 15);
	}

	private static int satelliteSampleStrideColumns(final byte detailLevel, final V2Policy policy) {
		final int base = switch (policy) {
			case LEVEL_3_HIGH_RES -> 2;
			case LEVEL_4_SENTINEL_10M -> 3;
			case LEVEL_5_SENTINEL_10M_VEG -> 4;
			case LEVEL_6_30M -> 6;
			case LEVEL_7_DOWNSAMPLED -> 8;
			case LEVEL_8_MODIS -> 12;
		};

		final int level = Byte.toUnsignedInt(detailLevel);
		if (level >= 8) {
			return Math.max(base, 12);
		}
		if (level >= 6) {
			return Math.max(base, 8);
		}
		return base;
	}

	private static int overtureSampleStrideColumns(final byte detailLevel, final V2Policy policy) {
		final int base = switch (policy) {
			case LEVEL_3_HIGH_RES -> 2;
			case LEVEL_4_SENTINEL_10M -> 3;
			case LEVEL_5_SENTINEL_10M_VEG -> 4;
			case LEVEL_6_30M -> 5;
			case LEVEL_7_DOWNSAMPLED -> 6;
			case LEVEL_8_MODIS -> 8;
		};

		final int level = Byte.toUnsignedInt(detailLevel);
		if (level >= 8) {
			return Math.max(base, 10);
		}
		if (level >= 6) {
			return Math.max(base, 6);
		}
		return base;
	}

	private static int treeCoverSampleStrideColumns(final byte detailLevel, final V2Policy policy) {
		final int satelliteStride = satelliteSampleStrideColumns(detailLevel, policy);
		final int base = Math.max(2, satelliteStride * 2);

		final int level = Byte.toUnsignedInt(detailLevel);
		if (level >= 8) {
			return Math.max(base, 24);
		}
		if (level >= 6) {
			return Math.max(base, 12);
		}
		return base;
	}

	private static int biomeSampleStrideColumns(final byte detailLevel, final V2Policy policy) {
		final int base = switch (policy) {
			case LEVEL_3_HIGH_RES -> 1;
			case LEVEL_4_SENTINEL_10M -> 2;
			case LEVEL_5_SENTINEL_10M_VEG -> 3;
			case LEVEL_6_30M -> 4;
			case LEVEL_7_DOWNSAMPLED -> 6;
			case LEVEL_8_MODIS -> 8;
		};

		final int level = Byte.toUnsignedInt(detailLevel);
		if (level >= 8) {
			return Math.max(base, 8);
		}
		if (level >= 6) {
			return Math.max(base, 5);
		}
		return base;
	}

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

	private static double catmullRom(
			final double p0,
			final double p1,
			final double p2,
			final double p3,
			final double t) {
		final double t2 = t * t;
		final double t3 = t2 * t;
		return 0.5D * ((2.0D * p1)
				+ (-p0 + p2) * t
				+ ((2.0D * p0) - (5.0D * p1) + (4.0D * p2) - p3) * t2
				+ (-p0 + (3.0D * p1) - (3.0D * p2) + p3) * t3);
	}

	private static BlockState getLodUnderwaterMaterial(final LegacyCover cover) {
		return switch (cover) {
			case BARE_UNCONSOLIDATED, SPARSE_VEGETATION -> Blocks.SAND.defaultBlockState();
			case BARE_CONSOLIDATED, URBAN -> Blocks.STONE.defaultBlockState();
			default -> Blocks.DIRT.defaultBlockState();
		};
	}

	private static double hash01(final int x, final int z) {
		long h = 0x9E3779B97F4A7C15L;
		h ^= (long) x * 0xBF58476D1CE4E5B9L;
		h ^= (long) z * 0x94D049BB133111EBL;
		h ^= (h >>> 30);
		h *= 0xBF58476D1CE4E5B9L;
		h ^= (h >>> 27);
		h *= 0x94D049BB133111EBL;
		h ^= (h >>> 31);
		return (h >>> 11) * 0x1.0p-53;
	}

	private static double satlasClassToCanopyStrength(final int satlasClass) {
		return SatlasTreeCoverSampler.classToCanopyStrength(satlasClass);
	}

	private static double resolveCanopyStrength(
			final double satlasCanopyStrength,
			final double satelliteVegetationStrength) {
		if (satlasCanopyStrength >= 0.0D) {
			return satlasCanopyStrength;
		}
		if (satelliteVegetationStrength <= 0.0D) {
			return 0.0D;
		}
		return Mth.clamp(satelliteVegetationStrength * 0.55D, 0.0D, 0.85D);
	}

	private static CanopyColumn resolveMinecraftStyleCanopyColumn(
			final String biomeId,
			final int worldX,
			final int worldZ,
			final int lodBlockSpan,
			final double canopyStrength) {
		if (canopyStrength <= 0.02D) {
			return null;
		}

		final int gridSize = minecraftTreeGridSize(lodBlockSpan);
		final int cellX = Math.floorDiv(worldX, gridSize);
		final int cellZ = Math.floorDiv(worldZ, gridSize);
		final int radiusBias = canopyStrength >= 0.75D ? 1 : 0;

		int bestDist = Integer.MAX_VALUE;
		int bestRadius = 0;
		int bestHash = 0;

		for (int dz = -1; dz <= 1; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				final int testCellX = cellX + dx;
				final int testCellZ = cellZ + dz;
				final int centerHash = mixCanopyHash(testCellX, testCellZ, V2_TREE_CENTER_SALT);
				final int roll = (centerHash >>> 24) & 0xFF;
				final int threshold = Mth.clamp((int) Math.round(canopyStrength * 255.0D * 0.85D), 0, 255);
				if (roll > threshold) {
					continue;
				}

				final int centerX = testCellX * gridSize + Math.floorMod(centerHash, gridSize);
				final int centerZ = testCellZ * gridSize + Math.floorMod(centerHash >>> 8, gridSize);
				final int dist = Math.max(Math.abs(worldX - centerX), Math.abs(worldZ - centerZ));

				int radius = 2 + radiusBias;
				if (((centerHash >>> 19) & 0x3) == 0) {
					radius++;
				}
				radius = Math.max(1, Math.min(radius, 4));

				if (dist <= radius && dist < bestDist) {
					bestDist = dist;
					bestRadius = radius;
					bestHash = centerHash;
				}
			}
		}

		if (bestDist == Integer.MAX_VALUE) {
			return null;
		}

		final TreePalette palette = treePaletteFromBiomeId(biomeId, bestHash);
		int trunkHeight = 4 + ((bestHash >>> 16) & 1);
		if (canopyStrength >= 0.85D && ((bestHash >>> 21) & 1) == 1) {
			trunkHeight++;
		}
		if (lodBlockSpan >= 8) {
			trunkHeight = Math.max(3, trunkHeight - 1);
		}

		final int crownBase = Math.max(1, trunkHeight - 2);

		if (bestDist == 0) {
			int leavesHeight = 2 + ((bestHash >>> 20) & 1);
			if (lodBlockSpan >= 4) {
				leavesHeight = Math.max(1, leavesHeight - 1);
			}
			return new CanopyColumn(trunkHeight, 0, leavesHeight, palette.leaves(), palette.log());
		}

		int leafLift = crownBase;
		if (bestDist >= bestRadius) {
			leafLift = Math.max(1, crownBase - 1);
		}

		int leavesHeight = bestDist >= bestRadius ? 1 : 2;
		if (lodBlockSpan >= 8) {
			leavesHeight = 1;
		}

		return new CanopyColumn(0, leafLift, leavesHeight, palette.leaves(), null);
	}

	private static int minecraftTreeGridSize(final int lodBlockSpan) {
		if (lodBlockSpan <= 1) {
			return 6;
		}
		if (lodBlockSpan <= 2) {
			return 7;
		}
		if (lodBlockSpan <= 4) {
			return 8;
		}
		if (lodBlockSpan <= 8) {
			return 10;
		}
		return 12;
	}

	private static int mixCanopyHash(final int x, final int z, final int seed) {
		int h = seed;
		h ^= x * 0x1F1F1F1F;
		h = Integer.rotateLeft(h, 13);
		h ^= z * 0x45D9F3B;
		h ^= (h >>> 16);
		h *= 0x7FEB352D;
		h ^= (h >>> 15);
		h *= 0x846CA68B;
		h ^= (h >>> 16);
		return h;
	}

	private static TreePalette treePaletteFromBiomeId(final String biomeId, final int hash) {
		final String biome = biomeId == null ? "" : biomeId.toLowerCase();

		if (biome.contains("mangrove") || biome.contains("swamp")) {
			return new TreePalette(Blocks.MANGROVE_LEAVES.defaultBlockState(), Blocks.MANGROVE_LOG.defaultBlockState());
		}
		if (biome.contains("taiga") || biome.contains("snowy")) {
			return new TreePalette(Blocks.SPRUCE_LEAVES.defaultBlockState(), Blocks.SPRUCE_LOG.defaultBlockState());
		}
		if (biome.contains("jungle")) {
			return new TreePalette(Blocks.JUNGLE_LEAVES.defaultBlockState(), Blocks.JUNGLE_LOG.defaultBlockState());
		}
		if (biome.contains("savanna")) {
			return new TreePalette(Blocks.ACACIA_LEAVES.defaultBlockState(), Blocks.ACACIA_LOG.defaultBlockState());
		}
		if (biome.contains("cherry")) {
			return new TreePalette(Blocks.CHERRY_LEAVES.defaultBlockState(), Blocks.CHERRY_LOG.defaultBlockState());
		}
		if (biome.contains("dark_forest")) {
			return new TreePalette(Blocks.DARK_OAK_LEAVES.defaultBlockState(), Blocks.DARK_OAK_LOG.defaultBlockState());
		}
		if (((hash >>> 27) & 0x3) == 0) {
			return new TreePalette(Blocks.BIRCH_LEAVES.defaultBlockState(), Blocks.BIRCH_LOG.defaultBlockState());
		}
		return new TreePalette(Blocks.OAK_LEAVES.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState());
	}

	@Override
	public EDhApiWorldGeneratorReturnType getReturnType() {
		return v1Fallback.getReturnType();
	}

	@Override
	public boolean runApiValidation() {
		return v1Fallback.runApiValidation();
	}

	@Override
	public void close() {
		v1Fallback.close();
	}

	private record SatelliteSurface(BlockState blockState, double vegetationStrength, boolean forceExposeRock) {
	}

	private record TreePalette(BlockState leaves, BlockState log) {
	}

	private record BiomeSample(String biomeId, @Nullable IDhApiBiomeWrapper wrapper) {
	}

	private static class VanillaSurfaceLodOutput {
		private final IDhApiFullDataSource output;
		private final WrapperCache wrappers;
		private final int minY;
		private final int absoluteTop;

		private final List<DhApiTerrainDataPoint> columnDataPoints = new ArrayList<>();
		private int columnX;
		private int columnZ;
		@Nullable
		private IDhApiBiomeWrapper columnBiome;
		private int lastLayerTop;

		private VanillaSurfaceLodOutput(
				final IDhApiFullDataSource output,
				final WrapperCache wrappers,
				final int minY,
				final int absoluteTop) {
			this.output = output;
			this.wrappers = wrappers;
			this.minY = minY;
			this.absoluteTop = absoluteTop;
		}

		private void beginColumn(final int x, final int z, final IDhApiBiomeWrapper biome) {
			columnX = x;
			columnZ = z;
			columnBiome = biome;
			lastLayerTop = 0;
		}

		private void addLayerUpTo(final int inclusiveTopY, final BlockState blockState) {
			final int layerTop = Mth.clamp(inclusiveTopY - minY + 1, 0, absoluteTop);
			if (layerTop <= lastLayerTop) {
				return;
			}

			final IDhApiBlockStateWrapper block = wrappers.getBlockState(blockState);
			final IDhApiBiomeWrapper biome = Objects.requireNonNull(columnBiome);
			columnDataPoints.add(DhApiTerrainDataPoint.create(
					(byte) 0,
					0,
					15,
					lastLayerTop,
					layerTop,
					block,
					biome));
			lastLayerTop = layerTop;
		}

		private void endColumn() {
			if (lastLayerTop < absoluteTop) {
				final IDhApiBiomeWrapper biome = Objects.requireNonNull(columnBiome);
				columnDataPoints.add(DhApiTerrainDataPoint.create(
						(byte) 0,
						0,
						15,
						lastLayerTop,
						absoluteTop,
						wrappers.airBlock(),
						biome));
			}

			output.setApiDataPointColumn(columnX, columnZ, columnDataPoints);
			columnDataPoints.clear();
		}

		private void addCanopy(final TellusLodGenerator.CanopyColumn canopyColumn) {
			if (lastLayerTop >= absoluteTop) {
				return;
			}

			int layerTop = lastLayerTop;
			final IDhApiBiomeWrapper biome = Objects.requireNonNull(columnBiome);

			if (canopyColumn.trunkHeight > 0 && canopyColumn.trunkBlock != null) {
				final int trunkTop = Math.min(absoluteTop, layerTop + canopyColumn.trunkHeight);
				if (trunkTop > layerTop) {
					final IDhApiBlockStateWrapper trunkBlock = wrappers.getBlockState(canopyColumn.trunkBlock);
					columnDataPoints.add(
							DhApiTerrainDataPoint.create((byte) 0, 0, TellusLodGenerator.CANOPY_MAX_LIGHT, layerTop,
									trunkTop, trunkBlock,
									biome));
					layerTop = trunkTop;
				}
			}

			if (canopyColumn.leafLift > 0) {
				final int liftTop = Math.min(absoluteTop, layerTop + canopyColumn.leafLift);
				if (liftTop > layerTop) {
					columnDataPoints.add(
							DhApiTerrainDataPoint.create((byte) 0, 0, TellusLodGenerator.CANOPY_MAX_LIGHT, layerTop,
									liftTop,
									wrappers.airBlock(), biome));
					layerTop = liftTop;
				}
			}

			if (canopyColumn.leavesHeight > 0 && canopyColumn.leavesBlock != null) {
				final int canopyTop = Math.min(absoluteTop, layerTop + canopyColumn.leavesHeight);
				if (canopyTop > layerTop) {
					final IDhApiBlockStateWrapper canopyBlock = wrappers.getBlockState(canopyColumn.leavesBlock);
					columnDataPoints.add(
							DhApiTerrainDataPoint.create((byte) 0, 0, TellusLodGenerator.CANOPY_MAX_LIGHT, layerTop,
									canopyTop, canopyBlock,
									biome));
					layerTop = canopyTop;
				}
			}

			lastLayerTop = layerTop;
		}
	}

	private static class WrapperCache {
		private final IDhApiLevelWrapper levelWrapper;
		private final IDhApiBlockStateWrapper airBlock;
		private final Map<BlockState, IDhApiBlockStateWrapper> blockStates = new IdentityHashMap<>();
		private final Map<String, IDhApiBiomeWrapper> biomeCache = new HashMap<>();

		private WrapperCache(final IDhApiLevelWrapper levelWrapper) {
			this.levelWrapper = levelWrapper;
			this.airBlock = DhApi.Delayed.wrapperFactory.getAirBlockStateWrapper();
		}

		private IDhApiBlockStateWrapper airBlock() {
			return airBlock;
		}

		private IDhApiBlockStateWrapper getBlockState(final BlockState blockState) {
			return blockStates.computeIfAbsent(blockState, this::lookupBlockState);
		}

		private IDhApiBlockStateWrapper lookupBlockState(final BlockState blockState) {
			return DhApi.Delayed.wrapperFactory.getBlockStateWrapper(new BlockState[] { blockState }, levelWrapper);
		}

		private IDhApiBiomeWrapper getBiome(final Holder<Biome> biome) {
			return biome.unwrapKey().map(key -> getBiome(key.identifier().toString())).orElse(null);
		}

		private IDhApiBiomeWrapper getBiome(final String biomeId) {
			return biomeCache.computeIfAbsent(biomeId, this::lookupBiomeById);
		}

		@Nullable
		private IDhApiBiomeWrapper lookupBiomeById(final String biomeId) {
			try {
				return DhApi.Delayed.wrapperFactory.getBiomeWrapper(biomeId, levelWrapper);
			} catch (final IOException ignored) {
				return null;
			}
		}
	}

	private enum V2Policy {
		LEVEL_3_HIGH_RES,
		LEVEL_4_SENTINEL_10M,
		LEVEL_5_SENTINEL_10M_VEG,
		LEVEL_6_30M,
		LEVEL_7_DOWNSAMPLED,
		LEVEL_8_MODIS;

		private static V2Policy forDetailLevel(final int detailLevel) {
			return switch (detailLevel) {
				case 0, 1, 2, 3 -> LEVEL_3_HIGH_RES;
				case 4 -> LEVEL_4_SENTINEL_10M;
				case 5 -> LEVEL_5_SENTINEL_10M_VEG;
				case 6 -> LEVEL_6_30M;
				case 7 -> LEVEL_7_DOWNSAMPLED;
				default -> LEVEL_8_MODIS;
			};
		}
	}
}
