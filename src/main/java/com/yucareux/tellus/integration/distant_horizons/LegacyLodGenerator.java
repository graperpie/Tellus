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
import com.yucareux.tellus.legacy.backend.GeoChunk;
import com.yucareux.tellus.legacy.backend.GeoView;
import com.yucareux.tellus.legacy.backend.earth.EarthAttachments;
import com.yucareux.tellus.legacy.backend.earth.EarthLayers;
import com.yucareux.tellus.legacy.backend.earth.cover.LegacyCover;
import com.yucareux.tellus.world.data.koppen.TellusKoppenSource;
import com.yucareux.tellus.legacy.backend.projection.Projection;
import com.yucareux.tellus.legacy.backend.raster.EnumRaster;
import com.yucareux.tellus.legacy.backend.raster.RasterShape;
import com.yucareux.tellus.legacy.backend.raster.ShortRaster;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import com.yucareux.tellus.worldgen.EarthBiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import com.yucareux.tellus.legacy.backend.earth.EarthTiles;
import com.yucareux.tellus.legacy.backend.projection.cylindrical.Equirectangular;
import com.yucareux.tellus.legacy.backend.loader.ConcurrencyLimiter;
import com.yucareux.tellus.legacy.backend.tile.GuavaTileCache;
import com.yucareux.tellus.world.data.snow.SnowLineGrid;
import com.yucareux.tellus.integration.distant_horizons.TellusLodGenerator.CanopyColumn;
import com.yucareux.tellus.integration.distant_horizons.TellusLodGenerator.CanopyProfile;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.util.concurrent.ForkJoinPool;
import org.jetbrains.annotations.Nullable;

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
import java.util.function.Consumer;

public final class LegacyLodGenerator implements IDhApiWorldGenerator {
    private static final int FORCED_CANOPY_CELL_SIZE = 2;
    private static final int SNOW_ROCK_RELIEF_MIN = 30;
    private static final int SNOW_ROCK_RELIEF_MAX = 170;
    private static final int SNOW_ROCK_SLOPE_MIN_RELIEF_METERS = 55;
    private static final int SNOW_ROCK_SLOPE_FULL_RELIEF_METERS = 210;
    private static final double SNOW_ROCK_MAX_EXPOSURE = 0.75D;
    private static final int SNOW_ROCK_ALTITUDE_FULL_EXPOSURE_METERS = 650;
    private static final int SNOW_ROCK_PATCH_CELL_SHIFT = 8;
    private static final double SNOW_ASPECT_EXPOSURE_BIAS_MAX = 0.14D;

    private final IDhApiLevelWrapper levelWrapper;
    private final EarthLayers earthLayers;
    private final Projection projection;
    private final EarthGeneratorSettings settings;
    private final TellusKoppenSource koppenSource;
    private final SnowLineGrid snowLineGrid;
    private final EarthBiomeSource biomeSource;
    private final ThreadLocal<WrapperCache> wrapperCache;

    public LegacyLodGenerator(final IDhApiLevelWrapper levelWrapper, final EarthChunkGenerator generator) {
        this.levelWrapper = levelWrapper;
        this.settings = generator.settings();

        // Correct metersPerBlock for Legacy datasets
        // Legacy datasets (geo3) are roughly 111km per 360 degrees,
        // worldScale is blocks per 360 degrees (Equatorial circumference)
        // Equirectangular projection expects metersPerBlock.
        this.projection = new Equirectangular(settings.worldScale());

        final EarthTiles.Config config = new EarthTiles.Config(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build(),
                new ConcurrencyLimiter(16),
                Paths.get("tellus_cache", "legacy"),
                ForkJoinPool.commonPool(),
                ForkJoinPool.commonPool());
        final EarthTiles tiles = config.create(new GuavaTileCache(Duration.ofMinutes(5), 1000));
        this.earthLayers = EarthLayers.create(tiles, projection, ForkJoinPool.commonPool());
        this.koppenSource = new TellusKoppenSource();
        this.snowLineGrid = new SnowLineGrid();
        this.biomeSource = (EarthBiomeSource) generator.getBiomeSource();
        this.wrapperCache = ThreadLocal.withInitial(() -> new WrapperCache(levelWrapper));
    }

    @Override
    public void preGeneratorTaskStart() {
    }

    @Override
    public byte getLargestDataDetailLevel() {
        return 24;
    }

    @Override
    public CompletableFuture<Void> generateLod(final int chunkPosMinX, final int chunkPosMinZ, final int lodPosX,
            final int lodPosZ, final byte detailLevel, final IDhApiFullDataSource pooledFullDataSource,
            final EDhApiDistantGeneratorMode generatorMode, final ExecutorService worldGeneratorThreadPool,
            final Consumer<IDhApiFullDataSource> resultConsumer) {
        final int lodSizePoints = pooledFullDataSource.getWidthInDataColumns();
        final int lodSizeBlocks = lodSizePoints * (1 << detailLevel);

        final int x0 = SectionPos.sectionToBlockCoord(chunkPosMinX);
        final int z0 = SectionPos.sectionToBlockCoord(chunkPosMinZ);
        final int x1 = x0 + lodSizeBlocks - 1;
        final int z1 = z0 + lodSizeBlocks - 1;
        final GeoView blockSampleView = new GeoView(x0, z0, x1, z1);

        final RasterShape outputShape = new RasterShape(lodSizePoints, lodSizePoints);

        return earthLayers.get(blockSampleView, outputShape).thenAcceptAsync(
                geoChunk -> {
                    if (geoChunk.isPresent()) {
                        buildLod(pooledFullDataSource, geoChunk.get(), x0, z0, detailLevel);
                    }
                    resultConsumer.accept(pooledFullDataSource);
                },
                worldGeneratorThreadPool);
    }

    private void buildLod(final IDhApiFullDataSource output, final GeoChunk geoChunk, final int x0, final int z0,
            final byte detailLevel) {
        final WrapperCache wrappers = wrapperCache.get();
        final int minY = levelWrapper.getMinHeight();
        final int maxY = minY + levelWrapper.getMaxHeight();
        final int absoluteTop = levelWrapper.getMaxHeight();
        final VanillaSurfaceLodOutput lodOutput = new VanillaSurfaceLodOutput(output, wrappers,
                minY, absoluteTop);

        final Optional<EarthAttachments> earth = EarthAttachments.from(geoChunk);
        if (earth.isEmpty()) {
            return;
        }

        final ShortRaster elevation = earth.get().elevation();
        final EnumRaster<LegacyCover> landCover = earth.get().landCover();
        final int seaLevel = settings.resolveSeaLevel();
        final float heightScale = (float) (settings.terrestrialHeightScale() / projection.idealMetersPerBlock());

        // Simple biome fallback since we don't have the full biome source readily
        // available here without more plumbing
        // But we can approximate or just use Plains for the wrapper if needed, or query
        // the world
        // However, DH expects biomes. Let's try to query the level wrapper if possible,
        // or just use a default.
        // Actually, we should ideally use the biome source from the generator, but we
        // are isolated.
        // For legacy port, let's just use the world's biome if we can't get it easily.
        // Or, we can just use "Plains" as a placeholder since we are controlling the
        // blocks manually.
        // Wait, DH needs biomes for colormaps.
        // We really should pass a biome source. But let's check if we can get it from
        // levelWrapper.
        // The levelWrapper provides access to the level.

        // The levelWrapper provides access to the level.

        // Use a default biome for now as accessing registry from levelWrapper is tricky
        // and we control the blocks manually anyway.
        // We use "minecraft:plains" as a safe default that always exists.
        IDhApiBiomeWrapper defaultBiomeWrapper = wrappers.getBiome("minecraft:plains");

        for (int z = 0; z < elevation.height(); z++) {
            for (int x = 0; x < elevation.width(); x++) {

                // Sample biome at world coordinates
                // We use the center of the LOD column for sampling
                final int worldX = x0 + (x << detailLevel) + (1 << (detailLevel - 1));
                final int worldZ = z0 + (z << detailLevel) + (1 << (detailLevel - 1));

                final Holder<Biome> biomeHolder = biomeSource.getBiomeAtBlock(worldX, worldZ);
                final IDhApiBiomeWrapper biomeWrapper = wrappers.getBiome(biomeHolder);

                lodOutput.beginColumn(x, z, biomeWrapper != null ? biomeWrapper : defaultBiomeWrapper);

                final int elevationValue = elevation.getInt(x, z);
                final int surfaceY = Mth.clamp(Mth.floor((elevationValue * heightScale) + settings.heightOffset()),
                        minY, maxY);
                final LegacyCover cover = landCover.get(x, z);
                final double lat = projection.lat(worldX, worldZ);
                final double lon = projection.lon(worldX, worldZ);
                final int snowLineMeters = snowLineGrid.getSnowLineElevation(lat, lon);
                final boolean aboveSnowLine = elevationValue >= snowLineMeters;

                // Improved Water Logic
                // 1. Ocean: implied by elevation < seaLevel (Legacy behavior)
                // 2. Upland Water: implied by cover == LegacyCover.WATER but elevation >=
                // seaLevel

                final boolean isOcean = surfaceY < seaLevel;
                final boolean isUplandWater = cover == LegacyCover.WATER && surfaceY >= seaLevel;
                final boolean forceCanopyPlaceholder = shouldForceCanopyPlaceholder(cover) && !aboveSnowLine;
                boolean addSnowLayer = false;

                if (isOcean) {
                    // Ocean logic: Fill from bottom up to seaLevel
                    final BlockState underwaterMaterial = getLodUnderwaterMaterial(cover);
                    lodOutput.addLayerUpTo(surfaceY, underwaterMaterial);
                    lodOutput.addLayerUpTo(seaLevel, Blocks.WATER.defaultBlockState());
                } else if (isUplandWater) {
                    // Upland Water logic: Create artificial depth
                    // Elevation data for water bodies usually represents the SURFACE level.
                    // We need to carve out a floor to give it volume for shaders.
                    final int waterTo = surfaceY;
                    final int floorTo = Math.max(minY, waterTo - 25); // 25 blocks depth

                    final BlockState floorMaterial = getLodUnderwaterMaterial(cover);
                    lodOutput.addLayerUpTo(floorTo, floorMaterial);
                    lodOutput.addLayerUpTo(waterTo, Blocks.WATER.defaultBlockState());
                } else {
                    // Dry Land Logic
                    BlockState surfaceMaterial = getSurfaceMaterial(cover, surfaceY, worldX, worldZ);
                    if (forceCanopyPlaceholder) {
                        // Brown placeholder terrain should read as forest floor in LOD.
                        surfaceMaterial = Blocks.GRASS_BLOCK.defaultBlockState();
                    }
                    if (aboveSnowLine) {
                        final int localRelief = localReliefMeters(elevation, x, z);
                        final int altitudeAboveSnowLine = elevationValue - snowLineMeters;
                        final double coldFacingFactor = coldFacingSlopeFactor(elevation, x, z, lat);
                        final double rockExposure = rockySnowExposure(localRelief, altitudeAboveSnowLine, cover,
                                worldX, worldZ, coldFacingFactor);
                        final boolean exposeRock = hash01(worldX, worldZ) < rockExposure;

                        if (exposeRock) {
                            // Steep and rough alpine zones keep exposed rock in LODs.
                            surfaceMaterial = selectAlpineRockMaterial(
                                    localRelief,
                                    altitudeAboveSnowLine,
                                    worldX,
                                    worldZ);
                            // Keep a light snow cap only on rocky alpine surfaces.
                            addSnowLayer = true;
                        } else {
                            // Above snow line, keep only snow/stone materials.
                            surfaceMaterial = Blocks.SNOW_BLOCK.defaultBlockState();
                        }
                    }
                    lodOutput.addLayerUpTo(surfaceY, surfaceMaterial);
                }

                if (isForest(cover)) {
                    final CanopyProfile profile = TellusLodGenerator.getCanopyProfile(biomeHolder);
                    final int canopyCellSize = forceCanopyPlaceholder ? FORCED_CANOPY_CELL_SIZE : (1 << detailLevel);
                    CanopyColumn canopy = TellusLodGenerator.resolveCanopyColumn(profile, worldX, worldZ,
                            canopyCellSize);
                    if (canopy == null && forceCanopyPlaceholder) {
                        canopy = forcedPlaceholderCanopy(worldX, worldZ, cover);
                    }
                    if (canopy != null) {
                        lodOutput.addCanopy(canopy);
                    }
                }

                if (addSnowLayer) {
                    lodOutput.addSnowLayer();
                }

                lodOutput.endColumn();
            }
        }
    }

    private static boolean shouldForceCanopyPlaceholder(final LegacyCover cover) {
        return switch (cover) {
            case TREE_OR_SHRUB_COVER,
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
                    SPARSE_VEGETATION,
                    SPARSE_TREE -> true;
            default -> false;
        };
    }

    private static CanopyColumn forcedPlaceholderCanopy(final int worldX, final int worldZ, final LegacyCover cover) {
        final int hash = mixHash(worldX, worldZ, 0x5A3C9E21);
        final boolean needle = switch (cover) {
            case NEEDLE_LEAF_EVERGREEN,
                    NEEDLE_LEAF_EVERGREEN_CLOSED,
                    NEEDLE_LEAF_EVERGREEN_OPEN,
                    NEEDLE_LEAF_DECIDUOUS,
                    NEEDLE_LEAF_DECIDUOUS_CLOSED,
                    NEEDLE_LEAF_DECIDUOUS_OPEN -> true;
            default -> false;
        };

        final int trunkHeight = 2 + ((hash >>> 2) & 0x1);
        final int leafLift = 1;
        final int leavesHeight = needle ? (2 + ((hash >>> 5) & 0x1)) : (3 + ((hash >>> 5) & 0x1));
        final BlockState leaves = needle ? Blocks.SPRUCE_LEAVES.defaultBlockState() : Blocks.OAK_LEAVES.defaultBlockState();
        final BlockState trunk = needle ? Blocks.SPRUCE_LOG.defaultBlockState() : Blocks.OAK_LOG.defaultBlockState();
        return new CanopyColumn(trunkHeight, leafLift, leavesHeight, leaves, trunk);
    }

    private static int mixHash(final int x, final int z, final int salt) {
        int h = x * 0x1F1F1F1F;
        h = Integer.rotateLeft(h, 13) ^ (z * 0x45D9F3B);
        h = Integer.rotateLeft(h, 7) ^ salt;
        h ^= h >>> 16;
        h *= 0x7FEB352D;
        h ^= h >>> 15;
        h *= 0x846CA68B;
        h ^= h >>> 16;
        return h;
    }

    private static int localReliefMeters(final ShortRaster elevation, final int x, final int z) {
        final int center = elevation.getInt(x, z);
        int relief = 0;

        for (int dz = -1; dz <= 1; dz++) {
            final int nz = z + dz;
            if (nz < 0 || nz >= elevation.height()) {
                continue;
            }
            for (int dx = -1; dx <= 1; dx++) {
                final int nx = x + dx;
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (nx < 0 || nx >= elevation.width()) {
                    continue;
                }
                final int delta = Math.abs(elevation.getInt(nx, nz) - center);
                if (delta > relief) {
                    relief = delta;
                }
            }
        }

        return relief;
    }

    private static double rockySnowExposure(final int localReliefMeters, final int altitudeAboveSnowLineMeters,
            final LegacyCover cover, final int worldX, final int worldZ, final double coldFacingFactor) {
        final double slopeFactor = Mth.clamp(
            (localReliefMeters - SNOW_ROCK_SLOPE_MIN_RELIEF_METERS)
                / (double) (SNOW_ROCK_SLOPE_FULL_RELIEF_METERS - SNOW_ROCK_SLOPE_MIN_RELIEF_METERS),
            0.0D,
            1.0D);
        if (slopeFactor <= 0.0D) {
            return 0.0D;
        }

        final double reliefFactor = Mth.clamp(
                (localReliefMeters - SNOW_ROCK_RELIEF_MIN) / (double) (SNOW_ROCK_RELIEF_MAX - SNOW_ROCK_RELIEF_MIN),
                0.0D,
                1.0D);
        final double altitudeFactor = Mth.clamp(
                altitudeAboveSnowLineMeters / (double) SNOW_ROCK_ALTITUDE_FULL_EXPOSURE_METERS,
                0.0D,
                1.0D);

        double exposure = reliefFactor * SNOW_ROCK_MAX_EXPOSURE;
        exposure += altitudeFactor * 0.22D;
        if (cover == LegacyCover.BARE_CONSOLIDATED || cover == LegacyCover.SPARSE_VEGETATION) {
            exposure += 0.15D;
        }
        if (cover == LegacyCover.PERMANENT_SNOW) {
            exposure -= 0.10D;
        }

        // Coarse value noise creates clustered "bald" wind-scoured patches.
        final double patchNoise = coarsePatchNoise(worldX, worldZ);
        exposure += Math.max(0.0D, patchNoise - 0.35D) * 0.80D * slopeFactor;

        // Cold-facing slopes (away from sun) retain snow more, sun-facing slopes expose more rock.
        final double sunFacingFactor = 1.0D - coldFacingFactor;
        final double aspectBias = (sunFacingFactor - coldFacingFactor) * SNOW_ASPECT_EXPOSURE_BIAS_MAX;
        exposure += aspectBias * slopeFactor;

        // Break up wide monochrome slopes with stable per-column noise.
        exposure += (hash01(worldX ^ 0x4A3B, worldZ ^ 0x19D2) - 0.5D) * 0.14D * slopeFactor;
        return Mth.clamp(exposure, 0.0D, 0.95D);
    }

    private static double coldFacingSlopeFactor(
            final ShortRaster elevation,
            final int x,
            final int z,
            final double latitude) {
        final int west = elevation.getInt(Math.max(0, x - 1), z);
        final int east = elevation.getInt(Math.min(elevation.width() - 1, x + 1), z);
        final int north = elevation.getInt(x, Math.max(0, z - 1));
        final int south = elevation.getInt(x, Math.min(elevation.height() - 1, z + 1));

        final double gradX = (east - west) * 0.5D;
        final double gradZ = (south - north) * 0.5D;
        final double gradientMag = Math.sqrt((gradX * gradX) + (gradZ * gradZ));
        if (gradientMag < 1.0D) {
            return 0.5D;
        }

        // Northern hemisphere: south-facing gets more sun; southern hemisphere is the opposite.
        final double sunwardZ = latitude >= 0.0D ? 1.0D : -1.0D;
        final double slopeSouthness = gradZ / gradientMag;
        final double sunFacing = Mth.clamp((slopeSouthness * sunwardZ + 1.0D) * 0.5D, 0.0D, 1.0D);
        return 1.0D - sunFacing;
    }

    private static BlockState selectAlpineRockMaterial(
            final int localReliefMeters,
            final int altitudeAboveSnowLineMeters,
            final int worldX,
            final int worldZ) {
        final double roughness = Mth.clamp(
                localReliefMeters / (double) SNOW_ROCK_SLOPE_FULL_RELIEF_METERS,
                0.0D,
                1.0D);
        final double altitude = Mth.clamp(
                altitudeAboveSnowLineMeters / (double) SNOW_ROCK_ALTITUDE_FULL_EXPOSURE_METERS,
                0.0D,
                1.0D);
        final double pick = hash01(worldX ^ 0x2F11, worldZ ^ 0x7D31);

        if (roughness < 0.35D && altitude < 0.35D && pick < 0.16D) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (pick < (0.30D + (roughness * 0.22D))) {
            return Blocks.ANDESITE.defaultBlockState();
        }
        return Blocks.STONE.defaultBlockState();
    }

    private static double coarsePatchNoise(final int worldX, final int worldZ) {
        final int cx = worldX >> SNOW_ROCK_PATCH_CELL_SHIFT;
        final int cz = worldZ >> SNOW_ROCK_PATCH_CELL_SHIFT;

        final double fx = (worldX & ((1 << SNOW_ROCK_PATCH_CELL_SHIFT) - 1))
                / (double) (1 << SNOW_ROCK_PATCH_CELL_SHIFT);
        final double fz = (worldZ & ((1 << SNOW_ROCK_PATCH_CELL_SHIFT) - 1))
                / (double) (1 << SNOW_ROCK_PATCH_CELL_SHIFT);

        final double v00 = hash01(cx, cz);
        final double v10 = hash01(cx + 1, cz);
        final double v01 = hash01(cx, cz + 1);
        final double v11 = hash01(cx + 1, cz + 1);

        final double sx = smoothstep(fx);
        final double sz = smoothstep(fz);
        final double ix0 = lerp(v00, v10, sx);
        final double ix1 = lerp(v01, v11, sx);
        return lerp(ix0, ix1, sz);
    }

    private static double smoothstep(final double t) {
        return t * t * (3.0D - (2.0D * t));
    }

    private static double lerp(final double a, final double b, final double t) {
        return a + (b - a) * t;
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

    private static boolean isForest(final LegacyCover cover) {
        return switch (cover) {
            case TREE_OR_SHRUB_COVER,
                    BROADLEAF_EVERGREEN,
                    BROADLEAF_DECIDUOUS, BROADLEAF_DECIDUOUS_CLOSED, BROADLEAF_DECIDUOUS_OPEN,
                    NEEDLE_LEAF_EVERGREEN, NEEDLE_LEAF_EVERGREEN_CLOSED, NEEDLE_LEAF_EVERGREEN_OPEN,
                    NEEDLE_LEAF_DECIDUOUS, NEEDLE_LEAF_DECIDUOUS_CLOSED, NEEDLE_LEAF_DECIDUOUS_OPEN,
                    MIXED_LEAF_TYPE,
                    TREE_AND_SHRUB_WITH_HERBACEOUS_COVER, HERBACEOUS_COVER_WITH_TREE_AND_SHRUB,
                    SHRUBLAND, SHRUBLAND_EVERGREEN, SHRUBLAND_DECIDUOUS,
                    SPARSE_TREE,
                    FRESH_FLOODED_FOREST, SALINE_FLOODED_FOREST, FLOODED_VEGETATION ->
                true;
            default -> false;
        };
    }

    private BlockState getSurfaceMaterial(final LegacyCover cover, final int surfaceY, final int worldX,
            final int worldZ) {
        // Legacy surface rules
        final double lat = projection.lat(worldX, worldZ);
        final double lon = projection.lon(worldX, worldZ);

        /*
         * if (isSnowyRegion(lat, lon, worldX, worldZ)) {
         * // we don't need biome temp check for now, relying on lat/lon
         * return Blocks.SNOW_BLOCK.defaultBlockState();
         * }
         */

        return switch (cover) {
            case BROADLEAF_DECIDUOUS -> Blocks.GRASS_BLOCK.defaultBlockState();
            case NEEDLE_LEAF_EVERGREEN -> Blocks.PODZOL.defaultBlockState();
            case BROADLEAF_EVERGREEN -> Blocks.GRASS_BLOCK.defaultBlockState();
            case SHRUBLAND -> Blocks.GRASS_BLOCK.defaultBlockState();
            case SPARSE_VEGETATION -> Blocks.COARSE_DIRT.defaultBlockState();
            case BARE_CONSOLIDATED -> Blocks.STONE.defaultBlockState();
            case BARE_UNCONSOLIDATED -> Blocks.SAND.defaultBlockState();
            case URBAN -> Blocks.BRICKS.defaultBlockState();
            case WATER -> Blocks.WATER.defaultBlockState();
            case FLOODED_VEGETATION -> Blocks.GRASS_BLOCK.defaultBlockState(); // Wetland approximation
            case IRRIGATED_CROPLAND -> Blocks.GRASS_BLOCK.defaultBlockState(); // Paddy field
            case RAINFED_CROPLAND -> Blocks.FARMLAND.defaultBlockState();
            /*
             * case PERMANENT_SNOW -> Blocks.SNOW_BLOCK.defaultBlockState();
             */
            default -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    private BlockState getLodUnderwaterMaterial(final LegacyCover cover) {
        return switch (cover) {
            case BARE_UNCONSOLIDATED, SPARSE_VEGETATION -> Blocks.SAND.defaultBlockState();
            case BARE_CONSOLIDATED, URBAN -> Blocks.STONE.defaultBlockState();
            /*
             * case PERMANENT_SNOW -> Blocks.PACKED_ICE.defaultBlockState();
             */
            default -> Blocks.DIRT.defaultBlockState();
        };
    }

    private boolean isSnowyRegion(final double lat, final double lon, final int worldX, final int worldZ) {
        // First check: Koppen-Geiger climate zones (Bundled offline data)
        final String koppen = koppenSource.sampleRawCode(worldX, worldZ, settings.worldScale());
        if (koppen != null && (koppen.equals("ET") || koppen.equals("EF"))) {
            return true;
        }

        // Second check: Hardcoded fallback regions
        if (Math.abs(lat) > 60)
            return true;
        if (lat >= 27 && lat <= 36 && lon >= 70 && lon <= 100)
            return true; // Himalayas
        if (lat >= 45 && lat <= 48 && lon >= 5 && lon <= 16)
            return true; // Alps
        if (lat >= -56 && lat <= 10 && lon >= -80 && lon <= -60)
            return true; // Andes
        if (lat >= 37 && lat <= 60 && lon >= -120 && lon <= -105)
            return true; // Rockies
        return false;
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

        public VanillaSurfaceLodOutput(final IDhApiFullDataSource output, final WrapperCache wrappers,
                final int minY, final int absoluteTop) {
            this.output = output;
            this.wrappers = wrappers;
            this.minY = minY;
            this.absoluteTop = absoluteTop;
        }

        public void beginColumn(final int x, final int z, final IDhApiBiomeWrapper biome) {
            columnX = x;
            columnZ = z;
            columnBiome = biome;
            lastLayerTop = 0;
        }

        public void addLayerUpTo(final int inclusiveTopY, final BlockState blockState) {
            final int layerTop = Mth.clamp(inclusiveTopY - minY + 1, 0, absoluteTop);
            if (layerTop <= lastLayerTop) {
                return;
            }

            final IDhApiBlockStateWrapper block = wrappers.getBlockState(blockState);
            final IDhApiBiomeWrapper biome = Objects.requireNonNull(columnBiome);

            columnDataPoints.add(DhApiTerrainDataPoint.create(
                    (byte) 0,
                    0, // Block light
                    15, // Sky light
                    lastLayerTop,
                    layerTop,
                    block,
                    biome));
            lastLayerTop = layerTop;
        }

        public void endColumn() {
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

        public void addCanopy(final CanopyColumn canopyColumn) {
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

        public void addSnowLayer() {
            if (lastLayerTop >= absoluteTop) {
                return;
            }

            int snowHeight = 1; // 1 block of snow for LODs
            final int snowTop = Math.min(absoluteTop, lastLayerTop + snowHeight);
            if (snowTop > lastLayerTop) {
                final IDhApiBiomeWrapper biome = Objects.requireNonNull(columnBiome);
                final IDhApiBlockStateWrapper snowBlock = wrappers.getBlockState(Blocks.SNOW_BLOCK.defaultBlockState());
                columnDataPoints.add(
                        DhApiTerrainDataPoint.create((byte) 0, 0, 15, lastLayerTop, snowTop, snowBlock, biome));
                lastLayerTop = snowTop;
            }
        }
    }

    private static class WrapperCache {
        private final IDhApiLevelWrapper levelWrapper;
        private final IDhApiBlockStateWrapper airBlock;
        @Nullable
        private final IDhApiBiomeWrapper defaultBiome;
        private final Map<BlockState, IDhApiBlockStateWrapper> blockStates = new IdentityHashMap<>();
        private final Map<String, IDhApiBiomeWrapper> biomeCache = new HashMap<>();
        private final Map<Holder<Biome>, IDhApiBiomeWrapper> holderBiomeCache = new HashMap<>();

        private WrapperCache(final IDhApiLevelWrapper levelWrapper) {
            this.levelWrapper = levelWrapper;
            airBlock = DhApi.Delayed.wrapperFactory.getAirBlockStateWrapper();
            defaultBiome = lookupBiomeById("minecraft:the_void");
        }

        public IDhApiBlockStateWrapper airBlock() {
            return airBlock;
        }

        public IDhApiBlockStateWrapper getBlockState(final BlockState blockState) {
            return blockStates.computeIfAbsent(blockState, this::lookupBlockState);
        }

        private IDhApiBlockStateWrapper lookupBlockState(final BlockState blockState) {
            return DhApi.Delayed.wrapperFactory.getBlockStateWrapper(new BlockState[] { blockState }, levelWrapper);
        }

        public IDhApiBiomeWrapper getBiome(final Holder<Biome> biome) {
            return holderBiomeCache.computeIfAbsent(biome, this::lookupBiomeByHolder);
        }

        @Nullable
        private IDhApiBiomeWrapper lookupBiomeByHolder(final Holder<Biome> biome) {
            return biome.unwrapKey().map(key -> getBiome(key.identifier().toString())).orElse(null);
        }

        public IDhApiBiomeWrapper getBiome(final String biomeId) {
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

    @Override
    public EDhApiWorldGeneratorReturnType getReturnType() {
        return EDhApiWorldGeneratorReturnType.API_DATA_SOURCES;
    }

    @Override
    public boolean runApiValidation() {
        return false;
    }

    @Override
    public void close() {
    }
}
