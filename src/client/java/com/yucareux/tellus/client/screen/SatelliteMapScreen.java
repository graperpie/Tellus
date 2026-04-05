package com.yucareux.tellus.client.screen;

import com.yucareux.tellus.client.widget.map.SlippyMap;
import com.yucareux.tellus.client.widget.map.SlippyMapPoint;
import com.yucareux.tellus.client.widget.map.SlippyMapTileCache;
import com.yucareux.tellus.client.widget.map.SlippyMapWidget;
import com.yucareux.tellus.client.widget.map.component.MarkerMapComponent;
import com.yucareux.tellus.worldgen.EarthChunkGenerator;
import com.yucareux.tellus.worldgen.EarthCoordinateShift;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class SatelliteMapScreen extends Screen {
	private static final int DEFAULT_ZOOM = 6;
	private static final String SATELLITE_ATTRIBUTION = "Tiles © Esri, Maxar, Earthstar Geographics";
	private static final String SATELLITE_CACHE_NAMESPACE = "satellite_esri_v2";
	private static final String SATELLITE_TEMPLATE =
			"https://services.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}";

	private final @Nullable Screen parent;
	private SlippyMapWidget mapWidget;
	private MarkerMapComponent markerComponent;

	public SatelliteMapScreen(final @Nullable Screen parent) {
		super(Component.translatable("gui.tellus.satellite_map"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (this.mapWidget != null) {
			this.mapWidget.close();
		}

		final int mapX = 12;
		final int mapY = 24;
		final int mapWidth = this.width - 24;
		final int mapHeight = this.height - 52;

		final SlippyMapTileCache cache = new SlippyMapTileCache(SATELLITE_CACHE_NAMESPACE, SATELLITE_TEMPLATE);
		final SlippyMap map = new SlippyMap(mapWidth, mapHeight, cache, SATELLITE_ATTRIBUTION);
		this.mapWidget = new SlippyMapWidget(mapX, mapY, mapWidth, mapHeight, map);

		final SlippyMapPoint playerPoint = currentPlayerPoint();
		this.markerComponent = new MarkerMapComponent(playerPoint);
		this.mapWidget.addComponent(this.markerComponent);
		this.mapWidget.getMap().focus(playerPoint.getLatitude(), playerPoint.getLongitude(), DEFAULT_ZOOM);

		this.addRenderableOnly(this.mapWidget);
		this.addWidget(this.mapWidget);

		this.addRenderableWidget(Button.builder(Component.translatable("gui.tellus.recenter"), button -> recenter())
				.bounds(12, this.height - 22, 100, 20)
				.build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> closeScreen())
				.bounds(this.width - 112, this.height - 22, 100, 20)
				.build());
	}

	private void recenter() {
		if (this.mapWidget == null || this.markerComponent == null) {
			return;
		}
		final SlippyMapPoint point = currentPlayerPoint();
		this.markerComponent.moveMarker(point.getLatitude(), point.getLongitude());
		this.mapWidget.getMap().focus(point.getLatitude(), point.getLongitude(), this.mapWidget.getMap().getCameraZoom());
	}

	private SlippyMapPoint currentPlayerPoint() {
		final Minecraft minecraft = this.minecraft;
		if (minecraft == null || minecraft.player == null) {
			return new SlippyMapPoint(0.0D, 0.0D);
		}
		final EarthGeneratorSettings settings = resolveSettings(minecraft);
		final double lat = EarthCoordinateShift.latitudeFromWorldBlock(settings, minecraft.player.getZ());
		final double lon = EarthCoordinateShift.longitudeFromWorldBlock(settings, minecraft.player.getX());
		return new SlippyMapPoint(lat, lon);
	}

	private static EarthGeneratorSettings resolveSettings(final Minecraft client) {
		if (client.getSingleplayerServer() != null && client.level != null) {
			final var serverLevel = client.getSingleplayerServer().getLevel(client.level.dimension());
			if (serverLevel != null) {
				final ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
				if (generator instanceof EarthChunkGenerator earthGenerator) {
					return earthGenerator.settings();
				}
			}
		}
		return EarthGeneratorSettings.DEFAULT;
	}

	private void closeScreen() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	@Override
	public boolean keyPressed(@NonNull KeyEvent event) {
		if (this.minecraft != null && this.minecraft.options.keyInventory.matches(event)) {
			closeScreen();
			return true;
		}
		if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_M) {
			closeScreen();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, 0xD0101010);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
		graphics.drawString(this.font, "M: close, Mouse wheel: zoom, Drag: pan", 12, 8, 0xB0FFFFFF);
		super.render(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void onClose() {
		closeScreen();
	}

	@Override
	public void removed() {
		if (this.mapWidget != null) {
			this.mapWidget.close();
		}
	}
}
