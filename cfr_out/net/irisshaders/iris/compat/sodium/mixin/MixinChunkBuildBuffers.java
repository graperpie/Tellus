/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder
 *  net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package net.irisshaders.iris.compat.sodium.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={ChunkBuildBuffers.class})
public class MixinChunkBuildBuffers
implements BlockSensitiveBufferBuilder {
    @Shadow
    @Final
    private Reference2ReferenceOpenHashMap<TerrainRenderPass, BakedChunkModelBuilder> builders;

    @Override
    public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
        for (BakedChunkModelBuilder value : this.builders.values()) {
            ((BlockSensitiveBufferBuilder)value).beginBlock(block, renderType, blockEmission, localPosX, localPosY, localPosZ);
        }
    }

    @Override
    public void overrideBlock(int block) {
        for (BakedChunkModelBuilder value : this.builders.values()) {
            ((BlockSensitiveBufferBuilder)value).overrideBlock(block);
        }
    }

    @Override
    public void restoreBlock() {
        for (BakedChunkModelBuilder value : this.builders.values()) {
            ((BlockSensitiveBufferBuilder)value).restoreBlock();
        }
    }

    @Override
    public void endBlock() {
        for (BakedChunkModelBuilder value : this.builders.values()) {
            ((BlockSensitiveBufferBuilder)value).endBlock();
        }
    }

    @Override
    public void ignoreMidBlock(boolean b) {
        for (BakedChunkModelBuilder value : this.builders.values()) {
            ((BlockSensitiveBufferBuilder)value).ignoreMidBlock(b);
        }
    }
}

