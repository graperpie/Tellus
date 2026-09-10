/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4725
 *  net.minecraft.class_5253$class_8045
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Overwrite
 *  org.spongepowered.asm.mixin.Unique
 */
package net.irisshaders.iris.mixin.bettermipmaps;

import net.irisshaders.iris.helpers.ColorSRGB;
import net.minecraft.class_4725;
import net.minecraft.class_5253;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={class_4725.class})
public class MixinMipmapGenerator {
    @Overwrite
    private static int method_24101(int one, int two, int three, int four, boolean checkAlpha) {
        return MixinMipmapGenerator.weightedAverageColor(MixinMipmapGenerator.weightedAverageColor(one, two), MixinMipmapGenerator.weightedAverageColor(three, four));
    }

    @Unique
    private static int weightedAverageColor(int one, int two) {
        int alphaTwo;
        int alphaOne = class_5253.class_8045.method_48342((int)one);
        if (alphaOne == (alphaTwo = class_5253.class_8045.method_48342((int)two))) {
            return MixinMipmapGenerator.averageRgb(one, two, alphaOne);
        }
        if (alphaOne == 0) {
            return two & 0xFFFFFF | alphaTwo >> 2 << 24;
        }
        if (alphaTwo == 0) {
            return one & 0xFFFFFF | alphaOne >> 2 << 24;
        }
        float scale = 1.0f / (float)(alphaOne + alphaTwo);
        float relativeWeightOne = (float)alphaOne * scale;
        float relativeWeightTwo = (float)alphaTwo * scale;
        float oneR = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48345((int)one)) * relativeWeightOne;
        float oneG = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48346((int)one)) * relativeWeightOne;
        float oneB = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48347((int)one)) * relativeWeightOne;
        float twoR = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48345((int)two)) * relativeWeightTwo;
        float twoG = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48346((int)two)) * relativeWeightTwo;
        float twoB = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48347((int)two)) * relativeWeightTwo;
        float linearR = oneR + twoR;
        float linearG = oneG + twoG;
        float linearB = oneB + twoB;
        int averageAlpha = alphaOne + alphaTwo >> 1;
        return ColorSRGB.linearToSrgb(linearR, linearG, linearB, averageAlpha);
    }

    @Unique
    private static int averageRgb(int a, int b, int alpha) {
        float ar = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48345((int)a));
        float ag = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48346((int)a));
        float ab = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48347((int)a));
        float br = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48345((int)b));
        float bg = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48346((int)b));
        float bb = ColorSRGB.srgbToLinear(class_5253.class_8045.method_48347((int)b));
        return ColorSRGB.linearToSrgb((ar + br) * 0.5f, (ag + bg) * 0.5f, (ab + bb) * 0.5f, alpha);
    }
}

