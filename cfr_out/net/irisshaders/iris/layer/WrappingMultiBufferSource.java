/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 */
package net.irisshaders.iris.layer;

import java.util.function.Function;
import net.minecraft.class_1921;

public interface WrappingMultiBufferSource {
    public void pushWrappingFunction(Function<class_1921, class_1921> var1);

    public void popWrappingFunction();

    public void assertWrapStackEmpty();
}

