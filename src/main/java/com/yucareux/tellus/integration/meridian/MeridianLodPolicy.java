package com.yucareux.tellus.integration.meridian;

import net.minecraft.util.Mth;

/**
 * Decides how much source data a Meridian LOD tile is allowed to pull.
 *
 * <p>Meridian tiles are 66x66 columns at a spacing of {@code 1 << level} blocks, which lines up
 * with a Distant Horizons detail level, so the sampling policy is carried over from
 * {@code LegacyLodGeneratorV2} unchanged. The point of all of it is that quality is dropped when
 * the data is fetched rather than after: a coarse tile asks for coarse imagery and coarse
 * elevation instead of downloading full resolution tiles and averaging them away.
 */
public final class MeridianLodPolicy {
	private enum Band {
		HIGH_RES,
		SENTINEL_10M,
		SENTINEL_10M_VEG,
		THIRTY_M,
		DOWNSAMPLED,
		MODIS;

		private static Band forLevel(final int level) {
			return switch (level) {
				case 0, 1, 2, 3 -> HIGH_RES;
				case 4 -> SENTINEL_10M;
				case 5 -> SENTINEL_10M_VEG;
				case 6 -> THIRTY_M;
				case 7 -> DOWNSAMPLED;
				default -> MODIS;
			};
		}
	}

	private MeridianLodPolicy() {
	}

	/** Columns between satellite imagery samples; intermediate columns are interpolated. */
	public static int satelliteStrideColumns(final int level) {
		final int base = switch (Band.forLevel(level)) {
			case HIGH_RES -> 2;
			case SENTINEL_10M -> 3;
			case SENTINEL_10M_VEG -> 4;
			case THIRTY_M -> 6;
			case DOWNSAMPLED -> 8;
			case MODIS -> 12;
		};

		if (level >= 8) {
			return Math.max(base, 12);
		}
		if (level >= 6) {
			return Math.max(base, 8);
		}
		return base;
	}


	/** Columns between biome classifications. */
	public static int biomeStrideColumns(final int level) {
		final int base = switch (Band.forLevel(level)) {
			case HIGH_RES -> 1;
			case SENTINEL_10M -> 2;
			case SENTINEL_10M_VEG -> 3;
			case THIRTY_M -> 4;
			case DOWNSAMPLED -> 6;
			case MODIS -> 8;
		};

		if (level >= 8) {
			return Math.max(base, 8);
		}
		if (level >= 6) {
			return Math.max(base, 5);
		}
		return base;
	}

	/**
	 * Web Mercator zoom for satellite imagery, chosen so one imagery pixel stays close to one LOD
	 * column. Zoom is reduced further at high latitudes, where Web Mercator compresses
	 * metres per pixel.
	 */
	public static int satelliteZoom(final int level, final double latitude, final double metersPerBlock) {
		int zoom = switch (level) {
			case 0, 1 -> 15;
			case 2 -> 14;
			case 3 -> 13;
			case 4 -> 12;
			case 5 -> 11;
			case 6 -> 10;
			case 7 -> 9;
			case 8 -> 8;
			case 9 -> 7;
			default -> 6;
		};

		final int blockSpan = 1 << Math.min(level, 24);
		final double metersPerColumn = blockSpan * metersPerBlock;
		if (metersPerColumn >= 4096.0D) {
			zoom = Math.min(zoom, 6);
		} else if (metersPerColumn >= 2048.0D) {
			zoom = Math.min(zoom, 7);
		} else if (metersPerColumn >= 1024.0D) {
			zoom = Math.min(zoom, 8);
		}

		final double absLat = Math.abs(latitude);
		if (absLat >= 70.0D) {
			zoom -= 2;
		} else if (absLat >= 55.0D) {
			zoom -= 1;
		}

		return Mth.clamp(zoom, 5, 15);
	}

	/**
	 * Whether roads/buildings are worth querying at all at this level. Matches V2's
	 * {@code overtureStreamingActive} cutoff - past this the tile footprint is big enough that a road
	 * or a single building wouldn't be visible at LOD anyway.
	 */
	public static boolean overtureActive(final int level) {
		return level <= 6;
	}

}
