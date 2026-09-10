/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_9799
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package net.irisshaders.batchedentityrendering.mixin;

import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.minecraft.class_9799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={class_9799.class})
public abstract class MixinByteBufferBuilder
implements MemoryTrackingBuffer {
    @Shadow
    private int field_52083;
    @Shadow
    private int field_52084;

    @Shadow
    public abstract void close();

    @Override
    public long getAllocatedSize() {
        return this.field_52083;
    }

    @Override
    public long getUsedSize() {
        return this.field_52084;
    }

    @Override
    public void freeAndDeleteBuffer() {
        this.close();
    }
}

