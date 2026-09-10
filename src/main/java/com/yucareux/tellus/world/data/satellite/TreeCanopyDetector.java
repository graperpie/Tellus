package com.yucareux.tellus.world.data.satellite;

import net.minecraft.util.Mth;

/**
 * Pure satellite-RGB tree canopy detector.
 *
 * <p>No Satlas, no land-cover dependency. Places with trees in imagery get trees in game:
 * dark/mid greens with strong green dominance, real saturation and vegetation indices (VARI/ExG).
 * Bright lime fields, grey urban, yellow sand, blue water/ice and white snow all return 0.
 *
 * <p>Shared by chunk-gen, Meridian LOD and Distant Horizons V2 so all three agree.
 */
public final class TreeCanopyDetector {
	private TreeCanopyDetector() {
	}

	public enum CanopyBand {
		NONE,
		SPARSE,
		OPEN,
		MEDIUM,
		DENSE,
		CLOSED;
	}

	/**
	 * Canopy strength in [0,1]. 0 means no canopy (no trees). Negative rgb (no imagery) also 0;
	 * callers fall back to cover-class in that case.
	 */
	public static double canopyStrength(final int rgb) {
		if (rgb < 0) {
			return 0.0D;
		}
		final double r = ((rgb >> 16) & 0xFF) / 255.0D;
		final double g = ((rgb >> 8) & 0xFF) / 255.0D;
		final double b = (rgb & 0xFF) / 255.0D;
		final double max = Math.max(r, Math.max(g, b));
		final double min = Math.min(r, Math.min(g, b));
		final double value = max;
		final double chroma = max - min;
		final double saturation = max <= 1.0E-5D ? 0.0D : chroma / max;
		final double hue = hueDegrees(r, g, b, max, chroma);
		final double blueDominance = b - Math.max(r, g);
		final double greenDominance = g - Math.max(r, b);
		final double vari = (g - r) / Math.max(0.12D, g + r - b);
		final double exg = (2.0D * g) - r - b;

		// Water / ice / snow: never canopy.
		if (blueDominance > 0.05D) {
			return 0.0D;
		}
		if (value > 0.90D && saturation < 0.10D && Math.abs(r - g) < 0.045D && Math.abs(g - b) < 0.045D) {
			return 0.0D;
		}
		// Must be green-hued with real green dominance and saturation.
		if (hue < 60.0D || hue > 168.0D) {
			return 0.0D;
		}
		if (greenDominance <= 0.030D) {
			return 0.0D;
		}
		if (saturation < 0.12D) {
			return 0.0D;
		}
		if (vari <= 0.02D && exg <= 0.02D) {
			return 0.0D;
		}

		double raw = Mth.clamp((vari * 0.9D) + (Math.max(0.0D, exg) * 0.55D) + (Math.max(0.0D, greenDominance) * 0.6D), 0.0D, 1.0D);

		// Bright lime fields / crops read green but are not canopy: suppress hard.
		if (value > 0.72D) {
			raw *= 0.25D;
		} else if (value > 0.62D && saturation < 0.30D) {
			raw *= 0.45D;
		}
		// Pale grey-green (dry grass, olive scrub): not canopy.
		if (saturation < 0.16D && value > 0.45D) {
			raw *= 0.4D;
		}
		// Dark saturated conifer green: boost so real forest saturates.
		if (value < 0.42D && saturation > 0.20D) {
			raw = Math.max(raw, 0.60D);
		}

		// Final gate: weak signal = grass, not trees.
		if (raw < 0.30D) {
			return 0.0D;
		}
		return Mth.clamp((raw - 0.30D) / 0.70D, 0.0D, 1.0D);
	}

	public static boolean isCanopy(final int rgb) {
		return canopyStrength(rgb) > 0.0D;
	}

	/**
	 * Imagery fallback when rgb is missing: ESA WorldCover 10 = tree cover, 95 = mangroves.
	 */
	public static boolean isCanopyOrCoverFallback(final int rgb, final int coverClass) {
		if (rgb >= 0) {
			return isCanopy(rgb);
		}
		return coverClass == 10 || coverClass == 95;
	}

	public static CanopyBand bandForStrength(final double strength) {
		if (strength <= 0.0D) {
			return CanopyBand.NONE;
		}
		if (strength < 0.25D) {
			return CanopyBand.SPARSE;
		}
		if (strength < 0.45D) {
			return CanopyBand.OPEN;
		}
		if (strength < 0.65D) {
			return CanopyBand.MEDIUM;
		}
		if (strength < 0.85D) {
			return CanopyBand.DENSE;
		}
		return CanopyBand.CLOSED;
	}

	/** Chance percent that a candidate position actually plants. NONE = 0 (fixes dense LOD0/1). */
	public static int chancePercent(final CanopyBand band) {
		return switch (band) {
			case NONE -> 0;
			case SPARSE -> 12;
			case OPEN -> 28;
			case MEDIUM -> 50;
			case DENSE -> 68;
			case CLOSED -> 82;
		};
	}

	public static int baseHeight(final CanopyBand band) {
		return switch (band) {
			case NONE -> 0;
			case SPARSE -> 6;
			case OPEN -> 8;
			case MEDIUM -> 10;
			case DENSE -> 12;
			case CLOSED -> 14;
		};
	}

	public static int maxHeight(final CanopyBand band) {
		return switch (band) {
			case NONE -> 0;
			case SPARSE -> 10;
			case OPEN -> 13;
			case MEDIUM -> 16;
			case DENSE -> 19;
			case CLOSED -> 22;
		};
	}

	private static double hueDegrees(final double r, final double g, final double b, final double max, final double chroma) {
		if (chroma <= 1.0E-6D) {
			return 0.0D;
		}
		double hue;
		if (max == r) {
			hue = ((g - b) / chroma) % 6.0D;
		} else if (max == g) {
			hue = ((b - r) / chroma) + 2.0D;
		} else {
			hue = ((r - g) / chroma) + 4.0D;
		}
		hue *= 60.0D;
		if (hue < 0.0D) {
			hue += 360.0D;
		}
		return hue;
	}
}
