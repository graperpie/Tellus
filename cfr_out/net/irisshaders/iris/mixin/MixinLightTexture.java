/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1309
 *  net.minecraft.class_310
 *  net.minecraft.class_765
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_1309;
import net.minecraft.class_310;
import net.minecraft.class_765;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_765.class})
public class MixinLightTexture {
    @Shadow
    @Final
    private class_310 field_4137;

    @Inject(method={"updateLightTexture"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel;getSkyDarken(F)F")})
    private void resetDarknessValue(float $$0, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setDarknessLightFactor(0.0f);
    }

    @Inject(method={"calculateDarknessScale"}, at={@At(value="RETURN")})
    private void storeDarknessValue(class_1309 $$0, float $$1, float $$2, CallbackInfoReturnable<Float> cir) {
        CapturedRenderingState.INSTANCE.setDarknessLightFactor((float)((double)((Float)cir.getReturnValue()).floatValue() * (Double)this.field_4137.field_1690.method_42472().method_41753()));
    }
}

