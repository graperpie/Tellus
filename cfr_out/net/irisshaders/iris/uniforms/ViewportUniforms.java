/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 */
package net.irisshaders.iris.uniforms;

import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.minecraft.class_310;

public final class ViewportUniforms {
    private ViewportUniforms() {
    }

    public static void addViewportUniforms(UniformHolder uniforms) {
        uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "viewHeight", () -> class_310.method_1551().method_1522().field_1481).uniform1f(UniformUpdateFrequency.PER_FRAME, "viewWidth", () -> class_310.method_1551().method_1522().field_1482).uniform1f(UniformUpdateFrequency.PER_FRAME, "aspectRatio", ViewportUniforms::getAspectRatio);
    }

    private static float getAspectRatio() {
        return (float)class_310.method_1551().method_1522().field_1482 / (float)class_310.method_1551().method_1522().field_1481;
    }
}

