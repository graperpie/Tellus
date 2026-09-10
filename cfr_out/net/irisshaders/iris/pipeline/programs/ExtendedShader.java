/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_281
 *  net.minecraft.class_281$class_282
 *  net.minecraft.class_284
 *  net.minecraft.class_285
 *  net.minecraft.class_293
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_3679
 *  net.minecraft.class_5912
 *  net.minecraft.class_5913
 *  net.minecraft.class_5944
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix3f
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package net.irisshaders.iris.pipeline.programs;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.image.ImageHolder;
import net.irisshaders.iris.gl.program.IrisProgramTypes;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.gl.uniform.DynamicLocationalUniformHolder;
import net.irisshaders.iris.mixinterface.ShaderInstanceInterface;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.class_281;
import net.minecraft.class_284;
import net.minecraft.class_285;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_3679;
import net.minecraft.class_5912;
import net.minecraft.class_5913;
import net.minecraft.class_5944;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class ExtendedShader
extends class_5944
implements ShaderInstanceInterface {
    private static final Matrix4f IDENTITY = new Matrix4f().identity();
    private static final class_284 FAKE_UNIFORM = new class_284("", 1, 2, null);
    private final boolean intensitySwizzle;
    private final List<BufferBlendOverride> bufferBlendOverrides;
    private final boolean hasOverrides;
    private final class_284 modelViewInverse;
    private final class_284 projectionInverse;
    private final class_284 normalMatrix;
    private final CustomUniforms customUniforms;
    private final IrisRenderingPipeline parent;
    private final ProgramUniforms uniforms;
    private final ProgramSamplers samplers;
    private final ProgramImages images;
    private final GlFramebuffer writingToBeforeTranslucent;
    private final GlFramebuffer writingToAfterTranslucent;
    private final BlendModeOverride blendModeOverride;
    private final float alphaTest;
    private final boolean usesTessellation;
    private final Matrix4f tempMatrix4f = new Matrix4f();
    private final Matrix3f tempMatrix3f = new Matrix3f();
    private final float[] tempFloats = new float[16];
    private final float[] tempFloats2 = new float[9];
    private class_281 geometry;
    private class_281 tessControl;
    private class_281 tessEval;

    public ExtendedShader(class_5912 resourceFactory, String name, class_293 vertexFormat, boolean usesTessellation, GlFramebuffer writingToBeforeTranslucent, GlFramebuffer writingToAfterTranslucent, BlendModeOverride blendModeOverride, AlphaTest alphaTest, Consumer<DynamicLocationalUniformHolder> uniformCreator, BiConsumer<SamplerHolder, ImageHolder> samplerCreator, boolean isIntensity, IrisRenderingPipeline parent, @Nullable List<BufferBlendOverride> bufferBlendOverrides, CustomUniforms customUniforms) throws IOException {
        super(resourceFactory, name, vertexFormat);
        this.setupDebugNames(name);
        ProgramUniforms.Builder uniformBuilder = ProgramUniforms.builder(name, this.method_1270());
        ProgramSamplers.Builder samplerBuilder = ProgramSamplers.builder(this.method_1270(), IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS);
        ProgramImages.Builder imageBuilder = ProgramImages.builder(this.method_1270());
        uniformCreator.accept(uniformBuilder);
        samplerCreator.accept(samplerBuilder, imageBuilder);
        customUniforms.mapholderToPass(uniformBuilder, this);
        this.uniforms = uniformBuilder.buildUniforms();
        this.samplers = samplerBuilder.build();
        this.images = imageBuilder.build();
        this.usesTessellation = usesTessellation;
        this.writingToBeforeTranslucent = writingToBeforeTranslucent;
        this.writingToAfterTranslucent = writingToAfterTranslucent;
        this.blendModeOverride = blendModeOverride;
        this.bufferBlendOverrides = bufferBlendOverrides;
        this.hasOverrides = bufferBlendOverrides != null && !bufferBlendOverrides.isEmpty();
        this.alphaTest = alphaTest.reference();
        this.parent = parent;
        this.customUniforms = customUniforms;
        this.intensitySwizzle = isIntensity;
        this.modelViewInverse = this.method_34582("ModelViewMatInverse");
        this.projectionInverse = this.method_34582("ProjMatInverse");
        this.normalMatrix = this.method_34582("NormalMat");
    }

    private void setupDebugNames(String name) {
        GLDebug.nameObject(33505, this.method_1274().method_34417(), name + "_vertex.vsh");
        GLDebug.nameObject(33505, this.method_1278().method_34417(), name + "_fragment.fsh");
        GLDebug.nameObject(33506, this.method_1270(), name);
    }

    public void method_34585() {
        ProgramUniforms.clearActiveUniforms();
        ProgramSamplers.clearActiveSamplers();
        if (this.blendModeOverride != null || this.hasOverrides) {
            BlendModeOverride.restore();
        }
        class_310.method_1551().method_1522().method_1235(false);
    }

    public void method_34586() {
        CapturedRenderingState.INSTANCE.setCurrentAlphaTest(this.alphaTest);
        class_285.method_22094((int)this.method_1270());
        this.setupTextures();
        this.updateMatrices();
        this.updateUniforms();
        this.applyBlendModes();
        this.bindFramebuffer();
    }

    private void setupTextures() {
        if (this.intensitySwizzle) {
            IrisRenderSystem.texParameteriv(RenderSystem.getShaderTexture((int)0), TextureType.TEXTURE_2D.getGlType(), 36422, new int[]{6403, 6403, 6403, 6403});
        }
        IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 0, RenderSystem.getShaderTexture((int)0));
        IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 1, RenderSystem.getShaderTexture((int)1));
        IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 2, RenderSystem.getShaderTexture((int)2));
        ImmediateState.usingTessellation = this.usesTessellation;
    }

    private void updateMatrices() {
        if (this.field_29471 != null && this.projectionInverse != null) {
            this.projectionInverse.method_1253(this.tempMatrix4f.set(this.field_29471.method_35664()).invert().get(this.tempFloats));
        } else if (this.projectionInverse != null) {
            this.projectionInverse.method_1250(IDENTITY);
        }
        if (this.field_29470 != null) {
            if (this.modelViewInverse != null) {
                this.modelViewInverse.method_1253(this.tempMatrix4f.set(this.field_29470.method_35664()).invert().get(this.tempFloats));
            }
            if (this.normalMatrix != null) {
                this.normalMatrix.method_1253(this.tempMatrix3f.set((Matrix4fc)this.tempMatrix4f.set(this.field_29470.method_35664())).invert().transpose().get(this.tempFloats2));
            }
        }
    }

    private void updateUniforms() {
        this.uploadIfNotNull(this.projectionInverse);
        this.uploadIfNotNull(this.modelViewInverse);
        this.uploadIfNotNull(this.normalMatrix);
        this.field_29490.forEach(this::uploadIfNotNull);
        this.samplers.update();
        this.uniforms.update();
        this.customUniforms.push(this);
        this.images.update();
    }

    private void applyBlendModes() {
        if (this.blendModeOverride != null) {
            this.blendModeOverride.apply();
        }
        if (this.hasOverrides) {
            this.bufferBlendOverrides.forEach(BufferBlendOverride::apply);
        }
    }

    private void bindFramebuffer() {
        if (this.parent.isBeforeTranslucent) {
            this.writingToBeforeTranslucent.bind();
        } else {
            this.writingToAfterTranslucent.bind();
        }
    }

    @Nullable
    public class_284 method_34582(@NotNull String name) {
        class_284 uniform = super.method_34582("iris_" + name);
        if (uniform == null && (name.equalsIgnoreCase("OverlayUV") || name.equalsIgnoreCase("LightUV"))) {
            return FAKE_UNIFORM;
        }
        return uniform;
    }

    private void uploadIfNotNull(class_284 uniform) {
        if (uniform != null) {
            uniform.method_1300();
        }
    }

    public void method_34418() {
        super.method_34418();
        this.attachExtraShaders();
    }

    private void attachExtraShaders() {
        if (this.geometry != null) {
            this.geometry.method_1281((class_3679)this);
        }
        if (this.tessControl != null) {
            this.tessControl.method_1281((class_3679)this);
        }
        if (this.tessEval != null) {
            this.tessEval.method_1281((class_3679)this);
        }
    }

    @Override
    public void iris$createExtraShaders(class_5912 factory, String name) {
        this.createGeometryShader(factory, name);
        this.createTessControlShader(factory, name);
        this.createTessEvalShader(factory, name);
    }

    @Override
    public void setShouldSkip(MethodHandle s) {
    }

    private void createGeometryShader(class_5912 factory, String name) {
        this.createShader(factory, name, "_geometry.gsh", IrisProgramTypes.GEOMETRY, program -> {
            this.geometry = program;
        });
    }

    private void createTessControlShader(class_5912 factory, String name) {
        this.createShader(factory, name, "_tessControl.tcs", IrisProgramTypes.TESS_CONTROL, program -> {
            this.tessControl = program;
        });
    }

    private void createTessEvalShader(class_5912 factory, String name) {
        this.createShader(factory, name, "_tessEval.tes", IrisProgramTypes.TESS_EVAL, program -> {
            this.tessEval = program;
        });
    }

    private void createShader(class_5912 factory, String name, String suffix, class_281.class_282 type, Consumer<class_281> programSetter) {
        factory.method_14486(class_2960.method_60655((String)"minecraft", (String)(name + suffix))).ifPresent(resource -> {
            try {
                class_281 program = class_281.method_1283((class_281.class_282)type, (String)name, (InputStream)resource.method_14482(), (String)resource.method_14480(), (class_5913)new class_5913(this){

                    @Nullable
                    public String method_34233(boolean bl, String string) {
                        return null;
                    }
                });
                GLDebug.nameObject(33505, program.method_34417(), name + suffix);
                programSetter.accept(program);
            }
            catch (IOException e) {
                Iris.logger.error("Failed to create shader program", e);
            }
        });
    }

    public class_281 getGeometry() {
        return this.geometry;
    }

    public class_281 getTessControl() {
        return this.tessControl;
    }

    public class_281 getTessEval() {
        return this.tessEval;
    }

    public boolean hasActiveImages() {
        return this.images.getActiveImages() > 0;
    }
}

