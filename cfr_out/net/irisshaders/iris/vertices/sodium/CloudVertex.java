/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.api.math.MatrixHelper
 *  net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute
 *  net.caffeinemc.mods.sodium.api.vertex.attributes.common.NormalAttribute
 *  net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute
 *  org.joml.Matrix4f
 */
package net.irisshaders.iris.vertices.sodium;

import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.NormalAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import org.joml.Matrix4f;

public final class CloudVertex {
    public static final int STRIDE = 20;
    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_COLOR = 12;
    private static final int OFFSET_NORMAL = 16;

    public static void put(long ptr, Matrix4f matrix, float x, float y, float z, int color, int normal) {
        float xt = MatrixHelper.transformPositionX((Matrix4f)matrix, (float)x, (float)y, (float)z);
        float yt = MatrixHelper.transformPositionY((Matrix4f)matrix, (float)x, (float)y, (float)z);
        float zt = MatrixHelper.transformPositionZ((Matrix4f)matrix, (float)x, (float)y, (float)z);
        CloudVertex.put(ptr, xt, yt, zt, color, normal);
    }

    public static void put(long ptr, float x, float y, float z, int color, int normal) {
        PositionAttribute.put((long)(ptr + 0L), (float)x, (float)y, (float)z);
        ColorAttribute.set((long)(ptr + 12L), (int)color);
        NormalAttribute.set((long)(ptr + 16L), (int)normal);
    }
}

