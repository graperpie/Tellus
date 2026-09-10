/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4668
 */
package net.irisshaders.iris.layer;

import net.irisshaders.iris.layer.GbufferPrograms;
import net.minecraft.class_4668;

public final class BlockEntityRenderStateShard
extends class_4668 {
    public static final BlockEntityRenderStateShard INSTANCE = new BlockEntityRenderStateShard();

    private BlockEntityRenderStateShard() {
        super("iris:is_block_entity", GbufferPrograms::beginBlockEntities, GbufferPrograms::endBlockEntities);
    }
}

