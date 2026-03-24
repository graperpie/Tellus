package com.yucareux.tellus.client.debug;

import com.yucareux.tellus.Tellus;
import com.yucareux.tellus.legacy.backend.projection.Projection;
import com.yucareux.tellus.legacy.backend.projection.cylindrical.Equirectangular;
import com.yucareux.tellus.world.data.satellite.SatelliteTileSampler;
import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Duration;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.ChunkGenerator;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class SatelliteDebugHudOverlay {
	private static final int MAP_SIZE = 128;
	private static final int GRID = 32;
	private static final int MAP_PADDING = 10;
	private static final int SAMPLE_RADIUS_BLOCKS = 768;
	private static final int ZOOM_LEVEL = 15;
	private static final int BG_COLOR = 0xB0101010;
	private static final int LOADING_COLOR = 0x50000000;

	private final SatelliteTileSampler sampler;
	private boolean enabled;
	private boolean toggleKeyDown;

	private SatelliteDebugHudOverlay(final Minecraft client) {
		final HttpClient httpClient = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		final Path cachePath = client.gameDirectory.toPath().resolve("tellus/cache/satellite_debug");
		this.sampler = new SatelliteTileSampler(httpClient, cachePath);
	}

	public static void register(final Minecraft client) {
		final SatelliteDebugHudOverlay overlay = new SatelliteDebugHudOverlay(client);
		ClientTickEvents.END_CLIENT_TICK.register(overlay::onClientTick);
		HudRenderCallback.EVENT.register(overlay::onHudRender);
	}

	private void onClientTick(final Minecraft client) {
		final boolean currentlyDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_F8);
		if (currentlyDown && !toggleKeyDown) {
			enabled = !enabled;
			if (client.player != null) {
				final Component message = Component.literal(enabled
						? "Tellus satellite debug overlay enabled"
						: "Tellus satellite debug overlay disabled");
				client.player.displayClientMessage(message, true);
			}
		}
		toggleKeyDown = currentlyDown;
	}

	private void onHudRender(final GuiGraphics context, final net.minecraft.client.DeltaTracker deltaTracker) {
		final Minecraft client = Minecraft.getInstance();
		if (!enabled || client.player == null || client.level == null) {
			return;
		}

		final double worldScale = resolveWorldScale(client);
		final Projection projection = new Equirectangular(worldScale);
		final int startX = client.getWindow().getGuiScaledWidth() - MAP_SIZE - MAP_PADDING;
		final int startY = MAP_PADDING;
		final int cell = MAP_SIZE / GRID;
		final double playerX = client.player.getX();
		final double playerZ = client.player.getZ();

		context.fill(startX - 1, startY - 1, startX + MAP_SIZE + 1, startY + MAP_SIZE + 1, 0xC0000000);
		context.fill(startX, startY, startX + MAP_SIZE, startY + MAP_SIZE, BG_COLOR);

		for (int gz = 0; gz < GRID; gz++) {
			for (int gx = 0; gx < GRID; gx++) {
				final double localX = ((gx + 0.5D) / GRID) * 2.0D - 1.0D;
				final double localZ = ((gz + 0.5D) / GRID) * 2.0D - 1.0D;
				final double worldX = playerX + localX * SAMPLE_RADIUS_BLOCKS;
				final double worldZ = playerZ + localZ * SAMPLE_RADIUS_BLOCKS;
				final double lat = projection.lat(worldX, worldZ);
				final double lon = projection.lon(worldX, worldZ);
				final int rgb = sampler.sampleRgbNonBlocking(lat, lon, ZOOM_LEVEL);
				final int color = rgb < 0 ? LOADING_COLOR : 0xB0000000 | rgb;
				final int px0 = startX + gx * cell;
				final int py0 = startY + gz * cell;
				context.fill(px0, py0, px0 + cell, py0 + cell, color);
			}
		}

		final int cx = startX + (MAP_SIZE / 2);
		final int cy = startY + (MAP_SIZE / 2);
		context.fill(cx - 1, startY, cx + 1, startY + MAP_SIZE, 0x80FFFFFF);
		context.fill(startX, cy - 1, startX + MAP_SIZE, cy + 1, 0x80FFFFFF);

		context.drawString(
				client.font,
				I18n.get("property.tellus.legacy_lod_version.v2") + " satellite debug",
				startX,
				startY + MAP_SIZE + 4,
				0xFFFFFF);
	}

	private static double resolveWorldScale(final Minecraft client) {
		try {
			if (client.getSingleplayerServer() != null && client.level != null) {
				final var serverLevel = client.getSingleplayerServer().getLevel(client.level.dimension());
				if (serverLevel != null) {
					final ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
					if (generator instanceof EarthChunkGenerator earthGenerator) {
						return earthGenerator.settings().worldScale();
					}
				}
			}
		} catch (final Exception ignored) {
			Tellus.LOGGER.debug("Failed to resolve world scale for satellite debug HUD");
		}
		return EarthGeneratorSettings.DEFAULT.worldScale();
	}
}
