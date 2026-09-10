/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1959
 *  net.minecraft.class_1972
 *  net.minecraft.class_5321
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.uniforms.BiomeUniforms;
import net.minecraft.class_1959;
import net.minecraft.class_1972;
import net.minecraft.class_5321;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_1972.class})
public class MixinBiomes {
    @Unique
    private static int currentId = 0;

    @Inject(method={"register"}, at={@At(value="TAIL")})
    private static void iris$registerBiome(String string, CallbackInfoReturnable<class_5321<class_1959>> cir) {
        BiomeUniforms.getBiomeMap().put((Object)((class_5321)cir.getReturnValue()), currentId++);
    }
}

