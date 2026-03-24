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
import com.yucareux.tellus.legacy.backend.earth.EarthTiles;
import com.yucareux.tellus.legacy.backend.earth.cover.LegacyCover;
import com.yucareux.tellus.legacy.backend.loader.ConcurrencyLimiter;
import com.yucareux.tellus.legacy.backend.projection.Projection;
import com.yucareux.tellus.legacy.backend.projection.cylindrical.Equirectangular;
import com.yucareux.tellus.legacy.backend.raster.EnumRaster;
import com.yucareux.tellus.legacy.backend.raster.RasterShape;
import com.yucareux.tellus.legacy.backend.raster.ShortRaster;
import com.yucareux.tellus.legacy.backend.tile.GuavaTileCache;
import com.yucareux.tellus.world.data.satellite.SatelliteTileSampler;
import com.yucareux.tellus.worldgen.EarthBiomeSource;
import com.yucareux.tellus.worldgen.EarthChunkGenerator;
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
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class LegacyLodGeneratorV2 implements IDhApiWorldGenerator {
	private final LegacyLodGenerator v1Fallback;
	private final IDhApiLevelWrapper levelWrapper;
	private final EarthLayers earthLayers;
	private final Projection projection;
	private final EarthGeneratorSettings settings;
	private final EarthBiomeSource biomeSource;
	private final SatelliteTileSampler satelliteSampler;
	private final ThreadLocal<WrapperCache> wrapperCache;

	public LegacyLodGeneratorV2(final IDhApiLevelWrapper levelWrapper, final EarthChunkGenerator generator) {
		this.levelWrapper = levelWrapper;
		this.v1Fallback = new LegacyLodGenerator(levelWrapper, generator);
		this.settings = generator.settings();
		this.projection = new Equirectangular(settings.worldScale());
		this.biomeSource = (EarthBiomeSource) generator.getBiomeSource();
		this.wrapperCache = ThreadLocal.withInitial(() -> new WrapperCache(levelWrapper));

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
		final int x1 = x0 + lodSizeBlocks - 1;
		final int z1 = z0 + lodSizeBlocks - 1;
		final GeoView blockSampleView = new GeoView(x0, z0, x1, z1);

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

		for (int z = 0; z < elevation.height(); z++) {
			for (int x = 0; x < elevation.width(); x++) {
				final int worldX = x0 + (x << detailLevel) + (1 << (detailLevel - 1));
				final int worldZ = z0 + (z << detailLevel) + (1 << (detailLevel - 1));

				final Holder<Biome> biomeHolder = biomeSource.getBiomeAtBlock(worldX, worldZ);
				final IDhApiBiomeWrapper biomeWrapper = wrappers.getBiome(biomeHolder);
				lodOutput.beginColumn(x, z, biomeWrapper != null ? biomeWrapper : defaultBiomeWrapper);

				final int elevationValue = elevation.getInt(x, z);
				final int surfaceY = Mth.clamp(Mth.floor((elevationValue * heightScale) + settings.heightOffset()), minY,
						maxY);
				final LegacyCover cover = landCover.get(x, z);
				final double lat = projection.lat(worldX, worldZ);
				final double lon = projection.lon(worldX, worldZ);
				final boolean aboveSnowLine = false;

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
					final SatelliteSurface satelliteSurface = classifySatelliteSurface(
							policy,
							lat,
							lon,
							biomeHolder,
							worldX,
							worldZ,
							aboveSnowLine);
					BlockState surfaceMaterial = satelliteSurface.blockState();
					if (aboveSnowLine && !satelliteSurface.forceExposeRock()) {
						surfaceMaterial = Blocks.SNOW_BLOCK.defaultBlockState();
					}
					lodOutput.addLayerUpTo(surfaceY, surfaceMaterial);

					if (policy == V2Policy.LEVEL_5_SENTINEL_10M_VEG && satelliteSurface.vegetationStrength() >= 0.20D
							&& !aboveSnowLine) {
						final TellusLodGenerator.CanopyColumn canopy = TellusLodGenerator.resolveCanopyColumn(
								TellusLodGenerator.getCanopyProfile(biomeHolder),
								worldX,
								worldZ,
								Math.max(2, 1 << Math.max(0, detailLevel - 1)));
						if (canopy != null) {
							lodOutput.addCanopy(canopy);
						}
					}
				}

				lodOutput.endColumn();
			}
		}
	}

	private SatelliteSurface classifySatelliteSurface(
			final V2Policy policy,
			final double lat,
			final double lon,
			final Holder<Biome> biomeHolder,
			final int worldX,
			final int worldZ,
			final boolean aboveSnowLine) {
		final int zoom = policySatelliteZoom(policy);
		final int rgb = satelliteSampler.sampleRgb(lat, lon, zoom);
		if (rgb < 0) {
			return fallbackSurfaceFromBiome(biomeHolder, aboveSnowLine);
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
		if (saturation < 0.30D && hue >= 18.0D && hue <= 60.0D && value > 0.50D) {
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
		return fallbackSurfaceFromBiome(biomeHolder, aboveSnowLine);
	}

	private static SatelliteSurface fallbackSurfaceFromBiome(
			final Holder<Biome> biomeHolder,
			final boolean aboveSnowLine) {
		if (aboveSnowLine) {
			return new SatelliteSurface(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

		final String biomeId = biomeHolder.unwrapKey()
				.map(key -> key.identifier().toString())
				.orElse("")
				.toLowerCase();

		if (biomeId.contains("desert")) {
			return new SatelliteSurface(Blocks.SAND.defaultBlockState(), 0.0D, false);
		}
		if (biomeId.contains("badlands")) {
			return new SatelliteSurface(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
		}
		if (biomeId.contains("mountain") || biomeId.contains("peak") || biomeId.contains("stony")) {
			return new SatelliteSurface(Blocks.STONE.defaultBlockState(), 0.0D, true);
		}
		if (biomeId.contains("taiga") || biomeId.contains("forest") || biomeId.contains("jungle")) {
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

	private static int policySatelliteZoom(final V2Policy policy) {
		return switch (policy) {
			case LEVEL_3_HIGH_RES -> 13;
			case LEVEL_4_SENTINEL_10M -> 12;
			case LEVEL_5_SENTINEL_10M_VEG -> 11;
			case LEVEL_6_30M -> 10;
			case LEVEL_7_DOWNSAMPLED -> 10;
			case LEVEL_8_MODIS -> 9;
		};
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
		private final Map<Holder<Biome>, IDhApiBiomeWrapper> holderBiomeCache = new HashMap<>();

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
			return holderBiomeCache.computeIfAbsent(biome, this::lookupBiomeByHolder);
		}

		@Nullable
		private IDhApiBiomeWrapper lookupBiomeByHolder(final Holder<Biome> biome) {
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
