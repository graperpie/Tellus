/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1011
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.mipmap;

import net.minecraft.class_1011;
import org.jetbrains.annotations.Nullable;

public interface CustomMipmapGenerator {
    public class_1011[] generateMipLevels(class_1011[] var1, int var2);

    public static interface Provider {
        @Nullable
        public CustomMipmapGenerator getMipmapGenerator();
    }
}

