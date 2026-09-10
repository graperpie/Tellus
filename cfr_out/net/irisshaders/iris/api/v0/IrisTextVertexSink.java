/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_293
 */
package net.irisshaders.iris.api.v0;

import java.nio.ByteBuffer;
import net.minecraft.class_293;

public interface IrisTextVertexSink {
    public class_293 getUnderlyingVertexFormat();

    public ByteBuffer getUnderlyingByteBuffer();

    public void quad(float var1, float var2, float var3, float var4, float var5, int var6, float var7, float var8, float var9, float var10, int var11);
}

