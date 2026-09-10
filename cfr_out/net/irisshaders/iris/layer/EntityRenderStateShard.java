/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4668
 */
package net.irisshaders.iris.layer;

import net.irisshaders.iris.layer.GbufferPrograms;
import net.minecraft.class_4668;

public final class EntityRenderStateShard
extends class_4668 {
    public static final EntityRenderStateShard INSTANCE = new EntityRenderStateShard();

    private EntityRenderStateShard() {
        super("iris:is_entity", GbufferPrograms::beginEntities, GbufferPrograms::endEntities);
    }
}

