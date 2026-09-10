/*
 * Decompiled with CFR 0.152.
 */
package net.irisshaders.iris.vertices;

public interface BlockSensitiveBufferBuilder {
    public void beginBlock(int var1, byte var2, byte var3, int var4, int var5, int var6);

    public void overrideBlock(int var1);

    public void restoreBlock();

    public void endBlock();

    public void ignoreMidBlock(boolean var1);
}

