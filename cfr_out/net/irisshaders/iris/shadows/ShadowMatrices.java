/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4587
 *  net.minecraft.class_7833
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package net.irisshaders.iris.shadows;

import net.minecraft.class_4587;
import net.minecraft.class_7833;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class ShadowMatrices {
    public static final float NEAR = -100.05f;
    public static final float FAR = 156.0f;

    public static Matrix4f createOrthoMatrix(float halfPlaneLength, float nearPlane, float farPlane) {
        return new Matrix4f().setOrthoSymmetric(halfPlaneLength * 2.0f, halfPlaneLength * 2.0f, nearPlane, farPlane);
    }

    public static Matrix4f createPerspectiveMatrix(float fov) {
        float yScale = (float)(1.0 / Math.tan(Math.toRadians(fov) * 0.5));
        return new Matrix4f(yScale, 0.0f, 0.0f, 0.0f, 0.0f, yScale, 0.0f, 0.0f, 0.0f, 0.0f, -0.21851201f, -1.0f, 0.0f, 0.0f, 121.91214f, 1.0f);
    }

    public static void createBaselineModelViewMatrix(class_4587 target, float shadowAngle, float sunPathRotation, float nearPlane, float farPlane) {
        float skyAngle = shadowAngle < 0.25f ? shadowAngle + 0.75f : shadowAngle - 0.25f;
        target.method_23760().method_23762().identity();
        target.method_23760().method_23761().identity();
        target.method_22907(class_7833.field_40714.rotationDegrees(90.0f));
        target.method_22907(class_7833.field_40718.rotationDegrees(skyAngle * -360.0f));
        target.method_22907(class_7833.field_40714.rotationDegrees(sunPathRotation));
    }

    public static void snapModelViewToGrid(class_4587 target, float shadowIntervalSize, double cameraX, double cameraY, double cameraZ) {
        if (Math.abs(shadowIntervalSize) == 0.0f) {
            return;
        }
        float offsetX = (float)cameraX % shadowIntervalSize;
        float offsetY = (float)cameraY % shadowIntervalSize;
        float offsetZ = (float)cameraZ % shadowIntervalSize;
        float halfIntervalSize = shadowIntervalSize / 2.0f;
        target.method_23760().method_23761().translate(offsetX -= halfIntervalSize, offsetY -= halfIntervalSize, offsetZ -= halfIntervalSize);
    }

    public static void createModelViewMatrix(class_4587 target, float shadowAngle, float shadowIntervalSize, float sunPathRotation, double cameraX, double cameraY, double cameraZ, float nearPlane, float farPlane) {
        ShadowMatrices.createBaselineModelViewMatrix(target, shadowAngle, sunPathRotation, nearPlane, farPlane);
        ShadowMatrices.snapModelViewToGrid(target, shadowIntervalSize, cameraX, cameraY, cameraZ);
    }

    private static final class Tests {
        private Tests() {
        }

        public static void main(String[] args) {
            Matrix4f expected = new Matrix4f(0.03125f, 0.0f, 0.0f, 0.0f, 0.0f, 0.03125f, 0.0f, 0.0f, 0.0f, 0.0f, -0.007814026f, 0.0f, 0.0f, 0.0f, -1.0003906f, 1.0f);
            Tests.test("ortho projection hpl=32", expected, ShadowMatrices.createOrthoMatrix(32.0f, 0.05f, 256.0f));
            Matrix4f expected110 = new Matrix4f(0.009090909f, 0.0f, 0.0f, 0.0f, 0.0f, 0.009090909f, 0.0f, 0.0f, 0.0f, 0.0f, -0.007814026f, 0.0f, 0.0f, 0.0f, -1.0003906f, 1.0f);
            Tests.test("ortho projection hpl=110", expected110, ShadowMatrices.createOrthoMatrix(110.0f, 0.05f, 256.0f));
            Matrix4f expected90Proj = new Matrix4f(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0003906f, -1.0f, 0.0f, 0.0f, -0.10001954f, 0.0f);
            Tests.test("perspective projection fov=90", expected90Proj, ShadowMatrices.createPerspectiveMatrix(90.0f));
            Matrix4f expectedModelViewAtDawn = new Matrix4f(0.2154504f, 5.8204815E-8f, 0.9765147f, 0.0f, -0.97651476f, 1.2841845E-8f, 0.21545039f, 0.0f, 0.0f, -0.99999994f, 5.9604645E-8f, 0.0f, 0.3800215f, 1.0264281f, -100.44631f, 1.0f);
            class_4587 modelView = new class_4587();
            ShadowMatrices.createModelViewMatrix(modelView, 0.03451777f, 2.0f, 0.0f, 0.646045982837677, 82.53274536132812, -514.0264282226562, -100.05f, 156.0f);
            Tests.test("model view at dawn", expectedModelViewAtDawn, modelView.method_23760().method_23761());
        }

        private static void test(String name, Matrix4f expected, Matrix4f created) {
            if (expected.equals((Matrix4fc)created, 5.0E-4f)) {
                System.err.println("test " + name + " failed: ");
                System.err.println("    expected: ");
                System.err.print(expected);
                System.err.println("    created: ");
                System.err.print(created.toString());
            } else {
                System.out.println("test " + name + " passed");
            }
        }
    }
}

