/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1922
 *  net.minecraft.class_2248
 *  net.minecraft.class_2338
 *  net.minecraft.class_2680
 *  net.minecraft.class_4970$class_4971
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.class_1922;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_4970;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_4970.class_4971.class}, priority=990)
public abstract class MixinBlockStateBehavior {
    @Shadow
    public abstract class_2248 method_26204();

    @Shadow
    protected abstract class_2680 method_26233();

    @Inject(method={"getShadeBrightness"}, at={@At(value="RETURN")}, cancellable=true)
    public void getShadeBrightness(class_1922 pBlockBehaviour$BlockStateBase0, class_2338 pBlockPos1, CallbackInfoReturnable<Float> cir) {
        float originalValue = ((Float)cir.getReturnValue()).floatValue();
        float aoLightValue = WorldRenderingSettings.INSTANCE.getAmbientOcclusionLevel();
        cir.setReturnValue((Object)Float.valueOf(1.0f - aoLightValue * (1.0f - originalValue)));
    }
}

