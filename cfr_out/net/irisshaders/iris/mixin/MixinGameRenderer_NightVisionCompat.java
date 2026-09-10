/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1294
 *  net.minecraft.class_1309
 *  net.minecraft.class_757
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin;

import net.minecraft.class_1294;
import net.minecraft.class_1309;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_757.class}, priority=1010)
public class MixinGameRenderer_NightVisionCompat {
    @Inject(method={"getNightVisionScale"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/effect/MobEffectInstance;endsWithin(I)Z")}, cancellable=true, require=0)
    private static void iris$safecheckNightvisionStrength(class_1309 livingEntity, float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (livingEntity.method_6112(class_1294.field_5925) == null) {
            cir.setReturnValue((Object)Float.valueOf(0.0f));
        }
    }
}

