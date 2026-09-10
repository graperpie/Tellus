/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_9799
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package net.irisshaders.batchedentityrendering.mixin;

import java.util.SequencedMap;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.minecraft.class_1921;
import net.minecraft.class_4597;
import net.minecraft.class_9799;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={class_4597.class_4598.class})
public class MixinBufferSource
implements MemoryTrackingBuffer {
    @Shadow
    @Final
    protected class_9799 field_52156;
    @Shadow
    @Final
    protected SequencedMap<class_1921, class_9799> field_20953;

    @Override
    public long getAllocatedSize() {
        long allocatedSize = ((MemoryTrackingBuffer)this.field_52156).getAllocatedSize();
        for (class_9799 builder : this.field_20953.values()) {
            allocatedSize += ((MemoryTrackingBuffer)builder).getAllocatedSize();
        }
        return allocatedSize;
    }

    @Override
    public long getUsedSize() {
        long allocatedSize = ((MemoryTrackingBuffer)this.field_52156).getUsedSize();
        for (class_9799 builder : this.field_20953.values()) {
            allocatedSize += ((MemoryTrackingBuffer)builder).getUsedSize();
        }
        return allocatedSize;
    }

    @Override
    public void freeAndDeleteBuffer() {
        ((MemoryTrackingBuffer)this.field_52156).freeAndDeleteBuffer();
        for (class_9799 builder : this.field_20953.values()) {
            ((MemoryTrackingBuffer)builder).freeAndDeleteBuffer();
        }
    }
}

