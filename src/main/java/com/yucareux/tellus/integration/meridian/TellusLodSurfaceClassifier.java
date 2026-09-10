package com.yucareux.tellus.integration.meridian;

import com.yucareux.tellus.legacy.backend.earth.cover.LegacyCover;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns satellite imagery samples into the block states, biomes and canopy parameters that a long
 * range LOD column needs.
 *
 * <p>This is a port of the classification half of {@code LegacyLodGeneratorV2}, which feeds Distant
 * Horizons. It is kept separate from that class so the Distant Horizons path stays untouched; the
 * two are worth merging once the Meridian backend has proven itself.
 */
public final class TellusLodSurfaceClassifier {
	/**
	 * Every block state this classifier can emit. Meridian sizes its render palette from
	 * {@code SamplerTables.blocks()} exactly once, when the tile manager opens, so the set has to be
	 * closed and known up front - a state added later would silently render as stone.
	 */
	public static final List<BlockState> BLOCK_PALETTE = List.of(
			Blocks.WATER.defaultBlockState(),
			Blocks.PACKED_ICE.defaultBlockState(),
			Blocks.SNOW_BLOCK.defaultBlockState(),
			Blocks.SPRUCE_LEAVES.defaultBlockState(),
			Blocks.PODZOL.defaultBlockState(),
			Blocks.GRASS_BLOCK.defaultBlockState(),
			Blocks.LIME_CONCRETE.defaultBlockState(),
			Blocks.MOSS_BLOCK.defaultBlockState(),
			Blocks.SMOOTH_STONE.defaultBlockState(),
			Blocks.ANDESITE.defaultBlockState(),
			Blocks.STONE.defaultBlockState(),
			Blocks.RED_TERRACOTTA.defaultBlockState(),
			Blocks.BRICKS.defaultBlockState(),
			Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
			Blocks.RED_SAND.defaultBlockState(),
			Blocks.SAND.defaultBlockState(),
			Blocks.COARSE_DIRT.defaultBlockState(),
			Blocks.MUD.defaultBlockState(),
			Blocks.STONE_BRICKS.defaultBlockState(),
			Blocks.BLACK_CONCRETE.defaultBlockState(),
			Blocks.COBBLED_DEEPSLATE.defaultBlockState(),
			Blocks.BASALT.defaultBlockState(),
			Blocks.DIRT.defaultBlockState(),
			Blocks.GRAVEL.defaultBlockState(),
			Blocks.DEEPSLATE.defaultBlockState(),
			Blocks.MANGROVE_LEAVES.defaultBlockState(),
			Blocks.MANGROVE_LOG.defaultBlockState(),
			Blocks.SPRUCE_LOG.defaultBlockState(),
			Blocks.JUNGLE_LEAVES.defaultBlockState(),
			Blocks.JUNGLE_LOG.defaultBlockState(),
			Blocks.ACACIA_LEAVES.defaultBlockState(),
			Blocks.ACACIA_LOG.defaultBlockState(),
			Blocks.CHERRY_LEAVES.defaultBlockState(),
			Blocks.CHERRY_LOG.defaultBlockState(),
			Blocks.DARK_OAK_LEAVES.defaultBlockState(),
			Blocks.DARK_OAK_LOG.defaultBlockState(),
			Blocks.BIRCH_LEAVES.defaultBlockState(),
			Blocks.BIRCH_LOG.defaultBlockState(),
			Blocks.OAK_LEAVES.defaultBlockState(),
			Blocks.OAK_LOG.defaultBlockState(),
			Blocks.GRAY_CONCRETE.defaultBlockState(),
			Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());

	/** Every biome id {@link #classifyBiomeId} can return, in table order. */
	public static final List<String> BIOME_PALETTE = List.of(
			"minecraft:ocean",
			"minecraft:river",
			"minecraft:frozen_peaks",
			"minecraft:snowy_slopes",
			"minecraft:mangrove_swamp",
			"minecraft:swamp",
			"minecraft:jagged_peaks",
			"minecraft:stony_peaks",
			"minecraft:windswept_hills",
			"minecraft:taiga",
			"minecraft:jungle",
			"minecraft:savanna",
			"minecraft:forest",
			"minecraft:plains",
			"minecraft:desert",
			"minecraft:badlands");

	private TellusLodSurfaceClassifier() {
	}

	public record SurfaceSample(BlockState blockState, double vegetationStrength, boolean forceExposeRock) {
	}

	public record TreePalette(BlockState leaves, BlockState log) {
	}

	// ------------------------------------------------------------------ biomes

	public static String classifyBiomeId(
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

	public static boolean isForestCover(final LegacyCover cover) {
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

	public static boolean isGrassOrCropCover(final LegacyCover cover) {
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

	public static boolean isSparseCover(final LegacyCover cover) {
		return switch (cover) {
			case SPARSE_VEGETATION,
					SPARSE_SHRUB,
					SPARSE_HERBACEOUS_COVER -> true;
			default -> false;
		};
	}

	public static boolean isShrubCover(final LegacyCover cover) {
		return switch (cover) {
			case SHRUBLAND,
					SHRUBLAND_EVERGREEN,
					SHRUBLAND_DECIDUOUS -> true;
			default -> false;
		};
	}

	public static boolean isBareCover(final LegacyCover cover) {
		return switch (cover) {
			case BARE,
					BARE_CONSOLIDATED,
					BARE_UNCONSOLIDATED,
					URBAN -> true;
			default -> false;
		};
	}

	public static boolean isFloodedCover(final LegacyCover cover) {
		return switch (cover) {
			case FRESH_FLOODED_FOREST,
					SALINE_FLOODED_FOREST,
					FLOODED_VEGETATION -> true;
			default -> false;
		};
	}

	// --------------------------------------------------------------- materials

	public static BlockState underwaterMaterial(final LegacyCover cover) {
		return switch (cover) {
			case BARE_UNCONSOLIDATED, SPARSE_VEGETATION -> Blocks.SAND.defaultBlockState();
			case BARE_CONSOLIDATED, URBAN -> Blocks.STONE.defaultBlockState();
			default -> Blocks.DIRT.defaultBlockState();
		};
	}

	public static TreePalette treePaletteFromBiomeId(final String biomeId, final int hash) {
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

	// ------------------------------------------------------- satellite imagery

	/**
	 * Classifies one satellite imagery pixel into a surface block plus a vegetation strength in
	 * {@code [0, 1]}. {@code rgb} is negative when no imagery was available for the column, in which
	 * case the land cover and biome carry the classification on their own.
	 */
	public static SurfaceSample classifySatelliteSurface(
			final int rgb,
			final String biomeId,
			final LegacyCover cover,
			final boolean aboveSnowLine) {
		if (cover == LegacyCover.PERMANENT_SNOW) {
			return new SurfaceSample(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

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

		// 1. Water (deep blue to medium blue)
		if (blueDominance > 0.08D && value < 0.62D && saturation > 0.10D) {
			return new SurfaceSample(Blocks.WATER.defaultBlockState(), 0.0D, false);
		}

		// 2. Ice (light blue)
		if (blueDominance > 0.05D && value > 0.65D && saturation > 0.08D && saturation < 0.35D && hue >= 180.0D
				&& hue <= 250.0D) {
			return new SurfaceSample(Blocks.PACKED_ICE.defaultBlockState(), 0.0D, false);
		}

		// 3. Snow (white / very high brightness)
		final boolean nearWhite = Math.abs(r - g) < 0.045D && Math.abs(g - b) < 0.045D;
		if (value > 0.90D && saturation < 0.10D && nearWhite) {
			return new SurfaceSample(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

		if (aboveSnowLine && value > 0.66D && saturation < 0.15D) {
			return new SurfaceSample(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

		// 4. Green vegetation (dark to light green)
		if (hue >= 58.0D && hue <= 165.0D && greenDominance > 0.03D) {
			if (value < 0.42D && saturation > 0.20D) {
				return new SurfaceSample(Blocks.SPRUCE_LEAVES.defaultBlockState(), 0.45D, false);
			}

			if (value < 0.56D && saturation > 0.14D) {
				if (vari > 0.10D || exg > 0.06D) {
					return new SurfaceSample(Blocks.PODZOL.defaultBlockState(), 0.35D, false);
				}
			}

			if (value >= 0.42D && value <= 0.70D && saturation > 0.12D) {
				if (vegetationStrength > 0.14D && value > 0.45D) {
					return new SurfaceSample(Blocks.GRASS_BLOCK.defaultBlockState(), vegetationStrength, false);
				}
			}

			if (value > 0.70D && saturation > 0.22D && saturation < 0.45D && hue >= 70.0D && hue <= 105.0D) {
				return new SurfaceSample(Blocks.LIME_CONCRETE.defaultBlockState(), 0.25D, false);
			}

			if (value > 0.35D && value < 0.58D && saturation < 0.13D && hue > 98.0D) {
				return new SurfaceSample(Blocks.MOSS_BLOCK.defaultBlockState(), 0.20D, false);
			}
		}

		// 5. Alpine rock, preferred over dirt on mountains
		if ((aboveSnowLine || biomeRocky)
				&& saturation < 0.30D
				&& hue >= 18.0D
				&& hue <= 60.0D
				&& value > 0.50D) {
			if (value > 0.78D) {
				return new SurfaceSample(Blocks.SMOOTH_STONE.defaultBlockState(), 0.0D, true);
			}
			if (value > 0.62D) {
				return new SurfaceSample(Blocks.ANDESITE.defaultBlockState(), 0.0D, true);
			}
			return new SurfaceSample(Blocks.STONE.defaultBlockState(), 0.0D, true);
		}

		// 6. Red and orange (rooftops, terracotta, red sand)
		if (hue >= 355.0D || hue <= 50.0D) {
			if (hue >= 350.0D || hue <= 15.0D) {
				if (saturation > 0.50D && value > 0.40D && value < 0.68D) {
					return new SurfaceSample(Blocks.RED_TERRACOTTA.defaultBlockState(), 0.0D, false);
				}
				if (saturation > 0.45D && r > 0.60D && value < 0.62D) {
					return new SurfaceSample(Blocks.BRICKS.defaultBlockState(), 0.0D, true);
				}
			}

			if (hue >= 15.0D && hue <= 38.0D && saturation > 0.40D) {
				if (value > 0.68D) {
					return new SurfaceSample(Blocks.ORANGE_TERRACOTTA.defaultBlockState(), 0.0D, false);
				} else if (value > 0.52D) {
					return new SurfaceSample(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
				}
			}
		}

		// 7. Yellow / sand
		if (hue >= 40.0D && hue <= 60.0D) {
			if (saturation > 0.32D && value > 0.68D) {
				return new SurfaceSample(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
			if (saturation > 0.26D && value > 0.56D && value < 0.74D) {
				return new SurfaceSample(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
		}

		// 8. Brown (dirt, podzol, coarse dirt)
		if (hue >= 15.0D && hue <= 38.0D && saturation > 0.16D && saturation < 0.42D) {
			if (value > 0.46D && value < 0.62D) {
				return new SurfaceSample(Blocks.COARSE_DIRT.defaultBlockState(), 0.0D, false);
			}
			if (value >= 0.32D && value <= 0.48D) {
				return new SurfaceSample(Blocks.PODZOL.defaultBlockState(), 0.15D, false);
			}
		}

		// 9. Swampy green-brown (mud, mangrove roots)
		if (hue >= 50.0D && hue <= 95.0D && value < 0.50D && saturation > 0.10D && saturation < 0.34D) {
			if (g > r && (g - r) > 0.05D) {
				return new SurfaceSample(Blocks.MUD.defaultBlockState(), 0.10D, false);
			}
		}

		// 10. Gray, which covers both bare rock and urban surfaces
		if (saturation < 0.17D && value > 0.22D) {
			if ((isForestCover(cover) || isGrassOrCropCover(cover)) && !biomeDesert && !biomeBadlands) {
				if (value > 0.60D) {
					return new SurfaceSample(
							Blocks.GRASS_BLOCK.defaultBlockState(), Math.max(0.16D, vegetationStrength), false);
				}
				if (value > 0.42D) {
					return new SurfaceSample(
							Blocks.PODZOL.defaultBlockState(), Math.max(0.10D, vegetationStrength * 0.6D), false);
				}
				return new SurfaceSample(
						Blocks.COARSE_DIRT.defaultBlockState(), Math.max(0.06D, vegetationStrength * 0.45D), false);
			}

			if (isFloodedCover(cover)) {
				return new SurfaceSample(Blocks.MUD.defaultBlockState(), 0.10D, false);
			}

			if (cover == LegacyCover.URBAN) {
				if (value > 0.74D) {
					return new SurfaceSample(Blocks.STONE_BRICKS.defaultBlockState(), 0.0D, true);
				}
				if (value >= 0.52D) {
					return new SurfaceSample(Blocks.ANDESITE.defaultBlockState(), 0.0D, true);
				}
				if (value >= 0.38D) {
					return new SurfaceSample(Blocks.STONE.defaultBlockState(), 0.0D, true);
				}
				if (value >= 0.24D) {
					return new SurfaceSample(Blocks.GRAY_CONCRETE.defaultBlockState(), 0.0D, true);
				}
				return new SurfaceSample(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 0.0D, true);
			}

			if (isBareCover(cover) || isSparseCover(cover)) {
				if (biomeDesert) {
					return new SurfaceSample(Blocks.SAND.defaultBlockState(), 0.0D, false);
				}
				if (biomeBadlands) {
					return new SurfaceSample(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
				}
				if (biomeRocky || aboveSnowLine) {
					if (value > 0.65D) {
						return new SurfaceSample(Blocks.ANDESITE.defaultBlockState(), 0.0D, true);
					}
					return new SurfaceSample(Blocks.STONE.defaultBlockState(), 0.0D, true);
				}
				return new SurfaceSample(Blocks.COARSE_DIRT.defaultBlockState(), 0.0D, false);
			}

			return fallbackSurfaceFromCoverAndBiome(cover, biomeId, aboveSnowLine);
		}

		return fallbackSurfaceFromCoverAndBiome(cover, biomeId, aboveSnowLine);
	}

	public static SurfaceSample fallbackSurfaceFromCoverAndBiome(
			final LegacyCover cover,
			final String biomeId,
			final boolean aboveSnowLine) {
		if (aboveSnowLine || cover == LegacyCover.PERMANENT_SNOW) {
			return new SurfaceSample(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

		if (isFloodedCover(cover)) {
			return new SurfaceSample(Blocks.MUD.defaultBlockState(), 0.10D, false);
		}

		if (isForestCover(cover)) {
			return new SurfaceSample(Blocks.PODZOL.defaultBlockState(), 0.25D, false);
		}

		if (isGrassOrCropCover(cover)) {
			return new SurfaceSample(Blocks.GRASS_BLOCK.defaultBlockState(), 0.12D, false);
		}

		if (isSparseCover(cover)) {
			final String sparseBiome = biomeId == null ? "" : biomeId.toLowerCase();
			if (sparseBiome.contains("desert")) {
				return new SurfaceSample(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
			if (sparseBiome.contains("badlands")) {
				return new SurfaceSample(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
			}
			return new SurfaceSample(Blocks.COARSE_DIRT.defaultBlockState(), 0.0D, false);
		}

		if (cover == LegacyCover.URBAN) {
			return new SurfaceSample(Blocks.STONE_BRICKS.defaultBlockState(), 0.0D, true);
		}

		if (isBareCover(cover)) {
			final String biome = biomeId == null ? "" : biomeId.toLowerCase();
			if (biome.contains("desert")) {
				return new SurfaceSample(Blocks.SAND.defaultBlockState(), 0.0D, false);
			}
			if (biome.contains("badlands")) {
				return new SurfaceSample(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
			}
			return new SurfaceSample(Blocks.COARSE_DIRT.defaultBlockState(), 0.0D, false);
		}

		return fallbackSurfaceFromBiome(biomeId, aboveSnowLine);
	}

	public static SurfaceSample fallbackSurfaceFromBiome(final String biomeId, final boolean aboveSnowLine) {
		if (aboveSnowLine) {
			return new SurfaceSample(Blocks.SNOW_BLOCK.defaultBlockState(), 0.0D, false);
		}

		final String biome = biomeId == null ? "" : biomeId.toLowerCase();

		if (biome.contains("desert")) {
			return new SurfaceSample(Blocks.SAND.defaultBlockState(), 0.0D, false);
		}
		if (biome.contains("badlands")) {
			return new SurfaceSample(Blocks.RED_SAND.defaultBlockState(), 0.0D, false);
		}
		if (biome.contains("mountain") || biome.contains("peak") || biome.contains("stony")) {
			return new SurfaceSample(Blocks.STONE.defaultBlockState(), 0.0D, true);
		}
		if (biome.contains("taiga") || biome.contains("forest") || biome.contains("jungle")) {
			return new SurfaceSample(Blocks.PODZOL.defaultBlockState(), 0.25D, false);
		}

		return new SurfaceSample(Blocks.GRASS_BLOCK.defaultBlockState(), 0.10D, false);
	}

	public static double computeHueDegrees(
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
}
