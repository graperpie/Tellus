package com.yucareux.tellus.integration.meridian;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Picks a building's rough silhouette and materials from a small closed set of archetypes, instead
 * of every building in the world being the same flat light-gray box.
 *
 * <p>{@code ColumnSample} is one point per column - it has no room for real walls, a real roof shape
 * is the only kind of variety it can actually represent, by letting a column's height vary across
 * the footprint. Every block used here already lives in {@link TellusLodSurfaceClassifier#BLOCK_PALETTE},
 * so this needs no palette changes.
 */
public final class MeridianBuildingBlueprints {
	/** How a building's per-column height varies across its footprint. */
	public enum RoofShape {
		/** Uniform height - the old behaviour, used for boxy commercial/industrial buildings. */
		FLAT,
		/** Rises from the eaves (footprint edge, along the shorter axis) to a ridge line down the long axis. */
		GABLE,
		/** A shorter perimeter "base" with a full-height inset "tower" core - a stepped skyline silhouette. */
		SETBACK
	}

	public record Blueprint(BlockState wall, BlockState roof, RoofShape roofShape) {
	}

	private static final Blueprint[] RESIDENTIAL = {
			new Blueprint(Blocks.BRICKS.defaultBlockState(), Blocks.RED_TERRACOTTA.defaultBlockState(), RoofShape.GABLE),
			new Blueprint(Blocks.STONE_BRICKS.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState(), RoofShape.GABLE),
			new Blueprint(Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.RED_TERRACOTTA.defaultBlockState(), RoofShape.GABLE)
	};

	private static final Blueprint[] COMMERCIAL = {
			new Blueprint(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), Blocks.GRAY_CONCRETE.defaultBlockState(), RoofShape.FLAT),
			new Blueprint(Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), RoofShape.FLAT)
	};

	private static final Blueprint[] INDUSTRIAL = {
			new Blueprint(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.GRAY_CONCRETE.defaultBlockState(), RoofShape.FLAT),
			new Blueprint(Blocks.BASALT.defaultBlockState(), Blocks.GRAY_CONCRETE.defaultBlockState(), RoofShape.FLAT)
	};

	private static final Blueprint[] HIGHRISE = {
			new Blueprint(Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), RoofShape.SETBACK),
			new Blueprint(Blocks.SMOOTH_STONE.defaultBlockState(), Blocks.GRAY_CONCRETE.defaultBlockState(), RoofShape.SETBACK)
	};

	private static final int HIGHRISE_HEIGHT_BLOCKS = 30;
	private static final double INDUSTRIAL_MIN_FOOTPRINT_SQUARE_METERS = 300.0D;
	private static final int INDUSTRIAL_MAX_HEIGHT_BLOCKS = 14;

	private MeridianBuildingBlueprints() {
	}

	/**
	 * Height and footprint area steer the archetype toward something plausible (a 40-story footprint
	 * doesn't get a gable roof); a hash of the building's own identity picks which variant within that
	 * archetype, so two towers next to each other don't look identical.
	 */
	public static Blueprint select(final int heightBlocks, final double footprintAreaSquareMeters, final int hash) {
		final Blueprint[] pool;
		if (heightBlocks >= HIGHRISE_HEIGHT_BLOCKS) {
			pool = HIGHRISE;
		} else if (footprintAreaSquareMeters >= INDUSTRIAL_MIN_FOOTPRINT_SQUARE_METERS && heightBlocks <= INDUSTRIAL_MAX_HEIGHT_BLOCKS) {
			pool = INDUSTRIAL;
		} else if (Integer.remainderUnsigned(hash, 3) == 0) {
			pool = COMMERCIAL;
		} else {
			pool = RESIDENTIAL;
		}

		return pool[Integer.remainderUnsigned(hash >>> 8, pool.length)];
	}
}
