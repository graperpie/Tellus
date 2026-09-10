package com.yucareux.tellus.mixin.meridian;

import com.leclowndu93150.meridian.api.terrain.TerrainSampler;
import com.leclowndu93150.meridian.client.terrain.ClientTerrainHolder;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Opens up the maps Meridian's client keeps its per dimension samplers in.
 *
 * <p>Meridian only ever fills these from its own worldgen sync, which describes vanilla noise
 * generators. Tellus supplies its own sampler instead, so it needs to put one in the same place the
 * LOD tile manager reads from. The maps are mutable, so getters are enough.
 */
@Mixin(ClientTerrainHolder.class)
public interface ClientTerrainHolderAccessor {
	@Accessor("SAMPLERS")
	static Map<ResourceKey<Level>, TerrainSampler> tellus$samplers() {
		throw new AssertionError("mixin not applied");
	}

	@Accessor("SEEDS")
	static Map<ResourceKey<Level>, Long> tellus$seeds() {
		throw new AssertionError("mixin not applied");
	}

	@Accessor("DIGESTS")
	static Map<ResourceKey<Level>, String> tellus$digests() {
		throw new AssertionError("mixin not applied");
	}
}
