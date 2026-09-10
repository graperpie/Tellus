/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3532
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package net.irisshaders.iris.uniforms;

import java.util.function.Supplier;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shadows.ShadowMatrices;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_3532;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class MatrixUniforms {
    private MatrixUniforms() {
    }

    public static void addMatrixUniforms(UniformHolder uniforms, PackDirectives directives) {
        MatrixUniforms.addMatrix(uniforms, "ModelView", CapturedRenderingState.INSTANCE::getGbufferModelView);
        MatrixUniforms.addMatrix(uniforms, "Projection", CapturedRenderingState.INSTANCE::getGbufferProjection);
        MatrixUniforms.addDHMatrix(uniforms, "Projection", DHCompat::getProjection);
        MatrixUniforms.addShadowMatrix(uniforms, "ModelView", () -> new Matrix4f((Matrix4fc)ShadowRenderer.createShadowModelView(directives.getSunPathRotation(), directives.getShadowDirectives().getIntervalSize(), class_3532.method_15347((float)directives.getShadowDirectives().getNearPlane(), (float)-1.0f) ? (float)(-DHCompat.getRenderDistance() * 16) : directives.getShadowDirectives().getNearPlane(), class_3532.method_15347((float)directives.getShadowDirectives().getFarPlane(), (float)-1.0f) ? (float)(DHCompat.getRenderDistance() * 16) : directives.getShadowDirectives().getFarPlane()).method_23760().method_23761()));
        MatrixUniforms.addShadowMatrix(uniforms, "Projection", () -> ShadowMatrices.createOrthoMatrix(directives.getShadowDirectives().getDistance(), class_3532.method_15347((float)directives.getShadowDirectives().getNearPlane(), (float)-1.0f) ? (float)(-DHCompat.getRenderDistance() * 16) : directives.getShadowDirectives().getNearPlane(), class_3532.method_15347((float)directives.getShadowDirectives().getFarPlane(), (float)-1.0f) ? (float)(DHCompat.getRenderDistance() * 16) : directives.getShadowDirectives().getFarPlane()));
    }

    private static void addMatrix(UniformHolder uniforms, String name, Supplier<Matrix4fc> supplier) {
        uniforms.uniformMatrix(UniformUpdateFrequency.PER_FRAME, "gbuffer" + name, supplier).uniformMatrix(UniformUpdateFrequency.PER_FRAME, "gbuffer" + name + "Inverse", new Inverted(supplier)).uniformMatrix(UniformUpdateFrequency.PER_FRAME, "gbufferPrevious" + name, new Previous(supplier));
    }

    private static void addDHMatrix(UniformHolder uniforms, String name, Supplier<Matrix4fc> supplier) {
        uniforms.uniformMatrix(UniformUpdateFrequency.PER_FRAME, "dh" + name, supplier).uniformMatrix(UniformUpdateFrequency.PER_FRAME, "dh" + name + "Inverse", new Inverted(supplier)).uniformMatrix(UniformUpdateFrequency.PER_FRAME, "dhPrevious" + name, new Previous(supplier));
    }

    private static void addShadowMatrix(UniformHolder uniforms, String name, Supplier<Matrix4fc> supplier) {
        uniforms.uniformMatrix(UniformUpdateFrequency.PER_FRAME, "shadow" + name, supplier).uniformMatrix(UniformUpdateFrequency.PER_FRAME, "shadow" + name + "Inverse", new Inverted(supplier));
    }

    private record Inverted(Supplier<Matrix4fc> parent) implements Supplier<Matrix4fc>
    {
        @Override
        public Matrix4fc get() {
            Matrix4f copy = new Matrix4f(this.parent.get());
            copy.invert();
            return copy;
        }
    }

    private static class Previous
    implements Supplier<Matrix4fc> {
        private final Supplier<Matrix4fc> parent;
        private Matrix4f previous;

        Previous(Supplier<Matrix4fc> parent) {
            this.parent = parent;
            this.previous = new Matrix4f();
        }

        @Override
        public Matrix4fc get() {
            Matrix4f copy = new Matrix4f(this.parent.get());
            Matrix4f previous = new Matrix4f((Matrix4fc)this.previous);
            this.previous = copy;
            return previous;
        }
    }
}

