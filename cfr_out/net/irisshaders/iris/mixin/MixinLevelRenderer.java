/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_124
 *  net.minecraft.class_1921
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_4599
 *  net.minecraft.class_4604
 *  net.minecraft.class_638
 *  net.minecraft.class_757
 *  net.minecraft.class_761
 *  net.minecraft.class_765
 *  net.minecraft.class_7833
 *  net.minecraft.class_9779
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 *  org.spongepowered.asm.mixin.injection.Slice
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.layer.IsOutlineRenderStateShard;
import net.irisshaders.iris.layer.OuterWrappedRenderType;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shadows.frustum.fallback.NonCullingFrustum;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.IrisTimeUniforms;
import net.minecraft.class_124;
import net.minecraft.class_1921;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4599;
import net.minecraft.class_4604;
import net.minecraft.class_638;
import net.minecraft.class_757;
import net.minecraft.class_761;
import net.minecraft.class_765;
import net.minecraft.class_7833;
import net.minecraft.class_9779;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_761.class})
public class MixinLevelRenderer {
    @Unique
    private static final String CLEAR = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V";
    @Unique
    private static final String RENDER_SKY = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V";
    @Unique
    private static final String RENDER_CLOUDS = "Lnet/minecraft/client/renderer/LevelRenderer;renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FDDD)V";
    @Unique
    private static final String RENDER_WEATHER = "Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V";
    @Shadow
    @Final
    private class_310 field_4088;
    @Unique
    private WorldRenderingPipeline pipeline;
    @Shadow
    private class_4599 field_20951;
    @Shadow
    private int field_4073;
    @Shadow
    private class_4604 field_27740;
    @Shadow
    @Nullable
    private class_638 field_4085;
    @Unique
    private boolean warned;
    @Unique
    private NonCullingFrustum overrideFrustum;

    @Inject(method={"renderLevel"}, at={@At(value="HEAD")})
    private void iris$setupPipeline(class_9779 deltaTracker, boolean renderBlockOutline, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f modelView, Matrix4f projection, CallbackInfo callback) {
        DHCompat.checkFrame();
        IrisTimeUniforms.updateTime();
        CapturedRenderingState.INSTANCE.setGbufferModelView((Matrix4fc)modelView);
        CapturedRenderingState.INSTANCE.setGbufferProjection(projection);
        float fakeTickDelta = deltaTracker.method_60637(false);
        CapturedRenderingState.INSTANCE.setTickDelta(fakeTickDelta);
        CapturedRenderingState.INSTANCE.setCloudTime(((float)this.field_4073 + fakeTickDelta) * 0.03f);
        this.pipeline = Iris.getPipelineManager().preparePipeline(Iris.getCurrentDimension());
        if (this.pipeline.shouldDisableFrustumCulling()) {
            this.overrideFrustum = new NonCullingFrustum();
            this.overrideFrustum.method_23088(camera.method_19326().field_1352, camera.method_19326().field_1351, camera.method_19326().field_1350);
            this.field_27740 = this.overrideFrustum;
        } else {
            this.overrideFrustum = null;
        }
        IrisRenderSystem.backupAndDisableCullingState(this.pipeline.shouldDisableOcclusionCulling());
        if (Iris.shouldActivateWireframe() && this.field_4088.method_1542()) {
            IrisRenderSystem.setPolygonMode(6913);
        }
    }

    @ModifyArg(method={"renderLevel"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"), index=1)
    private class_4604 changeFrustum(class_4604 frustum) {
        return this.overrideFrustum != null ? this.overrideFrustum : frustum;
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V", shift=At.Shift.AFTER, remap=false)})
    private void iris$beginLevelRender(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.beginLevelRendering();
        this.pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @Inject(method={"renderLevel"}, at={@At(value="RETURN", shift=At.Shift.BEFORE)})
    private void iris$endLevelRender(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f modelMatrix, Matrix4f matrix4f2, CallbackInfo ci) {
        HandRenderer.INSTANCE.renderTranslucent((Matrix4fc)modelMatrix, deltaTracker.method_60637(true), camera, gameRenderer, this.pipeline);
        class_310.method_1551().method_16011().method_15405("iris_final");
        this.pipeline.finalizeLevelRendering();
        this.pipeline = null;
        if (!this.warned) {
            this.warned = true;
            Iris.getUpdateChecker().getBetaInfo().ifPresent(info -> class_310.method_1551().field_1705.method_1743().method_1812((class_2561)class_2561.method_43470((String)("A new beta is out for Iris " + info.betaTag + ". Please redownload it.")).method_27695(new class_124[]{class_124.field_1067, class_124.field_1061})));
        }
        IrisRenderSystem.restoreCullingState();
        if (Iris.shouldActivateWireframe() && this.field_4088.method_1542()) {
            IrisRenderSystem.setPolygonMode(6914);
        }
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V")})
    private void iris$renderTerrainShadows(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.renderShadows((LevelRendererAccessor)((Object)this), camera);
    }

    @ModifyVariable(method={"renderSky"}, at=@At(value="HEAD"), index=5, argsOnly=true)
    private boolean iris$alwaysRenderSky(boolean value) {
        return false;
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V")})
    private void iris$beginSky(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.CUSTOM_SKY);
        RenderSystem.setShader(class_757::method_34539);
    }

    @Inject(method={"renderSky"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/FogRenderer;levelFogColor()V")})
    private void iris$renderSky$beginNormalSky(Matrix4f matrix4f, Matrix4f matrix4f2, float f, class_4184 camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.SKY);
    }

    @Inject(method={"renderSky"}, at={@At(value="FIELD", target="Lnet/minecraft/client/renderer/LevelRenderer;SUN_LOCATION:Lnet/minecraft/resources/ResourceLocation;")})
    private void iris$setSunRenderStage(Matrix4f matrix4f, Matrix4f matrix4f2, float f, class_4184 camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.SUN);
    }

    @Inject(method={"renderSky"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseColor(FF)[F")})
    private void iris$setSunsetRenderStage(Matrix4f matrix4f, Matrix4f matrix4f2, float f, class_4184 camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.SUNSET);
    }

    @Inject(method={"renderSky"}, at={@At(value="FIELD", target="Lnet/minecraft/client/renderer/LevelRenderer;MOON_LOCATION:Lnet/minecraft/resources/ResourceLocation;")})
    private void iris$setMoonRenderStage(Matrix4f matrix4f, Matrix4f matrix4f2, float f, class_4184 camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.MOON);
    }

    @Inject(method={"renderSky"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F")})
    private void iris$setStarRenderStage(Matrix4f matrix4f, Matrix4f matrix4f2, float f, class_4184 camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.STARS);
    }

    @Inject(method={"renderSky"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/player/LocalPlayer;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;")})
    private void iris$setVoidRenderStage(Matrix4f matrix4f, Matrix4f matrix4f2, float f, class_4184 camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.VOID);
    }

    @Inject(method={"renderSky"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel;getTimeOfDay(F)F")}, slice={@Slice(from=@At(value="FIELD", target="Lcom/mojang/math/Axis;YP:Lcom/mojang/math/Axis;"))})
    private void iris$renderSky$tiltSun(Matrix4f matrix4f, Matrix4f matrix4f2, float f, class_4184 camera, boolean bl, Runnable runnable, CallbackInfo ci, @Local class_4587 poseStack) {
        poseStack.method_22907(class_7833.field_40718.rotationDegrees(this.pipeline.getSunPathRotation()));
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", shift=At.Shift.AFTER)})
    private void iris$endSky(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V")})
    private void iris$beginClouds(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.CLOUDS);
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FDDD)V", shift=At.Shift.AFTER)})
    private void iris$endClouds(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @Inject(method={"renderSectionLayer"}, at={@At(value="HEAD")})
    private void iris$beginTerrainLayer(class_1921 renderType, double d, double e, double f, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.fromTerrainRenderType(renderType));
    }

    @Inject(method={"renderSectionLayer"}, at={@At(value="RETURN")})
    private void iris$endTerrainLayer(class_1921 renderType, double d, double e, double f, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V")})
    private void iris$beginWeather(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.RAIN_SNOW);
    }

    @ModifyArg(method={"Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;depthMask(Z)V", ordinal=0, remap=false))
    private boolean iris$writeRainAndSnowToDepthBuffer(boolean depthMaskEnabled) {
        if (this.pipeline.shouldWriteRainAndSnowToDepthBuffer()) {
            return true;
        }
        return depthMaskEnabled;
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderSnowAndRain(Lnet/minecraft/client/renderer/LightTexture;FDDD)V", shift=At.Shift.AFTER)})
    private void iris$endWeather(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderWorldBorder(Lnet/minecraft/client/Camera;)V")})
    private void iris$beginWorldBorder(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.WORLD_BORDER);
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderWorldBorder(Lnet/minecraft/client/Camera;)V", shift=At.Shift.AFTER)})
    private void iris$endWorldBorder(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V")})
    private void iris$setDebugRenderStage(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.DEBUG);
    }

    @Inject(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V", shift=At.Shift.AFTER)})
    private void iris$resetDebugRenderStage(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.setPhase(WorldRenderingPhase.NONE);
    }

    @ModifyArg(method={"renderLevel"}, at=@At(value="INVOKE", target="net/minecraft/client/renderer/MultiBufferSource$BufferSource.getBuffer (Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), slice=@Slice(from=@At(value="CONSTANT", args={"stringValue=outline"}), to=@At(value="INVOKE", target="net/minecraft/client/renderer/LevelRenderer.renderHitOutline (Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V")))
    private class_1921 iris$beginBlockOutline(class_1921 type) {
        return new OuterWrappedRenderType("iris:is_outline", type, IsOutlineRenderStateShard.INSTANCE);
    }

    @Inject(method={"renderLevel"}, at={@At(value="CONSTANT", args={"stringValue=translucent"})})
    private void iris$beginTranslucents(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f modelMatrix, Matrix4f matrix4f2, CallbackInfo ci) {
        this.pipeline.beginHand();
        HandRenderer.INSTANCE.renderSolid((Matrix4fc)modelMatrix, deltaTracker.method_60637(true), camera, gameRenderer, this.pipeline);
        class_310.method_1551().method_16011().method_15405("iris_pre_translucent");
        this.pipeline.beginTranslucents();
    }
}

