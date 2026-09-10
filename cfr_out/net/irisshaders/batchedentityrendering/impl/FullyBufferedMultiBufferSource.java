/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps
 *  net.minecraft.class_1921
 *  net.minecraft.class_310
 *  net.minecraft.class_3695
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_9799
 */
package net.irisshaders.batchedentityrendering.impl;

import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.BufferSegment;
import net.irisshaders.batchedentityrendering.impl.BufferSegmentRenderer;
import net.irisshaders.batchedentityrendering.impl.Groupable;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.irisshaders.batchedentityrendering.impl.SegmentedBufferBuilder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.ordering.GraphTranslucencyRenderOrderManager;
import net.irisshaders.batchedentityrendering.impl.ordering.RenderOrderManager;
import net.irisshaders.iris.layer.WrappingMultiBufferSource;
import net.minecraft.class_1921;
import net.minecraft.class_310;
import net.minecraft.class_3695;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_9799;

public class FullyBufferedMultiBufferSource
extends class_4597.class_4598
implements MemoryTrackingBuffer,
Groupable,
WrappingMultiBufferSource {
    private static final int NUM_BUFFERS = 32;
    private final RenderOrderManager renderOrderManager;
    private final SegmentedBufferBuilder[] builders;
    private final LinkedHashMap<class_1921, Integer> affinities;
    private final BufferSegmentRenderer segmentRenderer;
    private final UnflushableWrapper unflushableWrapper;
    private final List<Function<class_1921, class_1921>> wrappingFunctionStack;
    private final Map<class_1921, List<BufferSegment>> typeToSegment = new HashMap<class_1921, List<BufferSegment>>();
    private int drawCalls;
    private int renderTypes;
    private Function<class_1921, class_1921> wrappingFunction = null;
    private boolean isReady;
    private List<class_1921> renderOrder = new ArrayList<class_1921>();

    public FullyBufferedMultiBufferSource() {
        super(new class_9799(0), (SequencedMap)Object2ObjectSortedMaps.emptyMap());
        this.renderOrderManager = new GraphTranslucencyRenderOrderManager();
        this.builders = new SegmentedBufferBuilder[32];
        for (int i = 0; i < this.builders.length; ++i) {
            this.builders[i] = new SegmentedBufferBuilder(this);
        }
        this.affinities = new LinkedHashMap(32, 0.75f, true);
        this.drawCalls = 0;
        this.segmentRenderer = new BufferSegmentRenderer();
        this.unflushableWrapper = new UnflushableWrapper(this);
        this.wrappingFunctionStack = new ArrayList<Function<class_1921, class_1921>>();
    }

    private static long toMib(long x) {
        return x / 1024L / 1024L;
    }

    public class_4588 getBuffer(class_1921 renderType) {
        this.removeReady();
        if (this.wrappingFunction != null) {
            renderType = this.wrappingFunction.apply(renderType);
        }
        this.renderOrderManager.begin(renderType);
        Integer affinity = this.affinities.get(renderType);
        if (affinity == null) {
            if (this.affinities.size() < this.builders.length) {
                affinity = this.affinities.size();
            } else {
                Iterator<Map.Entry<class_1921, Integer>> iterator = this.affinities.entrySet().iterator();
                Map.Entry<class_1921, Integer> evicted = iterator.next();
                iterator.remove();
                this.affinities.remove(evicted.getKey());
                affinity = evicted.getValue();
            }
            this.affinities.put(renderType, affinity);
        }
        return this.builders[affinity].getBuffer(renderType);
    }

    private void removeReady() {
        this.isReady = false;
        this.typeToSegment.clear();
        this.renderOrder.clear();
    }

    public void readyUp() {
        this.isReady = true;
        class_3695 profiler = class_310.method_1551().method_16011();
        profiler.method_15396("collect");
        for (SegmentedBufferBuilder builder : this.builders) {
            List<BufferSegment> segments = builder.getSegments();
            for (BufferSegment segment : segments) {
                this.typeToSegment.computeIfAbsent(segment.type(), type -> new ArrayList()).add(segment);
            }
        }
        profiler.method_15405("resolve ordering");
        this.renderOrder = this.renderOrderManager.getRenderOrder();
        this.renderOrderManager.reset();
        this.affinities.clear();
        profiler.method_15407();
    }

    public void method_22993() {
        class_3695 profiler = class_310.method_1551().method_16011();
        if (!this.isReady) {
            this.readyUp();
        }
        profiler.method_15396("draw buffers");
        for (class_1921 type : this.renderOrder) {
            if (!this.typeToSegment.containsKey(type)) continue;
            type.method_23516();
            ++this.renderTypes;
            for (BufferSegment segment : this.typeToSegment.getOrDefault(type, Collections.emptyList())) {
                this.segmentRenderer.drawInner(segment);
                ++this.drawCalls;
            }
            type.method_23518();
        }
        int targetClearTime = this.getTargetClearTime();
        for (SegmentedBufferBuilder builder : this.builders) {
            builder.clearBuffers(targetClearTime);
        }
        profiler.method_15405("reset");
        this.removeReady();
        profiler.method_15407();
    }

    public void endBatchWithType(TransparencyType transparencyType) {
        class_3695 profiler = class_310.method_1551().method_16011();
        if (!this.isReady) {
            this.readyUp();
        }
        profiler.method_15396("draw buffers");
        ArrayList<class_1921> types = new ArrayList<class_1921>();
        for (class_1921 type : this.renderOrder) {
            if (((BlendingStateHolder)type).getTransparencyType() != transparencyType) continue;
            types.add(type);
            type.method_23516();
            ++this.renderTypes;
            for (BufferSegment segment : this.typeToSegment.getOrDefault(type, Collections.emptyList())) {
                this.segmentRenderer.drawInner(segment);
                ++this.drawCalls;
            }
            this.typeToSegment.remove(type);
            type.method_23518();
        }
        profiler.method_15405("reset type " + String.valueOf((Object)transparencyType));
        this.renderOrder.removeAll(types);
        profiler.method_15407();
    }

    private int getTargetClearTime() {
        long sizeInMiB = FullyBufferedMultiBufferSource.toMib(this.getAllocatedSize());
        if (sizeInMiB > 5000L) {
            return 1000;
        }
        if (sizeInMiB > 1000L) {
            return 5000;
        }
        return 10000;
    }

    public int getDrawCalls() {
        return this.drawCalls;
    }

    public int getRenderTypes() {
        return this.renderTypes;
    }

    public void resetDrawCalls() {
        this.drawCalls = 0;
        this.renderTypes = 0;
    }

    public void method_22994(class_1921 type) {
    }

    public class_4597.class_4598 getUnflushableWrapper() {
        return this.unflushableWrapper;
    }

    @Override
    public long getAllocatedSize() {
        long size = 0L;
        for (SegmentedBufferBuilder builder : this.builders) {
            size += builder.getAllocatedSize();
        }
        return size;
    }

    @Override
    public long getUsedSize() {
        long size = 0L;
        for (SegmentedBufferBuilder builder : this.builders) {
            size += builder.getUsedSize();
        }
        return size;
    }

    @Override
    public void freeAndDeleteBuffer() {
        for (SegmentedBufferBuilder builder : this.builders) {
            builder.freeAndDeleteBuffer();
        }
    }

    @Override
    public void startGroup() {
        this.renderOrderManager.startGroup();
    }

    @Override
    public boolean maybeStartGroup() {
        return this.renderOrderManager.maybeStartGroup();
    }

    @Override
    public void endGroup() {
        this.renderOrderManager.endGroup();
    }

    @Override
    public void pushWrappingFunction(Function<class_1921, class_1921> wrappingFunction) {
        if (this.wrappingFunction != null) {
            this.wrappingFunctionStack.add(this.wrappingFunction);
        }
        this.wrappingFunction = wrappingFunction;
    }

    @Override
    public void popWrappingFunction() {
        this.wrappingFunction = this.wrappingFunctionStack.isEmpty() ? null : this.wrappingFunctionStack.removeLast();
    }

    @Override
    public void assertWrapStackEmpty() {
        if (!this.wrappingFunctionStack.isEmpty() || this.wrappingFunction != null) {
            throw new IllegalStateException("Wrapping function stack not empty!");
        }
    }

    public void weAreOutOfMemory() {
        for (SegmentedBufferBuilder builder : this.builders) {
            builder.lastDitchAttempt();
        }
    }

    private static class UnflushableWrapper
    extends class_4597.class_4598
    implements Groupable {
        private final FullyBufferedMultiBufferSource wrapped;

        UnflushableWrapper(FullyBufferedMultiBufferSource wrapped) {
            super(new class_9799(0), (SequencedMap)Object2ObjectSortedMaps.emptyMap());
            this.wrapped = wrapped;
        }

        public class_4588 getBuffer(class_1921 renderType) {
            return this.wrapped.getBuffer(renderType);
        }

        public void method_22993() {
        }

        public void method_22994(class_1921 type) {
        }

        @Override
        public void startGroup() {
            this.wrapped.startGroup();
        }

        @Override
        public boolean maybeStartGroup() {
            return this.wrapped.maybeStartGroup();
        }

        @Override
        public void endGroup() {
            this.wrapped.endGroup();
        }
    }
}

