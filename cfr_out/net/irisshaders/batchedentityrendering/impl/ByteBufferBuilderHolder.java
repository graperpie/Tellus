/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_9799
 */
package net.irisshaders.batchedentityrendering.impl;

import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.minecraft.class_9799;

public class ByteBufferBuilderHolder
implements MemoryTrackingBuffer {
    private final class_9799 builder;
    private long lastUse = System.currentTimeMillis();

    public ByteBufferBuilderHolder(class_9799 builder) {
        this.builder = builder;
    }

    public class_9799 getBuffer() {
        return this.builder;
    }

    public boolean deleteOrClear(int clearTime) {
        if (System.currentTimeMillis() - this.lastUse > (long)clearTime) {
            this.builder.close();
            return true;
        }
        this.builder.method_60809();
        return false;
    }

    public boolean delete(int clearTime) {
        if (System.currentTimeMillis() - this.lastUse > (long)clearTime) {
            this.builder.close();
            return true;
        }
        return false;
    }

    public void forceDelete() {
        this.builder.close();
    }

    @Override
    public long getAllocatedSize() {
        return ((MemoryTrackingBuffer)this.builder).getAllocatedSize();
    }

    @Override
    public long getUsedSize() {
        return ((MemoryTrackingBuffer)this.builder).getUsedSize();
    }

    @Override
    public void freeAndDeleteBuffer() {
        this.builder.close();
    }

    public void wasUsed() {
        this.lastUse = System.currentTimeMillis();
    }
}

