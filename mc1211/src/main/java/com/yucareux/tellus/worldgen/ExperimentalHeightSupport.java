package com.yucareux.tellus.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Startup wiring and self-checks for "Increase Height" - the dense packed-coordinate profile in
 * {@link HighYPackedCoordinateProfile}. Trimmed down from upstream Tellus's version to the parts
 * this codebase actually has infrastructure for: dimension/BlockPos validation and world border
 * sizing. Upstream's spawn/preload-area guards depend on a terrain-preload subsystem this project
 * doesn't have, so they're left out rather than ported half-working.
 */
public final class ExperimentalHeightSupport {
	private ExperimentalHeightSupport() {
	}

	public static boolean isRuntimeProfileActive() {
		return HighYPackedCoordinateProfile.isEnabled();
	}

	public static String launchPropertyInstruction() {
		return HighYPackedCoordinateProfile.launchPropertyInstruction();
	}

	/**
	 * Widens the world border to the packed profile's safe horizontal range when Increase Height is
	 * active. The dense packing covers a full-width Mercator projection (+-20,037,509 blocks), so -
	 * unlike the old fixed-bit-split hack - normal (non-experimental) worlds keep vanilla's full
	 * border and never need to give up horizontal range for height.
	 */
	public static void configureWorldBorder(final EarthGeneratorSettings settings, final WorldBorder border) {
		Objects.requireNonNull(settings, "settings");
		Objects.requireNonNull(border, "border");
		if (settings.experimentalIncreaseHeight()) {
			final int absoluteLimit = Math.max(-HighYPackedCoordinateProfile.X_MIN, HighYPackedCoordinateProfile.X_MAX + 1);
			border.setAbsoluteMaxSize(absoluteLimit);
		}
	}

	/**
	 * Fails loudly and immediately if a world asks for Increase Height but the packed-coordinate
	 * profile isn't actually active (missing launch property) or {@code BlockPos}/{@code
	 * DimensionType} don't match what the profile expects - instead of silently generating a world
	 * whose terrain gets corrupted or clipped above/below the range vanilla can represent.
	 */
	public static void validateOrThrow(final EarthGeneratorSettings settings, final EarthGeneratorSettings.HeightLimits limits) {
		if (!settings.experimentalIncreaseHeight()) {
			return;
		}

		final List<String> failures = new ArrayList<>();
		validateRuntimeProfile(failures);
		validateDimensionHeightLimits(limits, failures);
		final int minY = limits.minY();
		final int maxY = limits.minY() + limits.height() - 1;
		validatePackedRoundTrip(0, minY, 0, failures);
		validatePackedRoundTrip(0, maxY, 0, failures);
		validatePackedRoundTrip(HighYPackedCoordinateProfile.X_MAX, maxY, HighYPackedCoordinateProfile.Z_MAX, failures);
		validatePackedRoundTrip(HighYPackedCoordinateProfile.X_MIN, minY, HighYPackedCoordinateProfile.Z_MIN, failures);
		if (!failures.isEmpty()) {
			throw new IllegalStateException("Increase Height cannot be enabled safely on this Minecraft build: " + String.join("; ", failures));
		}
	}

	private static void validateRuntimeProfile(final List<String> failures) {
		if (!HighYPackedCoordinateProfile.isEnabled()) {
			final String requested = HighYPackedCoordinateProfile.requestedProfile();
			failures.add(
					"launch with "
							+ HighYPackedCoordinateProfile.launchPropertyInstruction()
							+ " to enable the experimental packed-coordinate profile"
							+ (requested.isBlank() ? "" : " (current value: '" + requested + "')"));
		}

		if (BlockPos.PACKED_Y_LENGTH != HighYPackedCoordinateProfile.Y_BITS) {
			failures.add("BlockPos.PACKED_Y_LENGTH=" + BlockPos.PACKED_Y_LENGTH + ", expected " + HighYPackedCoordinateProfile.Y_BITS);
		}
	}

	private static void validateDimensionHeightLimits(final EarthGeneratorSettings.HeightLimits limits, final List<String> failures) {
		final int minY = limits.minY();
		final int maxY = limits.minY() + limits.height() - 1;
		if (DimensionType.MIN_Y > minY || DimensionType.MAX_Y < maxY || DimensionType.Y_SIZE < limits.height()) {
			failures.add(
					"DimensionType height range "
							+ DimensionType.MIN_Y
							+ ".."
							+ DimensionType.MAX_Y
							+ " (size "
							+ DimensionType.Y_SIZE
							+ ") does not cover Tellus limits "
							+ minY
							+ ".."
							+ maxY
							+ " (height "
							+ limits.height()
							+ ")");
		}
	}

	private static void validatePackedRoundTrip(final int x, final int y, final int z, final List<String> failures) {
		final long packed = BlockPos.asLong(x, y, z);
		final BlockPos unpacked = BlockPos.of(packed);
		if (unpacked.getX() != x || unpacked.getY() != y || unpacked.getZ() != z) {
			failures.add(
					"expected (" + x + "," + y + "," + z + ") but decoded (" + unpacked.getX() + "," + unpacked.getY() + "," + unpacked.getZ() + ")");
		}
	}
}
