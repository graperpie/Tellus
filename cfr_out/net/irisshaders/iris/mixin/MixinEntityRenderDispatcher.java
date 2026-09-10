/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntFunction
 *  net.minecraft.class_1297
 *  net.minecraft.class_2338
 *  net.minecraft.class_243
 *  net.minecraft.class_2791
 *  net.minecraft.class_4538
 *  net.minecraft.class_4587
 *  net.minecraft.class_4587$class_4665
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597
 *  net.minecraft.class_898
 *  org.joml.Quaternionf
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_1297;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2791;
import net.minecraft.class_4538;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_898;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_898.class})
public class MixinEntityRenderDispatcher {
    @Unique
    private static final String RENDER_SHADOW = "renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V";
    @Unique
    private static final String RENDER_BLOCK_SHADOW = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderBlockShadow(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;DDDFF)V";
    @Unique
    private static final NamespacedId shadowId = new NamespacedId("minecraft", "entity_shadow");
    @Unique
    private static final NamespacedId flameId = new NamespacedId("minecraft", "entity_flame");
    @Unique
    private static int cachedId;

    @Inject(method={"renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$maybeSuppressEntityShadow(class_4587 poseStack, class_4597 bufferSource, class_1297 entity, float opacity, float tickDelta, class_4538 level, float radius, CallbackInfo ci) {
        if (!MixinEntityRenderDispatcher.iris$maybeSuppressShadow(ci)) {
            Object2IntFunction<NamespacedId> entityIds = WorldRenderingSettings.INSTANCE.getEntityIds();
            if (entityIds == null) {
                return;
            }
            cachedId = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
            CapturedRenderingState.INSTANCE.setCurrentEntity(entityIds.getInt((Object)shadowId));
        }
    }

    @Inject(method={"renderShadow"}, at={@At(value="RETURN")})
    private static void restoreShadow(class_4587 pPoseStack0, class_4597 pMultiBufferSource1, class_1297 pEntity2, float pFloat3, float pFloat4, class_4538 pLevelReader5, float pFloat6, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentEntity(cachedId);
        cachedId = 0;
    }

    @Inject(method={"renderBlockShadow"}, at={@At(value="HEAD")}, cancellable=true)
    private static void renderBlockShadow(class_4587.class_4665 pPoseStack$Pose0, class_4588 pVertexConsumer1, class_2791 pChunkAccess2, class_4538 pLevelReader3, class_2338 pBlockPos4, double pDouble5, double pDouble6, double pDouble7, float pFloat8, float pFloat9, CallbackInfo ci) {
        MixinEntityRenderDispatcher.iris$maybeSuppressShadow(ci);
    }

    @Inject(method={"renderOffsetShadow"}, at={@At(value="HEAD")}, cancellable=true, require=0, remap=false, expect=0)
    private static void iris$maybeSuppressEntityShadow(class_4587 poseStack, class_4597 bufferSource, class_1297 entity, float opacity, float tickDelta, class_4538 level, float radius, class_243 offset, CallbackInfo ci) {
        MixinEntityRenderDispatcher.iris$maybeSuppressShadow(ci);
    }

    @Unique
    private static boolean iris$maybeSuppressShadow(CallbackInfo ci) {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null && pipeline.shouldDisableVanillaEntityShadows()) {
            ci.cancel();
            return true;
        }
        return false;
    }

    @Inject(method={"renderFlame"}, at={@At(value="HEAD")})
    private void iris$setFlameId(class_4587 pEntityRenderDispatcher0, class_4597 pMultiBufferSource1, class_1297 pEntity2, Quaternionf pQuaternionf3, CallbackInfo ci) {
        Object2IntFunction<NamespacedId> entityIds = WorldRenderingSettings.INSTANCE.getEntityIds();
        if (entityIds == null) {
            return;
        }
        cachedId = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        CapturedRenderingState.INSTANCE.setCurrentEntity(entityIds.getInt((Object)flameId));
    }

    @Inject(method={"renderFlame"}, at={@At(value="RETURN")})
    private void restoreFlameId(class_4587 pEntityRenderDispatcher0, class_4597 pMultiBufferSource1, class_1297 pEntity2, Quaternionf pQuaternionf3, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentEntity(cachedId);
        cachedId = 0;
    }
}

