/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_5636
 *  net.minecraft.class_638$class_5271
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin.sky;

import net.minecraft.class_310;
import net.minecraft.class_5636;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_638.class_5271.class})
public class MixinClientLevelData_DisableVoidPlane {
    @Inject(method={"getHorizonHeight"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$getHorizonHeight(CallbackInfoReturnable<Double> cir) {
        class_5636 fogType = class_310.method_1551().field_1773.method_19418().method_19334();
        if (fogType != class_5636.field_27888) {
            cir.setReturnValue((Object)Double.NEGATIVE_INFINITY);
        }
    }
}

