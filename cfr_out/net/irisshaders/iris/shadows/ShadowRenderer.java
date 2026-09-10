/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.blaze3d.systems.RenderSystem
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  net.minecraft.class_1297
 *  net.minecraft.class_1921
 *  net.minecraft.class_2338
 *  net.minecraft.class_2586
 *  net.minecraft.class_310
 *  net.minecraft.class_3532
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_4599
 *  net.minecraft.class_4604
 *  net.minecraft.class_638
 *  net.minecraft.class_746
 *  net.minecraft.class_761
 *  net.minecraft.class_898
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3d
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 */
package net.irisshaders.iris.shadows;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.irisshaders.batchedentityrendering.impl.BatchingDebugMessageHelper;
import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.RenderBuffersExt;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shaderpack.properties.ShadowCullState;
import net.irisshaders.iris.shadows.CullingDataCache;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.irisshaders.iris.shadows.ShadowMatrices;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.shadows.frustum.BoxCuller;
import net.irisshaders.iris.shadows.frustum.CullEverythingFrustum;
import net.irisshaders.iris.shadows.frustum.FrustumHolder;
import net.irisshaders.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import net.irisshaders.iris.shadows.frustum.advanced.ReversedAdvancedShadowCullingFrustum;
import net.irisshaders.iris.shadows.frustum.fallback.BoxCullingFrustum;
import net.irisshaders.iris.shadows.frustum.fallback.NonCullingFrustum;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.CelestialUniforms;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.class_1297;
import net.minecraft.class_1921;
import net.minecraft.class_2338;
import net.minecraft.class_2586;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4599;
import net.minecraft.class_4604;
import net.minecraft.class_638;
import net.minecraft.class_746;
import net.minecraft.class_761;
import net.minecraft.class_898;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ShadowRenderer {
    public static boolean ACTIVE = false;
    public static List<class_2586> visibleBlockEntities;
    public static int renderDistance;
    public static Matrix4f MODELVIEW;
    public static Matrix4f PROJECTION;
    public static class_4604 FRUSTUM;
    private final float halfPlaneLength;
    private final float nearPlane;
    private final float farPlane;
    private final float voxelDistance;
    private final float renderDistanceMultiplier;
    private final float entityShadowDistanceMultiplier;
    private final int resolution;
    private final float intervalSize;
    private final Float fov;
    private final ShadowRenderTargets targets;
    private final ShadowCullState packCullingState;
    private final ShadowCompositeRenderer compositeRenderer;
    private final boolean shouldRenderTerrain;
    private final boolean shouldRenderTranslucent;
    private final boolean shouldRenderEntities;
    private final boolean shouldRenderPlayer;
    private final boolean shouldRenderBlockEntities;
    private final boolean shouldRenderDH;
    private final float sunPathRotation;
    private final class_4599 buffers;
    private final RenderBuffersExt renderBuffersExt;
    private final List<MipmapPass> mipmapPasses = new ArrayList<MipmapPass>();
    private final String debugStringOverall;
    private final boolean separateHardwareSamplers;
    private final boolean shouldRenderLightBlockEntities;
    private final IrisRenderingPipeline pipeline;
    private boolean packHasVoxelization;
    private FrustumHolder terrainFrustumHolder;
    private FrustumHolder entityFrustumHolder;
    private String debugStringTerrain = "(unavailable)";
    private int renderedShadowEntities = 0;
    private int renderedShadowBlockEntities = 0;

    public ShadowRenderer(IrisRenderingPipeline pipeline, ProgramSource shadow, PackDirectives directives, ShadowRenderTargets shadowRenderTargets, ShadowCompositeRenderer compositeRenderer, CustomUniforms customUniforms, boolean separateHardwareSamplers) {
        this.pipeline = pipeline;
        this.separateHardwareSamplers = separateHardwareSamplers;
        PackShadowDirectives shadowDirectives = directives.getShadowDirectives();
        this.halfPlaneLength = shadowDirectives.getDistance();
        this.nearPlane = shadowDirectives.getNearPlane();
        this.farPlane = shadowDirectives.getFarPlane();
        this.voxelDistance = shadowDirectives.getVoxelDistance();
        this.renderDistanceMultiplier = shadowDirectives.getDistanceRenderMul();
        this.entityShadowDistanceMultiplier = shadowDirectives.getEntityShadowDistanceMul();
        this.resolution = shadowDirectives.getResolution();
        this.intervalSize = shadowDirectives.getIntervalSize();
        this.shouldRenderTerrain = shadowDirectives.shouldRenderTerrain();
        this.shouldRenderTranslucent = shadowDirectives.shouldRenderTranslucent();
        this.shouldRenderEntities = shadowDirectives.shouldRenderEntities();
        this.shouldRenderPlayer = shadowDirectives.shouldRenderPlayer();
        this.shouldRenderBlockEntities = shadowDirectives.shouldRenderBlockEntities();
        this.shouldRenderLightBlockEntities = shadowDirectives.shouldRenderLightBlockEntities();
        this.shouldRenderDH = shadowDirectives.isDhShadowEnabled().orElse(false);
        this.compositeRenderer = compositeRenderer;
        this.debugStringOverall = "half plane = " + this.halfPlaneLength + " meters @ " + this.resolution + "x" + this.resolution;
        this.terrainFrustumHolder = new FrustumHolder();
        this.entityFrustumHolder = new FrustumHolder();
        this.fov = shadowDirectives.getFov();
        this.targets = shadowRenderTargets;
        if (shadow != null) {
            this.packHasVoxelization = shadow.getGeometrySource().isPresent();
            this.packCullingState = shadowDirectives.getCullingState();
        } else {
            this.packHasVoxelization = false;
            this.packCullingState = ShadowCullState.DEFAULT;
        }
        this.sunPathRotation = directives.getSunPathRotation();
        int processors = Runtime.getRuntime().availableProcessors();
        this.buffers = new class_4599(processors);
        this.renderBuffersExt = this.buffers instanceof RenderBuffersExt ? (RenderBuffersExt)this.buffers : null;
        this.configureSamplingSettings(shadowDirectives);
    }

    public static class_4587 createShadowModelView(float sunPathRotation, float intervalSize, float nearPlane, float farPlane) {
        Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;
        class_4587 modelView = new class_4587();
        ShadowMatrices.createModelViewMatrix(modelView, ShadowRenderer.getShadowAngle(), intervalSize, sunPathRotation, cameraX, cameraY, cameraZ, nearPlane, farPlane);
        return modelView;
    }

    private static class_638 getLevel() {
        return Objects.requireNonNull(class_310.method_1551().field_1687);
    }

    private static float getSkyAngle() {
        return ShadowRenderer.getLevel().method_30274(CapturedRenderingState.INSTANCE.getTickDelta());
    }

    private static float getSunAngle() {
        float skyAngle = ShadowRenderer.getSkyAngle();
        if (skyAngle < 0.75f) {
            return skyAngle + 0.25f;
        }
        return skyAngle - 0.75f;
    }

    private static float getShadowAngle() {
        float shadowAngle = ShadowRenderer.getSunAngle();
        if (!CelestialUniforms.isDay()) {
            shadowAngle -= 0.5f;
        }
        return shadowAngle;
    }

    public void setUsesImages(boolean usesImages) {
        this.packHasVoxelization = this.packHasVoxelization || usesImages;
    }

    private void configureSamplingSettings(PackShadowDirectives shadowDirectives) {
        ImmutableList<PackShadowDirectives.DepthSamplingSettings> depthSamplingSettings = shadowDirectives.getDepthSamplingSettings();
        Int2ObjectMap<PackShadowDirectives.SamplingSettings> colorSamplingSettings = shadowDirectives.getColorSamplingSettings();
        RenderSystem.activeTexture((int)33988);
        this.configureDepthSampler(this.targets.getDepthTexture().getTextureId(), (PackShadowDirectives.DepthSamplingSettings)depthSamplingSettings.get(0));
        this.configureDepthSampler(this.targets.getDepthTextureNoTranslucents().getTextureId(), (PackShadowDirectives.DepthSamplingSettings)depthSamplingSettings.get(1));
        for (int i = 0; i < this.targets.getNumColorTextures(); ++i) {
            if (this.targets.get(i) == null) continue;
            int glTextureId = this.targets.get(i).getMainTexture();
            this.configureSampler(glTextureId, (PackShadowDirectives.SamplingSettings)colorSamplingSettings.computeIfAbsent(i, a -> new PackShadowDirectives.SamplingSettings()));
        }
        RenderSystem.activeTexture((int)33984);
    }

    private void configureDepthSampler(int glTextureId, PackShadowDirectives.DepthSamplingSettings settings) {
        if (settings.getHardwareFiltering() && !this.separateHardwareSamplers) {
            IrisRenderSystem.texParameteri(glTextureId, 3553, 34892, 34894);
        }
        IrisRenderSystem.texParameteriv(glTextureId, 3553, 36422, new int[]{6403, 6403, 6403, 1});
        this.configureSampler(glTextureId, settings);
    }

    private void configureSampler(int glTextureId, PackShadowDirectives.SamplingSettings settings) {
        if (settings.getMipmap()) {
            int filteringMode = settings.getNearest() ? 9984 : 9987;
            this.mipmapPasses.add(new MipmapPass(glTextureId, filteringMode));
        }
        if (!settings.getNearest()) {
            IrisRenderSystem.texParameteri(glTextureId, 3553, 10241, 9729);
            IrisRenderSystem.texParameteri(glTextureId, 3553, 10240, 9729);
        } else {
            IrisRenderSystem.texParameteri(glTextureId, 3553, 10241, 9728);
            IrisRenderSystem.texParameteri(glTextureId, 3553, 10240, 9728);
        }
    }

    private void generateMipmaps() {
        RenderSystem.activeTexture((int)33988);
        for (MipmapPass mipmapPass : this.mipmapPasses) {
            this.setupMipmappingForTexture(mipmapPass.texture(), mipmapPass.targetFilteringMode());
        }
        RenderSystem.activeTexture((int)33984);
    }

    private void setupMipmappingForTexture(int texture, int filteringMode) {
        IrisRenderSystem.generateMipmaps(texture, 3553);
        IrisRenderSystem.texParameteri(texture, 3553, 10241, filteringMode);
    }

    private FrustumHolder createShadowFrustum(float renderMultiplier, FrustumHolder holder) {
        String reason;
        double distance;
        if (this.packCullingState == ShadowCullState.DEFAULT && this.packHasVoxelization || this.packCullingState == ShadowCullState.DISTANCE) {
            distance = this.halfPlaneLength * renderMultiplier;
            reason = this.packCullingState == ShadowCullState.DISTANCE ? "(set by shader pack)" : "(voxelization detected)";
            if (distance <= 0.0 || distance > (double)(class_310.method_1551().field_1690.method_38521() * 16)) {
                String distanceInfo = "render distance = " + class_310.method_1551().field_1690.method_38521() * 16 + " blocks ";
                distanceInfo = distanceInfo + (class_310.method_1551().method_1542() ? "(capped by normal render distance)" : "(capped by normal/server render distance)");
                String cullingInfo = "disabled " + reason;
                return holder.setInfo(new NonCullingFrustum(), distanceInfo, cullingInfo);
            }
        } else {
            Object cullingInfo;
            BoxCuller boxCuller;
            String distanceInfo;
            boolean isReversed;
            boolean bl = isReversed = this.packCullingState == ShadowCullState.REVERSED;
            if (isReversed && renderMultiplier < 0.0f) {
                renderMultiplier = 1.0f;
            }
            double distance2 = (isReversed ? this.voxelDistance : this.halfPlaneLength) * renderMultiplier;
            String setter = "(set by shader pack)";
            if (renderMultiplier < 0.0f) {
                distance2 = IrisVideoSettings.shadowDistance * 16;
                setter = "(set by user)";
            }
            if (distance2 >= (double)(class_310.method_1551().field_1690.method_38521() * 16) && !isReversed) {
                distanceInfo = "render distance = " + class_310.method_1551().field_1690.method_38521() * 16 + " blocks ";
                distanceInfo = (String)distanceInfo + (class_310.method_1551().method_1542() ? "(capped by normal render distance)" : "(capped by normal/server render distance)");
                boxCuller = null;
            } else {
                distanceInfo = distance2 + " blocks " + setter;
                if (distance2 == 0.0 && !isReversed) {
                    cullingInfo = "no shadows rendered";
                    holder.setInfo(new CullEverythingFrustum(), distanceInfo, (String)cullingInfo);
                }
                boxCuller = new BoxCuller(distance2);
            }
            cullingInfo = (isReversed ? "Reversed" : "Advanced") + " Frustum Culling enabled";
            Vector4f shadowLightPosition = new CelestialUniforms(this.sunPathRotation).getShadowLightPositionInWorldSpace();
            Vector3f shadowLightVectorFromOrigin = new Vector3f(shadowLightPosition.x(), shadowLightPosition.y(), shadowLightPosition.z());
            shadowLightVectorFromOrigin.normalize();
            Matrix4f projView = (this.shouldRenderDH && DHCompat.hasRenderingEnabled() ? DHCompat.getProjection() : CapturedRenderingState.INSTANCE.getGbufferProjection()).mul(CapturedRenderingState.INSTANCE.getGbufferModelView(), new Matrix4f());
            if (isReversed) {
                return holder.setInfo(new ReversedAdvancedShadowCullingFrustum((Matrix4fc)projView, (Matrix4fc)PROJECTION, shadowLightVectorFromOrigin, boxCuller, new BoxCuller(this.halfPlaneLength * renderMultiplier)), distanceInfo, (String)cullingInfo);
            }
            return holder.setInfo(new AdvancedShadowCullingFrustum((Matrix4fc)projView, (Matrix4fc)PROJECTION, shadowLightVectorFromOrigin, boxCuller), distanceInfo, (String)cullingInfo);
        }
        String distanceInfo = distance + " blocks (set by shader pack)";
        String cullingInfo = "distance only " + reason;
        BoxCuller boxCuller = new BoxCuller(distance);
        holder.setInfo(new BoxCullingFrustum(boxCuller), distanceInfo, cullingInfo);
        return holder;
    }

    public void setupShadowViewport() {
        RenderSystem.viewport((int)0, (int)0, (int)this.resolution, (int)this.resolution);
    }

    public void renderShadows(LevelRendererAccessor levelRenderer, class_4184 playerCamera) {
        if (IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance) == 0) {
            return;
        }
        class_310 client = class_310.method_1551();
        levelRenderer.getLevel().method_16107().method_15405("shadows");
        ACTIVE = true;
        renderDistance = (int)(this.halfPlaneLength * this.renderDistanceMultiplier / 16.0f);
        if (this.renderDistanceMultiplier < 0.0f) {
            renderDistance = IrisVideoSettings.shadowDistance;
        }
        visibleBlockEntities = new ArrayList<class_2586>();
        class_4599 playerBuffers = levelRenderer.getRenderBuffers();
        levelRenderer.setRenderBuffers(this.buffers);
        visibleBlockEntities = new ArrayList<class_2586>();
        this.setupShadowViewport();
        class_4587 modelView = ShadowRenderer.createShadowModelView(this.sunPathRotation, this.intervalSize, this.nearPlane, this.farPlane);
        MODELVIEW = new Matrix4f((Matrix4fc)modelView.method_23760().method_23761());
        Matrix4f shadowProjection = this.fov != null ? ShadowMatrices.createPerspectiveMatrix(this.fov.floatValue()) : ShadowMatrices.createOrthoMatrix(this.halfPlaneLength, class_3532.method_15347((float)this.nearPlane, (float)-1.0f) ? (float)(-DHCompat.getRenderDistance() * 16) : this.nearPlane, class_3532.method_15347((float)this.farPlane, (float)-1.0f) ? (float)(DHCompat.getRenderDistance() * 16) : this.farPlane);
        IrisRenderSystem.setShadowProjection(shadowProjection);
        PROJECTION = shadowProjection;
        levelRenderer.getLevel().method_16107().method_15396("terrain_setup");
        if (levelRenderer instanceof CullingDataCache) {
            ((CullingDataCache)((Object)levelRenderer)).saveState();
        }
        levelRenderer.getLevel().method_16107().method_15396("initialize frustum");
        this.terrainFrustumHolder = this.createShadowFrustum(this.renderDistanceMultiplier, this.terrainFrustumHolder);
        FRUSTUM = this.terrainFrustumHolder.getFrustum();
        Vector3d cameraPos = CameraUniforms.getUnshiftedCameraPosition();
        double cameraX = cameraPos.x();
        double cameraY = cameraPos.y();
        double cameraZ = cameraPos.z();
        this.terrainFrustumHolder.getFrustum().method_23088(cameraX, cameraY, cameraZ);
        levelRenderer.getLevel().method_16107().method_15407();
        boolean wasChunkCullingEnabled = client.field_1730;
        client.field_1730 = false;
        boolean regenerateClouds = levelRenderer.shouldRegenerateClouds();
        ((class_761)levelRenderer).method_3292();
        levelRenderer.setShouldRegenerateClouds(regenerateClouds);
        levelRenderer.invokeSetupRender(playerCamera, this.terrainFrustumHolder.getFrustum(), false, false);
        client.field_1730 = wasChunkCullingEnabled;
        levelRenderer.getLevel().method_16107().method_15405("terrain");
        RenderSystem.disableCull();
        if (this.shouldRenderTerrain) {
            levelRenderer.invokeRenderSectionLayer(class_1921.method_23577(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
            levelRenderer.invokeRenderSectionLayer(class_1921.method_23581(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
            levelRenderer.invokeRenderSectionLayer(class_1921.method_23579(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
        }
        RenderSystem.viewport((int)0, (int)0, (int)this.resolution, (int)this.resolution);
        levelRenderer.getLevel().method_16107().method_15405("entities");
        float tickDelta = CapturedRenderingState.INSTANCE.getTickDelta();
        boolean hasEntityFrustum = false;
        if (this.entityShadowDistanceMultiplier == 1.0f || this.entityShadowDistanceMultiplier < 0.0f) {
            this.entityFrustumHolder.setInfo(this.terrainFrustumHolder.getFrustum(), this.terrainFrustumHolder.getDistanceInfo(), this.terrainFrustumHolder.getCullingInfo());
        } else {
            hasEntityFrustum = true;
            this.entityFrustumHolder = this.createShadowFrustum(this.renderDistanceMultiplier * this.entityShadowDistanceMultiplier, this.entityFrustumHolder);
        }
        class_4604 entityShadowFrustum = this.entityFrustumHolder.getFrustum();
        entityShadowFrustum.method_23088(cameraX, cameraY, cameraZ);
        if (this.renderBuffersExt != null) {
            this.renderBuffersExt.beginLevelRendering();
        }
        if (this.buffers instanceof DrawCallTrackingRenderBuffers) {
            ((DrawCallTrackingRenderBuffers)this.buffers).resetDrawCounts();
        }
        class_4597.class_4598 bufferSource = this.buffers.method_23000();
        class_898 dispatcher = levelRenderer.getEntityRenderDispatcher();
        if (this.shouldRenderEntities) {
            this.renderedShadowEntities = this.renderEntities(levelRenderer, dispatcher, bufferSource, modelView, tickDelta, entityShadowFrustum, cameraX, cameraY, cameraZ);
        } else if (this.shouldRenderPlayer) {
            this.renderedShadowEntities = this.renderPlayerEntity(levelRenderer, dispatcher, bufferSource, modelView, tickDelta, entityShadowFrustum, cameraX, cameraY, cameraZ);
        }
        levelRenderer.getLevel().method_16107().method_15405("build blockentities");
        if (this.shouldRenderBlockEntities) {
            this.renderedShadowBlockEntities = ShadowRenderingState.renderBlockEntities(this, this.buffers, modelView, playerCamera, cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum, false);
        } else if (this.shouldRenderLightBlockEntities) {
            this.renderedShadowBlockEntities = ShadowRenderingState.renderBlockEntities(this, this.buffers, modelView, playerCamera, cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum, true);
        }
        levelRenderer.getLevel().method_16107().method_15405("draw entities");
        if (bufferSource instanceof FullyBufferedMultiBufferSource) {
            FullyBufferedMultiBufferSource fullyBufferedMultiBufferSource = (FullyBufferedMultiBufferSource)bufferSource;
            fullyBufferedMultiBufferSource.readyUp();
        }
        bufferSource.method_22993();
        this.copyPreTranslucentDepth(levelRenderer);
        levelRenderer.getLevel().method_16107().method_15405("translucent terrain");
        if (this.shouldRenderTranslucent) {
            levelRenderer.invokeRenderSectionLayer(class_1921.method_23583(), cameraX, cameraY, cameraZ, MODELVIEW, shadowProjection);
        }
        if (this.renderBuffersExt != null) {
            this.renderBuffersExt.endLevelRendering();
        }
        IrisRenderSystem.restorePlayerProjection();
        this.debugStringTerrain = ((class_761)levelRenderer).method_3289();
        levelRenderer.getLevel().method_16107().method_15405("generate mipmaps");
        this.generateMipmaps();
        levelRenderer.getLevel().method_16107().method_15405("restore gl state");
        RenderSystem.enableCull();
        class_310.method_1551().method_1522().method_1235(false);
        RenderSystem.viewport((int)0, (int)0, (int)client.method_1522().field_1482, (int)client.method_1522().field_1481);
        if (levelRenderer instanceof CullingDataCache) {
            ((CullingDataCache)((Object)levelRenderer)).restoreState();
        }
        this.pipeline.removePhaseIfNeeded();
        GLDebug.pushGroup(901, "shadowcomp");
        this.compositeRenderer.renderAll();
        GLDebug.popGroup();
        levelRenderer.setRenderBuffers(playerBuffers);
        visibleBlockEntities = null;
        ACTIVE = false;
        levelRenderer.getLevel().method_16107().method_15407();
        levelRenderer.getLevel().method_16107().method_15405("updatechunks");
    }

    public int renderBlockEntities(class_4599 bufferSource, class_4587 modelView, class_4184 camera, double cameraX, double cameraY, double cameraZ, float tickDelta, boolean hasEntityFrustum, boolean lightsOnly) {
        ShadowRenderer.getLevel().method_16107().method_15396("build blockentities");
        int shadowBlockEntities = 0;
        BoxCuller culler = null;
        if (hasEntityFrustum) {
            culler = new BoxCuller(this.halfPlaneLength * (this.renderDistanceMultiplier * this.entityShadowDistanceMultiplier));
            culler.setPosition(cameraX, cameraY, cameraZ);
        }
        for (class_2586 entity : visibleBlockEntities) {
            if (lightsOnly && entity.method_11010().method_26213() == 0) continue;
            class_2338 pos = entity.method_11016();
            if (hasEntityFrustum && culler.isCulled(pos.method_10263() - 1, pos.method_10264() - 1, pos.method_10260() - 1, pos.method_10263() + 1, pos.method_10264() + 1, pos.method_10260() + 1)) continue;
            modelView.method_22903();
            modelView.method_22904((double)pos.method_10263() - cameraX, (double)pos.method_10264() - cameraY, (double)pos.method_10260() - cameraZ);
            class_310.method_1551().method_31975().method_3555(entity, tickDelta, modelView, (class_4597)this.buffers.method_23000());
            modelView.method_22909();
            ++shadowBlockEntities;
        }
        ShadowRenderer.getLevel().method_16107().method_15407();
        return shadowBlockEntities;
    }

    private int renderEntities(LevelRendererAccessor levelRenderer, class_898 dispatcher, class_4597.class_4598 bufferSource, class_4587 modelView, float tickDelta, class_4604 frustum, double cameraX, double cameraY, double cameraZ) {
        levelRenderer.getLevel().method_16107().method_15396("cull");
        ArrayList<class_1297> renderedEntities = new ArrayList<class_1297>(32);
        for (class_1297 entity2 : ShadowRenderer.getLevel().method_18112()) {
            if (!dispatcher.method_3950(entity2, frustum, cameraX, cameraY, cameraZ) || entity2.method_7325()) continue;
            renderedEntities.add(entity2);
        }
        levelRenderer.getLevel().method_16107().method_15405("sort");
        renderedEntities.sort(Comparator.comparingInt(entity -> entity.method_5864().hashCode()));
        levelRenderer.getLevel().method_16107().method_15405("build entity geometry");
        for (class_1297 entity2 : renderedEntities) {
            float realTickDelta = class_310.method_1551().field_1687.method_54719().method_54746(entity2) ? tickDelta : CapturedRenderingState.INSTANCE.getRealTickDelta();
            levelRenderer.invokeRenderEntity(entity2, cameraX, cameraY, cameraZ, realTickDelta, modelView, (class_4597)bufferSource);
        }
        levelRenderer.getLevel().method_16107().method_15407();
        return renderedEntities.size();
    }

    private int renderPlayerEntity(LevelRendererAccessor levelRenderer, class_898 dispatcher, class_4597.class_4598 bufferSource, class_4587 modelView, float tickDelta, class_4604 frustum, double cameraX, double cameraY, double cameraZ) {
        levelRenderer.getLevel().method_16107().method_15396("cull");
        class_746 player = class_310.method_1551().field_1724;
        int shadowEntities = 0;
        if (!dispatcher.method_3950((class_1297)player, frustum, cameraX, cameraY, cameraZ) || player.method_7325()) {
            levelRenderer.getLevel().method_16107().method_15407();
            return 0;
        }
        levelRenderer.getLevel().method_16107().method_15405("build geometry");
        if (!player.method_5685().isEmpty()) {
            for (int i = 0; i < player.method_5685().size(); ++i) {
                float realTickDelta = class_310.method_1551().field_1687.method_54719().method_54746((class_1297)player.method_5685().get(i)) ? tickDelta : CapturedRenderingState.INSTANCE.getRealTickDelta();
                levelRenderer.invokeRenderEntity((class_1297)player.method_5685().get(i), cameraX, cameraY, cameraZ, realTickDelta, modelView, (class_4597)bufferSource);
                ++shadowEntities;
            }
        }
        if (player.method_5854() != null) {
            float realTickDelta = class_310.method_1551().field_1687.method_54719().method_54746(player.method_5854()) ? tickDelta : CapturedRenderingState.INSTANCE.getRealTickDelta();
            levelRenderer.invokeRenderEntity(player.method_5854(), cameraX, cameraY, cameraZ, realTickDelta, modelView, (class_4597)bufferSource);
            ++shadowEntities;
        }
        float realTickDelta = class_310.method_1551().field_1687.method_54719().method_54746((class_1297)player) ? tickDelta : CapturedRenderingState.INSTANCE.getRealTickDelta();
        levelRenderer.invokeRenderEntity((class_1297)player, cameraX, cameraY, cameraZ, realTickDelta, modelView, (class_4597)bufferSource);
        levelRenderer.getLevel().method_16107().method_15407();
        return ++shadowEntities;
    }

    private void copyPreTranslucentDepth(LevelRendererAccessor levelRenderer) {
        levelRenderer.getLevel().method_16107().method_15405("translucent depth copy");
        this.targets.copyPreTranslucentDepth();
    }

    public void addDebugText(List<String> messages) {
        if (IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance) == 0) {
            messages.add("[Iris] Shadow Maps: off, shadow distance 0");
            return;
        }
        if (Iris.getIrisConfig().areDebugOptionsEnabled()) {
            messages.add("[Iris] Shadow Maps: " + this.debugStringOverall);
            messages.add("[Iris] Shadow Distance Terrain: " + this.terrainFrustumHolder.getDistanceInfo() + " Entity: " + this.entityFrustumHolder.getDistanceInfo());
            messages.add("[Iris] Shadow Culling Terrain: " + this.terrainFrustumHolder.getCullingInfo() + " Entity: " + this.entityFrustumHolder.getCullingInfo());
            messages.add("[Iris] Shadow Projection: " + this.getProjectionInfo());
            messages.add("[Iris] Shadow Terrain: " + this.debugStringTerrain + (this.shouldRenderTerrain ? "" : " (no terrain) ") + (this.shouldRenderTranslucent ? "" : "(no translucent)"));
            messages.add("[Iris] Shadow Entities: " + this.getEntitiesDebugString());
            messages.add("[Iris] Shadow Block Entities: " + this.getBlockEntitiesDebugString());
            class_4599 class_45992 = this.buffers;
            if (class_45992 instanceof DrawCallTrackingRenderBuffers) {
                DrawCallTrackingRenderBuffers drawCallTracker = (DrawCallTrackingRenderBuffers)class_45992;
                if (this.shouldRenderEntities || this.shouldRenderPlayer) {
                    messages.add("[Iris] Shadow Entity Batching: " + BatchingDebugMessageHelper.getDebugMessage(drawCallTracker));
                }
            }
        } else {
            messages.add("[Iris] Shadow info: " + this.debugStringTerrain);
            messages.add("[Iris] E: " + this.renderedShadowEntities);
            messages.add("[Iris] BE: " + this.renderedShadowBlockEntities);
        }
    }

    private String getProjectionInfo() {
        return "Near: " + this.nearPlane + " Far: " + this.farPlane + " distance " + this.halfPlaneLength;
    }

    private String getEntitiesDebugString() {
        return this.shouldRenderEntities || this.shouldRenderPlayer ? this.renderedShadowEntities + "/" + class_310.method_1551().field_1687.method_18120() : "disabled by pack";
    }

    private String getBlockEntitiesDebugString() {
        return this.shouldRenderBlockEntities || this.shouldRenderLightBlockEntities ? "" + this.renderedShadowBlockEntities : "disabled by pack";
    }

    public void destroy() {
        ((MemoryTrackingRenderBuffers)this.buffers).freeAndDeleteBuffers();
    }

    private record MipmapPass(int texture, int targetFilteringMode) {
    }
}

