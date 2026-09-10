/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableSet
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.systems.RenderSystem
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMaps
 *  net.minecraft.class_1043
 *  net.minecraft.class_1044
 *  net.minecraft.class_276
 *  net.minecraft.class_293
 *  net.minecraft.class_310
 *  net.minecraft.class_4184
 *  net.minecraft.class_5294$class_5401
 *  net.minecraft.class_5944
 *  net.minecraft.class_757
 *  org.apache.commons.lang3.StringUtils
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector4f
 *  org.lwjgl.opengl.ARBClearTexture
 */
package net.irisshaders.iris.pipeline;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.BlendModeOverride;
import net.irisshaders.iris.gl.buffer.ShaderStorageBufferHolder;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.gl.image.GlImage;
import net.irisshaders.iris.gl.image.ImageHolder;
import net.irisshaders.iris.gl.program.ComputeProgram;
import net.irisshaders.iris.gl.program.ProgramBuilder;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.sampler.SamplerHolder;
import net.irisshaders.iris.gl.sampler.SamplerLimits;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.ShaderAttributeInputs;
import net.irisshaders.iris.gl.texture.DepthBufferFormat;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.helpers.FakeChainedJsonException;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.helpers.Tri;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pathways.CenterDepthSampler;
import net.irisshaders.iris.pathways.HorizonRenderer;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.pathways.colorspace.ColorSpaceConverter;
import net.irisshaders.iris.pathways.colorspace.ColorSpaceFragmentConverter;
import net.irisshaders.iris.pbr.TextureInfoCache;
import net.irisshaders.iris.pbr.format.TextureFormat;
import net.irisshaders.iris.pbr.format.TextureFormatLoader;
import net.irisshaders.iris.pbr.texture.PBRTextureHolder;
import net.irisshaders.iris.pbr.texture.PBRTextureManager;
import net.irisshaders.iris.pbr.texture.PBRType;
import net.irisshaders.iris.pipeline.CompositePass;
import net.irisshaders.iris.pipeline.CompositeRenderer;
import net.irisshaders.iris.pipeline.CustomTextureManager;
import net.irisshaders.iris.pipeline.FinalPassRenderer;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.irisshaders.iris.pipeline.programs.FallbackShader;
import net.irisshaders.iris.pipeline.programs.ShaderCreator;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.pipeline.programs.ShaderMap;
import net.irisshaders.iris.pipeline.programs.SodiumPrograms;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.samplers.IrisImages;
import net.irisshaders.iris.samplers.IrisSamplers;
import net.irisshaders.iris.shaderpack.FilledIndirectPointer;
import net.irisshaders.iris.shaderpack.ImageInformation;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.loading.ProgramArrayId;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.shaderpack.programs.ComputeSource;
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shaderpack.properties.CloudSetting;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.properties.PackShadowDirectives;
import net.irisshaders.iris.shaderpack.properties.ParticleRenderingSettings;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.shadows.ShadowCompositeRenderer;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.targets.Blaze3dRenderTargetExt;
import net.irisshaders.iris.targets.BufferFlipper;
import net.irisshaders.iris.targets.ClearPass;
import net.irisshaders.iris.targets.ClearPassCreator;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.targets.backed.NativeImageBackedSingleColorTexture;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.CommonUniforms;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_276;
import net.minecraft.class_293;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_5294;
import net.minecraft.class_5944;
import net.minecraft.class_757;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.lwjgl.opengl.ARBClearTexture;

public class IrisRenderingPipeline
implements WorldRenderingPipeline,
ShaderRenderingPipeline {
    private final RenderTargets renderTargets;
    private final ShaderMap shaderMap;
    private final CustomUniforms customUniforms;
    private final ShadowCompositeRenderer shadowCompositeRenderer;
    private final Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> customTextureMap;
    private final ComputeProgram[] setup;
    private final boolean separateHardwareSamplers;
    private final ProgramFallbackResolver resolver;
    private final Supplier<ShadowRenderTargets> shadowTargetsSupplier;
    private final Set<class_5944> loadedShaders;
    private final CompositeRenderer beginRenderer;
    private final CompositeRenderer prepareRenderer;
    private final CompositeRenderer deferredRenderer;
    private final CompositeRenderer compositeRenderer;
    private final FinalPassRenderer finalPassRenderer;
    private final CustomTextureManager customTextureManager;
    private final class_1043 whitePixel;
    private final FrameUpdateNotifier updateNotifier;
    private final CenterDepthSampler centerDepthSampler;
    private final ColorSpaceConverter colorSpaceConverter;
    private final ImmutableSet<Integer> flippedBeforeShadow;
    private final ImmutableSet<Integer> flippedAfterPrepare;
    private final ImmutableSet<Integer> flippedAfterTranslucent;
    private final HorizonRenderer horizonRenderer = new HorizonRenderer();
    @Nullable
    private final ComputeProgram[] shadowComputes;
    private final float sunPathRotation;
    private final boolean shouldRenderUnderwaterOverlay;
    private final boolean shouldRenderVignette;
    private final boolean shouldWriteRainAndSnowToDepthBuffer;
    private final boolean oldLighting;
    private final OptionalInt forcedShadowRenderDistanceChunks;
    private final boolean frustumCulling;
    private final boolean occlusionCulling;
    private final CloudSetting cloudSetting;
    private final boolean shouldRenderSun;
    private final boolean shouldRenderWeather;
    private final boolean shouldRenderWeatherParticles;
    private final boolean shouldRenderMoon;
    private final boolean shouldRenderStars;
    private final boolean shouldRenderSkyDisc;
    private final boolean allowConcurrentCompute;
    @Nullable
    private final ShadowRenderer shadowRenderer;
    private final int shadowMapResolution;
    private final ParticleRenderingSettings particleRenderingSettings;
    private final PackDirectives packDirectives;
    private final Set<GlImage> customImages;
    private final GlImage[] clearImages;
    private final ShaderPack pack;
    private final PackShadowDirectives shadowDirectives;
    private final DHCompat dhCompat;
    private final int stackSize = 0;
    private final boolean skipAllRendering;
    private final CloudSetting dhCloudSetting;
    private final SodiumPrograms sodiumPrograms;
    public boolean isBeforeTranslucent;
    private boolean initializedBlockIds;
    private ShaderStorageBufferHolder shaderStorageBufferHolder;
    private ShadowRenderTargets shadowRenderTargets;
    private WorldRenderingPhase overridePhase = null;
    private WorldRenderingPhase phase = WorldRenderingPhase.NONE;
    private ImmutableList<ClearPass> clearPassesFull;
    private ImmutableList<ClearPass> clearPasses;
    private ImmutableList<ClearPass> shadowClearPasses;
    private ImmutableList<ClearPass> shadowClearPassesFull;
    private boolean destroyed = false;
    private boolean isRenderingWorld;
    private boolean isMainBound;
    private boolean shouldBindPBR;
    private int currentNormalTexture;
    private int currentSpecularTexture;
    private ColorSpace currentColorSpace;
    private GlFramebuffer defaultFB;
    private GlFramebuffer defaultFBAlt;
    private GlFramebuffer defaultFBShadow;
    private boolean shouldRemovePhase = false;

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public IrisRenderingPipeline(ProgramSet programSet) {
        ShaderPrinter.resetPrintState();
        this.shouldRenderUnderwaterOverlay = programSet.getPackDirectives().underwaterOverlay();
        this.shouldRenderVignette = programSet.getPackDirectives().vignette();
        this.shouldWriteRainAndSnowToDepthBuffer = programSet.getPackDirectives().rainDepth();
        this.oldLighting = programSet.getPackDirectives().isOldLighting();
        this.updateNotifier = new FrameUpdateNotifier();
        this.packDirectives = programSet.getPackDirectives();
        this.customTextureMap = programSet.getPackDirectives().getTextureMap();
        this.separateHardwareSamplers = programSet.getPack().hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS);
        this.shadowDirectives = this.packDirectives.getShadowDirectives();
        this.cloudSetting = programSet.getPackDirectives().getCloudSetting();
        this.dhCloudSetting = programSet.getPackDirectives().getDHCloudSetting();
        this.shouldRenderSun = programSet.getPackDirectives().shouldRenderSun();
        this.shouldRenderWeather = programSet.getPackDirectives().shouldRenderWeather();
        this.shouldRenderWeatherParticles = programSet.getPackDirectives().shouldRenderWeatherParticles();
        this.shouldRenderMoon = programSet.getPackDirectives().shouldRenderMoon();
        this.shouldRenderStars = programSet.getPackDirectives().shouldRenderStars();
        this.shouldRenderSkyDisc = programSet.getPackDirectives().shouldRenderSkyDisc();
        this.allowConcurrentCompute = programSet.getPackDirectives().getConcurrentCompute();
        this.skipAllRendering = programSet.getPackDirectives().skipAllRendering();
        this.frustumCulling = programSet.getPackDirectives().shouldUseFrustumCulling();
        this.occlusionCulling = programSet.getPackDirectives().shouldUseOcclusionCulling();
        this.resolver = new ProgramFallbackResolver(programSet);
        this.pack = programSet.getPack();
        class_276 main = class_310.method_1551().method_1522();
        int depthTextureId = main.method_30278();
        int internalFormat = TextureInfoCache.INSTANCE.getInfo(depthTextureId).getInternalFormat();
        DepthBufferFormat depthBufferFormat = DepthBufferFormat.fromGlEnumOrDefault(internalFormat);
        if (!this.pack.getBufferObjects().isEmpty()) {
            if (!IrisRenderSystem.supportsSSBO()) throw new IllegalStateException("Shader storage buffers/immutable buffer storage is not supported on this graphics card, however the shaderpack requested them? This shouldn't be possible.");
            this.shaderStorageBufferHolder = new ShaderStorageBufferHolder(this.pack.getBufferObjects(), main.field_1482, main.field_1481);
            this.shaderStorageBufferHolder.setupBuffers();
        } else {
            for (int i = 0; i < Math.min(16, SamplerLimits.get().getMaxShaderStorageUnits()); ++i) {
                IrisRenderSystem.bindBufferBase(37074, i, 0);
            }
        }
        this.customImages = new HashSet<GlImage>();
        for (ImageInformation information : programSet.getPack().getIrisCustomImages()) {
            if (information.isRelative()) {
                this.customImages.add(new GlImage.Relative(information.name(), information.samplerName(), information.format(), information.internalTextureFormat(), information.type(), information.clear(), information.relativeWidth(), information.relativeHeight(), main.field_1482, main.field_1481));
                continue;
            }
            this.customImages.add(new GlImage(information.name(), information.samplerName(), information.target(), information.format(), information.internalTextureFormat(), information.type(), information.clear(), information.width(), information.height(), information.depth()));
        }
        this.clearImages = (GlImage[])this.customImages.stream().filter(GlImage::shouldClear).toArray(GlImage[]::new);
        this.particleRenderingSettings = programSet.getPackDirectives().getParticleRenderingSettings() != ParticleRenderingSettings.UNSET ? programSet.getPackDirectives().getParticleRenderingSettings() : (programSet.getComposite(ProgramArrayId.Deferred).length > 0 && !programSet.getPackDirectives().shouldUseSeparateEntityDraws() ? ParticleRenderingSettings.AFTER : ParticleRenderingSettings.MIXED);
        this.renderTargets = new RenderTargets(main.field_1482, main.field_1481, depthTextureId, ((Blaze3dRenderTargetExt)main).iris$getDepthBufferVersion(), depthBufferFormat, programSet.getPackDirectives().getRenderTargetDirectives().getRenderTargetSettings(), programSet.getPackDirectives());
        this.sunPathRotation = programSet.getPackDirectives().getSunPathRotation();
        PackShadowDirectives shadowDirectives = programSet.getPackDirectives().getShadowDirectives();
        this.forcedShadowRenderDistanceChunks = shadowDirectives.isDistanceRenderMulExplicit() ? ((double)shadowDirectives.getDistanceRenderMul() >= 0.0 ? OptionalInt.of(((int)(shadowDirectives.getDistance() * shadowDirectives.getDistanceRenderMul()) + 15) / 16) : OptionalInt.of(-1)) : OptionalInt.empty();
        this.customUniforms = programSet.getPack().customUniforms.build(holder -> CommonUniforms.addNonDynamicUniforms(holder, programSet.getPack().getIdMap(), programSet.getPackDirectives(), this.updateNotifier));
        GlStateManager._activeTexture((int)33986);
        this.customTextureManager = new CustomTextureManager(programSet.getPackDirectives(), programSet.getPack().getCustomTextureDataMap(), programSet.getPack().getIrisCustomTextureDataMap(), programSet.getPack().getCustomNoiseTexture());
        this.whitePixel = new NativeImageBackedSingleColorTexture(255, 255, 255, 255);
        GlStateManager._activeTexture((int)33984);
        BufferFlipper flipper = new BufferFlipper();
        this.centerDepthSampler = new CenterDepthSampler(() -> this.renderTargets.getDepthTexture(), programSet.getPackDirectives().getCenterDepthHalfLife());
        this.shadowMapResolution = programSet.getPackDirectives().getShadowDirectives().getResolution();
        this.shadowTargetsSupplier = () -> {
            if (this.shadowRenderTargets == null) {
                this.shadowRenderTargets = new ShadowRenderTargets(this, this.shadowMapResolution, shadowDirectives);
            }
            return this.shadowRenderTargets;
        };
        if (shadowDirectives.isShadowEnabled() == OptionalBoolean.TRUE) {
            this.shadowTargetsSupplier.get();
        }
        this.shadowComputes = this.createShadowComputes(programSet.getShadowCompute(), programSet);
        this.beginRenderer = new CompositeRenderer(this, CompositePass.BEGIN, programSet.getPackDirectives(), programSet.getComposite(ProgramArrayId.Begin), programSet.getCompute(ProgramArrayId.Begin), this.renderTargets, this.shaderStorageBufferHolder, this.customTextureManager.getNoiseTexture(), this.updateNotifier, this.centerDepthSampler, flipper, this.shadowTargetsSupplier, TextureStage.BEGIN, this.customTextureManager.getCustomTextureIdMap().getOrDefault((Object)TextureStage.BEGIN, (Object2ObjectMap<String, TextureAccess>)Object2ObjectMaps.emptyMap()), this.customTextureManager.getIrisCustomTextures(), this.customImages, programSet.getPackDirectives().getExplicitFlips("begin_pre"), this.customUniforms);
        this.flippedBeforeShadow = flipper.snapshot();
        this.prepareRenderer = new CompositeRenderer(this, CompositePass.PREPARE, programSet.getPackDirectives(), programSet.getComposite(ProgramArrayId.Prepare), programSet.getCompute(ProgramArrayId.Prepare), this.renderTargets, this.shaderStorageBufferHolder, this.customTextureManager.getNoiseTexture(), this.updateNotifier, this.centerDepthSampler, flipper, this.shadowTargetsSupplier, TextureStage.PREPARE, this.customTextureManager.getCustomTextureIdMap().getOrDefault((Object)TextureStage.PREPARE, (Object2ObjectMap<String, TextureAccess>)Object2ObjectMaps.emptyMap()), this.customTextureManager.getIrisCustomTextures(), this.customImages, programSet.getPackDirectives().getExplicitFlips("prepare_pre"), this.customUniforms);
        this.flippedAfterPrepare = flipper.snapshot();
        this.deferredRenderer = new CompositeRenderer(this, CompositePass.DEFERRED, programSet.getPackDirectives(), programSet.getComposite(ProgramArrayId.Deferred), programSet.getCompute(ProgramArrayId.Deferred), this.renderTargets, this.shaderStorageBufferHolder, this.customTextureManager.getNoiseTexture(), this.updateNotifier, this.centerDepthSampler, flipper, this.shadowTargetsSupplier, TextureStage.DEFERRED, this.customTextureManager.getCustomTextureIdMap().getOrDefault((Object)TextureStage.DEFERRED, (Object2ObjectMap<String, TextureAccess>)Object2ObjectMaps.emptyMap()), this.customTextureManager.getIrisCustomTextures(), this.customImages, programSet.getPackDirectives().getExplicitFlips("deferred_pre"), this.customUniforms);
        this.flippedAfterTranslucent = flipper.snapshot();
        this.compositeRenderer = new CompositeRenderer(this, CompositePass.COMPOSITE, programSet.getPackDirectives(), programSet.getComposite(ProgramArrayId.Composite), programSet.getCompute(ProgramArrayId.Composite), this.renderTargets, this.shaderStorageBufferHolder, this.customTextureManager.getNoiseTexture(), this.updateNotifier, this.centerDepthSampler, flipper, this.shadowTargetsSupplier, TextureStage.COMPOSITE_AND_FINAL, this.customTextureManager.getCustomTextureIdMap().getOrDefault((Object)TextureStage.COMPOSITE_AND_FINAL, (Object2ObjectMap<String, TextureAccess>)Object2ObjectMaps.emptyMap()), this.customTextureManager.getIrisCustomTextures(), this.customImages, programSet.getPackDirectives().getExplicitFlips("composite_pre"), this.customUniforms);
        this.finalPassRenderer = new FinalPassRenderer(this, programSet, this.renderTargets, this.customTextureManager.getNoiseTexture(), this.shaderStorageBufferHolder, this.updateNotifier, flipper.snapshot(), this.centerDepthSampler, this.shadowTargetsSupplier, this.customTextureManager.getCustomTextureIdMap().getOrDefault((Object)TextureStage.COMPOSITE_AND_FINAL, (Object2ObjectMap<String, TextureAccess>)Object2ObjectMaps.emptyMap()), this.customTextureManager.getIrisCustomTextures(), this.customImages, this.compositeRenderer.getFlippedAtLeastOnceFinal(), this.customUniforms);
        Supplier<ImmutableSet> flipped = () -> this.isBeforeTranslucent ? this.flippedAfterPrepare : this.flippedAfterTranslucent;
        IntFunction<ProgramSamplers> createTerrainSamplers = programId -> {
            ProgramSamplers.Builder builder = ProgramSamplers.builder(programId, IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS);
            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(builder, this.customTextureManager.getCustomTextureIdMap().getOrDefault((Object)TextureStage.GBUFFERS_AND_SHADOW, (Object2ObjectMap<String, TextureAccess>)Object2ObjectMaps.emptyMap()));
            IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, flipped, this.renderTargets, false, this);
            IrisSamplers.addCustomTextures(builder, this.customTextureManager.getIrisCustomTextures());
            if (!this.shouldBindPBR) {
                this.shouldBindPBR = IrisSamplers.hasPBRSamplers(customTextureSamplerInterceptor);
            }
            IrisSamplers.addLevelSamplers(customTextureSamplerInterceptor, this, (class_1044)this.whitePixel, true, true, false);
            IrisSamplers.addWorldDepthSamplers(customTextureSamplerInterceptor, this.renderTargets);
            IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, this.customTextureManager.getNoiseTexture());
            IrisSamplers.addCustomImages(customTextureSamplerInterceptor, this.customImages);
            if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor)) {
                IrisSamplers.addShadowSamplers(customTextureSamplerInterceptor, Objects.requireNonNull(this.shadowRenderTargets), null, this.separateHardwareSamplers);
            }
            return builder.build();
        };
        IntFunction<ProgramImages> createTerrainImages = programId -> {
            ProgramImages.Builder builder = ProgramImages.builder(programId);
            IrisImages.addRenderTargetImages(builder, flipped, this.renderTargets);
            IrisImages.addCustomImages(builder, this.customImages);
            if (IrisImages.hasShadowImages(builder)) {
                IrisImages.addShadowColorImages(builder, Objects.requireNonNull(this.shadowRenderTargets), null);
            }
            return builder.build();
        };
        this.dhCompat = new DHCompat(this, shadowDirectives.isDhShadowEnabled().orElse(true));
        this.loadedShaders = new HashSet<class_5944>();
        this.shaderMap = new ShaderMap(key -> {
            try {
                if (key.isShadow()) {
                    if (this.shadowRenderTargets != null) {
                        return this.createShadowShader(key.getName(), this.resolver.resolve(key.getProgram()), (ShaderKey)((Object)key));
                    }
                    return null;
                }
                return this.createShader(key.getName(), this.resolver.resolve(key.getProgram()), (ShaderKey)((Object)key));
            }
            catch (FakeChainedJsonException e) {
                this.destroyShaders();
                throw e.getTrueException();
            }
            catch (IOException e) {
                this.destroyShaders();
                throw new RuntimeException(e);
            }
            catch (RuntimeException e) {
                this.destroyShaders();
                throw e;
            }
        });
        this.initializedBlockIds = false;
        WorldRenderingSettings.INSTANCE.setEntityIds(programSet.getPack().getIdMap().getEntityIdMap());
        WorldRenderingSettings.INSTANCE.setItemIds(programSet.getPack().getIdMap().getItemIdMap());
        WorldRenderingSettings.INSTANCE.setAmbientOcclusionLevel(programSet.getPackDirectives().getAmbientOcclusionLevel());
        WorldRenderingSettings.INSTANCE.setDisableDirectionalShading(this.shouldDisableDirectionalShading());
        WorldRenderingSettings.INSTANCE.setUseSeparateAo(programSet.getPackDirectives().shouldUseSeparateAo());
        WorldRenderingSettings.INSTANCE.setVoxelizeLightBlocks(programSet.getPackDirectives().shouldVoxelizeLightBlocks());
        WorldRenderingSettings.INSTANCE.setSeparateEntityDraws(programSet.getPackDirectives().shouldUseSeparateEntityDraws());
        if (this.shadowRenderTargets != null) {
            class_5944 shader = this.shaderMap.getShader(ShaderKey.SHADOW_TERRAIN_CUTOUT);
            boolean shadowUsesImages = false;
            if (shader instanceof ExtendedShader) {
                ExtendedShader shader2 = (ExtendedShader)shader;
                shadowUsesImages = shader2.hasActiveImages();
            }
            this.shadowClearPasses = ClearPassCreator.createShadowClearPasses(this.shadowRenderTargets, false, shadowDirectives);
            this.shadowClearPassesFull = ClearPassCreator.createShadowClearPasses(this.shadowRenderTargets, true, shadowDirectives);
            this.shadowCompositeRenderer = new ShadowCompositeRenderer(this, programSet.getPackDirectives(), programSet.getComposite(ProgramArrayId.ShadowComposite), programSet.getCompute(ProgramArrayId.ShadowComposite), this.shadowRenderTargets, this.shaderStorageBufferHolder, this.customTextureManager.getNoiseTexture(), this.updateNotifier, this.customTextureManager.getCustomTextureIdMap(TextureStage.SHADOWCOMP), this.customImages, programSet.getPackDirectives().getExplicitFlips("shadowcomp_pre"), this.customTextureManager.getIrisCustomTextures(), this.customUniforms);
            this.shadowRenderer = programSet.getPackDirectives().getShadowDirectives().isShadowEnabled().orElse(true) ? new ShadowRenderer(this, this.resolver.resolveNullable(ProgramId.ShadowSolid), programSet.getPackDirectives(), this.shadowRenderTargets, this.shadowCompositeRenderer, this.customUniforms, programSet.getPack().hasFeature(FeatureFlags.SEPARATE_HARDWARE_SAMPLERS)) : null;
            this.defaultFBShadow = this.shadowRenderTargets.createFramebufferWritingToMain(new int[]{0});
        } else {
            this.shadowClearPasses = ImmutableList.of();
            this.shadowClearPassesFull = ImmutableList.of();
            this.shadowCompositeRenderer = null;
            this.shadowRenderer = null;
        }
        this.sodiumPrograms = new SodiumPrograms(this, programSet, this.resolver, this.renderTargets, this.shadowTargetsSupplier, this.customUniforms);
        this.setup = this.createSetupComputes(programSet.getSetup(), programSet, TextureStage.SETUP);
        this.customUniforms.optimise();
        boolean hasRun = false;
        this.clearPassesFull = ClearPassCreator.createClearPasses(this.renderTargets, true, programSet.getPackDirectives().getRenderTargetDirectives());
        this.clearPasses = ClearPassCreator.createClearPasses(this.renderTargets, false, programSet.getPackDirectives().getRenderTargetDirectives());
        for (ComputeProgram program : this.setup) {
            if (program == null) continue;
            if (!hasRun) {
                hasRun = true;
                this.renderTargets.onFullClear();
                Vector3d fogColor3 = CapturedRenderingState.INSTANCE.getFogColor();
                Vector4f fogColor = new Vector4f((float)fogColor3.x, (float)fogColor3.y, (float)fogColor3.z, 1.0f);
                this.clearPassesFull.forEach(clearPass -> clearPass.execute(fogColor));
            }
            program.use();
            program.dispatch(1.0f, 1.0f);
        }
        if (hasRun) {
            ComputeProgram.unbind();
        }
        this.colorSpaceConverter = programSet.getPackDirectives().supportsColorCorrection() ? new ColorSpaceConverter(this){

            @Override
            public void rebuildProgram(int width, int height, ColorSpace colorSpace) {
            }

            @Override
            public void process(int target) {
            }
        } : new ColorSpaceFragmentConverter(main.field_1482, main.field_1481, IrisVideoSettings.colorSpace);
        this.currentColorSpace = IrisVideoSettings.colorSpace;
        int defaultTex = this.packDirectives.getFallbackTex();
        this.defaultFB = this.flippedAfterPrepare.contains((Object)defaultTex) ? this.renderTargets.createFramebufferWritingToAlt(new int[]{defaultTex}) : this.renderTargets.createFramebufferWritingToMain(new int[]{defaultTex});
        this.defaultFBAlt = this.flippedAfterTranslucent.contains((Object)defaultTex) ? this.renderTargets.createFramebufferWritingToAlt(new int[]{defaultTex}) : this.renderTargets.createFramebufferWritingToMain(new int[]{defaultTex});
    }

    private ComputeProgram[] createShadowComputes(ComputeSource[] compute, ProgramSet programSet) {
        ComputeProgram[] programs = new ComputeProgram[compute.length];
        for (int i = 0; i < programs.length; ++i) {
            ProgramBuilder builder;
            ComputeSource source = compute[i];
            if (source == null || source.getSource().isEmpty()) continue;
            try {
                String transformed = TransformPatcher.patchCompute(source.getName(), source.getSource().orElse(null), TextureStage.GBUFFERS_AND_SHADOW, this.customTextureMap);
                ShaderPrinter.printProgram(source.getName()).addSource(PatchShaderType.COMPUTE, transformed).print();
                builder = ProgramBuilder.beginCompute(source.getName(), transformed, IrisSamplers.WORLD_RESERVED_TEXTURE_UNITS);
            }
            catch (ShaderCompileException e) {
                throw e;
            }
            catch (RuntimeException e) {
                throw new RuntimeException("Shader compilation failed for compute " + source.getName() + "!", e);
            }
            CommonUniforms.addDynamicUniforms(builder, FogMode.OFF);
            this.customUniforms.assignTo(builder);
            Supplier<ImmutableSet<Integer>> flipped = () -> this.flippedBeforeShadow;
            TextureStage textureStage = TextureStage.GBUFFERS_AND_SHADOW;
            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(builder, this.customTextureManager.getCustomTextureIdMap(textureStage));
            IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, flipped, this.renderTargets, false, this);
            IrisSamplers.addCustomTextures(builder, this.customTextureManager.getIrisCustomTextures());
            IrisSamplers.addCustomImages(customTextureSamplerInterceptor, this.customImages);
            IrisImages.addRenderTargetImages(builder, flipped, this.renderTargets);
            IrisImages.addCustomImages(builder, this.customImages);
            IrisSamplers.addLevelSamplers(customTextureSamplerInterceptor, this, (class_1044)this.whitePixel, true, true, false);
            IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, this.customTextureManager.getNoiseTexture());
            if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor) && this.shadowRenderTargets != null) {
                IrisSamplers.addShadowSamplers(customTextureSamplerInterceptor, this.shadowRenderTargets, null, this.separateHardwareSamplers);
                IrisImages.addShadowColorImages(builder, this.shadowRenderTargets, null);
            }
            programs[i] = builder.buildCompute();
            this.customUniforms.mapholderToPass(builder, programs[i]);
            programs[i].setWorkGroupInfo(source.getWorkGroupRelative(), source.getWorkGroups(), FilledIndirectPointer.basedOff(this.shaderStorageBufferHolder, source.getIndirectPointer()));
        }
        return programs;
    }

    private ComputeProgram[] createSetupComputes(ComputeSource[] compute, ProgramSet programSet, TextureStage stage) {
        ComputeProgram[] programs = new ComputeProgram[compute.length];
        for (int i = 0; i < programs.length; ++i) {
            ProgramBuilder builder;
            ComputeSource source = compute[i];
            if (source == null || source.getSource().isEmpty()) continue;
            try {
                String transformed = TransformPatcher.patchCompute(source.getName(), source.getSource().orElse(null), stage, this.customTextureMap);
                ShaderPrinter.printProgram(source.getName()).addSource(PatchShaderType.COMPUTE, transformed).print();
                builder = ProgramBuilder.beginCompute(source.getName(), transformed, IrisSamplers.COMPOSITE_RESERVED_TEXTURE_UNITS);
            }
            catch (RuntimeException e) {
                throw new RuntimeException("Shader compilation failed for setup compute " + source.getName() + "!", e);
            }
            CommonUniforms.addDynamicUniforms(builder, FogMode.OFF);
            this.customUniforms.assignTo(builder);
            ImmutableSet empty = ImmutableSet.of();
            Supplier<ImmutableSet<Integer>> flipped = () -> empty;
            TextureStage textureStage = TextureStage.SETUP;
            ProgramSamplers.CustomTextureSamplerInterceptor customTextureSamplerInterceptor = ProgramSamplers.customTextureSamplerInterceptor(builder, this.customTextureManager.getCustomTextureIdMap(textureStage));
            IrisSamplers.addRenderTargetSamplers(customTextureSamplerInterceptor, flipped, this.renderTargets, true, this);
            IrisSamplers.addCustomTextures(builder, this.customTextureManager.getIrisCustomTextures());
            IrisSamplers.addCompositeSamplers(builder, this.renderTargets);
            IrisSamplers.addCustomImages(customTextureSamplerInterceptor, this.customImages);
            IrisImages.addRenderTargetImages(builder, flipped, this.renderTargets);
            IrisImages.addCustomImages(builder, this.customImages);
            IrisSamplers.addNoiseSampler(customTextureSamplerInterceptor, this.customTextureManager.getNoiseTexture());
            if (IrisSamplers.hasShadowSamplers(customTextureSamplerInterceptor) && this.shadowRenderTargets != null) {
                IrisSamplers.addShadowSamplers(customTextureSamplerInterceptor, this.shadowRenderTargets, null, this.separateHardwareSamplers);
                IrisImages.addShadowColorImages(builder, this.shadowRenderTargets, null);
            }
            programs[i] = builder.buildCompute();
            this.customUniforms.mapholderToPass(builder, programs[i]);
            programs[i].setWorkGroupInfo(source.getWorkGroupRelative(), source.getWorkGroups(), FilledIndirectPointer.basedOff(this.shaderStorageBufferHolder, source.getIndirectPointer()));
        }
        return programs;
    }

    private class_5944 createShader(String name, Optional<ProgramSource> source, ShaderKey key) throws IOException {
        if (source.isEmpty()) {
            return this.createFallbackShader(name, key);
        }
        return this.createShader(name, source.get(), key.getProgram(), key.getAlphaTest(), key.getVertexFormat(), key.getFogMode(), key.isIntensity(), key.shouldIgnoreLightmap(), key.isGlint(), key.isText(), key == ShaderKey.IE_COMPAT);
    }

    @Override
    public Object2ObjectMap<Tri<String, TextureType, TextureStage>, String> getTextureMap() {
        return this.customTextureMap;
    }

    private class_5944 createShader(String name, ProgramSource source, ProgramId programId, AlphaTest fallbackAlpha, class_293 vertexFormat, FogMode fogMode, boolean isIntensity, boolean isFullbright, boolean isGlint, boolean isText, boolean isIE) throws IOException {
        GlFramebuffer beforeTranslucent = this.renderTargets.createGbufferFramebuffer(this.flippedAfterPrepare, source.getDirectives().getDrawBuffers());
        GlFramebuffer afterTranslucent = this.renderTargets.createGbufferFramebuffer(this.flippedAfterTranslucent, source.getDirectives().getDrawBuffers());
        boolean isLines = programId == ProgramId.Line && this.resolver.has(ProgramId.Line);
        ShaderAttributeInputs inputs = new ShaderAttributeInputs(vertexFormat, isFullbright, isLines, isGlint, isText, isIE);
        Supplier<ImmutableSet<Integer>> flipped = () -> this.isBeforeTranslucent ? this.flippedAfterPrepare : this.flippedAfterTranslucent;
        ExtendedShader extendedShader = ShaderCreator.create(this, name, source, programId, beforeTranslucent, afterTranslucent, fallbackAlpha, vertexFormat, inputs, this.updateNotifier, this, flipped, fogMode, isIntensity, isFullbright, false, isLines, this.customUniforms);
        this.loadedShaders.add(extendedShader);
        return extendedShader;
    }

    private class_5944 createFallbackShader(String name, ShaderKey key) throws IOException {
        GlFramebuffer beforeTranslucent = this.renderTargets.createGbufferFramebuffer(this.flippedAfterPrepare, new int[]{0});
        GlFramebuffer afterTranslucent = this.renderTargets.createGbufferFramebuffer(this.flippedAfterTranslucent, new int[]{0});
        FallbackShader shader = ShaderCreator.createFallback(name, beforeTranslucent, afterTranslucent, key.getAlphaTest(), key.getVertexFormat(), null, this, key.getFogMode(), key == ShaderKey.GLINT, key.isText(), key.hasDiffuseLighting(), key.isIntensity(), key.shouldIgnoreLightmap());
        this.loadedShaders.add(shader);
        return shader;
    }

    private class_5944 createShadowShader(String name, Optional<ProgramSource> source, ShaderKey key) throws IOException {
        if (source.isEmpty()) {
            return this.createFallbackShadowShader(name, key);
        }
        return this.createShadowShader(name, source.get(), key.getProgram(), key.getAlphaTest(), key.getVertexFormat(), key.isIntensity(), key.shouldIgnoreLightmap(), key.isText(), key == ShaderKey.IE_COMPAT_SHADOW);
    }

    private class_5944 createFallbackShadowShader(String name, ShaderKey key) throws IOException {
        GlFramebuffer framebuffer = this.shadowRenderTargets.createShadowFramebuffer((ImmutableSet<Integer>)ImmutableSet.of(), new int[]{0});
        FallbackShader shader = ShaderCreator.createFallback(name, framebuffer, framebuffer, key.getAlphaTest(), key.getVertexFormat(), BlendModeOverride.OFF, this, key.getFogMode(), key == ShaderKey.GLINT, key.isText(), key.hasDiffuseLighting(), key.isIntensity(), key.shouldIgnoreLightmap());
        this.loadedShaders.add(shader);
        return shader;
    }

    private class_5944 createShadowShader(String name, ProgramSource source, ProgramId programId, AlphaTest fallbackAlpha, class_293 vertexFormat, boolean isIntensity, boolean isFullbright, boolean isText, boolean isIE) throws IOException {
        int[] nArray;
        ImmutableSet immutableSet = ImmutableSet.of();
        if (source.getDirectives().hasUnknownDrawBuffers()) {
            int[] nArray2 = new int[2];
            nArray2[0] = 0;
            nArray = nArray2;
            nArray2[1] = 1;
        } else {
            nArray = source.getDirectives().getDrawBuffers();
        }
        GlFramebuffer framebuffer = this.shadowRenderTargets.createShadowFramebuffer((ImmutableSet<Integer>)immutableSet, nArray);
        boolean isLines = programId == ProgramId.Line && this.resolver.has(ProgramId.Line);
        ShaderAttributeInputs inputs = new ShaderAttributeInputs(vertexFormat, isFullbright, isLines, false, isText, isIE);
        Supplier<ImmutableSet<Integer>> flipped = () -> this.flippedBeforeShadow;
        ExtendedShader extendedShader = ShaderCreator.create(this, name, source, programId, framebuffer, framebuffer, fallbackAlpha, vertexFormat, inputs, this.updateNotifier, this, flipped, FogMode.PER_VERTEX, isIntensity, isFullbright, true, isLines, this.customUniforms);
        this.loadedShaders.add(extendedShader);
        return extendedShader;
    }

    public void addGbufferOrShadowSamplers(SamplerHolder samplers, ImageHolder images, Supplier<ImmutableSet<Integer>> flipped, boolean isShadowPass, boolean hasTexture, boolean hasLightmap, boolean hasOverlay) {
        TextureStage textureStage = TextureStage.GBUFFERS_AND_SHADOW;
        ProgramSamplers.CustomTextureSamplerInterceptor samplerHolder = ProgramSamplers.customTextureSamplerInterceptor(samplers, this.customTextureManager.getCustomTextureIdMap().getOrDefault((Object)textureStage, (Object2ObjectMap<String, TextureAccess>)Object2ObjectMaps.emptyMap()));
        IrisSamplers.addRenderTargetSamplers(samplerHolder, flipped, this.renderTargets, false, this);
        IrisSamplers.addCustomTextures(samplerHolder, this.customTextureManager.getIrisCustomTextures());
        IrisImages.addRenderTargetImages(images, flipped, this.renderTargets);
        IrisImages.addCustomImages(images, this.customImages);
        if (!this.shouldBindPBR) {
            this.shouldBindPBR = IrisSamplers.hasPBRSamplers(samplerHolder);
        }
        IrisSamplers.addLevelSamplers(samplers, this, (class_1044)this.whitePixel, hasTexture, hasLightmap, hasOverlay);
        IrisSamplers.addWorldDepthSamplers(samplerHolder, this.renderTargets);
        IrisSamplers.addNoiseSampler(samplerHolder, this.customTextureManager.getNoiseTexture());
        IrisSamplers.addCustomImages(samplerHolder, this.customImages);
        if (IrisSamplers.hasShadowSamplers(samplerHolder)) {
            IrisSamplers.addShadowSamplers(samplerHolder, this.shadowTargetsSupplier.get(), null, this.separateHardwareSamplers);
        }
        if (isShadowPass || IrisImages.hasShadowImages(images)) {
            IrisImages.addShadowColorImages(images, this.shadowTargetsSupplier.get(), null);
        }
    }

    @Override
    public WorldRenderingPhase getPhase() {
        if (this.shouldRemovePhase) {
            this.phase = WorldRenderingPhase.NONE;
            this.shouldRemovePhase = false;
            GLDebug.popGroup();
        }
        if (this.overridePhase != null) {
            return this.overridePhase;
        }
        return this.phase;
    }

    public void removePhaseIfNeeded() {
        if (this.shouldRemovePhase) {
            this.phase = WorldRenderingPhase.NONE;
            this.shouldRemovePhase = false;
            GLDebug.popGroup();
        }
    }

    @Override
    public void setPhase(WorldRenderingPhase phase) {
        if (phase == WorldRenderingPhase.NONE) {
            if (this.shouldRemovePhase) {
                GLDebug.popGroup();
            }
            this.shouldRemovePhase = true;
            return;
        }
        this.shouldRemovePhase = false;
        if (phase == this.phase) {
            return;
        }
        GLDebug.popGroup();
        if (phase != WorldRenderingPhase.NONE && phase != WorldRenderingPhase.TERRAIN_CUTOUT && phase != WorldRenderingPhase.TERRAIN_CUTOUT_MIPPED && phase != WorldRenderingPhase.TRIPWIRE) {
            if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
                GLDebug.pushGroup(phase.ordinal(), "Shadow " + StringUtils.capitalize((String)phase.name().toLowerCase(Locale.ROOT).replace("_", " ")));
            } else {
                GLDebug.pushGroup(phase.ordinal(), StringUtils.capitalize((String)phase.name().toLowerCase(Locale.ROOT).replace("_", " ")));
            }
        }
        this.phase = phase;
    }

    @Override
    public void setOverridePhase(WorldRenderingPhase phase) {
        this.overridePhase = phase;
    }

    @Override
    public int getCurrentNormalTexture() {
        return this.currentNormalTexture;
    }

    @Override
    public int getCurrentSpecularTexture() {
        return this.currentSpecularTexture;
    }

    @Override
    public void onSetShaderTexture(int id) {
        if (this.shouldBindPBR && this.isRenderingWorld) {
            PBRTextureHolder pbrHolder = PBRTextureManager.INSTANCE.getOrLoadHolder(id);
            this.currentNormalTexture = pbrHolder.normalTexture().method_4624();
            this.currentSpecularTexture = pbrHolder.specularTexture().method_4624();
            TextureFormat textureFormat = TextureFormatLoader.getFormat();
            if (textureFormat != null) {
                int previousBinding = GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].field_5167;
                textureFormat.setupTextureParameters(PBRType.NORMAL, pbrHolder.normalTexture());
                textureFormat.setupTextureParameters(PBRType.SPECULAR, pbrHolder.specularTexture());
                GlStateManager._bindTexture((int)previousBinding);
            }
            PBRTextureManager.notifyPBRTexturesChanged();
        }
    }

    @Override
    public void beginLevelRendering() {
        ImmutableList<ClearPass> passes;
        this.isRenderingWorld = true;
        if (!this.initializedBlockIds) {
            WorldRenderingSettings.INSTANCE.setBlockStateIds(BlockMaterialMapping.createBlockStateIdMap(this.pack.getIdMap().getBlockProperties(), this.pack.getIdMap().getTagEntries()));
            WorldRenderingSettings.INSTANCE.setBlockTypeIds(BlockMaterialMapping.createBlockTypeMap(this.pack.getIdMap().getBlockRenderTypeMap()));
            class_310.method_1551().field_1769.method_3279();
            this.initializedBlockIds = true;
        }
        RenderSystem.activeTexture((int)33984);
        Vector4f emptyClearColor = new Vector4f(1.0f);
        GLDebug.pushGroup(100, "Clear textures");
        for (GlImage image2 : this.clearImages) {
            ARBClearTexture.glClearTexImage((int)image2.getId(), (int)0, (int)image2.getFormat().getGlFormat(), (int)image2.getPixelType().getGlFormat(), (int[])null);
        }
        if (this.shadowRenderTargets != null) {
            if (this.packDirectives.getShadowDirectives().isShadowEnabled() == OptionalBoolean.FALSE) {
                if (this.shadowRenderTargets.isFullClearRequired()) {
                    this.shadowClearPasses = ClearPassCreator.createShadowClearPasses(this.shadowRenderTargets, false, this.shadowDirectives);
                    this.shadowClearPassesFull = ClearPassCreator.createShadowClearPasses(this.shadowRenderTargets, true, this.shadowDirectives);
                    this.shadowRenderTargets.onFullClear();
                    for (ClearPass clearPass2 : this.shadowClearPassesFull) {
                        clearPass2.execute(emptyClearColor);
                    }
                }
            } else {
                ImmutableList<ClearPass> passes2;
                this.shadowRenderTargets.getDepthSourceFb().bind();
                RenderSystem.clear((int)256, (boolean)class_310.field_1703);
                for (ComputeProgram computeProgram : this.shadowComputes) {
                    if (computeProgram == null) continue;
                    computeProgram.use();
                    this.customUniforms.push(computeProgram);
                    computeProgram.dispatch(this.shadowMapResolution, this.shadowMapResolution);
                }
                if (this.shadowRenderTargets.isFullClearRequired()) {
                    this.shadowClearPasses = ClearPassCreator.createShadowClearPasses(this.shadowRenderTargets, false, this.shadowDirectives);
                    this.shadowClearPassesFull = ClearPassCreator.createShadowClearPasses(this.shadowRenderTargets, true, this.shadowDirectives);
                    passes2 = this.shadowClearPassesFull;
                    this.shadowRenderTargets.onFullClear();
                } else {
                    passes2 = this.shadowClearPasses;
                }
                for (ClearPass clearPass3 : passes2) {
                    clearPass3.execute(emptyClearColor);
                }
            }
        }
        this.updateNotifier.onNewFrame();
        this.customUniforms.update();
        class_276 main = class_310.method_1551().method_1522();
        int depthTextureId = main.method_30278();
        int internalFormat = TextureInfoCache.INSTANCE.getInfo(depthTextureId).getInternalFormat();
        DepthBufferFormat depthBufferFormat = DepthBufferFormat.fromGlEnumOrDefault(internalFormat);
        boolean changed = this.renderTargets.resizeIfNeeded(((Blaze3dRenderTargetExt)main).iris$getDepthBufferVersion(), depthTextureId, main.field_1482, main.field_1481, depthBufferFormat, this.packDirectives);
        if (changed) {
            this.beginRenderer.recalculateSizes();
            this.prepareRenderer.recalculateSizes();
            this.deferredRenderer.recalculateSizes();
            this.compositeRenderer.recalculateSizes();
            this.finalPassRenderer.recalculateSwapPassSize();
            if (this.shaderStorageBufferHolder != null) {
                this.shaderStorageBufferHolder.hasResizedScreen(main.field_1482, main.field_1481);
            }
            this.customImages.forEach(image -> image.updateNewSize(main.field_1482, main.field_1481));
            this.clearPassesFull.forEach(clearPass -> this.renderTargets.destroyFramebuffer(clearPass.getFramebuffer()));
            this.clearPasses.forEach(clearPass -> this.renderTargets.destroyFramebuffer(clearPass.getFramebuffer()));
            this.clearPassesFull = ClearPassCreator.createClearPasses(this.renderTargets, true, this.packDirectives.getRenderTargetDirectives());
            this.clearPasses = ClearPassCreator.createClearPasses(this.renderTargets, false, this.packDirectives.getRenderTargetDirectives());
        }
        if (changed || IrisVideoSettings.colorSpace != this.currentColorSpace) {
            this.currentColorSpace = IrisVideoSettings.colorSpace;
            this.colorSpaceConverter.rebuildProgram(main.field_1482, main.field_1481, this.currentColorSpace);
        }
        if (this.renderTargets.isFullClearRequired()) {
            this.renderTargets.onFullClear();
            passes = this.clearPassesFull;
        } else {
            passes = this.clearPasses;
        }
        Vector3d fogColor3 = CapturedRenderingState.INSTANCE.getFogColor();
        Vector4f fogColor = new Vector4f((float)fogColor3.x, (float)fogColor3.y, (float)fogColor3.z, 1.0f);
        for (ClearPass clearPass4 : passes) {
            clearPass4.execute(fogColor);
        }
        GLDebug.popGroup();
        main.method_1235(true);
        this.isMainBound = true;
        if (changed) {
            boolean hasRun = false;
            for (ComputeProgram program : this.setup) {
                if (program == null) continue;
                hasRun = true;
                program.use();
                program.dispatch(1.0f, 1.0f);
            }
            if (hasRun) {
                ComputeProgram.unbind();
            }
        }
        this.isBeforeTranslucent = true;
        this.beginRenderer.renderAll();
        this.setPhase(WorldRenderingPhase.SKY);
        class_5294.class_5401 skyType = class_310.method_1551().field_1687.method_28103().method_29992();
        if (this.shouldRenderSkyDisc && (skyType == class_5294.class_5401.field_25640 || class_310.method_1551().field_1687.method_8597().comp_642())) {
            RenderSystem.depthMask((boolean)false);
            RenderSystem.setShaderColor((float)fogColor.x, (float)fogColor.y, (float)fogColor.z, (float)fogColor.w);
            this.horizonRenderer.renderHorizon(CapturedRenderingState.INSTANCE.getGbufferModelView(), CapturedRenderingState.INSTANCE.getGbufferProjection(), class_757.method_34539());
            RenderSystem.depthMask((boolean)true);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        }
    }

    @Override
    public void renderShadows(LevelRendererAccessor worldRenderer, class_4184 playerCamera) {
        if (this.shadowRenderer != null) {
            this.shadowRenderer.renderShadows(worldRenderer, playerCamera);
        }
        this.prepareRenderer.renderAll();
    }

    @Override
    public void addDebugText(List<String> messages) {
        if (this.shadowRenderer != null) {
            messages.add("");
            this.shadowRenderer.addDebugText(messages);
        } else {
            messages.add("");
            messages.add("[Iris] Shadow Maps: not used by shader pack");
        }
    }

    @Override
    public OptionalInt getForcedShadowRenderDistanceChunksForDisplay() {
        return this.forcedShadowRenderDistanceChunks;
    }

    @Override
    public void beginHand() {
        this.centerDepthSampler.sampleCenterDepth();
        this.renderTargets.copyPreHandDepth();
    }

    @Override
    public void beginTranslucents() {
        if (this.destroyed) {
            throw new IllegalStateException("Tried to use a destroyed world rendering pipeline");
        }
        this.removePhaseIfNeeded();
        this.isBeforeTranslucent = false;
        this.renderTargets.copyPreTranslucentDepth();
        this.deferredRenderer.renderAll();
        RenderSystem.enableBlend();
        RenderSystem.setShader(class_757::method_34539);
    }

    @Override
    public void finalizeLevelRendering() {
        this.isRenderingWorld = false;
        this.removePhaseIfNeeded();
        this.compositeRenderer.renderAll();
        this.finalPassRenderer.renderFinalPass();
    }

    @Override
    public void finalizeGameRendering() {
        this.colorSpaceConverter.process(class_310.method_1551().method_1522().method_30277());
    }

    @Override
    public boolean shouldDisableVanillaEntityShadows() {
        return this.shadowRenderer != null;
    }

    @Override
    public boolean shouldRenderUnderwaterOverlay() {
        return this.shouldRenderUnderwaterOverlay;
    }

    @Override
    public boolean shouldRenderVignette() {
        return this.shouldRenderVignette;
    }

    @Override
    public boolean shouldRenderSun() {
        return this.shouldRenderSun;
    }

    @Override
    public boolean shouldRenderWeather() {
        return this.shouldRenderWeather;
    }

    @Override
    public boolean shouldRenderWeatherParticles() {
        return this.shouldRenderWeatherParticles;
    }

    @Override
    public boolean shouldRenderMoon() {
        return this.shouldRenderMoon;
    }

    @Override
    public boolean shouldRenderStars() {
        return this.shouldRenderStars;
    }

    @Override
    public boolean shouldRenderSkyDisc() {
        return this.shouldRenderSkyDisc;
    }

    @Override
    public boolean shouldWriteRainAndSnowToDepthBuffer() {
        return this.shouldWriteRainAndSnowToDepthBuffer;
    }

    @Override
    public ParticleRenderingSettings getParticleRenderingSettings() {
        return this.particleRenderingSettings;
    }

    @Override
    public boolean allowConcurrentCompute() {
        return this.allowConcurrentCompute;
    }

    @Override
    public boolean hasFeature(FeatureFlags flag) {
        return this.pack.hasFeature(flag);
    }

    @Override
    public boolean shouldDisableDirectionalShading() {
        return !this.oldLighting;
    }

    @Override
    public boolean shouldDisableFrustumCulling() {
        return !this.frustumCulling;
    }

    @Override
    public boolean shouldDisableOcclusionCulling() {
        return !this.occlusionCulling;
    }

    @Override
    public CloudSetting getCloudSetting() {
        return this.cloudSetting;
    }

    @Override
    public ShaderMap getShaderMap() {
        return this.shaderMap;
    }

    private void destroyShaders() {
        this.loadedShaders.forEach(shader -> {
            shader.method_34585();
            shader.close();
        });
    }

    @Override
    public void destroy() {
        int i;
        this.destroyed = true;
        this.destroyShaders();
        for (i = 0; i < 16; ++i) {
            GlStateManager.glActiveTexture((int)(33984 + i));
            IrisRenderSystem.unbindAllSamplers();
            GlStateManager._bindTexture((int)0);
        }
        GlStateManager.glActiveTexture((int)33984);
        for (i = 0; i < 12; ++i) {
            RenderSystem.setShaderTexture((int)i, (int)0);
        }
        if (this.shadowCompositeRenderer != null) {
            this.shadowCompositeRenderer.destroy();
        }
        this.prepareRenderer.destroy();
        this.compositeRenderer.destroy();
        this.deferredRenderer.destroy();
        this.finalPassRenderer.destroy();
        this.centerDepthSampler.destroy();
        this.customTextureManager.destroy();
        this.whitePixel.close();
        this.horizonRenderer.destroy();
        GlStateManager._glBindFramebuffer((int)36008, (int)0);
        GlStateManager._glBindFramebuffer((int)36009, (int)0);
        GlStateManager._glBindFramebuffer((int)36160, (int)0);
        class_310.method_1551().method_1522().method_1235(false);
        this.renderTargets.destroy();
        this.dhCompat.clearPipeline();
        this.customImages.forEach(GlResource::destroy);
        if (this.shadowRenderTargets != null) {
            this.shadowRenderTargets.destroy();
        }
        if (this.shadowRenderer != null) {
            this.shadowRenderer.destroy();
        }
        if (this.shaderStorageBufferHolder != null) {
            this.shaderStorageBufferHolder.destroyBuffers();
        }
    }

    @Override
    public boolean shouldOverrideShaders() {
        return this.isRenderingWorld && this.isMainBound;
    }

    @Override
    public SodiumPrograms getSodiumPrograms() {
        return this.sodiumPrograms;
    }

    @Override
    public FrameUpdateNotifier getFrameUpdateNotifier() {
        return this.updateNotifier;
    }

    @Override
    public float getSunPathRotation() {
        return this.sunPathRotation;
    }

    @Override
    public DHCompat getDHCompat() {
        return this.dhCompat;
    }

    protected class_1044 getWhitePixel() {
        return this.whitePixel;
    }

    @Override
    public void setIsMainBound(boolean bound) {
        this.isMainBound = bound;
    }

    public Optional<ProgramSource> getDHTerrainShader() {
        return this.resolver.resolve(ProgramId.DhTerrain);
    }

    public Optional<ProgramSource> getDHGenericShader() {
        return this.resolver.resolve(ProgramId.DhGeneric);
    }

    public Optional<ProgramSource> getDHWaterShader() {
        return this.resolver.resolve(ProgramId.DhWater);
    }

    public Optional<ProgramSource> getDHShadowShader() {
        return this.resolver.resolve(ProgramId.DhShadow);
    }

    public CustomUniforms getCustomUniforms() {
        return this.customUniforms;
    }

    public GlFramebuffer createDHFramebuffer(ProgramSource sources, boolean trans) {
        return this.renderTargets.createDHFramebuffer(trans ? this.flippedAfterTranslucent : this.flippedAfterPrepare, sources.getDirectives().getDrawBuffers());
    }

    public ImmutableSet<Integer> getFlippedBeforeShadow() {
        return this.flippedBeforeShadow;
    }

    public ImmutableSet<Integer> getFlippedAfterPrepare() {
        return this.flippedAfterPrepare;
    }

    public ImmutableSet<Integer> getFlippedAfterTranslucent() {
        return this.flippedAfterTranslucent;
    }

    public GlFramebuffer createDHFramebufferShadow(ProgramSource sources) {
        return this.shadowRenderTargets.createDHFramebuffer((ImmutableSet<Integer>)ImmutableSet.of(), new int[]{0, 1});
    }

    public boolean hasShadowRenderTargets() {
        return this.shadowRenderTargets != null;
    }

    public boolean skipAllRendering() {
        return this.skipAllRendering;
    }

    public CloudSetting getDHCloudSetting() {
        return this.dhCloudSetting;
    }

    public void bindDefault() {
        if (this.isBeforeTranslucent) {
            this.defaultFB.bind();
        } else {
            this.defaultFBAlt.bind();
        }
    }

    public void bindDefaultShadow() {
        this.defaultFBShadow.bind();
    }
}

