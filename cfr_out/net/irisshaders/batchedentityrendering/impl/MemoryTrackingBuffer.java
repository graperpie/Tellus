/*
 * Decompiled with CFR 0.152.
 */
package net.irisshaders.batchedentityrendering.impl;

public interface MemoryTrackingBuffer {
    public long getAllocatedSize();

    public long getUsedSize();

    public void freeAndDeleteBuffer();
}

