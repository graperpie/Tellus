/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_4184
 *  net.minecraft.class_4599
 *  net.minecraft.class_702
 *  net.minecraft.class_757
 *  net.minecraft.class_761
 *  net.minecraft.class_765
 *  net.minecraft.class_9779
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.fabric;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4599;
import net.minecraft.class_702;
import net.minecraft.class_757;
import net.minecraft.class_761;
import net.minecraft.class_765;
import net.minecraft.class_9779;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_761.class})
public abstract class MixinLevelRenderer {
    @Shadow
    @Final
    private class_310 field_4088;
    @Shadow
    private class_4599 field_20951;

    @Inject(method={"renderLevel"}, at={@At(value="HEAD")})
    private void iris$resetParticleManagerPhase(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        ((PhasedParticleEngine)this.field_4088.field_1713).setParticleRenderingPhase(ParticleRenderingPhase.EVERYTHING);
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;")})
    private void iris$renderOpaqueParticles(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.field_4088.method_16011().method_15405("opaque_particles");
        ParticleRenderingSettings settings = this.getRenderingSettings();
        float f = deltaTracker.method_60637(false);
        if (settings == ParticleRenderingSettings.BEFORE) {
            this.field_4088.field_1713.method_3049(lightTexture, camera, f);
        } else if (settings == ParticleRenderingSettings.MIXED) {
            ((PhasedParticleEngine)this.field_4088.field_1713).setParticleRenderingPhase(ParticleRenderingPhase.OPAQUE);
            this.field_4088.field_1713.method_3049(lightTexture, camera, f);
        }
    }

    @Redirect(method={"renderLevel"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/particle/ParticleEngine;render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;F)V"))
    private void iris$renderTranslucentAfterDeferred(class_702 instance, class_765 lightTexture, class_4184 camera, float f) {
        ParticleRenderingSettings settings = this.getRenderingSettings();
        if (settings == ParticleRenderingSettings.AFTER) {
            this.field_4088.field_1713.method_3049(lightTexture, camera, f);
        } else if (settings == ParticleRenderingSettings.MIXED) {
            ((PhasedParticleEngine)this.field_4088.field_1713).setParticleRenderingPhase(ParticleRenderingPhase.TRANSLUCENT);
            this.field_4088.field_1713.method_3049(lightTexture, camera, f);
        }
    }

    private ParticleRenderingSettings getRenderingSettings() {
        return Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::getParticleRenderingSettings).orElse(ParticleRenderingSettings.MIXED);
    }
}

