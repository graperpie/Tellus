/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1044
 *  net.minecraft.class_2960
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.pbr.TextureTracker;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={RenderSystem.class})
public class MixinRenderSystem {
    @Inject(method={"initRenderer"}, at={@At(value="RETURN")}, remap=false)
    private static void iris$onRendererInit(int debugVerbosity, boolean alwaysFalse, CallbackInfo ci) {
        Iris.duringRenderSystemInit();
        GLDebug.reloadDebugState();
        IrisRenderSystem.initRenderer();
        IrisSamplers.initRenderer();
        Iris.onRenderSystemInit();
    }

    @Inject(method={"_setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/texture/AbstractTexture;getId()I", shift=At.Shift.AFTER)})
    private static void _setShaderTexture(int unit, class_2960 resourceLocation, CallbackInfo ci, @Local class_1044 tex) {
        TextureTracker.INSTANCE.onSetShaderTexture(unit, tex.method_4624());
    }

    @Inject(method={"_setShaderTexture(II)V"}, at={@At(value="RETURN")}, remap=false)
    private static void _setShaderTexture(int unit, int glId, CallbackInfo ci) {
        TextureTracker.INSTANCE.onSetShaderTexture(unit, glId);
    }
}

