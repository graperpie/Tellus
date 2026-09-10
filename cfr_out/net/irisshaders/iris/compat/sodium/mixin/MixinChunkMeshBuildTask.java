/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask
 *  net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials
 *  net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder$Vertex
 *  net.caffeinemc.mods.sodium.client.util.task.CancellationToken
 *  net.minecraft.class_2338$class_2339
 *  net.minecraft.class_2680
 *  net.minecraft.class_3610
 *  net.minecraft.class_6089
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.compat.sodium.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3610;
import net.minecraft.class_6089;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ChunkBuilderMeshingTask.class})
public class MixinChunkMeshBuildTask {
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    @Inject(method={"execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;"}, at={@At(value="INVOKE", target="Lnet/minecraft/core/BlockPos$MutableBlockPos;set(III)Lnet/minecraft/core/BlockPos$MutableBlockPos;", ordinal=1)})
    private void iris$setLightBlock(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir, @Local ChunkBuildBuffers buffers, @Local class_2680 blockState, @Local(ordinal=0) class_2338.class_2339 blockPos) {
        if (WorldRenderingSettings.INSTANCE.getBlockStateIds() == null) {
            return;
        }
        if (WorldRenderingSettings.INSTANCE.shouldVoxelizeLightBlocks() && blockState.method_26204() instanceof class_6089) {
            ChunkModelBuilder buildBuffers = buffers.get(DefaultMaterials.CUTOUT);
            ((BlockSensitiveBufferBuilder)buffers).ignoreMidBlock(true);
            ((BlockSensitiveBufferBuilder)buffers).beginBlock(WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt((Object)blockState), (byte)0, (byte)blockState.method_26213(), 0, 0, 0);
            for (int i = 0; i < 4; ++i) {
                this.vertices[i].x = (float)(blockPos.method_10263() & 0xF) + 0.25f;
                this.vertices[i].y = (float)(blockPos.method_10264() & 0xF) + 0.25f;
                this.vertices[i].z = (float)(blockPos.method_10260() & 0xF) + 0.25f;
                this.vertices[i].u = 0.0f;
                this.vertices[i].v = 0.0f;
                this.vertices[i].color = 0;
                this.vertices[i].light = blockState.method_26213() << 4 | blockState.method_26213() << 20;
            }
            buildBuffers.getVertexBuffer(ModelQuadFacing.UNASSIGNED).push(this.vertices, DefaultMaterials.CUTOUT);
            ((BlockSensitiveBufferBuilder)buffers).ignoreMidBlock(false);
        }
    }

    @Inject(method={"execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderModel(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V")})
    private void iris$onRenderModel(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir, @Local ChunkBuildBuffers buffers, @Local class_2680 blockState, @Local(ordinal=0) class_2338.class_2339 blockPos) {
        if (WorldRenderingSettings.INSTANCE.getBlockStateIds() == null) {
            return;
        }
        ((BlockSensitiveBufferBuilder)buffers).beginBlock(WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt((Object)blockState), (byte)0, (byte)blockState.method_26213(), blockPos.method_10263(), blockPos.method_10264(), blockPos.method_10260());
    }

    @Inject(method={"execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/FluidRenderer;render(Lnet/caffeinemc/mods/sodium/client/world/LevelSlice;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;)V")})
    private void iris$onRenderLiquid(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir, @Local ChunkBuildBuffers buffers, @Local class_2680 blockState, @Local class_3610 fluidState, @Local(ordinal=0) class_2338.class_2339 blockPos) {
        if (WorldRenderingSettings.INSTANCE.getBlockStateIds() == null) {
            return;
        }
        ((BlockSensitiveBufferBuilder)buffers).beginBlock(WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt((Object)fluidState.method_15759()), (byte)1, (byte)blockState.method_26213(), blockPos.method_10263(), blockPos.method_10264(), blockPos.method_10260());
    }

    @Inject(method={"execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/block/state/BlockState;isSolidRender(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z")})
    private void iris$onEnd(ChunkBuildContext buildContext, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir, @Local ChunkBuildBuffers buffers, @Local class_2680 blockState) {
        ((BlockSensitiveBufferBuilder)buffers).endBlock();
    }
}

