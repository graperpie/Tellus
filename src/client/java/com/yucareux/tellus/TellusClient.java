package com.yucareux.tellus;

import com.mojang.blaze3d.platform.InputConstants;
import com.yucareux.tellus.client.debug.SatelliteDebugHudOverlay;
import com.yucareux.tellus.client.screen.EarthTeleportScreen;
import com.yucareux.tellus.client.screen.SatelliteMapScreen;
import com.yucareux.tellus.network.GeoTpOpenMapPayload;
import com.yucareux.tellus.network.TellusWeatherPayload;
import com.yucareux.tellus.world.realtime.SnowGrid;
import com.yucareux.tellus.world.realtime.TellusRealtimeState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public class TellusClient implements ClientModInitializer {
	private boolean satelliteMapKeyDown;

	@Override
	public void onInitializeClient() {
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			SatelliteDebugHudOverlay.register(client);
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			final boolean mapKeyDown = InputConstants.isKeyDown(client.getWindow(), GLFW.GLFW_KEY_M);
			if (mapKeyDown && !this.satelliteMapKeyDown && client.player != null && client.screen == null) {
				client.setScreen(new SatelliteMapScreen(null));
			}
			this.satelliteMapKeyDown = mapKeyDown;
		});

		ClientPlayNetworking.registerGlobalReceiver(GeoTpOpenMapPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				Minecraft minecraft = context.client();
				Screen parent = minecraft.screen;
				minecraft.setScreen(new EarthTeleportScreen(parent, payload.latitude(), payload.longitude()));
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(TellusWeatherPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				SnowGrid grid = payload.historicalSnowEnabled() && payload.spacingBlocks() > 0
						? new SnowGrid(
								payload.centerX(),
								payload.centerZ(),
								payload.spacingBlocks(),
								payload.snowIndex()
						)
						: SnowGrid.empty();
				TellusRealtimeState.updateWeatherState(
						payload.weatherEnabled(),
						payload.precipitationMode(),
						payload.historicalSnowEnabled(),
						grid
				);
			});
		});
	}
}
