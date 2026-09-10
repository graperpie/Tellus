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
import com.yucareux.tellus.world.data.osm.OsmBuildingFeature;
import com.yucareux.tellus.world.data.osm.OsmQueryMode;
import com.yucareux.tellus.world.data.osm.RoadFeature;
import com.yucareux.tellus.world.data.satellite.SatelliteTileSampler;
import com.yucareux.tellus.world.data.snow.SnowLineGrid;
import com.yucareux.tellus.world.data.satellite.TreeCanopyDetector;
import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import com.yucareux.tellus.worldgen.EarthCoordinateShift;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import com.yucareux.tellus.worldgen.EarthProjection;
import com.yucareux.tellus.worldgen.TellusWorldgenSources;
import com.yucareux.tellus.worldgen.building.BuildingBlueprint;
import com.yucareux.tellus.worldgen.building.BuildingProfile;
import com.yucareux.tellus.worldgen.building.TellusBuildingBlueprints;
import com.yucareux.tellus.worldgen.building.TellusBuildingLighting;
import com.yucareux.tellus.worldgen.building.TellusBuildingMaterials;
import com.yucareux.tellus.worldgen.building.TellusBuildingProfiles;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

//this code is ass, but it gets the job done lmao so no complaints

public final class LegacyLodGeneratorV2 implements IDhApiWorldGenerator {
	private static final int PROFILE_LOG_EVERY_CHUNKS = 32;
	private static final AtomicInteger PROFILE_CHUNK_COUNT = new AtomicInteger();
	private static final AtomicLong PROFILE_TOTAL_NS = new AtomicLong();
	private static final AtomicLong PROFILE_PREFETCH_NS = new AtomicLong();
	private static final AtomicLong PROFILE_BIOME_NS = new AtomicLong();
	private static final AtomicLong PROFILE_ELEVATION_NS = new AtomicLong();
	private static final AtomicLong PROFILE_SATELLITE_NS = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_NS = new AtomicLong();
	private static final AtomicLong PROFILE_PREFETCH_SAMPLES = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_SAMPLES = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_KNOWN_COLUMNS = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_ROAD_COLUMNS = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_BUILDING_COLUMNS = new AtomicLong();
	private static final AtomicLong PROFILE_OVERTURE_ANY_COLUMNS = new AtomicLong();
	private static final AtomicLong PROFILE_BIOME_SAMPLES = new AtomicLong();
	private static final AtomicLong PROFILE_CANOPY_COLUMNS = new AtomicLong();
	private static final int V2_TREE_CENTER_SALT = 0x4F3A2C17;

	private final LegacyLodGenerator v1Fallback;
	private final IDhApiLevelWrapper levelWrapper;
	private final EarthLayers earthLayers;
	private final Projection projection;
	private final EarthGeneratorSettings settings;
	private final EarthChunkGenerator generator;
	private final SatelliteTileSampler satelliteSampler;
	private final SnowLineGrid snowLineGrid;
	private final ThreadLocal<WrapperCache> wrapperCache;
	private final int spawnOriginOffsetX;
	private final int spawnOriginOffsetZ;

	public LegacyLodGeneratorV2(final IDhApiLevelWrapper levelWrapper, final EarthChunkGenerator generator) {
		this.levelWrapper = levelWrapper;
		this.v1Fallback = new LegacyLodGenerator(levelWrapper, generator);
		this.generator = generator;
		this.settings = generator.settings();
		this.projection = new Equirectangular(settings.worldScale());
		this.wrapperCache = ThreadLocal.withInitial(() -> new WrapperCache(levelWrapper));
		this.spawnOriginOffsetX = EarthCoordinateShift.spawnOffsetX(this.settings);
		this.spawnOriginOffsetZ = EarthCoordinateShift.spawnOffsetZ(this.settings);
		this.snowLineGrid = new SnowLineGrid();

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

		final EnumRaster<LegacyCover> landCover = earth.get().landCover();
		final int lodBlockSpan = 1 << Math.max(0, detailLevel);
		final int halfSpan = Math.max(1, lodBlockSpan >> 1);
		final int detail = Byte.toUnsignedInt(detailLevel);
		final ShortRaster elevation = earth.get().elevation();
		final int seaLevel = settings.resolveSeaLevel();
		final float heightScale = (float) (settings.terrestrialHeightScale() / projection.idealMetersPerBlock());
		final IDhApiBiomeWrapper defaultBiomeWrapper = wrappers.getBiome("minecraft:plains");
		final int satelliteStrideColumns = satelliteSampleStrideColumns(detailLevel, policy);
		final int overtureStrideColumns = overtureSampleStrideColumns(detailLevel, policy);
		final int biomeStrideColumns = biomeSampleStrideColumns(detailLevel, policy);
		final boolean overtureStreamingActive = detail <= 6;
		final boolean roadsActive = overtureStreamingActive && shouldRenderDhRoads(detail);
		final boolean buildingsActive = overtureStreamingActive && shouldRenderDhBuildings(detail);
		final int overtureQueryZoom = detailLevelOvertureZoom(detailLevel);
		final OsmQueryMode osmQueryMode = settings.distantHorizonsOsmNonBlockingFetch() ? OsmQueryMode.NON_BLOCKING : OsmQueryMode.BLOCKING;
		final Map<Long, Integer> satelliteRgbCache = new HashMap<>();
		final Map<Long, BiomeSample> biomeSampleCache = new HashMap<>();
		final int area = elevation.width() * elevation.height();
		final int[] surfaceYs = new int[area];
		final int[] elevationValues = new int[area];
		final BiomeSample[] biomeSamples = new BiomeSample[area];
		int biomeSampleCount = 0;
		int canopyColumns = 0;
		int overtureKnownColumns = 0;
		int overtureRoadColumns = 0;
		int overtureBuildingColumns = 0;
		int overtureAnyColumns = 0;

		for (int z = 0; z < elevation.height(); z++) {
			for (int x = 0; x < elevation.width(); x++) {
				final int index = z * elevation.width() + x;
				final LegacyCover cover = landCover.get(x, z);
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
				biomeSamples[index] = biomeSample;
				biomeNs += System.nanoTime() - biomeStartNs;

				final long elevationStartNs = System.nanoTime();
				final double selectedElevation = sampleElevationBicubic(elevation, x + 0.5D, z + 0.5D);
				final int elevationValue = Mth.floor(selectedElevation);
				elevationValues[index] = elevationValue;
				surfaceYs[index] = Mth.clamp(Mth.floor((elevationValue * heightScale) + settings.heightOffset()), minY,
						maxY);
				elevationNs += System.nanoTime() - elevationStartNs;
			}
		}

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
				overtureStrideColumns,
				overtureQueryZoom,
				roadsActive,
				buildingsActive);
		final int prefetchSamples = prefetchSatelliteSamples + prefetchOvertureSamples;
		prefetchNs += System.nanoTime() - prefetchStartNs;
		final long overtureBuildStartNs = System.nanoTime();
		final OverlayMask overlayMask = buildOsmOverlayMask(
				x0,
				z0,
				elevation.width(),
				elevation.height(),
				lodBlockSpan,
				halfSpan,
				roadsActive,
				buildingsActive,
				overtureQueryZoom,
				osmQueryMode);
		overtureNs += System.nanoTime() - overtureBuildStartNs;
		final BuildingRenderMask buildingRenderMask = buildingsActive
				? buildRenderedBuildings(
						x0,
						z0,
						elevation.width(),
						elevation.height(),
						lodBlockSpan,
						halfSpan,
						surfaceYs,
						biomeSamples,
						overtureQueryZoom,
						osmQueryMode)
				: BuildingRenderMask.empty(elevation.width(), elevation.height());

		for (int z = 0; z < elevation.height(); z++) {
			for (int x = 0; x < elevation.width(); x++) {
				final int index = z * elevation.width() + x;
				final int worldX = x0 + (x * lodBlockSpan) + halfSpan;
				final int worldZ = z0 + (z * lodBlockSpan) + halfSpan;
				final BiomeSample biomeSample = biomeSamples[index];
				final String biomeId = biomeSample.biomeId();
				final IDhApiBiomeWrapper biomeWrapper = biomeSample.wrapper();
				lodOutput.beginColumn(x, z, biomeWrapper != null ? biomeWrapper : defaultBiomeWrapper);

				final int surfaceY = surfaceYs[index];
				final LegacyCover cover = landCover.get(x, z);
				final double snowLat = projection.lat(worldX + this.spawnOriginOffsetX, worldZ + this.spawnOriginOffsetZ);
				final double snowLon = projection.lon(worldX + this.spawnOriginOffsetX, worldZ + this.spawnOriginOffsetZ);
				final boolean aboveSnowLine = elevationValues[index] >= this.snowLineGrid.getSnowLineElevation(snowLat, snowLon);

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
					final OverlaySample overlaySample = overlayMask.sampleAt(x, z);
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
					boolean renderedBuildingColumn = false;
					if (overlaySample.building()) {
						int buildingFeatureIndex = buildingRenderMask.featureIndexMask()[index];
						if (buildingFeatureIndex >= 0) {
							// Use opaque terrain as the base under buildings so DH columns stay solid.
							lodOutput.addLayerUpTo(surfaceY, getLodUnderwaterMaterial(cover));
							RenderedBuildingFeature renderedBuilding = buildingRenderMask.renderedFeatures().get(buildingFeatureIndex);
							appendRenderedBuildingColumn(
									lodOutput,
									renderedBuilding,
									buildingRenderMask.buildingBoundaryDistanceMask()[index],
									surfaceY,
									worldX,
									worldZ,
									lodBlockSpan,
									biomeWrapper != null ? biomeWrapper : defaultBiomeWrapper);
							renderedBuildingColumn = true;
						}
					}

					if (!renderedBuildingColumn) {
						BlockState surfaceMaterial = satelliteSurface.blockState();
						if (overlaySample.building()) {
							surfaceMaterial = Blocks.GRAY_CONCRETE.defaultBlockState();
						} else if (overlaySample.road()) {
							surfaceMaterial = Blocks.GRAY_CONCRETE.defaultBlockState();
						}
						if (aboveSnowLine && !satelliteSurface.forceExposeRock() && !overlaySample.hasAnyOverlay()) {
							surfaceMaterial = Blocks.SNOW_BLOCK.defaultBlockState();
						}
						lodOutput.addLayerUpTo(surfaceY, surfaceMaterial);
						if (overlaySample.building()) {
							int buildingTopY = Mth.clamp(
								surfaceY + Math.max(3, overlaySample.buildingHeightBlocks()),
								minY,
								maxY);
							if (buildingTopY > surfaceY) {
								byte buildingLight = overlaySample.buildingEdge() ? (byte) 15 : 0;
								lodOutput.addLayerUpTo(buildingTopY, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), buildingLight);
							}
						}

						if (!overlaySample.building() && shouldPlaceRoadLamp(worldX, worldZ, overlayMask, x, z, settings.worldScale())) {
							appendRoadLightColumn(lodOutput, surfaceY, settings.worldScale());
						}

						final double canopyStrength = TreeCanopyDetector.canopyStrength(sampledRgb);
						if (canopyStrength > 0.0D
								&& !aboveSnowLine
								&& !satelliteSurface.forceExposeRock()
								&& !overlaySample.hasAnyOverlay()) {
							final TreeDensityBand treeDensityBand = mapCanopyBand(TreeCanopyDetector.bandForStrength(canopyStrength));
							final CanopyColumn canopyColumn = resolveTreeDensityCanopyColumn(biomeId, treeDensityBand, worldX, worldZ, lodBlockSpan);
							if (canopyColumn != null) {
								lodOutput.addCanopy(canopyColumn);
								canopyColumns++;
							}
						}
					}
				}

				lodOutput.endColumn();
			}
		}
		biomeSampleCount = biomeSampleCache.size();

		final long totalNs = System.nanoTime() - chunkStartNs;
		PROFILE_TOTAL_NS.addAndGet(totalNs);
		PROFILE_PREFETCH_NS.addAndGet(prefetchNs);
		PROFILE_BIOME_NS.addAndGet(biomeNs);
		PROFILE_ELEVATION_NS.addAndGet(elevationNs);
		PROFILE_SATELLITE_NS.addAndGet(satelliteNs);
		PROFILE_OVERTURE_NS.addAndGet(overtureNs);
		PROFILE_PREFETCH_SAMPLES.addAndGet(prefetchSamples);
		PROFILE_OVERTURE_SAMPLES.addAndGet(prefetchOvertureSamples + overlayMask.sampleCount());
		PROFILE_OVERTURE_KNOWN_COLUMNS.addAndGet(overtureKnownColumns);
		PROFILE_OVERTURE_ROAD_COLUMNS.addAndGet(overtureRoadColumns);
		PROFILE_OVERTURE_BUILDING_COLUMNS.addAndGet(overtureBuildingColumns);
		PROFILE_OVERTURE_ANY_COLUMNS.addAndGet(overtureAnyColumns);
		PROFILE_BIOME_SAMPLES.addAndGet(biomeSampleCount);
		PROFILE_CANOPY_COLUMNS.addAndGet(canopyColumns);
		final int chunks = PROFILE_CHUNK_COUNT.incrementAndGet();

		if (chunks % PROFILE_LOG_EVERY_CHUNKS == 0) {
			final double avgTotalMs = PROFILE_TOTAL_NS.get() / 1_000_000.0D / chunks;
			final double avgPrefetchMs = PROFILE_PREFETCH_NS.get() / 1_000_000.0D / chunks;
			final double avgBiomeMs = PROFILE_BIOME_NS.get() / 1_000_000.0D / chunks;
			final double avgElevationMs = PROFILE_ELEVATION_NS.get() / 1_000_000.0D / chunks;
			final double avgSatelliteMs = PROFILE_SATELLITE_NS.get() / 1_000_000.0D / chunks;
			final double avgOvertureMs = PROFILE_OVERTURE_NS.get() / 1_000_000.0D / chunks;
			final double avgPrefetchSamples = PROFILE_PREFETCH_SAMPLES.get() / (double) chunks;
			final double avgOvertureSamples = PROFILE_OVERTURE_SAMPLES.get() / (double) chunks;
			final double avgOvertureKnownColumns = PROFILE_OVERTURE_KNOWN_COLUMNS.get() / (double) chunks;
			final double avgOvertureRoadColumns = PROFILE_OVERTURE_ROAD_COLUMNS.get() / (double) chunks;
			final double avgOvertureBuildingColumns = PROFILE_OVERTURE_BUILDING_COLUMNS.get() / (double) chunks;
			final double avgOvertureAnyColumns = PROFILE_OVERTURE_ANY_COLUMNS.get() / (double) chunks;
			final double avgBiomeSamples = PROFILE_BIOME_SAMPLES.get() / (double) chunks;
			final double avgCanopyColumns = PROFILE_CANOPY_COLUMNS.get() / (double) chunks;
			Tellus.LOGGER.info(
					"Tellus V2 LOD profile avg ({} chunks): total={}ms prefetch={}ms biome={}ms elevation={}ms satellite={}ms overture={}ms prefetchSamples={} overtureSamples={} overtureKnown={} overtureAny={} overtureRoad={} overtureBuildings={} canopyColumns={} biomeSamples={}",
					chunks,
					String.format("%.2f", avgTotalMs),
					String.format("%.2f", avgPrefetchMs),
					String.format("%.2f", avgBiomeMs),
					String.format("%.2f", avgElevationMs),
					String.format("%.2f", avgSatelliteMs),
					String.format("%.2f", avgOvertureMs),
					String.format("%.1f", avgPrefetchSamples),
					String.format("%.1f", avgOvertureSamples),
					String.format("%.1f", avgOvertureKnownColumns),
					String.format("%.1f", avgOvertureAnyColumns),
					String.format("%.1f", avgOvertureRoadColumns),
					String.format("%.1f", avgOvertureBuildingColumns),
					String.format("%.1f", avgCanopyColumns),
					String.format("%.1f", avgBiomeSamples));
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
			final int strideColumns,
			final int queryZoom,
			final boolean includeRoads,
			final boolean includeBuildings) {
		if (!includeRoads && !includeBuildings) {
			return 0;
		}

		int samples = 0;
		final Set<Long> prefetchedChunks = new HashSet<>();
		for (int z = 0; z < height; z += strideColumns) {
			for (int x = 0; x < width; x += strideColumns) {
				final int worldX = x0 + (x * lodBlockSpan) + halfSpan;
				final int worldZ = z0 + (z * lodBlockSpan) + halfSpan;
				final int chunkX = SectionPos.blockToSectionCoord(worldX);
				final int chunkZ = SectionPos.blockToSectionCoord(worldZ);
				final long chunkKey = (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
				if (prefetchedChunks.add(chunkKey)) {
					if (includeRoads) {
						TellusWorldgenSources.osmRoads().prefetchTiles(worldX, worldZ, settings.worldScale(), 1);
					}
					if (includeBuildings) {
						TellusWorldgenSources.osmBuildings().prefetchTiles(worldX, worldZ, settings.worldScale(), 1);
					}
					samples++;
				}
			}
		}
		return samples;
	}

	private boolean shouldRenderDhRoads(final int detailLevel) {
		return settings.enableRoads()
				&& settings.distantHorizonsOsmFeatures()
				&& settings.worldScale() > 0.0
				&& settings.worldScale() <= 15.0
				&& detailLevel <= settings.distantHorizonsOsmRoadMaxDetail();
	}

	private boolean shouldRenderDhBuildings(final int detailLevel) {
		return settings.enableBuildings()
				&& settings.distantHorizonsOsmFeatures()
				&& settings.worldScale() > 0.0
				&& settings.worldScale() <= 15.0
				&& detailLevel <= settings.distantHorizonsOsmBuildingMaxDetail();
	}

	private OverlayMask buildOsmOverlayMask(
			final int x0,
			final int z0,
			final int width,
			final int height,
			final int lodBlockSpan,
			final int halfSpan,
			final boolean roadsActive,
			final boolean buildingsActive,
			final int queryZoom,
			final OsmQueryMode mode) {
		if (width <= 0 || height <= 0 || (!roadsActive && !buildingsActive)) {
			return OverlayMask.empty(width, height);
		}

		final int[] worldXs = new int[width];
		final int[] worldZs = new int[height];
		for (int x = 0; x < width; x++) {
			worldXs[x] = x0 + (x * lodBlockSpan) + halfSpan;
		}
		for (int z = 0; z < height; z++) {
			worldZs[z] = z0 + (z * lodBlockSpan) + halfSpan;
		}

		final int minWorldX = Math.min(worldXs[0], worldXs[width - 1]);
		final int maxWorldX = Math.max(worldXs[0], worldXs[width - 1]);
		final int minWorldZ = Math.min(worldZs[0], worldZs[height - 1]);
		final int maxWorldZ = Math.max(worldZs[0], worldZs[height - 1]);
		final int area = width * height;
		final boolean[] roadMask = new boolean[area];
		final boolean[] buildingMask = new boolean[area];
		final boolean[] buildingEdgeMask = new boolean[area];
		final int[] buildingHeightMask = new int[area];
		final OsmQueryMode queryMode = mode == null ? OsmQueryMode.BLOCKING : mode;
		int sampleCount = 0;
		boolean roadsKnown = !roadsActive;
		boolean buildingsKnown = !buildingsActive;

		if (roadsActive) {
			sampleCount++;
			EarthChunkGenerator.OsmRoadQueryResult roads = generator.fetchOsmRoadsForAreaDetailed(
					minWorldX,
					minWorldZ,
					maxWorldX,
					maxWorldZ,
					64,
					queryMode);
			if (queryMode == OsmQueryMode.NON_BLOCKING && roads.hadCacheMisses() && roads.features().isEmpty()) {
				roads = generator.fetchOsmRoadsForAreaDetailed(
						minWorldX,
						minWorldZ,
						maxWorldX,
						maxWorldZ,
						64,
						OsmQueryMode.BLOCKING);
			}
			roadsKnown = !roads.hadCacheMisses();
			if (!roads.features().isEmpty()) {
				rasterizeRoadCoverage(
						roads.features(),
						worldXs,
						worldZs,
						width,
						height,
						lodBlockSpan,
						this.settings.worldScale(),
						roadMask);
			}
		}

		if (buildingsActive) {
			sampleCount++;
			int marginBlocks = Math.max(8, lodBlockSpan);
			EarthChunkGenerator.OsmBuildingQueryResult buildings = generator.fetchOsmBuildingsForAreaDetailed(
					minWorldX,
					minWorldZ,
					maxWorldX,
					maxWorldZ,
					marginBlocks,
					queryMode);
			if (queryMode == OsmQueryMode.NON_BLOCKING && buildings.hadCacheMisses() && buildings.features().isEmpty()) {
				buildings = generator.fetchOsmBuildingsForAreaDetailed(
						minWorldX,
						minWorldZ,
						maxWorldX,
						maxWorldZ,
						marginBlocks,
						OsmQueryMode.BLOCKING);
			}
			buildingsKnown = !buildings.hadCacheMisses();
			if (!buildings.features().isEmpty()) {
				rasterizeBuildingCoverage(
						buildings.features(),
						worldXs,
						worldZs,
						width,
						height,
						lodBlockSpan,
						this.settings.worldScale(),
						buildingMask,
						buildingHeightMask);
			}
		}

		for (int z = 0; z < height; z++) {
			for (int x = 0; x < width; x++) {
				final int index = z * width + x;
				if (!buildingMask[index]) {
					continue;
				}

				boolean edge = x == 0 || z == 0 || x == width - 1 || z == height - 1;
				if (!edge) {
					edge = !buildingMask[index - 1] || !buildingMask[index + 1] || !buildingMask[index - width] || !buildingMask[index + width];
				}

				buildingEdgeMask[index] = edge;
			}
		}

		return new OverlayMask(roadsKnown && buildingsKnown, roadMask, buildingMask, buildingEdgeMask, buildingHeightMask, width, height, sampleCount);
	}

	private BuildingRenderMask buildRenderedBuildings(
			final int x0,
			final int z0,
			final int width,
			final int height,
			final int lodBlockSpan,
			final int halfSpan,
			final int[] surfaceYs,
			final BiomeSample[] biomeSamples,
			final int queryZoom,
			final OsmQueryMode mode) {
		if (width <= 0 || height <= 0) {
			return BuildingRenderMask.empty(width, height);
		}

		final int[] worldXs = new int[width];
		final int[] worldZs = new int[height];
		for (int x = 0; x < width; x++) {
			worldXs[x] = x0 + (x * lodBlockSpan) + halfSpan;
		}
		for (int z = 0; z < height; z++) {
			worldZs[z] = z0 + (z * lodBlockSpan) + halfSpan;
		}

		final int minWorldX = Math.min(worldXs[0], worldXs[width - 1]);
		final int maxWorldX = Math.max(worldXs[0], worldXs[width - 1]);
		final int minWorldZ = Math.min(worldZs[0], worldZs[height - 1]);
		final int maxWorldZ = Math.max(worldZs[0], worldZs[height - 1]);
		final OsmQueryMode queryMode = mode == null ? OsmQueryMode.BLOCKING : mode;
		final int marginBlocks = Math.max(8, lodBlockSpan);
		EarthChunkGenerator.OsmBuildingQueryResult buildings = generator.fetchOsmBuildingsForAreaDetailed(
				minWorldX,
				minWorldZ,
				maxWorldX,
				maxWorldZ,
				marginBlocks,
					queryMode);
		if (queryMode == OsmQueryMode.NON_BLOCKING && buildings.hadCacheMisses() && buildings.features().isEmpty()) {
			buildings = generator.fetchOsmBuildingsForAreaDetailed(
					minWorldX,
				minWorldZ,
				maxWorldX,
				maxWorldZ,
				marginBlocks,
					OsmQueryMode.BLOCKING);
		}
		if (buildings.features().isEmpty()) {
			return BuildingRenderMask.empty(width, height);
		}

		EarthChunkGenerator.OsmRoadQueryResult roads = generator.fetchOsmRoadsForAreaDetailed(
				minWorldX,
				minWorldZ,
				maxWorldX,
				maxWorldZ,
				64,
					queryMode);
		if (queryMode == OsmQueryMode.NON_BLOCKING && roads.hadCacheMisses() && roads.features().isEmpty()) {
			roads = generator.fetchOsmRoadsForAreaDetailed(
					minWorldX,
				minWorldZ,
				maxWorldX,
				maxWorldZ,
				64,
					OsmQueryMode.BLOCKING);
		}

		final int area = width * height;
		final int[] featureIndexMask = new int[area];
		final boolean[] buildingEdgeMask = new boolean[area];
		final int[] buildingBoundaryDistanceMask = new int[area];
		final int[] buildingHeightMask = new int[area];
		java.util.Arrays.fill(featureIndexMask, -1);

		final List<RenderedBuildingFeature> renderedFeatures = new ArrayList<>(buildings.features().size());
		final long seed = mixHash(x0, z0, 0x51A7D1B7);
		for (OsmBuildingFeature feature : buildings.features()) {
			final int centerX = Mth.clamp((int) Math.round(feature.centroidWorld(settings.worldScale())[0]), minWorldX, maxWorldX);
			final int centerZ = Mth.clamp((int) Math.round(feature.centroidWorld(settings.worldScale())[1]), minWorldZ, maxWorldZ);
			final int sampleX = Mth.clamp((centerX - x0) / lodBlockSpan, 0, width - 1);
			final int sampleZ = Mth.clamp((centerZ - z0) / lodBlockSpan, 0, height - 1);
			final int surfaceY = surfaceYs[sampleZ * width + sampleX];
			final BiomeSample biomeSample = biomeSamples[sampleZ * width + sampleX];
			final BuildingProfile resolvedProfile = TellusBuildingProfiles.resolveProfile(
					feature,
					settings.worldScale(),
					null,
					false);
			final BuildingProfile profile = normalizeLodProfileForStoryCount(feature, resolvedProfile);
			final int baseY = surfaceY;
			final int floorY = baseY + 1;
			final int roofBaseY = Math.max(
					baseY + buildingHeightBlocks(feature.heightMeters(), settings.worldScale()),
					floorY + profile.floorCount() * profile.storeyHeightBlocks());
			final int topY = roofBaseY + Math.max(profile.parapetHeight(), profile.roofRise());
			final BuildingBlueprint blueprint = TellusBuildingBlueprints.create(
					feature.buildingId() != null ? feature.buildingId() : Long.toString(feature.featureId()),
					feature,
					profile,
					seed,
					baseY,
					floorY,
					roofBaseY,
					topY,
					roads.features(),
					settings.worldScale());
			final TellusBuildingMaterials.BuildingMaterialPalette palette = TellusBuildingMaterials.resolvePalette(blueprint);
			renderedFeatures.add(new RenderedBuildingFeature(blueprint, palette, biomeSample != null ? biomeSample.wrapper() : null));
		}

		for (int featureIndex = 0; featureIndex < renderedFeatures.size(); featureIndex++) {
			final RenderedBuildingFeature renderedFeature = renderedFeatures.get(featureIndex);
			final BuildingBlueprint blueprint = renderedFeature.blueprint();
			for (int z = 0; z < height; z++) {
				final int worldZ = worldZs[z];
				final int row = z * width;
				for (int x = 0; x < width; x++) {
					final int worldX = worldXs[x];
					if (!buildings.features().get(featureIndex).containsWorld(worldX, worldZ, settings.worldScale())) {
						continue;
					}
					final int index = row + x;
					featureIndexMask[index] = featureIndex;
					final boolean edge = x == 0 || z == 0 || x == width - 1 || z == height - 1
						|| !buildings.features().get(featureIndex).containsWorld(worldXs[Math.max(0, x - 1)], worldZ, settings.worldScale())
						|| !buildings.features().get(featureIndex).containsWorld(worldXs[Math.min(width - 1, x + 1)], worldZ, settings.worldScale())
						|| !buildings.features().get(featureIndex).containsWorld(worldX, worldZs[Math.max(0, z - 1)], settings.worldScale())
						|| !buildings.features().get(featureIndex).containsWorld(worldX, worldZs[Math.min(height - 1, z + 1)], settings.worldScale());
					buildingEdgeMask[index] = edge;
					buildingHeightMask[index] = Math.max(buildingHeightMask[index], blueprint.topY() - blueprint.baseY());
				}
			}
		}

		for (int featureIndex = 0; featureIndex < renderedFeatures.size(); featureIndex++) {
			populateBoundaryDistanceForFeature(featureIndexMask, width, height, featureIndex, buildingBoundaryDistanceMask);
		}

		return new BuildingRenderMask(
				featureIndexMask,
				buildingEdgeMask,
				buildingBoundaryDistanceMask,
				buildingHeightMask,
				renderedFeatures,
				width,
				height,
				buildings.hadCacheMisses() ? 0 : 1);
	}

	private static void populateBoundaryDistanceForFeature(
			final int[] featureIndexMask,
			final int width,
			final int height,
			final int featureIndex,
			final int[] boundaryDistanceMask) {
		final int area = width * height;
		final int[] distance = new int[area];
		Arrays.fill(distance, -1);
		final ArrayDeque<Integer> queue = new ArrayDeque<>();

		for (int index = 0; index < area; index++) {
			if (featureIndexMask[index] != featureIndex) {
				continue;
			}

			final int x = index % width;
			final int z = index / width;
			final boolean boundary = x == 0
					|| z == 0
					|| x == width - 1
					|| z == height - 1
					|| featureIndexMask[index - 1] != featureIndex
					|| featureIndexMask[index + 1] != featureIndex
					|| featureIndexMask[index - width] != featureIndex
					|| featureIndexMask[index + width] != featureIndex;
			if (boundary) {
				distance[index] = 0;
				queue.add(index);
			}
		}

		while (!queue.isEmpty()) {
			final int index = queue.removeFirst();
			final int x = index % width;
			final int z = index / width;
			final int nextDistance = distance[index] + 1;

			if (x > 0) {
				propagateBoundaryDistance(featureIndexMask, featureIndex, distance, queue, index - 1, nextDistance);
			}
			if (x + 1 < width) {
				propagateBoundaryDistance(featureIndexMask, featureIndex, distance, queue, index + 1, nextDistance);
			}
			if (z > 0) {
				propagateBoundaryDistance(featureIndexMask, featureIndex, distance, queue, index - width, nextDistance);
			}
			if (z + 1 < height) {
				propagateBoundaryDistance(featureIndexMask, featureIndex, distance, queue, index + width, nextDistance);
			}
		}

		for (int index = 0; index < area; index++) {
			if (featureIndexMask[index] == featureIndex) {
				boundaryDistanceMask[index] = Math.max(0, distance[index]);
			}
		}
	}

	private static void propagateBoundaryDistance(
			final int[] featureIndexMask,
			final int featureIndex,
			final int[] distance,
			final ArrayDeque<Integer> queue,
			final int index,
			final int nextDistance) {
		if (featureIndexMask[index] == featureIndex && (distance[index] == -1 || nextDistance < distance[index])) {
			distance[index] = nextDistance;
			queue.add(index);
		}
	}

	private void appendRenderedBuildingColumn(
			final VanillaSurfaceLodOutput lodOutput,
			final RenderedBuildingFeature renderedFeature,
			final int boundaryDistance,
			final int surfaceY,
			final int worldX,
			final int worldZ,
			final int cellSize,
			final IDhApiBiomeWrapper biomeWrapper) {
		final BuildingBlueprint blueprint = renderedFeature.blueprint();
		final TellusBuildingMaterials.BuildingMaterialPalette palette = renderedFeature.palette();
		final int clampedBoundaryDistance = Math.max(0, boundaryDistance);
		final int highestFloor = blueprint.highestActiveFloor(clampedBoundaryDistance);
		int cellTopY = blueprint.roofTopY(worldX, worldZ, clampedBoundaryDistance);
		final BuildingProfile profile = blueprint.profile();
		if (profile.roofProfile() == BuildingProfile.RoofProfile.FLAT
				|| profile.roofProfile() == BuildingProfile.RoofProfile.FLAT_CROWN
				|| profile.roofProfile() == BuildingProfile.RoofProfile.FLAT_SKYLIGHT) {
			cellTopY += profile.parapetHeight();
		}

		for (int floorIndex = 0; floorIndex <= highestFloor; floorIndex++) {
			if (!blueprint.isActiveOnFloor(clampedBoundaryDistance, floorIndex)) {
				continue;
			}

			final int spanStart = blueprint.floorBottomY(floorIndex);
			final int spanEnd = Math.min(cellTopY, blueprint.floorTopY(floorIndex));
			if (spanEnd < spanStart) {
				continue;
			}

			final BlockState facadeBlock = TellusBuildingMaterials.resolveLodFacadeBlock(blueprint, palette, boundaryDistance, floorIndex);
			final byte lightLevel = TellusBuildingLighting.resolveLodFacadeLightLevel(
					blueprint,
					facadeBlock,
					palette.window(),
					clampedBoundaryDistance,
					worldX,
					worldZ,
					floorIndex,
					Math.max(1, cellSize));
			final byte outlineLight = blueprint.isFacadeCell(clampedBoundaryDistance, floorIndex) ? (byte) 6 : 0;
			lodOutput.addSpan(spanStart, spanEnd, facadeBlock, (byte) Math.max(lightLevel, outlineLight));
		}

		final int roofBaseY = Math.max(blueprint.floorY(), blueprint.roofBaseY(clampedBoundaryDistance));
		final int roofTopY = cellTopY;
		if (roofTopY >= roofBaseY) {
			final boolean roofEdge = blueprint.isFacadeCell(clampedBoundaryDistance, highestFloor);
			lodOutput.addSpan(
					roofBaseY,
					roofTopY,
					TellusBuildingMaterials.resolveLodRoofBlock(palette, roofEdge),
					roofEdge ? (byte) 5 : (byte) 0);
		}

		// Hacky interior lighting: place sea lanterns on interior outline to light interior spaces.
		if (clampedBoundaryDistance >= 2 && highestFloor > 0) {
			final int setback = Math.max(1, blueprint.setbackForFloor(0) + 1);
			final int innerMinX = blueprint.minWorldX() + setback;
			final int innerMaxX = blueprint.maxWorldX() - setback;
			final int innerMinZ = blueprint.minWorldZ() + setback;
			final int innerMaxZ = blueprint.maxWorldZ() - setback;

			// Place sea lanterns on the interior perimeter at mid-height of the building.
			final int midFloor = highestFloor / 2;
			final int interiorLightY = (blueprint.floorBottomY(midFloor) + blueprint.floorTopY(midFloor)) / 2;

			if (worldX == innerMinX || worldX == innerMaxX || worldZ == innerMinZ || worldZ == innerMaxZ) {
				if (worldX >= innerMinX && worldX <= innerMaxX && worldZ >= innerMinZ && worldZ <= innerMaxZ) {
					lodOutput.addSpan(interiorLightY, interiorLightY + 1, Blocks.SEA_LANTERN.defaultBlockState(), (byte) 15);
				}
			}
		}
	}

	private record RenderedBuildingFeature(
			BuildingBlueprint blueprint,
			TellusBuildingMaterials.BuildingMaterialPalette palette,
			@Nullable IDhApiBiomeWrapper biomeWrapper) {
	}

	private record BuildingRenderMask(
			int[] featureIndexMask,
			boolean[] buildingEdgeMask,
			int[] buildingBoundaryDistanceMask,
			int[] buildingHeightMask,
			List<RenderedBuildingFeature> renderedFeatures,
			int width,
			int height,
			int sampleCount) {
		private static BuildingRenderMask empty(final int width, final int height) {
			return new BuildingRenderMask(
					new int[Math.max(0, width * height)],
					new boolean[Math.max(0, width * height)],
					new int[Math.max(0, width * height)],
					new int[Math.max(0, width * height)],
					List.of(),
					width,
					height,
					0);
		}
	}

	private static void rasterizeRoadCoverage(
			final List<RoadFeature> roads,
			final int[] worldXs,
			final int[] worldZs,
			final int width,
			final int height,
			final int cellSize,
			final double worldScale,
			final boolean[] roadMask) {
		if (roads.isEmpty() || width <= 0 || height <= 0 || cellSize <= 0) {
			return;
		}

		final double blocksPerDegree = EarthProjection.blocksPerDegree(worldScale);
		final double minWorldX = Math.min(worldXs[0], worldXs[width - 1]);
		final double maxWorldX = Math.max(worldXs[0], worldXs[width - 1]);
		final double minWorldZ = Math.min(worldZs[0], worldZs[height - 1]);
		final double maxWorldZ = Math.max(worldZs[0], worldZs[height - 1]);

		for (RoadFeature road : roads) {
			int points = road.pointCount();
			if (points < 2) {
				continue;
			}

			int widthBlocks = roadWidthForScale(road.roadClass().baseWidth(), worldScale);
			double halfWidth = Math.max(0.5, (widthBlocks - 1) * 0.5) + cellSize * 0.5;
			double radiusSq = halfWidth * halfWidth + 1.0E-6;
			double roadMinX = EarthProjection.lonToBlockX(road.minLon(), worldScale);
			double roadMaxX = EarthProjection.lonToBlockX(road.maxLon(), worldScale);
			double roadMinZ = EarthProjection.latToBlockZ(road.maxLat(), worldScale);
			double roadMaxZ = EarthProjection.latToBlockZ(road.minLat(), worldScale);
			if (roadMaxX < minWorldX - halfWidth
					|| roadMinX > maxWorldX + halfWidth
					|| roadMaxZ < minWorldZ - halfWidth
					|| roadMinZ > maxWorldZ + halfWidth) {
				continue;
			}

			double x1 = EarthProjection.lonToBlockX(road.lonAt(0), worldScale);
			double z1 = EarthProjection.latToBlockZ(road.latAt(0), worldScale);

			for (int i = 1; i < points; i++) {
				double x2 = EarthProjection.lonToBlockX(road.lonAt(i), worldScale);
				double z2 = EarthProjection.latToBlockZ(road.latAt(i), worldScale);
				double dx = x2 - x1;
				double dz = z2 - z1;
				double lenSq = dx * dx + dz * dz;
				if (lenSq <= 1.0E-6) {
					x1 = x2;
					z1 = z2;
					continue;
				}

				int minGridX = Mth.clamp((int) Math.floor((Math.min(x1, x2) - halfWidth - minWorldX) / cellSize), 0, width - 1);
				int maxGridX = Mth.clamp((int) Math.floor((Math.max(x1, x2) + halfWidth - minWorldX) / cellSize), 0, width - 1);
				int minGridZ = Mth.clamp((int) Math.floor((Math.min(z1, z2) - halfWidth - minWorldZ) / cellSize), 0, height - 1);
				int maxGridZ = Mth.clamp((int) Math.floor((Math.max(z1, z2) + halfWidth - minWorldZ) / cellSize), 0, height - 1);

				for (int gz = minGridZ; gz <= maxGridZ; gz++) {
					double sampleZ = worldZs[gz];
					int row = gz * width;
					for (int gx = minGridX; gx <= maxGridX; gx++) {
						int index = row + gx;
						if (roadMask[index]) {
							continue;
						}

						double sampleX = worldXs[gx];
						double t = ((sampleX - x1) * dx + (sampleZ - z1) * dz) / lenSq;
						t = Mth.clamp(t, 0.0D, 1.0D);
						double px = x1 + t * dx;
						double pz = z1 + t * dz;
						double ddx = sampleX - px;
						double ddz = sampleZ - pz;
						if (ddx * ddx + ddz * ddz <= radiusSq) {
							roadMask[index] = true;
						}
					}
				}

				x1 = x2;
				z1 = z2;
			}
		}
	}

	private static void rasterizeBuildingCoverage(
			final List<OsmBuildingFeature> buildings,
			final int[] worldXs,
			final int[] worldZs,
			final int width,
			final int height,
			final int cellSize,
			final double worldScale,
			final boolean[] buildingMask,
			final int[] buildingHeightMask) {
		if (buildings.isEmpty() || width <= 0 || height <= 0 || cellSize <= 0) {
			return;
		}

		final double minWorldX = Math.min(worldXs[0], worldXs[width - 1]);
		final double maxWorldX = Math.max(worldXs[0], worldXs[width - 1]);
		final double minWorldZ = Math.min(worldZs[0], worldZs[height - 1]);
		final double maxWorldZ = Math.max(worldZs[0], worldZs[height - 1]);

		for (OsmBuildingFeature building : buildings) {
			int heightBlocks = buildingHeightBlocks(building.heightMeters(), worldScale);
			double featureMinX = building.minBlockXForScale(worldScale);
			double featureMaxX = building.maxBlockXForScale(worldScale);
			double featureMinZ = building.minBlockZ(worldScale);
			double featureMaxZ = building.maxBlockZ(worldScale);
			if (featureMaxX < minWorldX || featureMinX > maxWorldX || featureMaxZ < minWorldZ || featureMinZ > maxWorldZ) {
				continue;
			}

			int minGridX = Mth.clamp((int) Math.floor((featureMinX - minWorldX) / cellSize), 0, width - 1);
			int maxGridX = Mth.clamp((int) Math.floor((featureMaxX - minWorldX) / cellSize), 0, width - 1);
			int minGridZ = Mth.clamp((int) Math.floor((featureMinZ - minWorldZ) / cellSize), 0, height - 1);
			int maxGridZ = Mth.clamp((int) Math.floor((featureMaxZ - minWorldZ) / cellSize), 0, height - 1);

			for (int gz = minGridZ; gz <= maxGridZ; gz++) {
				double sampleZ = worldZs[gz];
				int row = gz * width;
				for (int gx = minGridX; gx <= maxGridX; gx++) {
					int index = row + gx;
					if (building.containsWorld(worldXs[gx], sampleZ, worldScale)) {
						buildingMask[index] = true;
						buildingHeightMask[index] = Math.max(buildingHeightMask[index], heightBlocks);
					}
				}
			}
		}
	}

	private static int buildingHeightBlocks(final double meters, final double worldScale) {
		if (!(worldScale > 0.0D)) {
			return 3;
		}
		return Math.max(3, (int) Math.round(meters / worldScale));
	}

	private static BuildingProfile normalizeLodProfileForStoryCount(
			final OsmBuildingFeature feature,
			final BuildingProfile profile) {
		final int inferredFloorsFromHeight = TellusBuildingProfiles.inferFloorCount(feature.heightMeters());
		final int targetFloorCount = Math.max(profile.floorCount(), inferredFloorsFromHeight);
		int setbackEveryFloors = profile.setbackEveryFloors();
		int maxSetback = profile.maxSetback();

		// Keep tower facades at full apparent story count in LOD views.
		if (profile.archetype() == BuildingProfile.Archetype.TOWER) {
			setbackEveryFloors = 0;
			maxSetback = 0;
		}

		return new BuildingProfile(
				profile.archetype(),
				profile.roofProfile(),
				profile.climateFamily(),
				targetFloorCount,
				profile.storeyHeightBlocks(),
				profile.interiorsEnabled(),
				profile.parapetHeight(),
				profile.roofRise(),
				setbackEveryFloors,
				maxSetback,
				profile.windowSpacing());
	}

	private static int roadLightSpacingBlocks(final double worldScale) {
		if (!(worldScale > 0.0D)) {
			return 40;
		}

		return Mth.clamp((int) Math.round(40.0D / worldScale), 3, 40);
	}

	private static int roadLightFenceCount(final double worldScale) {
		if (worldScale <= 3.0D) {
			return 3;
		}

		return worldScale <= 8.0D ? 2 : 1;
	}

	private static boolean shouldPlaceRoadLamp(
			final int worldX,
			final int worldZ,
			final OverlayMask overlayMask,
			final int x,
			final int z,
			final double worldScale) {
		final OverlaySample sample = overlayMask.sampleAt(x, z);
		if (sample.road() || sample.building()) {
			return false;
		}

		final boolean roadNorth = overlayMask.sampleAt(x, z - 1).road();
		final boolean roadSouth = overlayMask.sampleAt(x, z + 1).road();
		final boolean roadWest = overlayMask.sampleAt(x - 1, z).road();
		final boolean roadEast = overlayMask.sampleAt(x + 1, z).road();

		final int roadNeighborCount = (roadNorth ? 1 : 0) + (roadSouth ? 1 : 0) + (roadWest ? 1 : 0) + (roadEast ? 1 : 0);
		if (roadNeighborCount == 0) {
			return false;
		}
		if (roadNeighborCount >= 3) {
			return false;
		}

		final boolean roadNorthSouth = (roadNorth || roadSouth) && !(roadWest || roadEast);
		final boolean roadEastWest = (roadWest || roadEast) && !(roadNorth || roadSouth);

		// Skip corner-adjacent cells on diagonal turns to avoid clumped lamp knots.
		if (!roadNorthSouth && !roadEastWest && roadNeighborCount == 2) {
			return false;
		}

		final int spacingBlocks = Math.max(3, (int) Math.round(roadLightSpacingBlocks(worldScale) * 0.75D));
		if (roadNorthSouth) {
			if (Math.floorMod(worldZ, Math.max(3, spacingBlocks)) != 0) {
				return false;
			}
			return true;
		}

		if (roadEastWest) {
			if (Math.floorMod(worldX, Math.max(3, spacingBlocks)) != 0) {
				return false;
			}
			return true;
		}

		final int hash = mixHash(worldX, worldZ, 0x51A7D1B7);
		return Math.floorMod(hash, Math.max(3, spacingBlocks)) == 0;
	}

	private static int mixHash(final int x, final int z, final int salt) {
		int hash = x * 0x9E3779B9 ^ z * 0x85EBCA6B ^ salt;
		hash ^= hash >>> 16;
		hash *= 0x7FEB352D;
		hash ^= hash >>> 15;
		hash *= 0x846CA68B;
		hash ^= hash >>> 16;
		return hash;
	}

	private static void appendRoadLightColumn(final VanillaSurfaceLodOutput lodOutput, final int baseY, final double worldScale) {
		final int fenceCount = roadLightFenceCount(worldScale);
		lodOutput.addLayerUpTo(baseY + 1, Blocks.STONE_BRICK_WALL.defaultBlockState(), (byte) 0);
		lodOutput.addLayerUpTo(baseY + fenceCount + 1, Blocks.OAK_FENCE.defaultBlockState(), (byte) 0);
		lodOutput.addLayerUpTo(baseY + fenceCount + 2, Blocks.GLOWSTONE.defaultBlockState(), (byte) 15);
		lodOutput.addLayerUpTo(baseY + fenceCount + 3, Blocks.SPRUCE_TRAPDOOR.defaultBlockState(), (byte) 15);
	}

	private static int roadWidthForScale(final int baseWidth, final double worldScale) {
		double factor = roadWidthFactorForScale(worldScale);
		return Math.max(1, (int) Math.round(baseWidth * factor));
	}

	private static double roadWidthFactorForScale(final double worldScale) {
		if (!(worldScale > 0.0)) {
			return 0.25;
		}
		if (worldScale <= 1.0) {
			return 1.8;
		}
		if (worldScale <= 5.0) {
			double t = (worldScale - 1.0) / 4.0;
			return Mth.lerp(Mth.clamp(t, 0.0D, 1.0D), 1.8D, 1.0D);
		}
		if (worldScale <= 10.0) {
			double t = (worldScale - 5.0) / 5.0;
			return Mth.lerp(Mth.clamp(t, 0.0D, 1.0D), 1.0D, 0.5D);
		}
		return 0.25;
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

		if (isShrubCover(cover) && absLat >= 20.0D && absLat <= 45.0D && elevationValue < 2400) {
			return absLat <= 33.0D ? "minecraft:desert" : "minecraft:badlands";
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
			if (absLat <= 33.0D) {
				return "minecraft:desert";
			}
			if (absLat <= 40.0D) {
				return "minecraft:badlands";
			}
			return "minecraft:plains";
		}

		if (isSparseCover(cover)) {
			if (absLat <= 33.0D) {
				return "minecraft:desert";
			}
			if (absLat <= 40.0D) {
				return "minecraft:badlands";
			}
			if (absLat >= 55.0D) {
				return "minecraft:taiga";
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
					LICHENS_AND_MOSSES -> true;
			default -> false;
		};
	}

	private static boolean isSparseCover(final LegacyCover cover) {
		return switch (cover) {
			case SPARSE_VEGETATION,
					SPARSE_SHRUB,
					SPARSE_HERBACEOUS_COVER -> true;
			default -> false;
		};
	}

	private static boolean isShrubCover(final LegacyCover cover) {
		return switch (cover) {
			case SHRUBLAND,
					SHRUBLAND_EVERGREEN,
					SHRUBLAND_DECIDUOUS -> true;
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
		
		// 1. WATER DETECTION (Deep Blue â†’ Medium Blue)
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

		// 4. GREEN VEGETATION DETECTION (Dark Green â†’ Medium Green â†’ Light Green)
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
				// Bright Yellow â†’ Sand
				return new SatelliteSurface(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
			if (saturation > 0.26D && value > 0.56D && value < 0.74D) {
				// Darker Yellow â†’ Sand variant
				return new SatelliteSurface(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
		}

		// 8. BROWN DETECTION (Dirt, Podzol, Coarse Dirt)
		if (hue >= 15.0D && hue <= 38.0D && saturation > 0.16D && saturation < 0.42D) {
			if (value > 0.46D && value < 0.62D) {
				// Brown â†’ Coarse Dirt / Regular Dirt
				return new SatelliteSurface(Blocks.COARSE_DIRT.defaultBlockState(), 0.0D, false);
			}
			if (value >= 0.32D && value <= 0.48D) {
				// Darker Brown â†’ Podzol
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

		// 10. GRAY DETECTION (Light Gray â†’ Stone/Andesite, Dark Gray â†’ Basalt/Blackstone)
		// Also urban areas and asphalt
		if (saturation < 0.17D && value > 0.22D) {
			if ((isForestCover(cover) || isGrassOrCropCover(cover)) && !biomeDesert && !biomeBadlands) {
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
					return new SatelliteSurface(Blocks.GRAY_CONCRETE.defaultBlockState(), 0.0D, true);
				}
				return new SatelliteSurface(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 0.0D, true);
			}

			if (isBareCover(cover) || isSparseCover(cover)) {
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
					return new SatelliteSurface(Blocks.GRAY_CONCRETE.defaultBlockState(), 0.0D, true);
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

		if (isSparseCover(cover)) {
			final String sparseBiome = biomeId == null ? "" : biomeId.toLowerCase();
			if (sparseBiome.contains("desert")) {
				return new SatelliteSurface(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
			if (sparseBiome.contains("badlands")) {
				return new SatelliteSurface(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
			}
			return new SatelliteSurface(Blocks.COARSE_DIRT.defaultBlockState(), 0.0D, false);
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

	// varies between satelite imagery zoom levels 6-15 depending on LOD detail level and latitude
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

	private int detailLevelOvertureZoom(final byte detailLevel) {
		final int level = Byte.toUnsignedInt(detailLevel);
		final int zoom = ifLevelToOvertureZoom(level);
		return Mth.clamp(zoom, 5, 14);
	}

	private static int ifLevelToOvertureZoom(final int level) {
		if (level <= 1) {
			return 14;
		}
		if (level == 2) {
			return 13;
		}
		if (level == 3) {
			return 12;
		}
		if (level == 4) {
			return 11;
		}
		if (level == 5) {
			return 10;
		}
		if (level == 6) {
			return 9;
		}

		// Step down by one zoom level for each higher LOD detail level.
		return Math.max(5, 9 - (level - 6));
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

	private static TreeDensityBand mapCanopyBand(final TreeCanopyDetector.CanopyBand band) {
		return switch (band) {
			case NONE -> TreeDensityBand.LOW;
			case SPARSE -> TreeDensityBand.LOW;
			case OPEN -> TreeDensityBand.MEDIUM_LOW;
			case MEDIUM -> TreeDensityBand.MEDIUM;
			case DENSE -> TreeDensityBand.MEDIUM_HIGH;
			case CLOSED -> TreeDensityBand.HIGH;
		};
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

	private static CanopyColumn resolveTreeDensityCanopyColumn(
			final String biomeId,
			final TreeDensityBand treeDensityBand,
			final int worldX,
			final int worldZ,
			final int lodBlockSpan) {
		final int gridSize = minecraftTreeGridSize(lodBlockSpan);
		final int cellX = Math.floorDiv(worldX, gridSize);
		final int cellZ = Math.floorDiv(worldZ, gridSize);
		final int biomeSeed = biomeId == null ? 0 : biomeId.toLowerCase(Locale.ROOT).hashCode();
		final int chancePercent = canopyChancePercent(treeDensityBand);
		int bestDist = Integer.MAX_VALUE;
		int bestRadius = 0;
		int bestHash = 0;
		boolean bestCenter = false;

		for (int dz = -1; dz <= 1; dz++) {
			final int testCellZ = cellZ + dz;
			for (int dx = -1; dx <= 1; dx++) {
				final int testCellX = cellX + dx;
				final int centerHash = mixCanopyHash(testCellX, testCellZ, 1831565813 ^ biomeSeed ^ (treeDensityBand.ordinal() * 0x9E3779B9));
				if (hasCanopyCenter(centerHash, chancePercent)) {
					final int offsetX = centerOffset(centerHash, gridSize);
					final int offsetZ = centerOffset(centerHash >>> 8, gridSize);
					final int centerX = testCellX * gridSize + offsetX;
					final int centerZ = testCellZ * gridSize + offsetZ;
					final int dist = Math.abs(worldX - centerX) + Math.abs(worldZ - centerZ);
					final int radius = canopyRadius(treeDensityBand, centerHash, gridSize);
					if (dist <= radius && dist < bestDist) {
						bestDist = dist;
						bestRadius = radius;
						bestHash = centerHash;
						bestCenter = dist == 0;
					}
				}
			}
		}

		if (bestDist == Integer.MAX_VALUE) {
			return null;
		}

		int crownHeight = canopyBaseHeight(treeDensityBand);
		final int falloff = bestRadius - bestDist;
		if (falloff >= 1) {
			crownHeight += 2;
		}
		if (falloff >= 3) {
			crownHeight += 2;
		}
		crownHeight += (bestHash >>> 19) & 1;
		if (bestCenter) {
			crownHeight += 2;
		}
		crownHeight = Math.min(crownHeight, canopyMaxHeight(treeDensityBand));
		if (crownHeight <= 0) {
			return null;
		}

		final int centerTrunkHeight = canopyTrunkHeight(treeDensityBand, bestHash);
		final int trunkHeight = bestCenter ? centerTrunkHeight : Math.max(0, centerTrunkHeight - 1);
		final int leafLift = canopyLeafLift(treeDensityBand, bestCenter, centerTrunkHeight, bestDist, bestHash);
		final TreePalette treePalette = treePaletteFromBiomeId(biomeId, bestHash);
		return new CanopyColumn(trunkHeight, leafLift, crownHeight, treePalette.leaves(), treePalette.log());
	}

	private static int canopyChancePercent(final TreeDensityBand treeDensityBand) {
		return switch (treeDensityBand) {
			case LOW -> 18;
			case MEDIUM_LOW -> 32;
			case MEDIUM -> 50;
			case MEDIUM_HIGH -> 68;
			case HIGH -> 82;
		};
	}

	private static int canopyBaseRadius(final TreeDensityBand treeDensityBand) {
		return switch (treeDensityBand) {
			case LOW -> 3;
			case MEDIUM_LOW -> 4;
			case MEDIUM -> 5;
			case MEDIUM_HIGH -> 6;
			case HIGH -> 7;
		};
	}

	private static int canopyBaseHeight(final TreeDensityBand treeDensityBand) {
		return switch (treeDensityBand) {
			case LOW -> 6;
			case MEDIUM_LOW -> 8;
			case MEDIUM -> 10;
			case MEDIUM_HIGH -> 12;
			case HIGH -> 14;
		};
	}

	private static int canopyMaxHeight(final TreeDensityBand treeDensityBand) {
		return switch (treeDensityBand) {
			case LOW -> 10;
			case MEDIUM_LOW -> 13;
			case MEDIUM -> 16;
			case MEDIUM_HIGH -> 19;
			case HIGH -> 22;
		};
	}

	private static int canopyTrunkHeight(final TreeDensityBand treeDensityBand, final int centerHash) {
		final int jitter = (centerHash >>> 21) & 1;
		return switch (treeDensityBand) {
			case LOW -> 2 + jitter;
			case MEDIUM_LOW -> 2 + jitter;
			case MEDIUM -> 3 + jitter;
			case MEDIUM_HIGH -> 3 + jitter;
			case HIGH -> 4 + jitter;
		};
	}

	private static int canopyLeafLift(
			final TreeDensityBand treeDensityBand,
			final boolean isCenter,
			final int centerTrunkHeight,
			final int bestDist,
			final int centerHash) {
		if (isCenter) {
			return 0;
		}

		// Minimal leaf lift: trees sit on their trunks naturally
		final int baseLift = Math.max(0, centerTrunkHeight - Math.max(1, bestDist - 1));
		int lift = switch (treeDensityBand) {
			case LOW -> Math.max(0, baseLift);
			case MEDIUM_LOW -> Math.max(0, baseLift - 1);
			case MEDIUM -> 0;
			case MEDIUM_HIGH -> 0;
			case HIGH -> 0;
		};
		if (bestDist > 2 && ((centerHash >>> 20) & 1) == 0) {
			lift = Math.max(0, lift - 1);
		}
		return lift;
	}

	private static int canopyRadius(final TreeDensityBand treeDensityBand, final int centerHash, final int gridSize) {
		int scaledRadius = Math.max(1, canopyBaseRadius(treeDensityBand) * gridSize / 8);
		scaledRadius = Math.min(scaledRadius, gridSize - 1);
		return scaledRadius + ((centerHash >>> 16) & 1);
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

	private static boolean hasCanopyCenter(final int centerHash, final int chancePercent) {
		final int roll = (centerHash >>> 24) & 0xFF;
		final int threshold = chancePercent * 255 / 100;
		return roll < threshold;
	}

	private static int centerOffset(final int hash, final int gridSize) {
		return Math.floorMod(hash, gridSize);
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

	private enum TreeDensityBand {
		LOW(Blocks.COARSE_DIRT.defaultBlockState()),
		MEDIUM_LOW(Blocks.MOSS_BLOCK.defaultBlockState()),
		MEDIUM(Blocks.GRASS_BLOCK.defaultBlockState()),
		MEDIUM_HIGH(Blocks.LIME_CONCRETE.defaultBlockState()),
		HIGH(Blocks.GREEN_CONCRETE.defaultBlockState());

		private final BlockState surfaceBlock;

		TreeDensityBand(final BlockState surfaceBlock) {
			this.surfaceBlock = surfaceBlock;
		}

		private BlockState surfaceBlock() {
			return this.surfaceBlock;
		}
	}

	private record BiomeSample(String biomeId, @Nullable IDhApiBiomeWrapper wrapper) {
	}

	private record OverlaySample(boolean known, boolean road, boolean building, boolean buildingEdge, int buildingHeightBlocks) {
		private boolean hasAnyOverlay() {
			return this.road || this.building;
		}
	}

	private record OverlayMask(
			boolean known,
			boolean[] roadMask,
			boolean[] buildingMask,
			boolean[] buildingEdgeMask,
			int[] buildingHeightMask,
			int width,
			int height,
			int sampleCount) {
		private static OverlayMask empty(final int width, final int height) {
			final int safeWidth = Math.max(0, width);
			final int safeHeight = Math.max(0, height);
			final int area = safeWidth * safeHeight;
			return new OverlayMask(false, new boolean[area], new boolean[area], new boolean[area], new int[area], safeWidth, safeHeight, 0);
		}

		private OverlaySample sampleAt(final int x, final int z) {
			if (x < 0 || z < 0 || x >= this.width || z >= this.height) {
				return new OverlaySample(false, false, false, false, 0);
			}

			final int index = z * this.width + x;
			final boolean road = this.roadMask[index];
			final boolean building = this.buildingMask[index];
			final boolean buildingEdge = this.buildingEdgeMask[index];
			final int heightBlocks = this.buildingHeightMask[index];
			return new OverlaySample(this.known, road, building, buildingEdge, heightBlocks);
		}
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
			columnDataPoints.clear();
			// Seed a 1-unit air layer so the column always starts at 0 and cannot report an initial gap.
			columnDataPoints.add(DhApiTerrainDataPoint.create(
					(byte) 0,
					0,
					15,
					0,
					1,
					wrappers.airBlock(),
					Objects.requireNonNull(columnBiome)));
			lastLayerTop = 1;
		}

		private void addLayerUpTo(final int inclusiveTopY, final BlockState blockState) {
			addLayerUpTo(inclusiveTopY, blockState, (byte) 0);
		}

		private void addLayerUpTo(final int inclusiveTopY, final BlockState blockState, final byte emittedLight) {
			final int layerTop = Mth.clamp(inclusiveTopY - minY + 1, 0, absoluteTop);
			if (layerTop <= lastLayerTop) {
				return;
			}

			final IDhApiBlockStateWrapper block = wrappers.getBlockState(blockState);
			final IDhApiBiomeWrapper biome = Objects.requireNonNull(columnBiome);
			columnDataPoints.add(DhApiTerrainDataPoint.create(
					(byte) 0,
					emittedLight,
					15,
					lastLayerTop,
					layerTop,
					block,
					biome));
			lastLayerTop = layerTop;
		}

		private void addSpan(final int startYInclusive, final int endYInclusive, final BlockState blockState, final byte emittedLight) {
			if (endYInclusive < startYInclusive) {
				return;
			}

			final int startTop = Mth.clamp(startYInclusive - minY + 1, 0, absoluteTop);
			final int endTop = Mth.clamp(endYInclusive - minY + 1, 0, absoluteTop);
			if (endTop <= lastLayerTop) {
				return;
			}

			if (startTop > lastLayerTop) {
				columnDataPoints.add(DhApiTerrainDataPoint.create(
						(byte) 0,
						0,
						15,
						lastLayerTop,
						startTop,
						wrappers.airBlock(),
						Objects.requireNonNull(columnBiome)));
				lastLayerTop = startTop;
			}

			if (endTop > lastLayerTop) {
				final IDhApiBlockStateWrapper block = wrappers.getBlockState(blockState);
				final IDhApiBiomeWrapper biome = Objects.requireNonNull(columnBiome);
				columnDataPoints.add(DhApiTerrainDataPoint.create(
						(byte) 0,
						emittedLight,
						15,
						lastLayerTop,
						endTop,
						block,
						biome));
				lastLayerTop = endTop;
			}
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

			try {
				output.setApiDataPointColumn(columnX, columnZ, columnDataPoints);
			} finally {
				columnDataPoints.clear();
			}
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
						return biome.unwrapKey().map(key -> getBiome(key.location().toString())).orElse(null);
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

