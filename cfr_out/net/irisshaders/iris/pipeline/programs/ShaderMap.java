/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_5944
 */
package net.irisshaders.iris.pipeline.programs;

import java.util.function.Function;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.minecraft.class_5944;

public class ShaderMap {
    private final class_5944[] shaders;

    public ShaderMap(Function<ShaderKey, class_5944> factory) {
        ShaderKey[] ids = ShaderKey.values();
        this.shaders = new class_5944[ids.length];
        for (int i = 0; i < ids.length; ++i) {
            this.shaders[i] = factory.apply(ids[i]);
        }
    }

    public class_5944 getShader(ShaderKey id) {
        return this.shaders[id.ordinal()];
    }
}

