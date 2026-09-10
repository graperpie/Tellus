package com.yucareux.tellus.mixin;

import com.yucareux.tellus.worldgen.HighYPackedCoordinateProfile;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the naive fixed-bit-split {@code BlockPosBitSwapMixin} with the validated dense
 * mixed-radix packing from upstream Tellus's "Increase Height" feature. Only takes effect when
 * {@link HighYPackedCoordinateProfile#isEnabled()} - i.e. the game was launched with
 * {@code -Dtellus.experimentalHeight.coordinateProfile=global_mercator_land_v2}. Otherwise every
 * overwritten method falls back to a vanilla-equivalent fixed 26/26-bit horizontal split, so
 * normal (non-experimental) worlds are unaffected.
 */
@Mixin(BlockPos.class)
public abstract class BlockPosHighYMixin {
	private static final int TELLUS$VANILLA_HORIZONTAL_BITS = 26;

	@Mutable
	@Shadow
	@Final
	public static int PACKED_Y_LENGTH;

	@Inject(method = "<clinit>", at = @At("TAIL"))
	private static void tellus$installHighYProfile(final CallbackInfo ci) {
		if (HighYPackedCoordinateProfile.isEnabled()) {
			PACKED_Y_LENGTH = HighYPackedCoordinateProfile.Y_BITS;
		}
	}

	/**
	 * @author Tellus
	 * @reason Decodes X from the dense global Mercator coordinate profile.
	 */
	@Overwrite
	public static int getX(final long packed) {
		if (HighYPackedCoordinateProfile.isEnabled()) {
			return HighYPackedCoordinateProfile.unpackX(packed);
		}

		final int leftShift = 64 - (PACKED_Y_LENGTH + TELLUS$VANILLA_HORIZONTAL_BITS) - TELLUS$VANILLA_HORIZONTAL_BITS;
		final int rightShift = 64 - TELLUS$VANILLA_HORIZONTAL_BITS;
		return (int) (packed << leftShift >> rightShift);
	}

	/**
	 * @author Tellus
	 * @reason Decodes Y from the dense global Mercator coordinate profile.
	 */
	@Overwrite
	public static int getY(final long packed) {
		if (HighYPackedCoordinateProfile.isEnabled()) {
			return HighYPackedCoordinateProfile.unpackY(packed);
		}

		return (int) (packed << 64 - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
	}

	/**
	 * @author Tellus
	 * @reason Decodes Z from the dense global Mercator coordinate profile.
	 */
	@Overwrite
	public static int getZ(final long packed) {
		if (HighYPackedCoordinateProfile.isEnabled()) {
			return HighYPackedCoordinateProfile.unpackZ(packed);
		}

		final int zOffset = PACKED_Y_LENGTH;
		return unpackSigned(packed, zOffset, TELLUS$VANILLA_HORIZONTAL_BITS);
	}

	/**
	 * @author Tellus
	 * @reason Encodes positions with the dense global Mercator coordinate profile.
	 */
	@Overwrite
	public static long asLong(final int x, final int y, final int z) {
		if (HighYPackedCoordinateProfile.isEnabled()) {
			return HighYPackedCoordinateProfile.packClamped(x, y, z);
		}

		final long horizontalMask = (1L << TELLUS$VANILLA_HORIZONTAL_BITS) - 1L;
		final long yMask = (1L << PACKED_Y_LENGTH) - 1L;
		final int zOffset = PACKED_Y_LENGTH;
		final int xOffset = PACKED_Y_LENGTH + TELLUS$VANILLA_HORIZONTAL_BITS;
		return ((long) x & horizontalMask) << xOffset | ((long) y & yMask) | ((long) z & horizontalMask) << zOffset;
	}

	private static int unpackSigned(final long packed, final int offset, final int bits) {
		final int leftShift = 64 - offset - bits;
		final int rightShift = 64 - bits;
		return (int) (packed << leftShift >> rightShift);
	}
}
