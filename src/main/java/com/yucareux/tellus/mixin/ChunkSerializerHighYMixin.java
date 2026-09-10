package com.yucareux.tellus.mixin;

import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The other reason a tall dimension isn't enough on its own: {@code ChunkSerializer} stores each
 * chunk section's Y coordinate as an NBT <em>byte</em> ({@code i2b} truncation on write), which only
 * represents section indices -128..127 - a maximum of 4096 blocks of height, regardless of what
 * {@code BlockPos} or {@code DimensionType} can address. A section saved above that silently wraps
 * to the wrong Y on save, and reads back at the wrong height on the next load - while sections still
 * resident in memory from generation this session look fine, because they never round-tripped
 * through the truncating byte.
 *
 * <p>Redirects the specific calls inside {@code read}/{@code write} that touch the byte tag or a
 * narrowed section-Y parameter, smuggling the real value between them with a thread-local rather
 * than rewriting either method wholesale.
 */
@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerHighYMixin {
	private static final ThreadLocal<Integer> TELLUS$READ_SECTION_Y = ThreadLocal.withInitial(() -> 0);
	private static final ThreadLocal<Integer> TELLUS$WRITE_SECTION_Y = ThreadLocal.withInitial(() -> 0);

	@Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;getByte(Ljava/lang/String;)B"))
	private static byte tellus$readFullSectionY(final CompoundTag tag, final String key) {
		final int sectionY = tag.getInt(key);
		TELLUS$READ_SECTION_Y.set(sectionY);
		return (byte) sectionY;
	}

	@Redirect(
			method = "read",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getSectionIndexFromSectionY(I)I"))
	private static int tellus$indexFullSectionY(final ServerLevel level, final int ignoredSectionY) {
		return level.getSectionIndexFromSectionY(TELLUS$READ_SECTION_Y.get());
	}

	@Redirect(
			method = "read",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/ChunkPos;I)Lnet/minecraft/core/SectionPos;"))
	private static SectionPos tellus$lightPositionWithFullSectionY(final ChunkPos chunkPos, final int ignoredSectionY) {
		return SectionPos.of(chunkPos, TELLUS$READ_SECTION_Y.get());
	}

	@Redirect(
			method = "write",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getSectionIndexFromSectionY(I)I"))
	private static int tellus$captureFullSectionY(final ChunkAccess chunk, final int sectionY) {
		TELLUS$WRITE_SECTION_Y.set(sectionY);
		return chunk.getSectionIndexFromSectionY(sectionY);
	}

	@Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putByte(Ljava/lang/String;B)V"))
	private static void tellus$writeFullSectionY(final CompoundTag tag, final String key, final byte ignoredSectionY) {
		tag.putInt(key, TELLUS$WRITE_SECTION_Y.get());
	}
}
