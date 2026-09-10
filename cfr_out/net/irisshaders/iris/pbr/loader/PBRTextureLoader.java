/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1044
 *  net.minecraft.class_3300
 *  org.jetbrains.annotations.NotNull
 */
package net.irisshaders.iris.pbr.loader;

import net.minecraft.class_1044;
import net.minecraft.class_3300;
import org.jetbrains.annotations.NotNull;

public interface PBRTextureLoader<T extends class_1044> {
    public void load(T var1, class_3300 var2, PBRTextureConsumer var3);

    public static interface PBRTextureConsumer {
        public void acceptNormalTexture(@NotNull class_1044 var1);

        public void acceptSpecularTexture(@NotNull class_1044 var1);
    }
}

