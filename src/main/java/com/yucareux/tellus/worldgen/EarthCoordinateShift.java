package com.yucareux.tellus.worldgen;

import net.minecraft.util.Mth;

public final class EarthCoordinateShift {
	private static final double EQUATOR_CIRCUMFERENCE = 40075017.0;

	private EarthCoordinateShift() {
	}

	public static int spawnOffsetX(EarthGeneratorSettings settings) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		return Mth.floor(settings.spawnLongitude() * blocksPerDegree);
	}

	public static int spawnOffsetZ(EarthGeneratorSettings settings) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		return Mth.floor(-settings.spawnLatitude() * blocksPerDegree);
	}

	public static int worldBlockXFromLongitude(EarthGeneratorSettings settings, double longitude) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		return Mth.floor(longitude * blocksPerDegree) - spawnOffsetX(settings);
	}

	public static int worldBlockZFromLatitude(EarthGeneratorSettings settings, double latitude) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		return Mth.floor(-latitude * blocksPerDegree) - spawnOffsetZ(settings);
	}

	public static double longitudeFromWorldBlock(EarthGeneratorSettings settings, double worldBlockX) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		if (blocksPerDegree == 0.0) {
			return 0.0;
		}
		double earthBlockX = worldBlockX + spawnOffsetX(settings);
		return earthBlockX / blocksPerDegree;
	}

	public static double latitudeFromWorldBlock(EarthGeneratorSettings settings, double worldBlockZ) {
		double blocksPerDegree = blocksPerDegree(settings.worldScale());
		if (blocksPerDegree == 0.0) {
			return 0.0;
		}
		double earthBlockZ = worldBlockZ + spawnOffsetZ(settings);
		return -earthBlockZ / blocksPerDegree;
	}

	private static double blocksPerDegree(double worldScale) {
		if (worldScale <= 0.0) {
			return 0.0;
		}
		return (EQUATOR_CIRCUMFERENCE / 360.0) / worldScale;
	}
}
