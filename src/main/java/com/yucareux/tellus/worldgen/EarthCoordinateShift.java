package com.yucareux.tellus.worldgen;

import net.minecraft.util.Mth;

public final class EarthCoordinateShift {
	private static final double EQUATOR_CIRCUMFERENCE = 40075017.0;

	private EarthCoordinateShift() {
	}

	public static int spawnOffsetX(final EarthGeneratorSettings settings) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		return Mth.floor(settings.spawnLongitude() * blocksPerDegree);
	}

	public static int spawnOffsetZ(final EarthGeneratorSettings settings) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		return Mth.floor(-settings.spawnLatitude() * blocksPerDegree);
	}

	public static int worldBlockXFromLongitude(final EarthGeneratorSettings settings, final double longitude) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		return Mth.floor(longitude * blocksPerDegree) - spawnOffsetX(settings);
	}

	public static int worldBlockZFromLatitude(final EarthGeneratorSettings settings, final double latitude) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		return Mth.floor(-latitude * blocksPerDegree) - spawnOffsetZ(settings);
	}

	public static double longitudeFromWorldBlock(final EarthGeneratorSettings settings, final double worldBlockX) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		if (blocksPerDegree == 0.0) {
			return 0.0;
		}
		double earthBlockX = worldBlockX + spawnOffsetX(settings);
		return earthBlockX / blocksPerDegree;
	}

	public static double latitudeFromWorldBlock(final EarthGeneratorSettings settings, final double worldBlockZ) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		if (blocksPerDegree == 0.0) {
			return 0.0;
		}
		double earthBlockZ = worldBlockZ + spawnOffsetZ(settings);
		return -earthBlockZ / blocksPerDegree;
	}

	private static double blocksPerDegree(final double worldScale) {
		if (worldScale <= 0.0) {
			return 0.0;
		}
		return (EQUATOR_CIRCUMFERENCE / 360.0) / worldScale;
	}
}
