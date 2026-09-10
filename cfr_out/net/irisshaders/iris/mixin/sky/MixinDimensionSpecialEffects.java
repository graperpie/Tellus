/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_5294
 *  net.minecraft.class_5636
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin.sky;

import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.minecraft.class_310;
import net.minecraft.class_5294;
import net.minecraft.class_5636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_5294.class})
public class MixinDimensionSpecialEffects {
    @Inject(method={"getSunriseColor"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$getSunriseColor(float timeOfDay, float partialTicks, CallbackInfoReturnable<float[]> cir) {
        class_5636 fogType;
        boolean blockSky = ((LevelRendererAccessor)class_310.method_1551().field_1769).invokeDoesMobEffectBlockSky(class_310.method_1551().field_1773.method_19418());
        if (blockSky) {
            cir.setReturnValue(null);
        }
        if ((fogType = class_310.method_1551().field_1773.method_19418().method_19334()) != class_5636.field_27888) {
            cir.setReturnValue(null);
        }
    }
}

