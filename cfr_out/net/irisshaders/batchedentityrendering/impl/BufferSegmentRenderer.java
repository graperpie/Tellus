/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_286
 *  net.minecraft.class_9801
 */
package net.irisshaders.batchedentityrendering.impl;

import net.irisshaders.batchedentityrendering.impl.BufferSegment;
import net.minecraft.class_286;
import net.minecraft.class_9801;

public class BufferSegmentRenderer {
    public void draw(BufferSegment segment) {
        if (segment.meshData() != null) {
            segment.type().method_23516();
            this.drawInner(segment);
            segment.type().method_23518();
        }
    }

    public void drawInner(BufferSegment segment) {
        class_286.method_43433((class_9801)segment.meshData());
    }
}

