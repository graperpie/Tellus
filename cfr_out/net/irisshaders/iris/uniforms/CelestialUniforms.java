/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_638
 *  net.minecraft.class_7833
 *  org.joml.Matrix4f
 *  org.joml.Quaternionfc
 *  org.joml.Vector4f
 */
package net.irisshaders.iris.uniforms;

import java.util.Objects;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_7833;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.joml.Vector4f;

public final class CelestialUniforms {
    private final float sunPathRotation;

    public CelestialUniforms(float sunPathRotation) {
        this.sunPathRotation = sunPathRotation;
    }

    public static float getSunAngle() {
        float skyAngle = CelestialUniforms.getSkyAngle();
        if (skyAngle < 0.75f) {
            return skyAngle + 0.25f;
        }
        return skyAngle - 0.75f;
    }

    private static float getShadowAngle() {
        float shadowAngle = CelestialUniforms.getSunAngle();
        if (!CelestialUniforms.isDay()) {
            shadowAngle -= 0.5f;
        }
        return shadowAngle;
    }

    private static Vector4f getUpPosition() {
        Vector4f upVector = new Vector4f(0.0f, 100.0f, 0.0f, 0.0f);
        Matrix4f preCelestial = new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferModelView());
        preCelestial.rotate((Quaternionfc)class_7833.field_40716.rotationDegrees(-90.0f));
        upVector = preCelestial.transform(upVector);
        return upVector;
    }

    public static boolean isDay() {
        return (double)CelestialUniforms.getSunAngle() <= 0.5;
    }

    private static class_638 getWorld() {
        return Objects.requireNonNull(class_310.method_1551().field_1687);
    }

    private static float getSkyAngle() {
        return CelestialUniforms.getWorld().method_30274(CapturedRenderingState.INSTANCE.getTickDelta());
    }

    public void addCelestialUniforms(UniformHolder uniforms) {
        uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "sunAngle", CelestialUniforms::getSunAngle).uniformTruncated3f(UniformUpdateFrequency.PER_FRAME, "sunPosition", this::getSunPosition).uniformTruncated3f(UniformUpdateFrequency.PER_FRAME, "moonPosition", this::getMoonPosition).uniform1f(UniformUpdateFrequency.PER_FRAME, "shadowAngle", CelestialUniforms::getShadowAngle).uniformTruncated3f(UniformUpdateFrequency.PER_FRAME, "shadowLightPosition", this::getShadowLightPosition).uniformTruncated3f(UniformUpdateFrequency.PER_FRAME, "upPosition", CelestialUniforms::getUpPosition);
    }

    private Vector4f getSunPosition() {
        return this.getCelestialPosition(100.0f);
    }

    private Vector4f getMoonPosition() {
        return this.getCelestialPosition(-100.0f);
    }

    public Vector4f getShadowLightPosition() {
        return CelestialUniforms.isDay() ? this.getSunPosition() : this.getMoonPosition();
    }

    public Vector4f getShadowLightPositionInWorldSpace() {
        return CelestialUniforms.isDay() ? this.getCelestialPositionInWorldSpace(100.0f) : this.getCelestialPositionInWorldSpace(-100.0f);
    }

    private Vector4f getCelestialPositionInWorldSpace(float y) {
        Vector4f position = new Vector4f(0.0f, y, 0.0f, 0.0f);
        Matrix4f celestial = new Matrix4f();
        celestial.identity();
        celestial.rotate((Quaternionfc)class_7833.field_40716.rotationDegrees(-90.0f));
        celestial.rotate((Quaternionfc)class_7833.field_40718.rotationDegrees(this.sunPathRotation));
        celestial.rotate((Quaternionfc)class_7833.field_40714.rotationDegrees(CelestialUniforms.getSkyAngle() * 360.0f));
        celestial.transform(position);
        return position;
    }

    private Vector4f getCelestialPosition(float y) {
        Vector4f position = new Vector4f(0.0f, y, 0.0f, 0.0f);
        Matrix4f celestial = new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferModelView());
        celestial.rotate((Quaternionfc)class_7833.field_40716.rotationDegrees(-90.0f));
        celestial.rotate((Quaternionfc)class_7833.field_40718.rotationDegrees(this.sunPathRotation));
        celestial.rotate((Quaternionfc)class_7833.field_40714.rotationDegrees(CelestialUniforms.getSkyAngle() * 360.0f));
        position = celestial.transform(position);
        return position;
    }
}

