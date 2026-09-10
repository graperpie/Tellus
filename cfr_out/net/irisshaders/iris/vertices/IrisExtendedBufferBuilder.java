/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_293
 *  net.minecraft.class_293$class_5596
 */
package net.irisshaders.iris.vertices;

import net.minecraft.class_293;

public interface IrisExtendedBufferBuilder {
    public class_293 iris$format();

    public class_293.class_5596 iris$mode();

    public boolean iris$extending();

    public boolean iris$isTerrain();

    public boolean iris$injectNormalAndUV1();

    public int iris$vertexCount();

    public void iris$incrementVertexCount();

    public void iris$resetVertexCount();

    public short iris$currentBlock();

    public short iris$currentRenderType();

    public int iris$currentLocalPosX();

    public int iris$currentLocalPosY();

    public int iris$currentLocalPosZ();
}

