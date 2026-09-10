/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4668
 */
package net.irisshaders.iris.layer;

import net.irisshaders.iris.layer.GbufferPrograms;
import net.minecraft.class_4668;

public class IsOutlineRenderStateShard
extends class_4668 {
    public static final IsOutlineRenderStateShard INSTANCE = new IsOutlineRenderStateShard();

    private IsOutlineRenderStateShard() {
        super("iris:is_outline", GbufferPrograms::beginOutline, GbufferPrograms::endOutline);
    }
}

