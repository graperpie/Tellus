/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.minecraft.class_1921
 *  net.minecraft.class_287
 *  net.minecraft.class_4588
 *  net.minecraft.class_9799
 *  net.minecraft.class_9801
 */
package net.irisshaders.batchedentityrendering.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.irisshaders.batchedentityrendering.impl.BufferBuilderExt;
import net.irisshaders.batchedentityrendering.impl.BufferSegment;
import net.irisshaders.batchedentityrendering.impl.ByteBufferBuilderHolder;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.irisshaders.batchedentityrendering.impl.RenderTypeUtil;
import net.irisshaders.batchedentityrendering.mixin.RenderTypeAccessor;
import net.minecraft.class_1921;
import net.minecraft.class_287;
import net.minecraft.class_4588;
import net.minecraft.class_9799;
import net.minecraft.class_9801;

public class SegmentedBufferBuilder
implements MemoryTrackingBuffer {
    private final Map<class_1921, ByteBufferBuilderHolder> buffers;
    private final Map<class_1921, class_287> builders;
    private final List<BufferSegment> segments;
    private final FullyBufferedMultiBufferSource parent;

    public SegmentedBufferBuilder(FullyBufferedMultiBufferSource parent) {
        this.parent = parent;
        this.buffers = new Object2ObjectOpenHashMap();
        this.builders = new Object2ObjectOpenHashMap();
        this.segments = new ArrayList<BufferSegment>();
    }

    private static boolean shouldSortOnUpload(class_1921 type) {
        return ((RenderTypeAccessor)type).shouldSortOnUpload();
    }

    public class_4588 getBuffer(class_1921 renderType) {
        try {
            ByteBufferBuilderHolder buffer = this.buffers.computeIfAbsent(renderType, r -> new ByteBufferBuilderHolder(new class_9799(renderType.method_22722())));
            buffer.wasUsed();
            class_287 builder = this.builders.computeIfAbsent(renderType, t -> new class_287(buffer.getBuffer(), renderType.method_23033(), renderType.method_23031()));
            if (RenderTypeUtil.isTriangleStripDrawMode(renderType)) {
                ((BufferBuilderExt)builder).splitStrip();
            }
            return builder;
        }
        catch (OutOfMemoryError e) {
            this.weAreOutOfMemory();
            return this.getBuffer(renderType);
        }
    }

    private void weAreOutOfMemory() {
        this.parent.weAreOutOfMemory();
    }

    public List<BufferSegment> getSegments() {
        this.builders.forEach((renderType, bufferBuilder) -> {
            try {
                class_9801 meshData = bufferBuilder.method_60794();
                if (meshData == null) {
                    return;
                }
                if (SegmentedBufferBuilder.shouldSortOnUpload(renderType)) {
                    meshData.method_60819(this.buffers.get(renderType).getBuffer(), RenderSystem.getVertexSorting());
                }
                this.segments.add(new BufferSegment(meshData, (class_1921)renderType));
            }
            catch (OutOfMemoryError e) {
                this.weAreOutOfMemory();
            }
        });
        this.builders.clear();
        ArrayList<BufferSegment> finalSegments = new ArrayList<BufferSegment>(this.segments);
        this.segments.clear();
        return finalSegments;
    }

    @Override
    public long getAllocatedSize() {
        long usedSize = 0L;
        for (ByteBufferBuilderHolder buffer : this.buffers.values()) {
            usedSize += buffer.getAllocatedSize();
        }
        return usedSize;
    }

    @Override
    public long getUsedSize() {
        long usedSize = 0L;
        for (ByteBufferBuilderHolder buffer : this.buffers.values()) {
            usedSize += buffer.getUsedSize();
        }
        return usedSize;
    }

    @Override
    public void freeAndDeleteBuffer() {
        for (ByteBufferBuilderHolder buffer : this.buffers.values()) {
            buffer.forceDelete();
        }
        this.buffers.clear();
    }

    public void clearBuffers(int clearTime) {
        this.buffers.values().removeIf(b -> b.deleteOrClear(clearTime));
    }

    public void lastDitchAttempt() {
        this.buffers.values().removeIf(b -> b.delete(500));
    }
}

