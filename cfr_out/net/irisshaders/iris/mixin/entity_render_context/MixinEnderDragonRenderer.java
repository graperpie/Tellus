/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_895
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.entity_render_context;

import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_895;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_895.class})
public class MixinEnderDragonRenderer {
    @Unique
    private static final NamespacedId END_BEAM = new NamespacedId("minecraft", "end_crystal_beam");
    @Unique
    private static int previousE;

    @Inject(method={"renderCrystalBeams"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V")})
    private static void changeId(float f, float g, float h, float i, int j, class_4587 poseStack, class_4597 multiBufferSource, int k, CallbackInfo ci) {
        if (WorldRenderingSettings.INSTANCE.getEntityIds() == null) {
            return;
        }
        previousE = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        CapturedRenderingState.INSTANCE.setCurrentEntity(WorldRenderingSettings.INSTANCE.getEntityIds().applyAsInt((Object)END_BEAM));
    }

    @Inject(method={"renderCrystalBeams"}, at={@At(value="RETURN")})
    private static void changeId2(CallbackInfo ci) {
        if (previousE != 0) {
            CapturedRenderingState.INSTANCE.setCurrentEntity(previousE);
            previousE = 0;
        }
    }
}

