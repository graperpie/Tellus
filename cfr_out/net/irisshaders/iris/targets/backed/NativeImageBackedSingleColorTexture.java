/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1011
 *  net.minecraft.class_1011$class_1012
 *  net.minecraft.class_1043
 *  net.minecraft.class_5253$class_8045
 */
package net.irisshaders.iris.targets.backed;

import net.minecraft.class_1011;
import net.minecraft.class_1043;
import net.minecraft.class_5253;

public class NativeImageBackedSingleColorTexture
extends class_1043 {
    public NativeImageBackedSingleColorTexture(int red, int green, int blue, int alpha) {
        super(NativeImageBackedSingleColorTexture.create(class_5253.class_8045.method_48344((int)alpha, (int)blue, (int)green, (int)red)));
    }

    public NativeImageBackedSingleColorTexture(int rgba) {
        this(rgba >> 24 & 0xFF, rgba >> 16 & 0xFF, rgba >> 8 & 0xFF, rgba & 0xFF);
    }

    private static class_1011 create(int color) {
        class_1011 image = new class_1011(class_1011.class_1012.field_4997, 1, 1, false);
        image.method_4305(0, 0, color);
        return image;
    }
}

