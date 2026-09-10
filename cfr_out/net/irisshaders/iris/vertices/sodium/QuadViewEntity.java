/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.lwjgl.system.MemoryUtil
 */
package net.irisshaders.iris.vertices.sodium;

import net.irisshaders.iris.vertices.views.QuadView;
import org.lwjgl.system.MemoryUtil;

public class QuadViewEntity
implements QuadView {
    private long writePointer;
    private int stride;

    public void setup(long writePointer, int stride) {
        this.writePointer = writePointer;
        this.stride = stride;
    }

    @Override
    public float x(int index) {
        return MemoryUtil.memGetFloat((long)(this.writePointer - (long)this.stride * (3L - (long)index)));
    }

    @Override
    public float y(int index) {
        return MemoryUtil.memGetFloat((long)(this.writePointer + 4L - (long)this.stride * (3L - (long)index)));
    }

    @Override
    public float z(int index) {
        return MemoryUtil.memGetFloat((long)(this.writePointer + 8L - (long)this.stride * (3L - (long)index)));
    }

    @Override
    public float u(int index) {
        return MemoryUtil.memGetFloat((long)(this.writePointer + 16L - (long)this.stride * (3L - (long)index)));
    }

    @Override
    public float v(int index) {
        return MemoryUtil.memGetFloat((long)(this.writePointer + 20L - (long)this.stride * (3L - (long)index)));
    }
}

