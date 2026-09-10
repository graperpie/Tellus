/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3532
 *  org.jetbrains.annotations.NotNull
 *  org.joml.Vector2f
 *  org.joml.Vector2fc
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 */
package net.irisshaders.iris.vertices;

import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.views.QuadView;
import net.irisshaders.iris.vertices.views.TriView;
import net.minecraft.class_3532;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

public abstract class NormalHelper {
    private static final float BIAS = 3.051851E-5f;

    private NormalHelper() {
    }

    public static int invertPackedNormal(int packed) {
        int ix = -(packed & 0xFF);
        int iy = -(packed >> 8 & 0xFF);
        int iz = -(packed >> 16 & 0xFF);
        return packed & 0xFF000000 | (iz &= 0xFF) << 16 | (iy &= 0xFF) << 8 | (ix &= 0xFF);
    }

    public static void octahedronEncode(Vector2f output, float x, float y, float z) {
        float oY;
        float oX;
        float nX = x;
        float nY = y;
        float nZ = z;
        float invL1 = 1.0f / (Math.abs(nX) + Math.abs(nY) + Math.abs(nZ));
        nX *= invL1;
        nY *= invL1;
        if ((nZ *= invL1) >= 0.0f) {
            oX = nX;
            oY = nY;
        } else {
            float absNX = Math.abs(nX);
            float absNY = Math.abs(nY);
            oX = (1.0f - absNY) * (nX >= 0.0f ? 1.0f : -1.0f);
            oY = (1.0f - absNX) * (nY >= 0.0f ? 1.0f : -1.0f);
        }
        oX = oX * 0.5f + 0.5f;
        oY = oY * 0.5f + 0.5f;
        output.set(oX, oY);
    }

    public static void tangentEncode(Vector2f output, Vector4f tangent) {
        NormalHelper.octahedronEncode(output, tangent.x, tangent.y, tangent.z);
        output.y = Math.max(output.y, 3.051851E-5f);
        output.y = output.y * 0.5f + 0.5f;
        output.y = tangent.w >= 0.0f ? output.y : 1.0f - output.y;
    }

    static Vector4f octahedron_tangent_decode(Vector2f p_oct) {
        Vector2f oct_compressed = new Vector2f((Vector2fc)p_oct);
        oct_compressed.y = oct_compressed.y * 2.0f - 1.0f;
        float r_sign = oct_compressed.y >= 0.0f ? 1.0f : -1.0f;
        oct_compressed.y = Math.abs(oct_compressed.y);
        Vector3f res = NormalHelper.octahedron_decode(oct_compressed.x, oct_compressed.y);
        return new Vector4f(res.x, res.y, res.z, r_sign);
    }

    private static Vector3f octahedron_decode(float inX, float inY) {
        Vector2f f = new Vector2f(inX * 2.0f - 1.0f, inY * 2.0f - 1.0f);
        Vector3f n = new Vector3f(f.x, f.y, 1.0f - Math.abs(f.x) - Math.abs(f.y));
        float t = class_3532.method_15363((float)(-n.z), (float)0.0f, (float)1.0f);
        n.x = n.x + (n.x >= 0.0f ? -t : t);
        n.y = n.y + (n.y >= 0.0f ? -t : t);
        return n.normalize();
    }

    public static void computeFaceNormal(@NotNull Vector3f saveTo, QuadView q) {
        float x0 = q.x(0);
        float y0 = q.y(0);
        float z0 = q.z(0);
        float x1 = q.x(1);
        float y1 = q.y(1);
        float z1 = q.z(1);
        float x2 = q.x(2);
        float y2 = q.y(2);
        float z2 = q.z(2);
        float x3 = q.x(3);
        float y3 = q.y(3);
        float z3 = q.z(3);
        NormalHelper.computeFaceNormalManual(saveTo, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
    }

    public static void computeFaceNormalManual(@NotNull Vector3f saveTo, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
        float dx0 = x2 - x0;
        float dy0 = y2 - y0;
        float dz0 = z2 - z0;
        float dx1 = x3 - x1;
        float dy1 = y3 - y1;
        float dz1 = z3 - z1;
        float normX = dy0 * dz1 - dz0 * dy1;
        float normY = dz0 * dx1 - dx0 * dz1;
        float normZ = dx0 * dy1 - dy0 * dx1;
        saveTo.set(normX, normY, normZ);
        saveTo.normalize();
    }

    public static void computeFaceNormalFlipped(@NotNull Vector3f saveTo, QuadView q) {
        float x0 = q.x(3);
        float y0 = q.y(3);
        float z0 = q.z(3);
        float x1 = q.x(2);
        float y1 = q.y(2);
        float z1 = q.z(2);
        float x2 = q.x(1);
        float y2 = q.y(1);
        float z2 = q.z(1);
        float x3 = q.x(0);
        float y3 = q.y(0);
        float z3 = q.z(0);
        NormalHelper.computeFaceNormalManual(saveTo, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3);
    }

    public static void computeFaceNormalTri(@NotNull Vector3f saveTo, TriView t) {
        float x0 = t.x(0);
        float y0 = t.y(0);
        float z0 = t.z(0);
        float x1 = t.x(1);
        float y1 = t.y(1);
        float z1 = t.z(1);
        float x2 = t.x(2);
        float y2 = t.y(2);
        float z2 = t.z(2);
        NormalHelper.computeFaceNormalManual(saveTo, x0, y0, z0, x1, y1, z1, x2, y2, z2, x0, y0, z0);
    }

    public static int computeTangentSmooth(float normalX, float normalY, float normalZ, TriView t) {
        float deltaV1;
        float deltaU2;
        float x0 = t.x(0);
        float y0 = t.y(0);
        float z0 = t.z(0);
        float x1 = t.x(1);
        float y1 = t.y(1);
        float z1 = t.z(1);
        float x2 = t.x(2);
        float y2 = t.y(2);
        float z2 = t.z(2);
        float d0 = x0 * normalX + y0 * normalY + z0 * normalZ;
        float d1 = x1 * normalX + y1 * normalY + z1 * normalZ;
        float d2 = x2 * normalX + y2 * normalY + z2 * normalZ;
        x0 -= d0 * normalX;
        y0 -= d0 * normalY;
        z0 -= d0 * normalZ;
        x1 -= d1 * normalX;
        y1 -= d1 * normalY;
        z1 -= d1 * normalZ;
        x2 -= d2 * normalX;
        y2 -= d2 * normalY;
        z2 -= d2 * normalZ;
        float edge1x = x1 - x0;
        float edge1y = y1 - y0;
        float edge1z = z1 - z0;
        float edge2x = x2 - x0;
        float edge2y = y2 - y0;
        float edge2z = z2 - z0;
        float u0 = t.u(0);
        float v0 = t.v(0);
        float u1 = t.u(1);
        float v1 = t.v(1);
        float u2 = t.u(2);
        float deltaU1 = u1 - u0;
        float v2 = t.v(2);
        float deltaV2 = v2 - v0;
        float fdenom = deltaU1 * deltaV2 - (deltaU2 = u2 - u0) * (deltaV1 = v1 - v0);
        float f = (double)fdenom == 0.0 ? 1.0f : 1.0f / fdenom;
        float tangentx = f * (deltaV2 * edge1x - deltaV1 * edge2x);
        float tangenty = f * (deltaV2 * edge1y - deltaV1 * edge2y);
        float tangentz = f * (deltaV2 * edge1z - deltaV1 * edge2z);
        float tcoeff = NormalHelper.rsqrt(tangentx * tangentx + tangenty * tangenty + tangentz * tangentz);
        tangentx *= tcoeff;
        tangenty *= tcoeff;
        float bitangentx = f * (-deltaU2 * edge1x + deltaU1 * edge2x);
        float bitangenty = f * (-deltaU2 * edge1y + deltaU1 * edge2y);
        float bitangentz = f * (-deltaU2 * edge1z + deltaU1 * edge2z);
        float bitcoeff = NormalHelper.rsqrt(bitangentx * bitangentx + bitangenty * bitangenty + bitangentz * bitangentz);
        float pbitangentx = tangenty * normalZ - (tangentz *= tcoeff) * normalY;
        float pbitangenty = tangentz * normalX - tangentx * normalZ;
        float pbitangentz = tangentx * normalY - tangenty * normalX;
        float dot = (bitangentx *= bitcoeff) * pbitangentx + (bitangenty *= bitcoeff) * pbitangenty + (bitangentz *= bitcoeff) * pbitangentz;
        float tangentW = dot < 0.0f ? -1.0f : 1.0f;
        return NormI8.pack(tangentx, tangenty, tangentz, tangentW);
    }

    public static int computeTangent(float normalX, float normalY, float normalZ, TriView t) {
        float deltaV1;
        float deltaU2;
        float x0 = t.x(0);
        float y0 = t.y(0);
        float z0 = t.z(0);
        float x1 = t.x(1);
        float y1 = t.y(1);
        float z1 = t.z(1);
        float x2 = t.x(2);
        float y2 = t.y(2);
        float z2 = t.z(2);
        float edge1x = x1 - x0;
        float edge1y = y1 - y0;
        float edge1z = z1 - z0;
        float edge2x = x2 - x0;
        float edge2y = y2 - y0;
        float edge2z = z2 - z0;
        float u0 = t.u(0);
        float v0 = t.v(0);
        float u1 = t.u(1);
        float v1 = t.v(1);
        float u2 = t.u(2);
        float deltaU1 = u1 - u0;
        float v2 = t.v(2);
        float deltaV2 = v2 - v0;
        float fdenom = deltaU1 * deltaV2 - (deltaU2 = u2 - u0) * (deltaV1 = v1 - v0);
        float f = (double)fdenom == 0.0 ? 1.0f : 1.0f / fdenom;
        float tangentx = f * (deltaV2 * edge1x - deltaV1 * edge2x);
        float tangenty = f * (deltaV2 * edge1y - deltaV1 * edge2y);
        float tangentz = f * (deltaV2 * edge1z - deltaV1 * edge2z);
        float tcoeff = NormalHelper.rsqrt(tangentx * tangentx + tangenty * tangenty + tangentz * tangentz);
        tangentx *= tcoeff;
        tangenty *= tcoeff;
        float bitangentx = f * (-deltaU2 * edge1x + deltaU1 * edge2x);
        float bitangenty = f * (-deltaU2 * edge1y + deltaU1 * edge2y);
        float bitangentz = f * (-deltaU2 * edge1z + deltaU1 * edge2z);
        float bitcoeff = NormalHelper.rsqrt(bitangentx * bitangentx + bitangenty * bitangenty + bitangentz * bitangentz);
        float pbitangentx = tangenty * normalZ - (tangentz *= tcoeff) * normalY;
        float pbitangenty = tangentz * normalX - tangentx * normalZ;
        float pbitangentz = tangentx * normalY - tangenty * normalX;
        float dot = (bitangentx *= bitcoeff) * pbitangentx + (bitangenty *= bitcoeff) * pbitangenty + (bitangentz *= bitcoeff) * pbitangentz;
        float tangentW = dot < 0.0f ? -1.0f : 1.0f;
        return NormI8.pack(tangentx, tangenty, tangentz, tangentW);
    }

    public static int computeTangent(float normalX, float normalY, float normalZ, float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2) {
        return NormalHelper.computeTangent(null, normalX, normalY, normalZ, x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2);
    }

    public static int computeTangent(Vector4f output, float normalX, float normalY, float normalZ, float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2) {
        float pbitangentz;
        float pbitangenty;
        float pbitangentx;
        float bitcoeff;
        float dot;
        float tcoeff;
        float edge1x = x1 - x0;
        float edge1y = y1 - y0;
        float edge1z = z1 - z0;
        float edge2x = x2 - x0;
        float edge2y = y2 - y0;
        float edge2z = z2 - z0;
        float deltaU1 = u1 - u0;
        float deltaV2 = v2 - v0;
        float deltaU2 = u2 - u0;
        float deltaV1 = v1 - v0;
        float fdenom = deltaU1 * deltaV2 - deltaU2 * deltaV1;
        float f = (double)fdenom == 0.0 ? 1.0f : 1.0f / fdenom;
        float tangentx = f * (deltaV2 * edge1x - deltaV1 * edge2x);
        float tangenty = f * (deltaV2 * edge1y - deltaV1 * edge2y);
        float tangentz = f * (deltaV2 * edge1z - deltaV1 * edge2z);
        if ((tangentx *= (tcoeff = NormalHelper.rsqrt(tangentx * tangentx + tangenty * tangenty + tangentz * tangentz))) == 0.0f && (tangenty *= tcoeff) == 0.0f && (tangentz *= tcoeff) == 0.0f) {
            return -1;
        }
        float bitangentx = f * (-deltaU2 * edge1x + deltaU1 * edge2x);
        float bitangenty = f * (-deltaU2 * edge1y + deltaU1 * edge2y);
        float bitangentz = f * (-deltaU2 * edge1z + deltaU1 * edge2z);
        float tangentW = (dot = (bitangentx *= (bitcoeff = NormalHelper.rsqrt(bitangentx * bitangentx + bitangenty * bitangenty + bitangentz * bitangentz))) * (pbitangentx = tangenty * normalZ - tangentz * normalY) + (bitangenty *= bitcoeff) * (pbitangenty = tangentz * normalX - tangentx * normalZ) + (bitangentz *= bitcoeff) * (pbitangentz = tangentx * normalY - tangenty * normalX)) < 0.0f ? -1.0f : 1.0f;
        if (output != null) {
            output.set(tangentx, tangenty, tangentz, tangentW);
        }
        return NormI8.pack(tangentx, tangenty, tangentz, tangentW);
    }

    private static float rsqrt(float value) {
        if (value == 0.0f) {
            return 1.0f;
        }
        return (float)(1.0 / Math.sqrt(value));
    }
}

