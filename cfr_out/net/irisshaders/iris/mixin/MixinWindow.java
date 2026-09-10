/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1041
 *  net.minecraft.class_323
 *  net.minecraft.class_3678
 *  net.minecraft.class_543
 *  org.lwjgl.glfw.GLFW
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.minecraft.class_1041;
import net.minecraft.class_323;
import net.minecraft.class_3678;
import net.minecraft.class_543;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1041.class}, priority=1010)
public class MixinWindow {
    @Inject(method={"<init>"}, at={@At(value="INVOKE", target="Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V", shift=At.Shift.AFTER)})
    private void iris$enableDebugContext(class_3678 arg, class_323 arg2, class_543 arg3, String string, String string2, CallbackInfo ci) {
        if (Iris.getIrisConfig().areDebugOptionsEnabled()) {
            GLFW.glfwWindowHint((int)139271, (int)1);
            GLFW.glfwWindowHint((int)139274, (int)0);
            Iris.logger.info("OpenGL debug context activated.");
        }
    }
}

