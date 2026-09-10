/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.minecraft.class_1944
 *  net.minecraft.class_2338
 *  net.minecraft.class_3532
 *  net.minecraft.class_4897
 *  net.minecraft.class_4968
 *  net.minecraft.class_746
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.mixinterface.BiomeAmbienceInterface;
import net.minecraft.class_1944;
import net.minecraft.class_2338;
import net.minecraft.class_3532;
import net.minecraft.class_4897;
import net.minecraft.class_4968;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_4897.class})
public class MixinBiomeAmbientSoundsHandler
implements BiomeAmbienceInterface {
    @Shadow
    @Final
    private class_746 field_22796;
    @Unique
    private float constantMoodiness;

    @Inject(method={"method_26271", "lambda$tick$3"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I", ordinal=0)}, require=1)
    private void calculateConstantMoodiness(class_4968 ambientMoodSettings, CallbackInfo ci, @Local class_2338 blockPos) {
        int j = this.field_22796.method_37908().method_8314(class_1944.field_9284, blockPos);
        this.constantMoodiness = j > 0 ? (this.constantMoodiness -= (float)j / (float)this.field_22796.method_37908().method_8315() * 0.001f) : (this.constantMoodiness -= (float)(this.field_22796.method_37908().method_8314(class_1944.field_9282, blockPos) - 1) / (float)ambientMoodSettings.method_26101());
        this.constantMoodiness = class_3532.method_15363((float)this.constantMoodiness, (float)0.0f, (float)1.0f);
    }

    @Override
    public float getConstantMood() {
        return this.constantMoodiness;
    }
}

