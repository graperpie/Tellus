/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniform
 *  org.joml.Matrix3fc
 *  org.lwjgl.opengl.GL30C
 *  org.lwjgl.system.MemoryStack
 */
package net.irisshaders.iris.pipeline.programs;

import java.nio.FloatBuffer;
import net.caffeinemc.mods.sodium.client.gl.shader.uniform.GlUniform;
import org.joml.Matrix3fc;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

public class GlUniformMatrix3f
extends GlUniform<Matrix3fc> {
    public GlUniformMatrix3f(int index) {
        super(index);
    }

    public void set(Matrix3fc value) {
        try (MemoryStack stack = MemoryStack.stackPush();){
            FloatBuffer buf = stack.callocFloat(9);
            value.get(buf);
            GL30C.glUniformMatrix3fv((int)this.index, (boolean)false, (FloatBuffer)buf);
        }
    }
}

