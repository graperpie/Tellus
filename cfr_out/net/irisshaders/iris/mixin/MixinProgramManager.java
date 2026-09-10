/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_285
 *  net.minecraft.class_3679
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.minecraft.class_285;
import net.minecraft.class_3679;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_285.class})
public class MixinProgramManager {
    @Inject(method={"releaseProgram"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V", remap=false)})
    private static void iris$releaseGeometry(class_3679 shader, CallbackInfo ci) {
        if (shader instanceof ExtendedShader && ((ExtendedShader)shader).getGeometry() != null) {
            ((ExtendedShader)shader).getGeometry().method_1282();
        }
        if (shader instanceof ExtendedShader && ((ExtendedShader)shader).getTessControl() != null) {
            ((ExtendedShader)shader).getTessControl().method_1282();
        }
        if (shader instanceof ExtendedShader && ((ExtendedShader)shader).getTessEval() != null) {
            ((ExtendedShader)shader).getTessEval().method_1282();
        }
    }
}

