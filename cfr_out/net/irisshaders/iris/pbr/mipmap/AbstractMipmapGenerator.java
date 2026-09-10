/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1011
 */
package net.irisshaders.iris.pbr.mipmap;

import net.irisshaders.iris.pbr.mipmap.CustomMipmapGenerator;
import net.minecraft.class_1011;

public abstract class AbstractMipmapGenerator
implements CustomMipmapGenerator {
    @Override
    public class_1011[] generateMipLevels(class_1011[] image, int mipLevel) {
        if (mipLevel + 1 <= image.length) {
            return image;
        }
        class_1011[] newImages = new class_1011[mipLevel + 1];
        if (mipLevel > 0) {
            for (int level = 1; level <= mipLevel; ++level) {
                class_1011 prevMipmap = level == 1 ? image[0] : newImages[level - 1];
                class_1011 mipmap = new class_1011(prevMipmap.method_4307() >> 1, prevMipmap.method_4323() >> 1, false);
                int width = mipmap.method_4307();
                int height = mipmap.method_4323();
                for (int x = 0; x < width; ++x) {
                    for (int y = 0; y < height; ++y) {
                        mipmap.method_4305(x, y, this.blend(prevMipmap.method_4315(x * 2, y * 2), prevMipmap.method_4315(x * 2 + 1, y * 2), prevMipmap.method_4315(x * 2, y * 2 + 1), prevMipmap.method_4315(x * 2 + 1, y * 2 + 1)));
                    }
                }
                newImages[level] = mipmap;
            }
        }
        newImages[0] = image[0];
        return newImages;
    }

    public abstract int blend(int var1, int var2, int var3, int var4);
}

