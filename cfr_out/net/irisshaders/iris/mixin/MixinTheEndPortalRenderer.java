/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_2350
 *  net.minecraft.class_2640
 *  net.minecraft.class_2960
 *  net.minecraft.class_4587
 *  net.minecraft.class_4587$class_4665
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597
 *  net.minecraft.class_840
 *  org.joml.Matrix3f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.minecraft.class_1921;
import net.minecraft.class_2350;
import net.minecraft.class_2640;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_840;
import org.joml.Matrix3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_840.class})
public class MixinTheEndPortalRenderer {
    @Unique
    private static final float RED = 0.075f;
    @Unique
    private static final float GREEN = 0.15f;
    @Unique
    private static final float BLUE = 0.2f;

    @Shadow
    protected float method_3594() {
        return 0.75f;
    }

    @Shadow
    protected float method_35793() {
        return 0.375f;
    }

    @Inject(method={"render(Lnet/minecraft/world/level/block/entity/TheEndPortalBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"}, at={@At(value="HEAD")}, cancellable=true)
    public void iris$onRender(class_2640 entity, float tickDelta, class_4587 poseStack, class_4597 multiBufferSource, int light, int overlay, CallbackInfo ci) {
        if (Iris.getCurrentPack().isEmpty()) {
            return;
        }
        ci.cancel();
        class_4588 vertexConsumer = multiBufferSource.getBuffer(class_1921.method_23572((class_2960)class_840.field_4407));
        class_4587.class_4665 pose = poseStack.method_23760();
        Matrix3f normal = poseStack.method_23760().method_23762();
        float progress = SystemTimeUniforms.TIMER.getFrameTimeCounter() * 0.01f % 1.0f;
        float topHeight = this.method_3594();
        float bottomHeight = this.method_35793();
        this.quad(entity, vertexConsumer, pose, normal, class_2350.field_11036, progress, overlay, light, 0.0f, topHeight, 1.0f, 1.0f, topHeight, 1.0f, 1.0f, topHeight, 0.0f, 0.0f, topHeight, 0.0f);
        this.quad(entity, vertexConsumer, pose, normal, class_2350.field_11033, progress, overlay, light, 0.0f, bottomHeight, 1.0f, 0.0f, bottomHeight, 0.0f, 1.0f, bottomHeight, 0.0f, 1.0f, bottomHeight, 1.0f);
        this.quad(entity, vertexConsumer, pose, normal, class_2350.field_11043, progress, overlay, light, 0.0f, topHeight, 0.0f, 1.0f, topHeight, 0.0f, 1.0f, bottomHeight, 0.0f, 0.0f, bottomHeight, 0.0f);
        this.quad(entity, vertexConsumer, pose, normal, class_2350.field_11039, progress, overlay, light, 0.0f, topHeight, 1.0f, 0.0f, topHeight, 0.0f, 0.0f, bottomHeight, 0.0f, 0.0f, bottomHeight, 1.0f);
        this.quad(entity, vertexConsumer, pose, normal, class_2350.field_11035, progress, overlay, light, 0.0f, topHeight, 1.0f, 0.0f, bottomHeight, 1.0f, 1.0f, bottomHeight, 1.0f, 1.0f, topHeight, 1.0f);
        this.quad(entity, vertexConsumer, pose, normal, class_2350.field_11034, progress, overlay, light, 1.0f, topHeight, 1.0f, 1.0f, bottomHeight, 1.0f, 1.0f, bottomHeight, 0.0f, 1.0f, topHeight, 0.0f);
    }

    @Unique
    private void quad(class_2640 entity, class_4588 vertexConsumer, class_4587.class_4665 pose, Matrix3f normal, class_2350 direction, float progress, int overlay, int light, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
        if (!entity.method_11400(direction)) {
            return;
        }
        float nx = direction.method_10148();
        float ny = direction.method_10164();
        float nz = direction.method_10165();
        vertexConsumer.method_56824(pose, x1, y1, z1).method_22915(0.075f, 0.15f, 0.2f, 1.0f).method_22913(0.0f + progress, 0.0f + progress).method_22922(overlay).method_60803(light).method_60831(pose, nx, ny, nz);
        vertexConsumer.method_56824(pose, x2, y2, z2).method_22915(0.075f, 0.15f, 0.2f, 1.0f).method_22913(0.0f + progress, 0.2f + progress).method_22922(overlay).method_60803(light).method_60831(pose, nx, ny, nz);
        vertexConsumer.method_56824(pose, x3, y3, z3).method_22915(0.075f, 0.15f, 0.2f, 1.0f).method_22913(0.2f + progress, 0.2f + progress).method_22922(overlay).method_60803(light).method_60831(pose, nx, ny, nz);
        vertexConsumer.method_56824(pose, x4, y4, z4).method_22915(0.075f, 0.15f, 0.2f, 1.0f).method_22913(0.2f + progress, 0.0f + progress).method_22922(overlay).method_60803(light).method_60831(pose, nx, ny, nz);
    }
}

