/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.v2.WrapWithCondition
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  net.minecraft.class_291
 *  net.minecraft.class_4066
 *  net.minecraft.class_5294
 *  net.minecraft.class_5539
 *  net.minecraft.class_5944
 *  net.minecraft.class_638
 *  net.minecraft.class_638$class_5271
 *  net.minecraft.class_7172
 *  net.minecraft.class_761
 *  net.minecraft.class_765
 *  net.minecraft.class_9801
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Slice
 */
package net.irisshaders.iris.mixin.sky;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.class_291;
import net.minecraft.class_4066;
import net.minecraft.class_5294;
import net.minecraft.class_5539;
import net.minecraft.class_5944;
import net.minecraft.class_638;
import net.minecraft.class_7172;
import net.minecraft.class_761;
import net.minecraft.class_765;
import net.minecraft.class_9801;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value={class_761.class})
public class MixinLevelRenderer_SunMoonToggle {
    @WrapOperation(method={"renderSky"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V")}, slice={@Slice(from=@At(value="FIELD", target="net/minecraft/client/renderer/LevelRenderer.SUN_LOCATION : Lnet/minecraft/resources/ResourceLocation;"), to=@At(value="FIELD", target="net/minecraft/client/renderer/LevelRenderer.MOON_LOCATION : Lnet/minecraft/resources/ResourceLocation;"))}, allow=1)
    private void iris$beforeDrawSun(class_9801 meshData, Operation<Void> original) {
        if (Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderSun).orElse(true).booleanValue()) {
            original.call(new Object[]{meshData});
        } else {
            meshData.close();
        }
    }

    @WrapWithCondition(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V")})
    private boolean iris$disableWeather(class_761 instance, class_765 lightTexture, float f, double d, double e, double g) {
        return Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderWeather).orElse(true);
    }

    @WrapOperation(method={"tickRain"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal=1)})
    private Object disableRainParticles(class_7172<?> instance, Operation<class_4066> original) {
        if (!Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderWeatherParticles).orElse(true).booleanValue()) {
            return class_4066.field_18199;
        }
        return original.call(new Object[]{instance});
    }

    @WrapOperation(method={"renderSky"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V")}, slice={@Slice(from=@At(value="FIELD", target="net/minecraft/client/renderer/LevelRenderer.MOON_LOCATION : Lnet/minecraft/resources/ResourceLocation;"), to=@At(value="INVOKE", target="net/minecraft/client/multiplayer/ClientLevel.getStarBrightness (F)F"))}, allow=1)
    private void iris$beforeDrawMoon(class_9801 meshData, Operation<Void> original) {
        if (Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderMoon).orElse(true).booleanValue()) {
            original.call(new Object[]{meshData});
        } else {
            meshData.close();
        }
    }

    @WrapOperation(method={"renderSky"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V")}, slice={@Slice(from=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/FogRenderer;levelFogColor()V"), to=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/VertexBuffer;unbind()V", ordinal=0))}, allow=1)
    private void iris$beforeDrawSkyDisc(class_291 instance, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, class_5944 shader, Operation<Void> original) {
        if (Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderSkyDisc).orElse(true).booleanValue()) {
            original.call(new Object[]{instance, modelViewMatrix, projectionMatrix, shader});
        }
    }

    @WrapOperation(method={"renderSky"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseColor(FF)[F")})
    private float[] iris$beforeDrawHorizon(class_5294 instance, float timeOfDay, float partialTicks, Operation<float[]> original) {
        if (Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderSkyDisc).orElse(true).booleanValue()) {
            return (float[])original.call(new Object[]{instance, Float.valueOf(timeOfDay), Float.valueOf(partialTicks)});
        }
        return null;
    }

    @WrapOperation(method={"renderSky"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;getHorizonHeight(Lnet/minecraft/world/level/LevelHeightAccessor;)D")})
    private double iris$beforeDrawHorizon(class_638.class_5271 instance, class_5539 level, Operation<Double> original) {
        if (Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderSkyDisc).orElse(true).booleanValue()) {
            return (Double)original.call(new Object[]{instance, level});
        }
        return Double.NEGATIVE_INFINITY;
    }

    @WrapOperation(method={"renderSky"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F")})
    private float iris$beforeDrawStars(class_638 instance, float partialTick, Operation<Float> original) {
        if (Iris.getPipelineManager().getPipeline().map(WorldRenderingPipeline::shouldRenderStars).orElse(true).booleanValue()) {
            return ((Float)original.call(new Object[]{instance, Float.valueOf(partialTick)})).floatValue();
        }
        return -0.1f;
    }
}

