/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1044
 *  net.minecraft.class_1049
 *  net.minecraft.class_1059
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.loader;

import java.util.HashMap;
import java.util.Map;
import net.irisshaders.iris.pbr.loader.AtlasPBRLoader;
import net.irisshaders.iris.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.pbr.loader.SimplePBRLoader;
import net.minecraft.class_1044;
import net.minecraft.class_1049;
import net.minecraft.class_1059;
import org.jetbrains.annotations.Nullable;

public class PBRTextureLoaderRegistry {
    public static final PBRTextureLoaderRegistry INSTANCE = new PBRTextureLoaderRegistry();
    private final Map<Class<?>, PBRTextureLoader<?>> loaderMap = new HashMap();

    public <T extends class_1044> void register(Class<? extends T> clazz, PBRTextureLoader<T> loader) {
        this.loaderMap.put(clazz, loader);
    }

    @Nullable
    public <T extends class_1044> PBRTextureLoader<T> getLoader(Class<? extends T> clazz) {
        return this.loaderMap.get(clazz);
    }

    static {
        INSTANCE.register(class_1049.class, new SimplePBRLoader());
        INSTANCE.register(class_1059.class, new AtlasPBRLoader());
    }
}

