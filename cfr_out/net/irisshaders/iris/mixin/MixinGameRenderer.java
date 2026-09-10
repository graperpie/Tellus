/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  net.minecraft.class_156
 *  net.minecraft.class_281
 *  net.minecraft.class_310
 *  net.minecraft.class_3300
 *  net.minecraft.class_4494
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_4599
 *  net.minecraft.class_5944
 *  net.minecraft.class_746
 *  net.minecraft.class_757
 *  net.minecraft.class_759
 *  net.minecraft.class_9779
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.program.IrisProgramTypes;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.class_156;
import net.minecraft.class_281;
import net.minecraft.class_310;
import net.minecraft.class_3300;
import net.minecraft.class_4494;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4599;
import net.minecraft.class_5944;
import net.minecraft.class_746;
import net.minecraft.class_757;
import net.minecraft.class_759;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_757.class})
public class MixinGameRenderer {
    @Shadow
    private boolean field_3992;

    @Inject(method={"getPositionShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overridePositionShader(CallbackInfoReturnable<class_5944> cir) {
        if (MixinGameRenderer.isSky()) {
            MixinGameRenderer.override(ShaderKey.SKY_BASIC, cir);
        } else if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_BASIC, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.BASIC, cir);
        }
    }

    @Inject(method={"getPositionColorShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overridePositionColorShader(CallbackInfoReturnable<class_5944> cir) {
        if (MixinGameRenderer.isSky()) {
            MixinGameRenderer.override(ShaderKey.SKY_BASIC_COLOR, cir);
        } else if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_BASIC_COLOR, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.BASIC_COLOR, cir);
        }
    }

    @Inject(method={"getPositionTexShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overridePositionTexShader(CallbackInfoReturnable<class_5944> cir) {
        if (MixinGameRenderer.isSky()) {
            MixinGameRenderer.override(ShaderKey.SKY_TEXTURED, cir);
        } else if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_TEX, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.TEXTURED, cir);
        }
    }

    @Inject(method={"getPositionTexColorShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overridePositionTexColorShader(CallbackInfoReturnable<class_5944> cir) {
        if (MixinGameRenderer.isSky()) {
            MixinGameRenderer.override(ShaderKey.SKY_TEXTURED_COLOR, cir);
        } else if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_TEX_COLOR, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.TEXTURED_COLOR, cir);
        }
    }

    @Inject(method={"getParticleShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideParticleShader(CallbackInfoReturnable<class_5944> cir) {
        if (MixinGameRenderer.isPhase(WorldRenderingPhase.RAIN_SNOW)) {
            MixinGameRenderer.override(ShaderKey.WEATHER, cir);
        } else if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_PARTICLES, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.PARTICLES, cir);
        }
    }

    @Inject(method={"getRendertypeCloudsShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overridePositionTexColorNormalShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_CLOUDS, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.CLOUDS, cir);
        }
    }

    @Inject(method={"getRendertypeSolidShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideSolidShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_TERRAIN_CUTOUT, cir);
        } else if (MixinGameRenderer.isBlockEntities() || MixinGameRenderer.isEntities()) {
            MixinGameRenderer.override(ShaderKey.MOVING_BLOCK, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.TERRAIN_SOLID, cir);
        }
    }

    @Inject(method={"getRendertypeCutoutShader", "getRendertypeCutoutMippedShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideCutoutShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_TERRAIN_CUTOUT, cir);
        } else if (MixinGameRenderer.isBlockEntities() || MixinGameRenderer.isEntities()) {
            MixinGameRenderer.override(ShaderKey.MOVING_BLOCK, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.TERRAIN_CUTOUT, cir);
        }
    }

    @Inject(method={"getRendertypeTranslucentShader", "getRendertypeTranslucentMovingBlockShader", "getRendertypeTripwireShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideTranslucentShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_TRANSLUCENT, cir);
        } else if (MixinGameRenderer.isBlockEntities() || MixinGameRenderer.isEntities()) {
            MixinGameRenderer.override(ShaderKey.MOVING_BLOCK, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.TERRAIN_TRANSLUCENT, cir);
        }
    }

    @Inject(method={"getRendertypeEntityCutoutShader", "getRendertypeEntityCutoutNoCullShader", "getRendertypeEntityCutoutNoCullZOffsetShader", "getRendertypeEntityDecalShader", "getRendertypeEntitySmoothCutoutShader", "getRendertypeArmorCutoutNoCullShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideEntityCutoutShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            if (MixinGameRenderer.isBlockEntities()) {
                MixinGameRenderer.override(ShaderKey.SHADOW_BLOCK, cir);
                return;
            }
            MixinGameRenderer.override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
        } else if (HandRenderer.INSTANCE.isActive()) {
            MixinGameRenderer.override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE, cir);
        } else if (MixinGameRenderer.isBlockEntities()) {
            MixinGameRenderer.override(ShaderKey.BLOCK_ENTITY_DIFFUSE, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.ENTITIES_CUTOUT_DIFFUSE, cir);
        }
    }

    @Inject(method={"getRendertypeEntityTranslucentShader", "getRendertypeEntityTranslucentCullShader", "getRendertypeItemEntityTranslucentCullShader", "getRendertypeBreezeWindShader", "getRendertypeEntityNoOutlineShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideEntityTranslucentShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            if (MixinGameRenderer.isBlockEntities()) {
                MixinGameRenderer.override(ShaderKey.SHADOW_BLOCK, cir);
                return;
            }
            MixinGameRenderer.override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
        } else if (HandRenderer.INSTANCE.isActive()) {
            MixinGameRenderer.override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE, cir);
        } else if (MixinGameRenderer.isBlockEntities()) {
            MixinGameRenderer.override(ShaderKey.BE_TRANSLUCENT, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.ENTITIES_TRANSLUCENT, cir);
        }
    }

    @Inject(method={"getRendertypeEnergySwirlShader", "getRendertypeEntityShadowShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideEnergySwirlShadowShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
        } else if (HandRenderer.INSTANCE.isActive()) {
            MixinGameRenderer.override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT : ShaderKey.HAND_TRANSLUCENT, cir);
        } else if (MixinGameRenderer.isBlockEntities()) {
            MixinGameRenderer.override(ShaderKey.BLOCK_ENTITY, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.ENTITIES_CUTOUT, cir);
        }
    }

    @Inject(method={"getRendertypeGlintShader", "getRendertypeGlintDirectShader", "getRendertypeGlintTranslucentShader", "getRendertypeArmorGlintShader", "getRendertypeEntityGlintDirectShader", "getRendertypeEntityGlintShader", "getRendertypeArmorEntityGlintShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideGlintShader(CallbackInfoReturnable<class_5944> cir) {
        if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.GLINT, cir);
        }
    }

    @Inject(method={"getRendertypeEntitySolidShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideEntitySolidDiffuseShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            if (MixinGameRenderer.isBlockEntities()) {
                MixinGameRenderer.override(ShaderKey.SHADOW_BLOCK, cir);
                return;
            }
            MixinGameRenderer.override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
        } else if (HandRenderer.INSTANCE.isActive()) {
            MixinGameRenderer.override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT_DIFFUSE : ShaderKey.HAND_WATER_DIFFUSE, cir);
        } else if (MixinGameRenderer.isBlockEntities()) {
            MixinGameRenderer.override(ShaderKey.BLOCK_ENTITY_DIFFUSE, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.ENTITIES_SOLID_DIFFUSE, cir);
        }
    }

    @Inject(method={"getRendertypeWaterMaskShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideEntitySolidShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
        } else if (HandRenderer.INSTANCE.isActive()) {
            MixinGameRenderer.override(HandRenderer.INSTANCE.isRenderingSolid() ? ShaderKey.HAND_CUTOUT : ShaderKey.HAND_TRANSLUCENT, cir);
        } else if (MixinGameRenderer.isBlockEntities()) {
            MixinGameRenderer.override(ShaderKey.BLOCK_ENTITY, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.ENTITIES_SOLID, cir);
        }
    }

    @Inject(method={"getRendertypeBeaconBeamShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideBeaconBeamShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_BEACON_BEAM, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.BEACON, cir);
        }
    }

    @Inject(method={"getRendertypeEntityAlphaShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideEntityAlphaShader(CallbackInfoReturnable<class_5944> cir) {
        if (!ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.ENTITIES_ALPHA, cir);
        }
    }

    @Inject(method={"getRendertypeEyesShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideEntityEyesShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
        } else if (MixinGameRenderer.isBlockEntities()) {
            MixinGameRenderer.override(ShaderKey.BLOCK_ENTITY, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.ENTITIES_EYES, cir);
        }
    }

    @Inject(method={"getRendertypeEntityTranslucentEmissiveShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideEntityTranslucentEmissiveShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            if (MixinGameRenderer.isBlockEntities()) {
                MixinGameRenderer.override(ShaderKey.SHADOW_BLOCK, cir);
                return;
            }
            MixinGameRenderer.override(ShaderKey.SHADOW_ENTITIES_CUTOUT, cir);
        } else if (MixinGameRenderer.isBlockEntities()) {
            MixinGameRenderer.override(ShaderKey.BLOCK_ENTITY, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.ENTITIES_EYES_TRANS, cir);
        }
    }

    @Inject(method={"getRendertypeLeashShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideLeashShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_LEASH, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.LEASH, cir);
        }
    }

    @Inject(method={"getRendertypeLightningShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideLightningShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_LIGHTNING, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.LIGHTNING, cir);
        }
    }

    @Inject(method={"getRendertypeCrumblingShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideCrumblingShader(CallbackInfoReturnable<class_5944> cir) {
        if (MixinGameRenderer.shouldOverrideShaders() && !ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.CRUMBLING, cir);
        }
    }

    @Inject(method={"getRendertypeTextShader", "getRendertypeTextSeeThroughShader", "getPositionColorTexLightmapShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideTextShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_TEXT, cir);
        } else if (HandRenderer.INSTANCE.isActive()) {
            MixinGameRenderer.override(ShaderKey.HAND_TEXT, cir);
        } else if (MixinGameRenderer.isBlockEntities()) {
            MixinGameRenderer.override(ShaderKey.TEXT_BE, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.TEXT, cir);
        }
    }

    @Inject(method={"getRendertypeTextBackgroundShader", "getRendertypeTextBackgroundSeeThroughShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideTextBackgroundShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_TEXT_BG, cir);
        } else {
            MixinGameRenderer.override(ShaderKey.TEXT_BG, cir);
        }
    }

    @Inject(method={"getRendertypeTextIntensityShader", "getRendertypeTextIntensitySeeThroughShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideTextIntensityShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_TEXT_INTENSITY, cir);
        } else if (HandRenderer.INSTANCE.isActive()) {
            MixinGameRenderer.override(ShaderKey.HAND_TEXT_INTENSITY, cir);
        } else if (MixinGameRenderer.isBlockEntities()) {
            MixinGameRenderer.override(ShaderKey.TEXT_INTENSITY_BE, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.TEXT_INTENSITY, cir);
        }
    }

    @Inject(method={"getRendertypeLinesShader"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$overrideLinesShader(CallbackInfoReturnable<class_5944> cir) {
        if (ShadowRenderer.ACTIVE) {
            MixinGameRenderer.override(ShaderKey.SHADOW_LINES, cir);
        } else if (MixinGameRenderer.shouldOverrideShaders()) {
            MixinGameRenderer.override(ShaderKey.LINES, cir);
        }
    }

    @Unique
    private static boolean isBlockEntities() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        return pipeline != null && pipeline.getPhase() == WorldRenderingPhase.BLOCK_ENTITIES;
    }

    @Unique
    private static boolean isEntities() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        return pipeline != null && pipeline.getPhase() == WorldRenderingPhase.ENTITIES;
    }

    @Unique
    private static boolean isSky() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            return switch (pipeline.getPhase()) {
                case WorldRenderingPhase.CUSTOM_SKY, WorldRenderingPhase.SKY, WorldRenderingPhase.SUNSET, WorldRenderingPhase.SUN, WorldRenderingPhase.STARS, WorldRenderingPhase.VOID, WorldRenderingPhase.MOON -> true;
                default -> false;
            };
        }
        return false;
    }

    @Unique
    private static boolean isPhase(WorldRenderingPhase phase) {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            return pipeline.getPhase() == phase;
        }
        return false;
    }

    @Unique
    private static boolean shouldOverrideShaders() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline)pipeline).shouldOverrideShaders();
        }
        return false;
    }

    @Unique
    private static void override(ShaderKey key, CallbackInfoReturnable<class_5944> cir) {
        class_5944 override;
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline && !ImmediateState.bypass && (override = ((ShaderRenderingPipeline)pipeline).getShaderMap().getShader(key)) != null) {
            cir.setReturnValue((Object)override);
        }
    }

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void iris$startFrame(class_9779 deltaTracker, boolean bl, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setRealTickDelta(deltaTracker.method_60637(true));
        SystemTimeUniforms.COUNTER.beginFrame();
        SystemTimeUniforms.TIMER.beginFrame(class_156.method_648());
    }

    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void iris$logSystem(class_310 arg, class_759 arg2, class_3300 arg3, class_4599 arg4, CallbackInfo ci) {
        Iris.logger.info("Hardware information:");
        Iris.logger.info("CPU: " + class_4494.method_22089());
        Iris.logger.info("GPU: " + class_4494.method_22090() + " (Supports OpenGL " + class_4494.method_22091() + ")");
        Iris.logger.info("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")");
    }

    @Redirect(method={"renderItemInHand"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/player/LocalPlayer;I)V"))
    private void iris$disableVanillaHandRendering(class_759 itemInHandRenderer, float tickDelta, class_4587 poseStack, class_4597.class_4598 bufferSource, class_746 localPlayer, int light) {
        if (Iris.isPackInUseQuick()) {
            return;
        }
        itemInHandRenderer.method_22976(tickDelta, poseStack, bufferSource, localPlayer, light);
    }

    @Inject(method={"renderLevel"}, at={@At(value="TAIL")})
    private void iris$runColorSpace(class_9779 deltaTracker, CallbackInfo ci) {
        Iris.getPipelineManager().getPipeline().ifPresent(WorldRenderingPipeline::finalizeGameRendering);
    }

    @Redirect(method={"reloadShaders"}, at=@At(value="INVOKE", target="Lcom/google/common/collect/Lists;newArrayList()Ljava/util/ArrayList;", remap=false))
    private ArrayList<class_281> iris$reloadGeometryShaders() {
        ArrayList programs = Lists.newArrayList();
        programs.addAll(IrisProgramTypes.GEOMETRY.method_1289().values());
        programs.addAll(IrisProgramTypes.TESS_CONTROL.method_1289().values());
        programs.addAll(IrisProgramTypes.TESS_EVAL.method_1289().values());
        return programs;
    }
}

