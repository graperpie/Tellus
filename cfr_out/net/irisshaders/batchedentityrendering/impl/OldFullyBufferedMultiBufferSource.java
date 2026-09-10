/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps
 *  net.minecraft.class_1921
 *  net.minecraft.class_287
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_9799
 */
package net.irisshaders.batchedentityrendering.impl;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.minecraft.class_1921;
import net.minecraft.class_287;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_9799;

public class OldFullyBufferedMultiBufferSource
extends class_4597.class_4598 {
    private final Map<class_1921, class_287> bufferBuilders = new HashMap<class_1921, class_287>();
    private final Object2IntMap<class_1921> unused = new Object2IntOpenHashMap();
    private final Set<class_287> activeBuffers = new HashSet<class_287>();
    private final Set<class_1921> typesThisFrame = new HashSet<class_1921>();
    private final List<class_1921> typesInOrder = new ArrayList<class_1921>();
    private boolean flushed = false;

    public OldFullyBufferedMultiBufferSource() {
        super(new class_9799(0), (SequencedMap)Object2ObjectSortedMaps.emptyMap());
    }

    private TransparencyType getTransparencyType(class_1921 type) {
        while (type instanceof WrappableRenderType) {
            type = ((WrappableRenderType)type).unwrap();
        }
        if (type instanceof BlendingStateHolder) {
            return ((BlendingStateHolder)type).getTransparencyType();
        }
        return TransparencyType.GENERAL_TRANSPARENT;
    }

    public class_4588 getBuffer(class_1921 renderType) {
        this.flushed = false;
        class_287 buffer = this.bufferBuilders.computeIfAbsent(renderType, type -> new class_287(new class_9799(type.method_22722()), renderType.method_23033(), renderType.method_23031()));
        if (this.activeBuffers.add(buffer)) {
            // empty if block
        }
        if (this.typesThisFrame.add(renderType)) {
            this.typesInOrder.add(renderType);
        }
        this.unused.removeInt((Object)renderType);
        return buffer;
    }

    public void method_22993() {
        if (this.flushed) {
            return;
        }
        ArrayList removedTypes = new ArrayList();
        this.unused.forEach((unusedType, unusedCount) -> {
            if (unusedCount < 10) {
                return;
            }
            class_287 buffer = this.bufferBuilders.remove(unusedType);
            removedTypes.add(unusedType);
            if (this.activeBuffers.contains(buffer)) {
                throw new IllegalStateException("A buffer was simultaneously marked as inactive and as active, something is very wrong...");
            }
        });
        for (class_1921 removed : removedTypes) {
            this.unused.removeInt((Object)removed);
        }
        this.typesInOrder.sort(Comparator.comparing(this::getTransparencyType));
        for (class_1921 type : this.typesInOrder) {
            this.drawInternal(type);
        }
        this.typesInOrder.clear();
        this.typesThisFrame.clear();
        this.flushed = true;
    }

    public void method_22994(class_1921 type) {
    }

    private void drawInternal(class_1921 type) {
        class_287 buffer = this.bufferBuilders.get(type);
        if (buffer == null) {
            return;
        }
        if (this.activeBuffers.remove(buffer)) {
            type.method_60895(buffer.method_60794());
        } else {
            int unusedCount = this.unused.getOrDefault((Object)type, 0);
            this.unused.put((Object)type, ++unusedCount);
        }
    }
}

