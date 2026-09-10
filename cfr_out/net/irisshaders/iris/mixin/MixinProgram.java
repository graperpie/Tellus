/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.blaze3d.platform.GlStateManager
 *  net.minecraft.class_281
 *  net.minecraft.class_281$class_282
 *  net.minecraft.class_5913
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlStateManager;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.minecraft.class_281;
import net.minecraft.class_5913;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_281.class})
public class MixinProgram {
    @Redirect(method={"compileShaderInternal"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;process(Ljava/lang/String;)Ljava/util/List;"))
    private static List<String> iris$allowSkippingMojImportDirectives(class_5913 includeHandler, String shaderSource) {
        if (!shaderSource.contains("moj_import")) {
            return Collections.singletonList(shaderSource);
        }
        return includeHandler.method_34229(shaderSource);
    }

    @Inject(method={"compileShaderInternal"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/platform/GlStateManager;glGetShaderInfoLog(II)Ljava/lang/String;", remap=false)}, cancellable=true)
    private static void iris$causeException(class_281.class_282 arg, String string, InputStream inputStream, String string2, class_5913 arg2, CallbackInfoReturnable<Integer> cir, @Local int i) {
        cir.setReturnValue((Object)i);
        throw new ShaderCompileException(string + arg.method_1284(), GlStateManager.glGetShaderInfoLog((int)i, (int)32768));
    }
}

