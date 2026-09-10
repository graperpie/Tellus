/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_287
 *  net.minecraft.class_4587$class_4665
 *  net.minecraft.class_4588
 *  net.minecraft.class_777
 *  org.spongepowered.asm.mixin.Mixin
 */
package net.irisshaders.iris.mixin.vertices.block_rendering;

import java.util.Arrays;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.class_287;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_777;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={class_287.class}, priority=999)
public abstract class MixinBufferBuilder_SeparateAo
implements class_4588 {
    public void method_22920(class_4587.class_4665 matrixEntry, class_777 quad, float[] brightnesses, float red, float green, float blue, float alpha, int[] lights, int overlay, boolean useQuadColorData) {
        if (WorldRenderingSettings.INSTANCE.shouldUseSeparateAo()) {
            float[] brightnesses1 = brightnesses;
            boolean brightnessIndex = false;
            brightnesses = new float[brightnesses.length];
            Arrays.fill(brightnesses, 1.0f);
        }
        super.method_22920(matrixEntry, quad, brightnesses, red, green, blue, alpha, lights, overlay, useQuadColorData);
    }
}

