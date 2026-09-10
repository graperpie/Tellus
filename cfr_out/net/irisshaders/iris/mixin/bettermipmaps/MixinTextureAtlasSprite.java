/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1011
 *  net.minecraft.class_1011$class_1012
 *  net.minecraft.class_2960
 *  net.minecraft.class_5253$class_8045
 *  net.minecraft.class_7764
 *  org.lwjgl.system.MemoryUtil
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package net.irisshaders.iris.mixin.bettermipmaps;

import java.util.Locale;
import net.irisshaders.iris.helpers.ColorSRGB;
import net.minecraft.class_1011;
import net.minecraft.class_2960;
import net.minecraft.class_5253;
import net.minecraft.class_7764;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={class_7764.class})
public class MixinTextureAtlasSprite {
    @Unique
    private static final float[] SRGB_TO_LINEAR = new float[256];
    @Mutable
    @Shadow
    @Final
    private class_1011 field_40539;

    @Unique
    private static void iris$fillInTransparentPixelColors(class_1011 nativeImage) {
        long ppPixel = MixinTextureAtlasSprite.getPointerRGBA(nativeImage);
        int pixelCount = nativeImage.method_4323() * nativeImage.method_4307();
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;
        float totalWeight = 0.0f;
        for (int pixelIndex = 0; pixelIndex < pixelCount; ++pixelIndex) {
            long pPixel = ppPixel + (long)pixelIndex * 4L;
            int color = MemoryUtil.memGetInt((long)pPixel);
            int alpha = class_5253.class_8045.method_48342((int)color);
            if (alpha == 0) continue;
            float weight = alpha;
            r += ColorSRGB.srgbToLinear(class_5253.class_8045.method_48345((int)color)) * weight;
            g += ColorSRGB.srgbToLinear(class_5253.class_8045.method_48346((int)color)) * weight;
            b += ColorSRGB.srgbToLinear(class_5253.class_8045.method_48347((int)color)) * weight;
            totalWeight += weight;
        }
        if (totalWeight == 0.0f) {
            return;
        }
        int averageColor = ColorSRGB.linearToSrgb(r /= totalWeight, g /= totalWeight, b /= totalWeight, 0);
        for (int pixelIndex = 0; pixelIndex < pixelCount; ++pixelIndex) {
            long pPixel = ppPixel + (long)pixelIndex * 4L;
            int color = MemoryUtil.memGetInt((long)pPixel);
            int alpha = class_5253.class_8045.method_48342((int)color);
            if (alpha != 0) continue;
            MemoryUtil.memPutInt((long)pPixel, (int)averageColor);
        }
    }

    @Unique
    private static long getPointerRGBA(class_1011 nativeImage) {
        if (nativeImage.method_4318() != class_1011.class_1012.field_4997) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Tried to get pointer to RGBA pixel data on NativeImage of wrong format; have %s", nativeImage.method_4318()));
        }
        return nativeImage.field_4988;
    }

    @Redirect(method={"<init>"}, at=@At(value="FIELD", target="Lnet/minecraft/client/renderer/texture/SpriteContents;originalImage:Lcom/mojang/blaze3d/platform/NativeImage;", opcode=181))
    private void iris$beforeGenerateMipLevels(class_7764 instance, class_1011 nativeImage, class_2960 resourceLocation) {
        if (resourceLocation.method_12832().contains("leaves")) {
            this.field_40539 = nativeImage;
            return;
        }
        MixinTextureAtlasSprite.iris$fillInTransparentPixelColors(nativeImage);
        this.field_40539 = nativeImage;
    }

    static {
        for (int i = 0; i < 256; ++i) {
            MixinTextureAtlasSprite.SRGB_TO_LINEAR[i] = (float)Math.pow((double)i / 255.0, 2.2);
        }
    }
}

