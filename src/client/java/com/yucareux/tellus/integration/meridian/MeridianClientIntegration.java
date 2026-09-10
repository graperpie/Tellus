package com.yucareux.tellus.integration.meridian;

import com.google.gson.JsonParser;
import com.leclowndu93150.meridian.api.terrain.TerrainSampler;
import com.leclowndu93150.meridian.client.lod.ClientLodHolder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.yucareux.tellus.mixin.meridian.ClientTerrainHolderAccessor;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Builds a Tellus terrain sampler on the client and hands it to Meridian.
 *
 * <p>Meridian's LOD renderer is client side and pulls columns from whatever sampler
 * {@code ClientTerrainHolder} holds for the current dimension. Its own sync can only describe
 * vanilla noise generators, so Tellus sends the generator settings itself and builds a sampler here.
 *
 * <p>Installation is checked every tick rather than done once on arrival. Meridian clears that map
 * wholesale whenever its own worldgen sync lands, and the two payloads arrive independently during
 * login, so a one shot install would be a coin flip on ordering. Re-checking is cheap and repairs
 * itself whichever way the race falls.
 */
@Environment(EnvType.CLIENT)
public final class MeridianClientIntegration {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final Map<ResourceKey<Level>, PendingWorld> PENDING = new ConcurrentHashMap<>();

	private record PendingWorld(EarthGeneratorSettings settings, long seed, String digest) {
	}

	private MeridianClientIntegration() {
	}

	public static void bootstrap() {
		ClientPlayNetworking.registerGlobalReceiver(
				TellusMeridianPayload.TYPE, (payload, context) -> context.client().execute(() -> accept(payload)));
		ClientTickEvents.END_CLIENT_TICK.register(MeridianClientIntegration::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PENDING.clear());
	}

	private static void accept(final TellusMeridianPayload payload) {
		final EarthGeneratorSettings settings;
		try {
			settings = EarthGeneratorSettings.CODEC
					.parse(JsonOps.INSTANCE, JsonParser.parseString(payload.settingsJson()))
					.getOrThrow();
		} catch (final RuntimeException e) {
			LOGGER.error("Could not read the Tellus generator settings sent for Meridian", e);
			return;
		}

		PENDING.put(
				payload.dimension(),
				new PendingWorld(settings, payload.seed(), digestOf(payload.settingsJson())));
		LOGGER.info("Meridian will render {} from Tellus elevation and satellite imagery", payload.dimension().location());
	}

	private static void tick(final Minecraft minecraft) {
		if (PENDING.isEmpty()) {
			return;
		}

		final ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}

		final ResourceKey<Level> dimension = level.dimension();
		final PendingWorld pending = PENDING.get(dimension);
		if (pending == null) {
			return;
		}

		final Map<ResourceKey<Level>, TerrainSampler> samplers = ClientTerrainHolderAccessor.tellus$samplers();
		final TerrainSampler existing = samplers.get(dimension);
		if (existing != null && TellusTerrainSampler.NAME.equals(existing.backendName())) {
			return;
		}

		final TerrainSampler sampler;
		try {
			sampler = new TellusTerrainSampler(
					pending.settings(), minecraft.getConnection().registryAccess().lookupOrThrow(Registries.BIOME));
		} catch (final RuntimeException e) {
			LOGGER.error("Could not open a Tellus terrain sampler for Meridian in {}", dimension.location(), e);
			PENDING.remove(dimension);
			return;
		}

		// Drop tiles built from whatever sampler was there before, so the tile manager reopens on ours.
		ClientLodHolder.clear();

		final TerrainSampler previous = samplers.put(dimension, sampler);
		if (previous != null) {
			previous.close();
		}

		ClientTerrainHolderAccessor.tellus$seeds().put(dimension, pending.seed());
		ClientTerrainHolderAccessor.tellus$digests().put(dimension, pending.digest());

		LOGGER.info("Installed the Tellus terrain sampler for Meridian in {}", dimension.location());
	}

	/**
	 * Short hash of the settings, used by Meridian to key its on disk sample cache. Changing world
	 * scale or DEM selection changes the digest, which retires cached columns instead of serving
	 * stale ones.
	 */
	private static String digestOf(final String settingsJson) {
		try {
			final byte[] hash =
					MessageDigest.getInstance("SHA-256").digest(settingsJson.getBytes(StandardCharsets.UTF_8));
			final StringBuilder hex = new StringBuilder();
			for (int i = 0; i < 8; i++) {
				hex.append(String.format(Locale.ROOT, "%02x", hash[i]));
			}
			return hex.toString();
		} catch (final NoSuchAlgorithmException e) {
			return "nodigest";
		}
	}
}
