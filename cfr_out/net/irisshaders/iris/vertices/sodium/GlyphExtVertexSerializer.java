/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics
 *  net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer
 *  net.minecraft.class_290
 *  net.minecraft.class_296
 *  org.joml.Vector3f
 *  org.lwjgl.system.MemoryUtil
 */
package net.irisshaders.iris.vertices.sodium;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import net.irisshaders.iris.vertices.sodium.QuadViewEntity;
import net.minecraft.class_290;
import net.minecraft.class_296;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

public class GlyphExtVertexSerializer
implements VertexSerializer {
    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_COLOR = 12;
    private static final int OFFSET_TEXTURE = 16;
    private static final int OFFSET_MID_TEXTURE = IrisVertexFormats.GLYPH.method_60835(IrisVertexFormats.MID_TEXTURE_ELEMENT);
    private static final int OFFSET_LIGHT = 24;
    private static final int OFFSET_NORMAL = IrisVertexFormats.GLYPH.method_60835(class_296.field_52113);
    private static final int OFFSET_TANGENT = IrisVertexFormats.GLYPH.method_60835(IrisVertexFormats.TANGENT_ELEMENT);
    private static final QuadViewEntity quad = new QuadViewEntity();
    private static final Vector3f saveNormal = new Vector3f();
    private static final int STRIDE = IrisVertexFormats.GLYPH.method_1362();

    private static void endQuad(float uSum, float vSum, long src, long dst) {
        uSum *= 0.25f;
        vSum *= 0.25f;
        quad.setup(dst, IrisVertexFormats.GLYPH.method_1362());
        NormalHelper.computeFaceNormal(saveNormal, quad);
        float normalX = GlyphExtVertexSerializer.saveNormal.x;
        float normalY = GlyphExtVertexSerializer.saveNormal.y;
        float normalZ = GlyphExtVertexSerializer.saveNormal.z;
        int normal = NormI8.pack(saveNormal);
        int tangent = NormalHelper.computeTangent(normalX, normalY, normalZ, quad);
        for (long vertex = 0L; vertex < 4L; ++vertex) {
            MemoryUtil.memPutFloat((long)(dst + (long)OFFSET_MID_TEXTURE - (long)STRIDE * vertex), (float)uSum);
            MemoryUtil.memPutFloat((long)(dst + (long)(OFFSET_MID_TEXTURE + 4) - (long)STRIDE * vertex), (float)vSum);
            MemoryUtil.memPutInt((long)(dst + (long)OFFSET_NORMAL - (long)STRIDE * vertex), (int)normal);
            MemoryUtil.memPutInt((long)(dst + (long)OFFSET_TANGENT - (long)STRIDE * vertex), (int)tangent);
        }
    }

    public void serialize(long src, long dst, int vertexCount) {
        float uSum = 0.0f;
        float vSum = 0.0f;
        for (int i = 0; i < vertexCount; ++i) {
            float u = MemoryUtil.memGetFloat((long)(src + 16L));
            float v = MemoryUtil.memGetFloat((long)(src + 16L + 4L));
            uSum += u;
            vSum += v;
            MemoryIntrinsics.copyMemory((long)src, (long)dst, (int)28);
            MemoryUtil.memPutShort((long)(dst + 32L), (short)((short)CapturedRenderingState.INSTANCE.getCurrentRenderedEntity()));
            MemoryUtil.memPutShort((long)(dst + 34L), (short)((short)CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity()));
            MemoryUtil.memPutShort((long)(dst + 36L), (short)((short)CapturedRenderingState.INSTANCE.getCurrentRenderedItem()));
            if (i == 3) continue;
            src += (long)class_290.field_20888.method_1362();
            dst += (long)IrisVertexFormats.GLYPH.method_1362();
        }
        GlyphExtVertexSerializer.endQuad(uSum, vSum, src, dst);
    }
}

