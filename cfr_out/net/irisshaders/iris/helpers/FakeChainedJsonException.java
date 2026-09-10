/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2973
 */
package net.irisshaders.iris.helpers;

import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.minecraft.class_2973;

public class FakeChainedJsonException
extends class_2973 {
    private final ShaderCompileException trueException;

    public FakeChainedJsonException(ShaderCompileException e) {
        super("", (Throwable)e);
        this.trueException = e;
    }

    public ShaderCompileException getTrueException() {
        return this.trueException;
    }
}

