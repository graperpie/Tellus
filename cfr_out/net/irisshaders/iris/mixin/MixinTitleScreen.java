/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_437
 *  net.minecraft.class_442
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.minecraft.class_2561;
import net.minecraft.class_437;
import net.minecraft.class_442;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_442.class})
public class MixinTitleScreen
extends class_437 {
    @Unique
    private static boolean iris$hasFirstInit;

    protected MixinTitleScreen(class_2561 arg) {
        super(arg);
    }

    @Inject(method={"init"}, at={@At(value="RETURN")})
    public void iris$firstInit(CallbackInfo ci) {
        if (!iris$hasFirstInit) {
            Iris.onLoadingComplete();
        }
        iris$hasFirstInit = true;
    }
}

