package com.yucareux.tellus.mixin.meridian;

import com.leclowndu93150.meridian.sync.DimensionGeneratorSync;
import com.leclowndu93150.meridian.sync.Gzip;
import com.leclowndu93150.meridian.sync.WorldgenSyncBuilder;
import com.leclowndu93150.meridian.sync.WorldgenSyncPayload;
import com.leclowndu93150.meridian.worldgen.WorldgenDump;
import com.yucareux.tellus.integration.meridian.MeridianIntegration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps Meridian's worldgen sync away from Tellus dimensions.
 *
 * <p>{@code WorldgenDump.encodeGenerator} throws for anything that is not a noise based chunk
 * generator, and the sync payload is built inside the player join handler, so without this a
 * Meridian client joining a Tellus world takes the exception during login. Tellus dimensions do not
 * belong in that payload anyway - the client gets their settings from
 * {@code TellusMeridianPayload} instead.
 *
 * <p>When no Tellus dimension is loaded this does nothing at all and Meridian's own code runs.
 */
@Mixin(WorldgenSyncBuilder.class)
public abstract class WorldgenSyncBuilderMixin {
	@Inject(method = "build", at = @At("HEAD"), cancellable = true, remap = false)
	private static void tellus$excludeTellusDimensions(
			final MinecraftServer server, final CallbackInfoReturnable<WorldgenSyncPayload> cir) {
		boolean anyTellus = false;
		for (final ServerLevel level : server.getAllLevels()) {
			if (MeridianIntegration.isTellusLevel(level)) {
				anyTellus = true;
				break;
			}
		}

		if (!anyTellus) {
			return;
		}

		final RegistryAccess access = server.registryAccess();
		final byte[] registries = Gzip.compress(WorldgenDump.toJson(
				WorldgenDump.encodeRegistries(access, server.overworld().getChunkSource().getGenerator())));

		final List<DimensionGeneratorSync> dimensions = new ArrayList<>();
		for (final ServerLevel level : server.getAllLevels()) {
			if (MeridianIntegration.isTellusLevel(level)) {
				continue;
			}

			final ChunkGenerator generator = level.getChunkSource().getGenerator();
			if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) {
				continue;
			}
			final Optional<ResourceKey<NoiseGeneratorSettings>> settingsKey = noiseGenerator.generatorSettings().unwrapKey();

			dimensions.add(new DimensionGeneratorSync(
					level.dimension(),
					level.getSeed(),
					level.getMinBuildHeight(),
					level.getHeight(),
					settingsKey,
					Gzip.compress(WorldgenDump.toJson(WorldgenDump.encodeGenerator(generator, access)))));
		}

		cir.setReturnValue(new WorldgenSyncPayload(registries, dimensions));
	}
}
