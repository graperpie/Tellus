package com.yucareux.tellus.client.debug;

import com.yucareux.tellus.Tellus;
import com.yucareux.tellus.client.widget.map.SlippyMap;
import com.yucareux.tellus.client.widget.map.SlippyMapPoint;
import com.yucareux.tellus.client.widget.map.SlippyMapTile;
import com.yucareux.tellus.client.widget.map.SlippyMapTileCache;
import com.yucareux.tellus.client.widget.map.SlippyMapTilePos;
import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import com.yucareux.tellus.worldgen.EarthCoordinateShift;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.lwjgl.glfw.GLFW;

public final class SatelliteDebugHudOverlay {
	private static final int MAP_SIZE = 192;
	private static final int MAP_PADDING = 10;
	private static final int MINIMAP_ZOOM = 16;
	private static final String SATELLITE_ATTRIBUTION = "Tiles © Esri, Maxar, Earthstar Geographics";
	private static final String SATELLITE_CACHE_NAMESPACE = "satellite_esri_v2";
	private static final String SATELLITE_TEMPLATE =
			"https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}";
	private static final int BG_COLOR = 0xB0101010;

	private final SlippyMap map;
	private boolean enabled;
	private boolean toggleKeyDown;

	private SatelliteDebugHudOverlay(final Minecraft client) {
		final SlippyMapTileCache cache = new SlippyMapTileCache(SATELLITE_CACHE_NAMESPACE, SATELLITE_TEMPLATE);
		this.map = new SlippyMap(MAP_SIZE, MAP_SIZE, cache, SATELLITE_ATTRIBUTION);
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

		final EarthGeneratorSettings settings = resolveSettings(client);
		final SlippyMapPoint playerPoint = new SlippyMapPoint(
				EarthCoordinateShift.latitudeFromWorldBlock(settings, client.player.getZ()),
				EarthCoordinateShift.longitudeFromWorldBlock(settings, client.player.getX()));
		this.map.focus(playerPoint.getLatitude(), playerPoint.getLongitude(), MINIMAP_ZOOM);

		final int startX = client.getWindow().getGuiScaledWidth() - MAP_SIZE - MAP_PADDING;
		final int startY = MAP_PADDING;

		context.fill(startX - 1, startY - 1, startX + MAP_SIZE + 1, startY + MAP_SIZE + 1, 0xC0000000);
		context.fill(startX, startY, startX + MAP_SIZE, startY + MAP_SIZE, BG_COLOR);
		context.enableScissor(startX, startY, startX + MAP_SIZE, startY + MAP_SIZE);

		final int cameraX = this.map.getCameraX();
		final int cameraY = this.map.getCameraY();
		final int cameraZoom = this.map.getCameraZoom();
		final List<SlippyMapTilePos> visibleTiles = this.map.getVisibleTiles();
		final List<SlippyMapTilePos> cascadedTiles = this.map.cascadeTiles(visibleTiles);
		cascadedTiles.sort(Comparator.comparingInt(SlippyMapTilePos::getZoom));

		for (final SlippyMapTilePos pos : cascadedTiles) {
			final SlippyMapTile tile = this.map.getTile(pos);
			renderTile(context, startX, startY, cameraX, cameraY, cameraZoom, pos, tile, deltaTracker.getGameTimeDeltaPartialTick(false));
		}

		context.disableScissor();

		final int cx = startX + (MAP_SIZE / 2);
		final int cy = startY + (MAP_SIZE / 2);
		context.fill(cx - 1, startY, cx + 1, startY + MAP_SIZE, 0x80FFFFFF);
		context.fill(startX, cy - 1, startX + MAP_SIZE, cy + 1, 0x80FFFFFF);

		final String footer = "F8 overlay | M fullscreen";
		context.drawString(client.font, footer, startX, startY + MAP_SIZE + 4, 0xFFFFFF);
	}

	private static void renderTile(
			final GuiGraphics graphics,
			final int originX,
			final int originY,
			final int cameraX,
			final int cameraY,
			final int cameraZoom,
			final SlippyMapTilePos pos,
			final SlippyMapTile image,
			final float delta) {
		image.update(delta);
		final Identifier location = image.getLocation();
		if (location == null) {
			return;
		}

		final int deltaZoom = cameraZoom - pos.getZoom();
		final double zoomScale = Math.pow(2.0D, deltaZoom);
		final int size = Mth.floor(SlippyMap.TILE_SIZE * zoomScale);
		final int renderX = (pos.getX() << deltaZoom) * SlippyMap.TILE_SIZE - cameraX;
		final int renderY = (pos.getY() << deltaZoom) * SlippyMap.TILE_SIZE - cameraY;
		final int textureSize = Math.max(SlippyMap.TILE_SIZE, size);

		graphics.pose().pushMatrix();
		graphics.pose().translate((float) originX, (float) originY);
		final int scaleFactor = Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
		final float scale = 1.0F / scaleFactor;
		graphics.pose().scale(scale, scale);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				location,
				renderX,
				renderY,
				0.0F,
				0.0F,
				size,
				size,
				textureSize,
				textureSize);
		graphics.pose().popMatrix();
	}

	private static EarthGeneratorSettings resolveSettings(final Minecraft client) {
		try {
			if (client.getSingleplayerServer() != null && client.level != null) {
				final var serverLevel = client.getSingleplayerServer().getLevel(client.level.dimension());
				if (serverLevel != null) {
					final ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
					if (generator instanceof EarthChunkGenerator earthGenerator) {
						return earthGenerator.settings();
					}
				}
			}
		} catch (final Exception ignored) {
			Tellus.LOGGER.debug("Failed to resolve Earth settings for satellite debug HUD");
		}
		return EarthGeneratorSettings.DEFAULT;
	}
}
