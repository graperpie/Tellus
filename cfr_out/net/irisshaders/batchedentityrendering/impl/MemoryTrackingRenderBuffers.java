/*
 * Decompiled with CFR 0.152.
 */
package net.irisshaders.batchedentityrendering.impl;

public interface MemoryTrackingRenderBuffers {
    public long getEntityBufferAllocatedSize();

    public long getMiscBufferAllocatedSize();

    public int getMaxBegins();

    public void freeAndDeleteBuffers();
}

