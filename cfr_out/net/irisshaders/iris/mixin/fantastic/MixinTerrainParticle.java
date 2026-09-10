/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_2338
 *  net.minecraft.class_2680
 *  net.minecraft.class_3999
 *  net.minecraft.class_4696
 *  net.minecraft.class_638
 *  net.minecraft.class_727
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin.fantastic;

import net.minecraft.class_1921;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3999;
import net.minecraft.class_4696;
import net.minecraft.class_638;
import net.minecraft.class_727;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_727.class})
public class MixinTerrainParticle {
    @Unique
    private boolean isOpaque;

    @Inject(method={"<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"}, at={@At(value="RETURN")})
    private void iris$resolveTranslucency(class_638 level, double x, double y, double z, double velocityX, double velocityY, double velocityZ, class_2680 blockState, class_2338 blockPos, CallbackInfo ci) {
        class_1921 type = class_4696.method_23679((class_2680)blockState);
        if (type == class_1921.method_23577() || type == class_1921.method_23581() || type == class_1921.method_23579()) {
            this.isOpaque = true;
        }
    }

    @Inject(method={"getRenderType"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$overrideParticleSheet(CallbackInfoReturnable<class_3999> cir) {
        if (this.isOpaque) {
            cir.setReturnValue((Object)class_3999.field_17827);
        }
    }
}

