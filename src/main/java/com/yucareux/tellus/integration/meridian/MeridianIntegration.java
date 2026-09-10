package com.yucareux.tellus.integration.meridian;

import com.leclowndu93150.meridian.registry.TerrainSamplerRegistry;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Wires Tellus into Meridian.
 *
 * <p>Two things have to happen. Meridian's backend registry gets a Tellus sampler so any code that
 * opens a sampler for a Tellus level gets real terrain instead of a noise router that does not
 * exist. And, because Meridian renders LODs on the client from a client side sampler, the generator
 * settings are pushed to joining players so that side can build one too.
 */
public final class MeridianIntegration {
	public static final String MOD_ID = "meridian";

	private static final Logger LOGGER = LogUtils.getLogger();

	private MeridianIntegration() {
	}

	public static void bootstrap() {
		TerrainSamplerRegistry.register(new TellusTerrainBackend());
		PayloadTypeRegistry.playS2C().register(TellusMeridianPayload.TYPE, TellusMeridianPayload.CODEC.cast());
		LOGGER.info("Registered the Tellus terrain backend with Meridian");
	}

	/** Sends the Tellus generator settings for every Tellus dimension the server is running. */
	public static void onPlayerJoin(final ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, TellusMeridianPayload.TYPE)) {
			return;
		}

		for (final ServerLevel level : player.server.getAllLevels()) {
			if (!(level.getChunkSource().getGenerator() instanceof EarthChunkGenerator generator)) {
				continue;
			}

			final EarthGeneratorSettings settings = generator.settings();
			EarthGeneratorSettings.CODEC
					.encodeStart(JsonOps.INSTANCE, settings)
					.resultOrPartial(error -> LOGGER.warn("Could not encode Tellus settings for Meridian: {}", error))
					.ifPresent(json -> ServerPlayNetworking.send(
							player,
							new TellusMeridianPayload(level.dimension(), level.getSeed(), json.toString())));
		}
	}

	/** Whether the level is one this integration should take over from Meridian's vanilla sync. */
	public static boolean isTellusLevel(final ServerLevel level) {
		return level.getChunkSource().getGenerator() instanceof EarthChunkGenerator;
	}
}
