/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_291
 *  net.minecraft.class_291$class_8555
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_310
 *  net.minecraft.class_4588
 *  net.minecraft.class_5944
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package net.irisshaders.iris.pathways;

import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_291;
import net.minecraft.class_293;
import net.minecraft.class_310;
import net.minecraft.class_4588;
import net.minecraft.class_5944;
import net.minecraft.class_9801;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class HorizonRenderer {
    private static final float TOP = 16.0f;
    private static final float BOTTOM = -16.0f;
    private static final float COS_22_5 = (float)Math.cos(Math.toRadians(22.5));
    private static final float SIN_22_5 = (float)Math.sin(Math.toRadians(22.5));
    private class_291 buffer;
    private int currentRenderDistance;

    public HorizonRenderer() {
        this.currentRenderDistance = class_310.method_1551().field_1690.method_38521();
        this.rebuildBuffer();
    }

    private void rebuildBuffer() {
        if (this.buffer != null) {
            this.buffer.close();
        }
        class_287 buffer = class_289.method_1348().method_60827(class_293.class_5596.field_27382, class_290.field_1592);
        this.buildHorizon(this.currentRenderDistance * 16, (class_4588)buffer);
        class_9801 meshData = buffer.method_60794();
        this.buffer = new class_291(class_291.class_8555.field_44793);
        this.buffer.method_1353();
        this.buffer.method_1352(meshData);
        class_289.method_1348().method_60828();
        class_291.method_1354();
    }

    private void buildQuad(class_4588 consumer, float x1, float z1, float x2, float z2) {
        consumer.method_22912(x1, -16.0f, z1);
        consumer.method_22912(x1, 16.0f, z1);
        consumer.method_22912(x2, 16.0f, z2);
        consumer.method_22912(x2, -16.0f, z2);
    }

    private void buildHalf(class_4588 consumer, float adjacent, float opposite, boolean invert) {
        if (invert) {
            adjacent = -adjacent;
            opposite = -opposite;
        }
        this.buildQuad(consumer, adjacent, -opposite, opposite, -adjacent);
        this.buildQuad(consumer, adjacent, opposite, adjacent, -opposite);
        this.buildQuad(consumer, opposite, adjacent, adjacent, opposite);
        this.buildQuad(consumer, -opposite, adjacent, opposite, adjacent);
    }

    private void buildOctagonalPrism(class_4588 consumer, float adjacent, float opposite) {
        this.buildHalf(consumer, adjacent, opposite, false);
        this.buildHalf(consumer, adjacent, opposite, true);
    }

    private void buildRegularOctagonalPrism(class_4588 consumer, float radius) {
        this.buildOctagonalPrism(consumer, radius * COS_22_5, radius * SIN_22_5);
    }

    private void buildBottomPlane(class_4588 consumer, int radius) {
        for (int x = -radius; x <= radius; x += 64) {
            for (int z = -radius; z <= radius; z += 64) {
                consumer.method_22912((float)(x + 64), -16.0f, (float)z);
                consumer.method_22912((float)x, -16.0f, (float)z);
                consumer.method_22912((float)x, -16.0f, (float)(z + 64));
                consumer.method_22912((float)(x + 64), -16.0f, (float)(z + 64));
            }
        }
    }

    private void buildTopPlane(class_4588 consumer, int radius) {
        for (int x = -radius; x <= radius; x += 64) {
            for (int z = -radius; z <= radius; z += 64) {
                consumer.method_22912((float)(x + 64), 16.0f, (float)z);
                consumer.method_22912((float)(x + 64), 16.0f, (float)(z + 64));
                consumer.method_22912((float)x, 16.0f, (float)(z + 64));
                consumer.method_22912((float)x, 16.0f, (float)z);
            }
        }
    }

    private void buildHorizon(int radius, class_4588 consumer) {
        if (radius > 256) {
            radius = 256;
        }
        this.buildRegularOctagonalPrism(consumer, radius);
        this.buildTopPlane(consumer, 384);
        this.buildBottomPlane(consumer, 384);
    }

    public void renderHorizon(Matrix4fc modelView, Matrix4fc projection, class_5944 shader) {
        if (this.currentRenderDistance != class_310.method_1551().field_1690.method_38521()) {
            this.currentRenderDistance = class_310.method_1551().field_1690.method_38521();
            this.rebuildBuffer();
        }
        this.buffer.method_1353();
        this.buffer.method_34427(new Matrix4f(modelView), new Matrix4f(projection), shader);
        class_291.method_1354();
    }

    public void destroy() {
        this.buffer.close();
    }
}

