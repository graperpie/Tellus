/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1011
 *  net.minecraft.class_1011$class_1012
 *  net.minecraft.class_1043
 */
package net.irisshaders.iris.targets.backed;

import java.util.Objects;
import java.util.Random;
import java.util.function.IntSupplier;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.gl.texture.TextureType;
import net.minecraft.class_1011;
import net.minecraft.class_1043;

public class NativeImageBackedNoiseTexture
extends class_1043
implements TextureAccess {
    public NativeImageBackedNoiseTexture(int size) {
        super(NativeImageBackedNoiseTexture.create(size));
    }

    private static class_1011 create(int size) {
        class_1011 image = new class_1011(class_1011.class_1012.field_4997, size, size, false);
        Random random = new Random(0L);
        for (int x = 0; x < size; ++x) {
            for (int y = 0; y < size; ++y) {
                int color = random.nextInt() | 0xFF000000;
                image.method_4305(x, y, color);
            }
        }
        return image;
    }

    public void method_4524() {
        class_1011 image = Objects.requireNonNull(this.method_4525());
        this.method_23207();
        image.method_22619(0, 0, 0, 0, 0, image.method_4307(), image.method_4323(), true, false, false, false);
    }

    @Override
    public TextureType getType() {
        return TextureType.TEXTURE_2D;
    }

    @Override
    public IntSupplier getTextureId() {
        return () -> ((NativeImageBackedNoiseTexture)this).method_4624();
    }
}

