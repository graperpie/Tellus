/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4587
 *  net.minecraft.class_757
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_757.class})
public class MixinTweakFarPlane {
    @Shadow
    private float field_4025;

    @Shadow
    public float method_32796() {
        throw new AssertionError();
    }

    @Redirect(method={"getProjectionMatrix"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GameRenderer;getDepthFar()F"))
    private float iris$tweakViewDistanceToMatchOptiFine(class_757 renderer) {
        if (Iris.getCurrentPack().isEmpty()) {
            return this.method_32796();
        }
        float tweakedViewDistance = this.field_4025;
        return tweakedViewDistance += 1024.0f;
    }

    @Unique
    private void iris$tweakViewDistanceBasedOnFog(float f, long l, class_4587 poseStack, CallbackInfo ci) {
        if (Iris.getCurrentPack().isEmpty()) {
            return;
        }
        this.field_4025 *= 0.95f;
    }
}

