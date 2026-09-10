/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_4184
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_4599
 *  net.minecraft.class_757
 *  net.minecraft.class_761
 *  net.minecraft.class_765
 *  net.minecraft.class_9779
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.batchedentityrendering.mixin;

import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.Groupable;
import net.irisshaders.batchedentityrendering.impl.RenderBuffersExt;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4597;
import net.minecraft.class_4599;
import net.minecraft.class_757;
import net.minecraft.class_761;
import net.minecraft.class_765;
import net.minecraft.class_9779;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_761.class}, priority=999)
public class MixinLevelRenderer {
    @Unique
    private static final String RENDER_ENTITY = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V";
    @Shadow
    private class_4599 field_20951;
    @Unique
    private Groupable groupable;

    @Inject(method={"renderLevel"}, at={@At(value="HEAD")})
    private void batchedentityrendering$beginLevelRender(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (this.field_20951 instanceof DrawCallTrackingRenderBuffers) {
            ((DrawCallTrackingRenderBuffers)this.field_20951).resetDrawCounts();
        }
        ((RenderBuffersExt)this.field_20951).beginLevelRendering();
        class_4597.class_4598 provider = this.field_20951.method_23000();
        if (provider instanceof Groupable) {
            this.groupable = (Groupable)provider;
        }
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V")})
    private void batchedentityrendering$preRenderEntity(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (this.groupable != null) {
            this.groupable.startGroup();
        }
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V", shift=At.Shift.AFTER)})
    private void batchedentityrendering$postRenderEntity(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (this.groupable != null) {
            this.groupable.endGroup();
        }
    }

    @Inject(method={"renderLevel"}, at={@At(value="CONSTANT", args={"stringValue=translucent"})})
    private void batchedentityrendering$beginTranslucents(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        class_4597.class_4598 class_45982 = this.field_20951.method_23000();
        if (class_45982 instanceof FullyBufferedMultiBufferSource) {
            FullyBufferedMultiBufferSource fullyBufferedMultiBufferSource = (FullyBufferedMultiBufferSource)class_45982;
            fullyBufferedMultiBufferSource.readyUp();
        }
        if (WorldRenderingSettings.INSTANCE.shouldSeparateEntityDraws()) {
            class_310.method_1551().method_16011().method_15405("entity_draws_opaque");
            class_45982 = this.field_20951.method_23000();
            if (class_45982 instanceof FullyBufferedMultiBufferSource) {
                FullyBufferedMultiBufferSource source = (FullyBufferedMultiBufferSource)class_45982;
                source.endBatchWithType(TransparencyType.OPAQUE);
                source.endBatchWithType(TransparencyType.OPAQUE_DECAL);
                source.endBatchWithType(TransparencyType.WATER_MASK);
            } else {
                this.field_20951.method_23000().method_22993();
            }
        } else {
            class_310.method_1551().method_16011().method_15405("entity_draws");
            this.field_20951.method_23000().method_22993();
        }
    }

    @Inject(method={"renderLevel"}, at={@At(value="CONSTANT", args={"stringValue=translucent"}, shift=At.Shift.AFTER)})
    private void batchedentityrendering$endTranslucents(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (WorldRenderingSettings.INSTANCE.shouldSeparateEntityDraws()) {
            this.field_20951.method_23000().method_22993();
        }
    }

    @Inject(method={"renderLevel"}, at={@At(value="RETURN")})
    private void batchedentityrendering$endLevelRender(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        ((RenderBuffersExt)this.field_20951).endLevelRendering();
        this.groupable = null;
    }
}

