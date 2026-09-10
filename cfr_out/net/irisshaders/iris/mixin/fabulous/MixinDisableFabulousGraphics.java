/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_315
 *  net.minecraft.class_5365
 *  net.minecraft.class_761
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.fabulous;

import net.irisshaders.iris.Iris;
import net.minecraft.class_310;
import net.minecraft.class_315;
import net.minecraft.class_5365;
import net.minecraft.class_761;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_761.class})
public class MixinDisableFabulousGraphics {
    @Inject(method={"onResourceManagerReload"}, at={@At(value="HEAD")})
    private void iris$disableFabulousGraphicsOnResourceReload(CallbackInfo ci) {
        this.iris$disableFabulousGraphics();
    }

    @Inject(method={"allChanged"}, at={@At(value="HEAD")})
    private void iris$disableFabulousGraphicsOnLevelRendererReload(CallbackInfo ci) {
        this.iris$disableFabulousGraphics();
    }

    @Unique
    private void iris$disableFabulousGraphics() {
        class_315 options = class_310.method_1551().field_1690;
        if (!Iris.getIrisConfig().areShadersEnabled()) {
            return;
        }
        if (options.method_42534().method_41753() == class_5365.field_25429) {
            options.method_42534().method_41748((Object)class_5365.field_25428);
        }
    }
}

