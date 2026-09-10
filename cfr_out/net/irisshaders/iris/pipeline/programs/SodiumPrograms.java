/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.primitives.Ints
 *  net.caffeinemc.mods.sodium.client.gl.GlObject
 *  net.caffeinemc.mods.sodium.client.gl.shader.GlProgram
 *  net.caffeinemc.mods.sodium.client.gl.shader.GlProgram$Builder
 *  net.caffeinemc.mods.sodium.client.gl.shader.GlShader
 *  net.caffeinemc.mods.sodium.client.gl.shader.ShaderType
 *  net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface
 *  net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext
 *  net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
 *  net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
 *  net.minecraft.class_2960
 *  org.lwjgl.opengl.GL43C
 */
package net.irisshaders.iris.pipeline.programs;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.caffeinemc.mods.sodium.client.gl.GlObject;
import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.gl.shader.GlShader;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderType;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTests;
import net.irisshaders.iris.gl.blending.BufferBlendOverride;
import net.irisshaders.iris.gl.framebuffer.GlFramebuffer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.SodiumShader;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.pipeline.transform.TransformPatcher;
import net.irisshaders.iris.shaderpack.loading.ProgramId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.shaderpack.programs.ProgramFallbackResolver;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSource;
import net.irisshaders.iris.shadows.ShadowRenderTargets;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.targets.RenderTargets;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.irisshaders.iris.vertices.sodium.terrain.FormatAnalyzer;
import net.minecraft.class_2960;
import org.lwjgl.opengl.GL43C;

public class SodiumPrograms {
    private final EnumMap<Pass, GlFramebuffer> framebuffers = new EnumMap(Pass.class);
    private final EnumMap<Pass, GlProgram<ChunkShaderInterface>> shaders = new EnumMap(Pass.class);
    private boolean hasBlockId;
    private boolean hasMidUv;
    private boolean hasNormal;
    private boolean hasMidBlock;

    public SodiumPrograms(IrisRenderingPipeline pipeline, ProgramSet programSet, ProgramFallbackResolver resolver, RenderTargets renderTargets, Supplier<ShadowRenderTargets> shadowRenderTargets, CustomUniforms customUniforms) {
        for (Pass pass : Pass.values()) {
            ProgramSource source = resolver.resolveNullable(pass.getOriginalId());
            Supplier<ImmutableSet<Integer>> flipState = this.getFlipState(pipeline, pass, pass == Pass.SHADOW || pass == Pass.SHADOW_CUTOUT);
            GlFramebuffer framebuffer = this.createFramebuffer(pass, source, shadowRenderTargets, renderTargets, flipState);
            this.framebuffers.put(pass, framebuffer);
            if (source == null) continue;
            AlphaTest alphaTest = this.getAlphaTest(pass, source);
            Map<PatchShaderType, String> transformed = this.transformShaders(source, alphaTest, programSet);
            GlProgram<ChunkShaderInterface> shader = this.createShader(pipeline, pass, source, alphaTest, customUniforms, flipState, this.createGlShaders(pass.name().toLowerCase(Locale.ROOT), transformed));
            this.shaders.put(pass, shader);
        }
        WorldRenderingSettings.INSTANCE.setVertexFormat(FormatAnalyzer.createFormat(this.hasBlockId, this.hasNormal, this.hasMidUv, this.hasMidBlock));
    }

    private AlphaTest getAlphaTest(Pass pass, ProgramSource source) {
        return source.getDirectives().getAlphaTestOverride().orElse(pass == Pass.TERRAIN_CUTOUT || pass == Pass.SHADOW_CUTOUT ? AlphaTests.ONE_TENTH_ALPHA : AlphaTest.ALWAYS);
    }

    private Map<PatchShaderType, String> transformShaders(ProgramSource source, AlphaTest alphaTest, ProgramSet programSet) {
        Map<PatchShaderType, String> transformed = TransformPatcher.patchSodium(source.getName(), source.getVertexSource().orElse(null), source.getGeometrySource().orElse(null), source.getTessControlSource().orElse(null), source.getTessEvalSource().orElse(null), source.getFragmentSource().orElse(null), alphaTest, programSet.getPackDirectives().getTextureMap());
        ShaderPrinter.printProgram("sodium_" + source.getName()).addSources(transformed).print();
        return transformed;
    }

    private Map<PatchShaderType, GlShader> createGlShaders(String passName, Map<PatchShaderType, String> transformed) {
        EnumMap<PatchShaderType, GlShader> newMap = new EnumMap<PatchShaderType, GlShader>(PatchShaderType.class);
        for (Map.Entry<PatchShaderType, String> entry : transformed.entrySet()) {
            if (entry.getValue() == null) continue;
            newMap.put(entry.getKey(), new GlShader(ShaderType.fromGlShaderType((int)entry.getKey().glShaderType.id), class_2960.method_60655((String)"iris", (String)("sodium-shader-" + passName)), entry.getValue()));
        }
        return newMap;
    }

    private Supplier<ImmutableSet<Integer>> getFlipState(IrisRenderingPipeline pipeline, Pass pass, boolean isShadowPass) {
        if (isShadowPass) {
            return pipeline::getFlippedBeforeShadow;
        }
        return () -> pass == Pass.TRANSLUCENT ? pipeline.getFlippedAfterTranslucent() : pipeline.getFlippedAfterPrepare();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private GlProgram<ChunkShaderInterface> createShader(IrisRenderingPipeline pipeline, Pass pass, ProgramSource source, AlphaTest alphaTest, CustomUniforms customUniforms, Supplier<ImmutableSet<Integer>> flipState, Map<PatchShaderType, GlShader> transformed) {
        GlProgram.Builder builder = GlProgram.builder((class_2960)class_2960.method_60655((String)"sodium", (String)("chunk_shader_for_" + pass.name().toLowerCase(Locale.ROOT))));
        for (GlShader shader : transformed.values()) {
            builder.attachShader(shader);
        }
        boolean containsTessellation = source.getTessEvalSource().isPresent();
        try {
            GlProgram<ChunkShaderInterface> glProgram = this.buildProgram(builder, pipeline, pass, source, alphaTest, customUniforms, flipState, containsTessellation);
            return glProgram;
        }
        finally {
            transformed.values().forEach(GlShader::delete);
        }
    }

    private GlFramebuffer createFramebuffer(Pass pass, ProgramSource source, Supplier<ShadowRenderTargets> shadowRenderTargets, RenderTargets renderTargets, Supplier<ImmutableSet<Integer>> flipState) {
        int[] nArray;
        if (pass == Pass.SHADOW || pass == Pass.SHADOW_CUTOUT || pass == Pass.SHADOW_TRANS) {
            int[] nArray2;
            ImmutableSet immutableSet = ImmutableSet.of();
            if (source == null) {
                int[] nArray3 = new int[2];
                nArray3[0] = 0;
                nArray2 = nArray3;
                nArray3[1] = 1;
            } else if (source.getDirectives().hasUnknownDrawBuffers()) {
                int[] nArray4 = new int[2];
                nArray4[0] = 0;
                nArray2 = nArray4;
                nArray4[1] = 1;
            } else {
                nArray2 = source.getDirectives().getDrawBuffers();
            }
            return shadowRenderTargets.get().createShadowFramebuffer((ImmutableSet<Integer>)immutableSet, nArray2);
        }
        if (source == null) {
            int[] nArray5 = new int[2];
            nArray5[0] = 0;
            nArray = nArray5;
            nArray5[1] = 1;
        } else if (source.getDirectives().hasUnknownDrawBuffers()) {
            int[] nArray6 = new int[1];
            nArray = nArray6;
            nArray6[0] = 0;
        } else {
            nArray = source.getDirectives().getDrawBuffers();
        }
        return renderTargets.createGbufferFramebuffer(flipState.get(), nArray);
    }

    private List<BufferBlendOverride> createBufferBlendOverrides(ProgramSource source) {
        ArrayList<BufferBlendOverride> overrides = new ArrayList<BufferBlendOverride>();
        source.getDirectives().getBufferBlendOverrides().forEach(information -> {
            int index = Ints.indexOf((int[])source.getDirectives().getDrawBuffers(), (int)information.index());
            if (index > -1) {
                overrides.add(new BufferBlendOverride(index, information.blendMode()));
            }
        });
        return overrides;
    }

    private GlProgram<ChunkShaderInterface> buildProgram(GlProgram.Builder builder, IrisRenderingPipeline pipeline, Pass pass, ProgramSource source, AlphaTest alphaTest, CustomUniforms customUniforms, Supplier<ImmutableSet<Integer>> flipState, boolean containsTessellation) {
        return builder.bindAttribute("a_Position", 0).bindAttribute("a_Color", 1).bindAttribute("a_TexCoord", 2).bindAttribute("a_LightAndData", 3).bindAttribute("mc_Entity", 11).bindAttribute("mc_midTexCoord", 12).bindAttribute("at_tangent", 13).bindAttribute("iris_Normal", 10).bindAttribute("at_midBlock", 14).link(shader -> {
            int handle = ((GlObject)shader).handle();
            GLDebug.nameObject(33506, handle, "sodium-terrain-" + pass.toString().toLowerCase(Locale.ROOT));
            if (!this.hasNormal) {
                boolean bl = this.hasNormal = GL43C.glGetAttribLocation((int)handle, (CharSequence)"iris_Normal") != -1;
            }
            if (!this.hasMidBlock) {
                boolean bl = this.hasMidBlock = GL43C.glGetAttribLocation((int)handle, (CharSequence)"at_midBlock") != -1;
            }
            if (!this.hasBlockId) {
                boolean bl = this.hasBlockId = GL43C.glGetAttribLocation((int)handle, (CharSequence)"mc_Entity") != -1;
            }
            if (!this.hasMidUv) {
                this.hasMidUv = GL43C.glGetAttribLocation((int)handle, (CharSequence)"mc_midTexCoord") != -1;
            }
            return new SodiumShader(pipeline, pass, (ShaderBindingContext)shader, handle, source.getDirectives().getBlendModeOverride().orElse(null), this.createBufferBlendOverrides(source), customUniforms, flipState, alphaTest.reference(), containsTessellation);
        });
    }

    public GlProgram<ChunkShaderInterface> getProgram(TerrainRenderPass pass) {
        Pass pass2 = this.mapTerrainRenderPass(pass);
        return this.shaders.get((Object)pass2);
    }

    public GlFramebuffer getFramebuffer(TerrainRenderPass pass) {
        Pass pass2 = this.mapTerrainRenderPass(pass);
        return this.framebuffers.get((Object)pass2);
    }

    private Pass mapTerrainRenderPass(TerrainRenderPass pass) {
        if (pass == DefaultTerrainRenderPasses.SOLID) {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? Pass.SHADOW : Pass.TERRAIN;
        }
        if (pass == DefaultTerrainRenderPasses.CUTOUT) {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? Pass.SHADOW_CUTOUT : Pass.TERRAIN_CUTOUT;
        }
        if (pass == DefaultTerrainRenderPasses.TRANSLUCENT) {
            return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? Pass.SHADOW_TRANS : Pass.TRANSLUCENT;
        }
        throw new IllegalArgumentException("Unknown pass: " + String.valueOf(pass));
    }

    public static enum Pass {
        SHADOW(ProgramId.ShadowSolid),
        SHADOW_CUTOUT(ProgramId.ShadowCutout),
        SHADOW_TRANS(ProgramId.ShadowWater),
        TERRAIN(ProgramId.TerrainSolid),
        TERRAIN_CUTOUT(ProgramId.TerrainCutout),
        TRANSLUCENT(ProgramId.Water);

        private final ProgramId originalId;

        private Pass(ProgramId originalId) {
            this.originalId = originalId;
        }

        public ProgramId getOriginalId() {
            return this.originalId;
        }
    }
}

