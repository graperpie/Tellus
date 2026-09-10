/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.api.util.ColorABGR
 *  net.caffeinemc.mods.sodium.api.util.ColorARGB
 *  net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder
 *  net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder$Vertex
 *  net.minecraft.class_3532
 *  org.joml.Vector2f
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 *  org.lwjgl.system.MemoryUtil
 */
package net.irisshaders.iris.vertices.sodium.terrain;

import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import net.irisshaders.iris.vertices.sodium.terrain.BlockContextHolder;
import net.irisshaders.iris.vertices.sodium.terrain.VertexEncoderInterface;
import net.irisshaders.iris.vertices.sodium.terrain.XHFPModelVertexType;
import net.minecraft.class_3532;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public class XHFPTerrainVertex
implements ChunkVertexEncoder,
VertexEncoderInterface {
    private static final int POSITION_MAX_VALUE = 0x100000;
    private static final int TEXTURE_MAX_VALUE = 32768;
    private static final float MODEL_ORIGIN = 8.0f;
    private static final float MODEL_RANGE = 32.0f;
    private static final int DEFAULT_NORMAL;
    private final Vector3f normal = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector4f tangent = new Vector4f(0.0f, 1.0f, 0.0f, 1.0f);
    private final int blockIdOffset;
    private final int normalOffset;
    private final int midBlockOffset;
    private final int midUvOffset;
    private final int stride;
    private final Vector2f normEncoded = new Vector2f();
    private final Vector2f tangEncoded = new Vector2f();
    private BlockContextHolder contextHolder;

    public XHFPTerrainVertex(int blockIdOffset, int normalOffset, int midUvOffset, int midBlockOffset, int stride) {
        this.blockIdOffset = blockIdOffset;
        this.normalOffset = normalOffset;
        this.midUvOffset = midUvOffset;
        this.midBlockOffset = midBlockOffset;
        this.stride = stride;
    }

    private static int packPositionHi(int x, int y, int z) {
        return (x >>> 10 & 0x3FF) << 0 | (y >>> 10 & 0x3FF) << 10 | (z >>> 10 & 0x3FF) << 20;
    }

    private static int packPositionLo(int x, int y, int z) {
        return (x & 0x3FF) << 0 | (y & 0x3FF) << 10 | (z & 0x3FF) << 20;
    }

    private static int quantizePosition(float position) {
        return (int)(XHFPTerrainVertex.normalizePosition(position) * 1048576.0f) & 0xFFFFF;
    }

    private static float normalizePosition(float v) {
        return (8.0f + v) / 32.0f;
    }

    private static int packTexture(int u, int v) {
        return (u & 0xFFFF) << 0 | (v & 0xFFFF) << 16;
    }

    private static int encodeTexture(float center, float x) {
        int bias = x < center ? 1 : -1;
        int quantized = Math.round(x * 32768.0f) + bias;
        return quantized & Short.MAX_VALUE | XHFPTerrainVertex.sign(bias) << 15;
    }

    private static int encodeLight(int light) {
        int sky = class_3532.method_15340((int)(light >>> 16 & 0xFF), (int)8, (int)248);
        int block = class_3532.method_15340((int)(light >>> 0 & 0xFF), (int)8, (int)248);
        return block << 0 | sky << 8;
    }

    private static int sign(int x) {
        return x >>> 31;
    }

    private static int packLightAndData(int light, int material, int section) {
        return (light & 0xFFFF) << 0 | (material & 0xFF) << 16 | (section & 0xFF) << 24;
    }

    private static int floorInt(float x) {
        return (int)Math.floor(x);
    }

    @Override
    public void iris$setContextHolder(BlockContextHolder holder) {
        this.contextHolder = holder;
    }

    public long write(long ptr, int material, ChunkVertexEncoder.Vertex[] vertices, int section) {
        int finalNorm;
        float texCentroidU = 0.0f;
        float texCentroidV = 0.0f;
        for (ChunkVertexEncoder.Vertex vertex : vertices) {
            texCentroidU += vertex.u;
            texCentroidV += vertex.v;
        }
        int midUV = XHFPModelVertexType.encodeOld(texCentroidU *= 0.25f, texCentroidV *= 0.25f);
        if (this.normalOffset != 0) {
            NormalHelper.computeFaceNormalManual(this.normal, vertices[0].x, vertices[0].y, vertices[0].z, vertices[1].x, vertices[1].y, vertices[1].z, vertices[2].x, vertices[2].y, vertices[2].z, vertices[3].x, vertices[3].y, vertices[3].z);
            int tangent = this.computeTangentForQuad(this.normal, vertices);
            NormalHelper.octahedronEncode(this.normEncoded, this.normal.x, this.normal.y, this.normal.z);
            NormalHelper.tangentEncode(this.tangEncoded, this.tangent);
            finalNorm = NormI8.pack(this.normEncoded.x, this.normEncoded.y, this.tangEncoded.x, this.tangEncoded.y);
        } else {
            finalNorm = DEFAULT_NORMAL;
        }
        for (int i = 0; i < 4; ++i) {
            ChunkVertexEncoder.Vertex vertex;
            vertex = vertices[i];
            int x = XHFPTerrainVertex.quantizePosition(vertex.x);
            int y = XHFPTerrainVertex.quantizePosition(vertex.y);
            int z = XHFPTerrainVertex.quantizePosition(vertex.z);
            int u = XHFPTerrainVertex.encodeTexture(texCentroidU, vertex.u);
            int v = XHFPTerrainVertex.encodeTexture(texCentroidV, vertex.v);
            int light = XHFPTerrainVertex.encodeLight(vertex.light);
            MemoryUtil.memPutInt((long)ptr, (int)XHFPTerrainVertex.packPositionHi(x, y, z));
            MemoryUtil.memPutInt((long)(ptr + 4L), (int)XHFPTerrainVertex.packPositionLo(x, y, z));
            MemoryUtil.memPutInt((long)(ptr + 8L), (int)(WorldRenderingSettings.INSTANCE.shouldUseSeparateAo() ? ColorABGR.withAlpha((int)vertex.color, (float)vertex.ao) : ColorARGB.mulRGB((int)vertex.color, (float)vertex.ao)));
            MemoryUtil.memPutInt((long)(ptr + 12L), (int)XHFPTerrainVertex.packTexture(u, v));
            MemoryUtil.memPutInt((long)(ptr + 16L), (int)XHFPTerrainVertex.packLightAndData(light, material, section));
            if (this.blockIdOffset != 0) {
                MemoryUtil.memPutInt((long)(ptr + (long)this.blockIdOffset), (int)this.packBlockId(this.contextHolder));
            }
            if (this.midBlockOffset != 0) {
                MemoryUtil.memPutInt((long)(ptr + (long)this.midBlockOffset), (int)(this.contextHolder.ignoreMidBlock() ? 0 : ExtendedDataHelper.computeMidBlock(vertex.x, vertex.y, vertex.z, this.contextHolder.getLocalPosX(), this.contextHolder.getLocalPosY(), this.contextHolder.getLocalPosZ())));
                MemoryUtil.memPutByte((long)(ptr + (long)this.midBlockOffset + 3L), (byte)this.contextHolder.getBlockEmission());
            }
            if (this.midUvOffset != 0) {
                MemoryUtil.memPutInt((long)(ptr + (long)this.midUvOffset), (int)midUV);
            }
            if (this.normalOffset != 0) {
                MemoryUtil.memPutInt((long)(ptr + (long)this.normalOffset), (int)finalNorm);
            }
            ptr += (long)this.stride;
        }
        return ptr;
    }

    private int computeTangentForQuad(Vector3f normal, ChunkVertexEncoder.Vertex[] vertices) {
        int tangent = NormalHelper.computeTangent(this.tangent, normal.x, normal.y, normal.z, vertices[0].x, vertices[0].y, vertices[0].z, vertices[0].u, vertices[0].v, vertices[1].x, vertices[1].y, vertices[1].z, vertices[1].u, vertices[1].v, vertices[2].x, vertices[2].y, vertices[2].z, vertices[2].u, vertices[2].v);
        if (tangent == -1) {
            tangent = NormalHelper.computeTangent(this.tangent, normal.x, normal.y, normal.z, vertices[2].x, vertices[2].y, vertices[2].z, vertices[2].u, vertices[2].v, vertices[3].x, vertices[3].y, vertices[3].z, vertices[3].u, vertices[3].v, vertices[0].x, vertices[0].y, vertices[0].z, vertices[0].u, vertices[0].v);
        }
        return tangent;
    }

    private int packBlockId(BlockContextHolder contextHolder) {
        return contextHolder.getBlockId() + 1 << 1 | contextHolder.getRenderType() & 1;
    }

    static {
        Vector2f normE = new Vector2f();
        Vector2f tangE = new Vector2f();
        NormalHelper.octahedronEncode(normE, 0.0f, 1.0f, 0.0f);
        NormalHelper.tangentEncode(tangE, new Vector4f(0.0f, 1.0f, 0.0f, 1.0f));
        DEFAULT_NORMAL = NormI8.pack(normE.x, normE.y, tangE.x, tangE.y);
    }
}

