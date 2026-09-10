/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_5912
 */
package net.irisshaders.iris.mixinterface;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import net.minecraft.class_5912;

public interface ShaderInstanceInterface {
    public void iris$createExtraShaders(class_5912 var1, String var2) throws IOException;

    public void setShouldSkip(MethodHandle var1);
}

