/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkVertexConsumer
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package net.irisshaders.iris.compat.sodium.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkVertexConsumer;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={ChunkVertexConsumer.class}, remap=false)
public class MixinChunkVertexConsumer
implements BlockSensitiveBufferBuilder {
    @Shadow
    @Final
    private ChunkModelBuilder modelBuilder;

    @Override
    public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
        ((BlockSensitiveBufferBuilder)this.modelBuilder).beginBlock(block, renderType, blockEmission, localPosX, localPosY, localPosZ);
    }

    @Override
    public void overrideBlock(int block) {
        ((BlockSensitiveBufferBuilder)this.modelBuilder).overrideBlock(block);
    }

    @Override
    public void restoreBlock() {
        ((BlockSensitiveBufferBuilder)this.modelBuilder).restoreBlock();
    }

    @Override
    public void endBlock() {
        ((BlockSensitiveBufferBuilder)this.modelBuilder).endBlock();
    }

    @Override
    public void ignoreMidBlock(boolean b) {
        ((BlockSensitiveBufferBuilder)this.modelBuilder).ignoreMidBlock(b);
    }
}

