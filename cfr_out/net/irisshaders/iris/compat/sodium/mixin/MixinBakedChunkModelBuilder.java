/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder
 *  net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.compat.sodium.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.sodium.terrain.BlockContextHolder;
import net.irisshaders.iris.vertices.sodium.terrain.VertexEncoderInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BakedChunkModelBuilder.class}, remap=false)
public class MixinBakedChunkModelBuilder
implements BlockSensitiveBufferBuilder {
    @Unique
    private final BlockContextHolder contextHolder = new BlockContextHolder();

    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void setupContextHolder(ChunkMeshBufferBuilder[] vertexBuffers, CallbackInfo ci) {
        for (ChunkMeshBufferBuilder vertexBuffer : vertexBuffers) {
            ((VertexEncoderInterface)vertexBuffer).iris$setContextHolder(this.contextHolder);
        }
    }

    @Override
    public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
        this.contextHolder.setBlockData(block, renderType, blockEmission, localPosX, localPosY, localPosZ);
    }

    @Override
    public void overrideBlock(int block) {
        this.contextHolder.overrideBlock(block);
    }

    @Override
    public void restoreBlock() {
        this.contextHolder.restoreBlock();
    }

    @Override
    public void endBlock() {
        this.contextHolder.setBlockData(0, (byte)0, (byte)0, 0, 0, 0);
    }

    @Override
    public void ignoreMidBlock(boolean b) {
        this.contextHolder.setIgnoreMidBlock(b);
    }
}

