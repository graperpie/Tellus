/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_284
 *  net.minecraft.class_285
 *  net.minecraft.class_293
 *  net.minecraft.class_310
 *  net.minecraft.class_5912
 *  net.minecraft.class_5944
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pipeline.programs;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.List;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_284;
import net.minecraft.class_285;
import net.minecraft.class_293;
import net.minecraft.class_310;
import net.minecraft.class_5912;
import net.minecraft.class_5944;
import org.jetbrains.annotations.Nullable;

public class FallbackShader
extends class_5944 {
    private final IrisRenderingPipeline parent;
    private final BlendModeOverride blendModeOverride;
    private final GlFramebuffer writingToBeforeTranslucent;
    private final GlFramebuffer writingToAfterTranslucent;
    @Nullable
    private final class_284 FOG_DENSITY;
    @Nullable
    private final class_284 FOG_IS_EXP2;
    private final int gtexture;
    private final int overlay;
    private final int lightmap;

    public FallbackShader(class_5912 resourceFactory, String string, class_293 vertexFormat, GlFramebuffer writingToBeforeTranslucent, GlFramebuffer writingToAfterTranslucent, BlendModeOverride blendModeOverride, float alphaValue, IrisRenderingPipeline parent) throws IOException {
        super(resourceFactory, string, vertexFormat);
        this.parent = parent;
        this.blendModeOverride = blendModeOverride;
        this.writingToBeforeTranslucent = writingToBeforeTranslucent;
        this.writingToAfterTranslucent = writingToAfterTranslucent;
        this.FOG_DENSITY = this.method_34582("FogDensity");
        this.FOG_IS_EXP2 = this.method_34582("FogIsExp2");
        this.gtexture = GlStateManager._glGetUniformLocation((int)this.method_1270(), (CharSequence)"gtexture");
        this.overlay = GlStateManager._glGetUniformLocation((int)this.method_1270(), (CharSequence)"overlay");
        this.lightmap = GlStateManager._glGetUniformLocation((int)this.method_1270(), (CharSequence)"lightmap");
        class_284 ALPHA_TEST_VALUE = this.method_34582("AlphaTestValue");
        if (ALPHA_TEST_VALUE != null) {
            ALPHA_TEST_VALUE.method_1251(alphaValue);
        }
    }

    public void method_34585() {
        super.method_34585();
        if (this.blendModeOverride != null) {
            BlendModeOverride.restore();
        }
        class_310.method_1551().method_1522().method_1235(false);
    }

    public void method_34586() {
        if (this.FOG_DENSITY != null && this.FOG_IS_EXP2 != null) {
            float fogDensity = CapturedRenderingState.INSTANCE.getFogDensity();
            if ((double)fogDensity >= 0.0) {
                this.FOG_DENSITY.method_1251(fogDensity);
                this.FOG_IS_EXP2.method_35649(1);
            } else {
                this.FOG_DENSITY.method_1251(0.0f);
                this.FOG_IS_EXP2.method_35649(0);
            }
        }
        IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 0, RenderSystem.getShaderTexture((int)0));
        IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 1, RenderSystem.getShaderTexture((int)1));
        IrisRenderSystem.bindTextureToUnit(TextureType.TEXTURE_2D.getGlType(), 2, RenderSystem.getShaderTexture((int)2));
        class_285.method_22094((int)this.method_1270());
        List uniformList = this.field_29490;
        for (class_284 uniform : uniformList) {
            this.uploadIfNotNull(uniform);
        }
        GlStateManager._glUniform1i((int)this.gtexture, (int)0);
        GlStateManager._glUniform1i((int)this.overlay, (int)1);
        GlStateManager._glUniform1i((int)this.lightmap, (int)2);
        if (this.blendModeOverride != null) {
            this.blendModeOverride.apply();
        }
        if (this.parent.isBeforeTranslucent) {
            this.writingToBeforeTranslucent.bind();
        } else {
            this.writingToAfterTranslucent.bind();
        }
    }

    private void uploadIfNotNull(class_284 uniform) {
        if (uniform != null) {
            uniform.method_1300();
        }
    }
}

