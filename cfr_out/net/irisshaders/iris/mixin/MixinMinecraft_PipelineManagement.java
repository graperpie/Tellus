/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_434$class_9678
 *  net.minecraft.class_437
 *  net.minecraft.class_638
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.minecraft.class_310;
import net.minecraft.class_434;
import net.minecraft.class_437;
import net.minecraft.class_638;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_310.class})
public class MixinMinecraft_PipelineManagement {
    @Inject(method={"clearClientLevel"}, at={@At(value="HEAD")})
    public void iris$trackLastDimensionOnLeave(class_437 arg, CallbackInfo ci) {
        Iris.lastDimension = Iris.getCurrentDimension();
    }

    @Inject(method={"setLevel"}, at={@At(value="HEAD")})
    private void iris$trackLastDimensionOnLevelChange(class_638 clientLevel, class_434.class_9678 reason, CallbackInfo ci) {
        Iris.lastDimension = Iris.getCurrentDimension();
    }

    @Inject(method={"updateLevelInEngines"}, at={@At(value="HEAD")})
    private void iris$resetPipeline(@Nullable class_638 level, CallbackInfo ci) {
        if (Iris.getCurrentDimension() != Iris.lastDimension) {
            Iris.logger.info("Reloading pipeline on dimension change: " + String.valueOf(Iris.lastDimension) + " => " + String.valueOf(Iris.getCurrentDimension()));
            Iris.getPipelineManager().destroyPipeline();
            if (level != null) {
                Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());
            }
        }
    }
}

