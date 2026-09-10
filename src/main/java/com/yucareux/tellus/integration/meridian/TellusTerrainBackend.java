package com.yucareux.tellus.integration.meridian;

import com.leclowndu93150.meridian.api.backend.TerrainSamplerBackend;
import com.leclowndu93150.meridian.api.backend.TerrainSamplerContext;
import com.leclowndu93150.meridian.api.terrain.TerrainSampler;
import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import java.util.Optional;

/**
 * Offers Meridian a terrain sampler for Tellus worlds.
 *
 * <p>Meridian sorts backends by descending priority and takes the first that claims the world, so
 * this sits above the built in density backend, which would otherwise accept every world and try to
 * read noise routers out of a generator that has none.
 */
public final class TellusTerrainBackend implements TerrainSamplerBackend {
	private static final int PRIORITY = 1000;

	@Override
	public String name() {
		return TellusTerrainSampler.NAME;
	}

	@Override
	public int priority() {
		return PRIORITY;
	}

	@Override
	public Optional<TerrainSampler> tryOpen(final TerrainSamplerContext context) {
		if (context.generator() instanceof EarthChunkGenerator generator) {
			return Optional.of(new TellusTerrainSampler(generator.settings(), context.biomes()));
		}

		return Optional.empty();
	}

	@Override
	public TerrainSampler open(final TerrainSamplerContext context) {
		return tryOpen(context)
				.orElseThrow(() -> new IllegalStateException("Tellus terrain backend only supports Tellus worlds"));
	}
}
