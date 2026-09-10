/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_293
 *  net.minecraft.class_296
 *  net.minecraft.class_5944
 *  net.minecraft.class_757
 */
package net.irisshaders.iris.pipeline.programs;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.class_293;
import net.minecraft.class_296;
import net.minecraft.class_5944;
import net.minecraft.class_757;

public class ShaderAccess {
    public static final class_293 IE_FORMAT = class_293.method_60833().method_60842("Position", class_296.field_52107).method_60842("Color", class_296.field_52108).method_60842("UV0", class_296.field_52109).method_60842("Normal", class_296.field_52113).method_60841(1).method_60840();

    public static class_5944 getParticleTranslucentShader() {
        class_5944 override;
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline && (override = ((ShaderRenderingPipeline)pipeline).getShaderMap().getShader(ShaderKey.PARTICLES_TRANS)) != null) {
            return override;
        }
        return class_757.method_34546();
    }

    public static class_5944 getIEVBOShader() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline)pipeline).getShaderMap().getShader(ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? ShaderKey.IE_COMPAT_SHADOW : ShaderKey.IE_COMPAT);
        }
        return null;
    }

    public static class_5944 getMekanismFlameShader() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline)pipeline).getShaderMap().getShader(ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? ShaderKey.MEKANISM_FLAME_SHADOW : ShaderKey.MEKANISM_FLAME);
        }
        return class_757.method_34543();
    }

    public static class_5944 getMekasuitShader() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline)pipeline).getShaderMap().getShader(ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? ShaderKey.SHADOW_ENTITIES_CUTOUT : ShaderKey.ENTITIES_TRANSLUCENT);
        }
        return class_757.method_34503();
    }

    public static class_5944 getSPSShader() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline)pipeline).getShaderMap().getShader(ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? ShaderKey.SHADOW_ENTITIES_CUTOUT : ShaderKey.SPS);
        }
        return class_757.method_34503();
    }
}

