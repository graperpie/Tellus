package com.yucareux.tellus.integration.meridian;

import com.google.gson.JsonNull;
import com.leclowndu93150.meridian.api.terrain.SamplerTables;
import com.leclowndu93150.meridian.api.terrain.TreeSpecies;
import com.yucareux.tellus.integration.meridian.TellusLodSurfaceClassifier.TreePalette;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block, biome and tree tables a Meridian column sample indexes into.
 *
 * <p>Meridian reads {@code SamplerTables} once, when the tile manager opens, and sizes its render
 * palette from it. The tables therefore have to be complete before the first tile is built, so
 * everything here is enumerated eagerly rather than interned on demand.
 */
public final class MeridianSamplerTables {
	private static final String[] TREE_NAMES = {
		"mangrove", "spruce", "jungle", "acacia", "cherry", "dark_oak", "birch", "oak"
	};

	private final SamplerTables tables;
	private final Map<BlockState, Integer> blockIndices;
	private final Map<String, Integer> biomeIndices;
	private final Map<TreePalette, Integer> treeKinds;

	private MeridianSamplerTables(
			final SamplerTables tables,
			final Map<BlockState, Integer> blockIndices,
			final Map<String, Integer> biomeIndices,
			final Map<TreePalette, Integer> treeKinds) {
		this.tables = tables;
		this.blockIndices = blockIndices;
		this.biomeIndices = biomeIndices;
		this.treeKinds = treeKinds;
	}

	public static MeridianSamplerTables build(final HolderGetter<Biome> biomes) {
		final List<BlockState> blocks = List.copyOf(TellusLodSurfaceClassifier.BLOCK_PALETTE);
		final Map<BlockState, Integer> blockIndices = new HashMap<>();
		for (int i = 0; i < blocks.size(); i++) {
			blockIndices.put(blocks.get(i), i);
		}

		final List<Optional<Holder<Biome>>> biomeHolders = new ArrayList<>();
		final Map<String, Integer> biomeIndices = new HashMap<>();
		final List<String> biomeIds = TellusLodSurfaceClassifier.BIOME_PALETTE;
		for (int i = 0; i < biomeIds.size(); i++) {
			final String id = biomeIds.get(i);
			biomeHolders.add(resolveBiome(biomes, id));
			biomeIndices.put(id, i);
		}

		final Map<TreePalette, Integer> treeKinds = new LinkedHashMap<>();
		final List<TreeSpecies> species = new ArrayList<>();
		for (int i = 0; i < TREE_NAMES.length; i++) {
			final String name = TREE_NAMES[i];
			final TreePalette palette = paletteFor(name);
			// Tree kinds are 1 based in ColumnSample; 0 means "no tree here".
			treeKinds.put(palette, i + 1);
			species.add(new TreeSpecies(
					name,
					palette.log(),
					palette.leaves(),
					4,
					22,
					name,
					// No configured feature: Meridian falls back to procedural canopies built from
					// the log and leaves states above, which is what we want for imagery driven trees.
					JsonNull.INSTANCE));
		}

		// Meridian 0.1.2 added a ground-features table; Tellus never sets a ground feature kind
		// on its columns, so the palette stays empty.
		final SamplerTables tables = new SamplerTables(
				List.copyOf(biomeHolders), blocks, List.copyOf(species), List.of(), List.of());
		return new MeridianSamplerTables(tables, blockIndices, biomeIndices, treeKinds);
	}

	private static TreePalette paletteFor(final String name) {
		return switch (name) {
			case "mangrove" -> new TreePalette(
					Blocks.MANGROVE_LEAVES.defaultBlockState(), Blocks.MANGROVE_LOG.defaultBlockState());
			case "spruce" -> new TreePalette(
					Blocks.SPRUCE_LEAVES.defaultBlockState(), Blocks.SPRUCE_LOG.defaultBlockState());
			case "jungle" -> new TreePalette(
					Blocks.JUNGLE_LEAVES.defaultBlockState(), Blocks.JUNGLE_LOG.defaultBlockState());
			case "acacia" -> new TreePalette(
					Blocks.ACACIA_LEAVES.defaultBlockState(), Blocks.ACACIA_LOG.defaultBlockState());
			case "cherry" -> new TreePalette(
					Blocks.CHERRY_LEAVES.defaultBlockState(), Blocks.CHERRY_LOG.defaultBlockState());
			case "dark_oak" -> new TreePalette(
					Blocks.DARK_OAK_LEAVES.defaultBlockState(), Blocks.DARK_OAK_LOG.defaultBlockState());
			case "birch" -> new TreePalette(
					Blocks.BIRCH_LEAVES.defaultBlockState(), Blocks.BIRCH_LOG.defaultBlockState());
			default -> new TreePalette(
					Blocks.OAK_LEAVES.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState());
		};
	}

	private static Optional<Holder<Biome>> resolveBiome(final HolderGetter<Biome> biomes, final String id) {
		final ResourceLocation location = ResourceLocation.tryParse(id);
		if (location == null) {
			return Optional.empty();
		}

		return biomes.get(ResourceKey.create(Registries.BIOME, location)).map(holder -> (Holder<Biome>) holder);
	}

	public SamplerTables tables() {
		return this.tables;
	}

	/** Index into {@link SamplerTables#blocks()}, or {@code UNKNOWN_INDEX} if the state is not in the palette. */
	public int blockIndex(final BlockState state) {
		final Integer index = this.blockIndices.get(state);
		return index == null ? 65535 : index;
	}

	/** Index into {@link SamplerTables#biomes()}, or {@code UNKNOWN_INDEX} for an unknown id. */
	public int biomeIndex(final String biomeId) {
		final Integer index = this.biomeIndices.get(biomeId);
		return index == null ? 65535 : index;
	}

	/** One based tree kind for {@code ColumnSample}, or {@code 0} when the palette is unknown. */
	public int treeKind(final TreePalette palette) {
		final Integer kind = this.treeKinds.get(palette);
		return kind == null ? 0 : kind;
	}
}
