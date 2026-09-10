/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_1017
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1044
 *  net.minecraft.class_1059
 *  net.minecraft.class_1293
 *  net.minecraft.class_1294
 *  net.minecraft.class_1297
 *  net.minecraft.class_1306
 *  net.minecraft.class_1309
 *  net.minecraft.class_1944
 *  net.minecraft.class_2338
 *  net.minecraft.class_2374
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 *  net.minecraft.class_5636
 *  net.minecraft.class_746
 *  net.minecraft.class_757
 *  org.joml.Math
 *  org.joml.Vector2f
 *  org.joml.Vector2i
 *  org.joml.Vector3d
 *  org.joml.Vector4f
 *  org.joml.Vector4i
 */
package net.irisshaders.iris.uniforms;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.compat.dh.DHCompat;
import net.irisshaders.iris.gl.state.FogMode;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.helpers.JomlConversions;
import net.irisshaders.iris.layer.GbufferPrograms;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.mixin.statelisteners.BooleanStateAccessor;
import net.irisshaders.iris.mixin.texture.TextureAtlasAccessor;
import net.irisshaders.iris.mixinterface.LocalPlayerInterface;
import net.irisshaders.iris.pbr.TextureInfoCache;
import net.irisshaders.iris.pbr.TextureTracker;
import net.irisshaders.iris.shaderpack.IdMap;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.uniforms.BiomeUniforms;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.uniforms.CelestialUniforms;
import net.irisshaders.iris.uniforms.ExternallyManagedUniforms;
import net.irisshaders.iris.uniforms.FogUniforms;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.IdMapUniforms;
import net.irisshaders.iris.uniforms.IrisExclusiveUniforms;
import net.irisshaders.iris.uniforms.IrisInternalUniforms;
import net.irisshaders.iris.uniforms.IrisTimeUniforms;
import net.irisshaders.iris.uniforms.MatrixUniforms;
import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.irisshaders.iris.uniforms.ViewportUniforms;
import net.irisshaders.iris.uniforms.WorldTimeUniforms;
import net.irisshaders.iris.uniforms.transforms.SmoothedFloat;
import net.irisshaders.iris.uniforms.transforms.SmoothedVec2f;
import net.minecraft.class_1044;
import net.minecraft.class_1059;
import net.minecraft.class_1293;
import net.minecraft.class_1294;
import net.minecraft.class_1297;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_1944;
import net.minecraft.class_2338;
import net.minecraft.class_2374;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_5636;
import net.minecraft.class_746;
import net.minecraft.class_757;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.joml.Vector4i;

public final class CommonUniforms {
    private static final class_310 client = class_310.method_1551();
    private static final Vector2i ZERO_VECTOR_2i = new Vector2i();
    private static final Vector4i ZERO_VECTOR_4i = new Vector4i(0, 0, 0, 0);
    private static final Vector3d ZERO_VECTOR_3d = new Vector3d();

    private CommonUniforms() {
    }

    public static void addDynamicUniforms(DynamicUniformHolder uniforms, FogMode fogMode) {
        ExternallyManagedUniforms.addExternallyManagedUniforms117(uniforms);
        FogUniforms.addFogUniforms(uniforms, fogMode);
        IrisInternalUniforms.addFogUniforms(uniforms, fogMode);
        uniforms.uniform1i("entityId", CapturedRenderingState.INSTANCE::getCurrentRenderedEntity, StateUpdateNotifiers.fallbackEntityNotifier);
        uniforms.uniform2i("atlasSize", () -> {
            int glId = RenderSystem.getShaderTexture((int)0);
            class_1044 texture = TextureTracker.INSTANCE.getTexture(glId);
            if (texture instanceof class_1059) {
                class_1059 atlas = (class_1059)texture;
                TextureAtlasAccessor atlasAccessor = (TextureAtlasAccessor)atlas;
                return new Vector2i(atlasAccessor.callGetWidth(), atlasAccessor.callGetHeight());
            }
            return ZERO_VECTOR_2i;
        }, listener -> {});
        uniforms.uniform2i("gtextureSize", () -> {
            int glId = GlStateManagerAccessor.getTEXTURES()[0].field_5167;
            TextureInfoCache.TextureInfo info = TextureInfoCache.INSTANCE.getInfo(glId);
            return new Vector2i(info.getWidth(), info.getHeight());
        }, StateUpdateNotifiers.bindTextureNotifier);
        uniforms.uniform4i("blendFunc", () -> {
            GlStateManager.class_1017 blend = GlStateManagerAccessor.getBLEND();
            if (((BooleanStateAccessor)blend.field_5045).isEnabled()) {
                return new Vector4i(blend.field_5049, blend.field_5048, blend.field_5047, blend.field_5046);
            }
            return ZERO_VECTOR_4i;
        }, StateUpdateNotifiers.blendFuncNotifier);
        uniforms.uniform1i("renderStage", () -> GbufferPrograms.getCurrentPhase().ordinal(), StateUpdateNotifiers.phaseChangeNotifier);
    }

    public static void addCommonUniforms(DynamicUniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier, FogMode fogMode) {
        CommonUniforms.addNonDynamicUniforms(uniforms, idMap, directives, updateNotifier);
        CommonUniforms.addDynamicUniforms(uniforms, fogMode);
    }

    public static void addNonDynamicUniforms(UniformHolder uniforms, IdMap idMap, PackDirectives directives, FrameUpdateNotifier updateNotifier) {
        CameraUniforms.addCameraUniforms(uniforms, updateNotifier);
        ViewportUniforms.addViewportUniforms(uniforms);
        WorldTimeUniforms.addWorldTimeUniforms(uniforms);
        SystemTimeUniforms.addSystemTimeUniforms(uniforms);
        BiomeUniforms.addBiomeUniforms(uniforms);
        new CelestialUniforms(directives.getSunPathRotation()).addCelestialUniforms(uniforms);
        IrisExclusiveUniforms.addIrisExclusiveUniforms(uniforms);
        IrisTimeUniforms.addTimeUniforms(uniforms);
        MatrixUniforms.addMatrixUniforms(uniforms, directives);
        IdMapUniforms.addIdMapUniforms(updateNotifier, uniforms, idMap, directives.isOldHandLight());
        CommonUniforms.generalCommonUniforms(uniforms, updateNotifier, directives);
    }

    public static void generalCommonUniforms(UniformHolder uniforms, FrameUpdateNotifier updateNotifier, PackDirectives directives) {
        ExternallyManagedUniforms.addExternallyManagedUniforms117(uniforms);
        SmoothedVec2f eyeBrightnessSmooth = new SmoothedVec2f(directives.getEyeBrightnessHalfLife(), directives.getEyeBrightnessHalfLife(), CommonUniforms::getEyeBrightness, updateNotifier);
        uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hideGUI", () -> CommonUniforms.client.field_1690.field_1842).uniform1b(UniformUpdateFrequency.PER_FRAME, "isRightHanded", () -> CommonUniforms.client.field_1690.method_42552().method_41753() == class_1306.field_6183).uniform1i(UniformUpdateFrequency.PER_FRAME, "isEyeInWater", CommonUniforms::isEyeInWater).uniform1f(UniformUpdateFrequency.PER_FRAME, "blindness", CommonUniforms::getBlindness).uniform1f(UniformUpdateFrequency.PER_FRAME, "darknessFactor", CommonUniforms::getDarknessFactor).uniform1f(UniformUpdateFrequency.PER_FRAME, "darknessLightFactor", CapturedRenderingState.INSTANCE::getDarknessLightFactor).uniform1f(UniformUpdateFrequency.PER_FRAME, "nightVision", CommonUniforms::getNightVision).uniform1b(UniformUpdateFrequency.PER_FRAME, "is_sneaking", CommonUniforms::isSneaking).uniform1b(UniformUpdateFrequency.PER_FRAME, "is_sprinting", CommonUniforms::isSprinting).uniform1b(UniformUpdateFrequency.PER_FRAME, "is_hurt", CommonUniforms::isHurt).uniform1b(UniformUpdateFrequency.PER_FRAME, "is_invisible", CommonUniforms::isInvisible).uniform1b(UniformUpdateFrequency.PER_FRAME, "is_burning", CommonUniforms::isBurning).uniform1b(UniformUpdateFrequency.PER_FRAME, "is_on_ground", CommonUniforms::isOnGround).uniform1f(UniformUpdateFrequency.PER_FRAME, "screenBrightness", () -> (Double)CommonUniforms.client.field_1690.method_42473().method_41753()).uniform4f(UniformUpdateFrequency.ONCE, "entityColor", () -> new Vector4f(0.0f, 0.0f, 0.0f, 0.0f)).uniform1i(UniformUpdateFrequency.ONCE, "blockEntityId", () -> -1).uniform1i(UniformUpdateFrequency.ONCE, "currentRenderedItemId", () -> -1).uniform1f(UniformUpdateFrequency.ONCE, "pi", () -> Math.PI).uniform1f(UniformUpdateFrequency.PER_TICK, "playerMood", CommonUniforms::getPlayerMood).uniform1f(UniformUpdateFrequency.PER_TICK, "constantMood", CommonUniforms::getConstantMood).uniform2i(UniformUpdateFrequency.PER_FRAME, "eyeBrightness", CommonUniforms::getEyeBrightness).uniform2i(UniformUpdateFrequency.PER_FRAME, "eyeBrightnessSmooth", () -> {
            Vector2f smoothed = eyeBrightnessSmooth.get();
            return new Vector2i((int)smoothed.x(), (int)smoothed.y());
        }).uniform1f(UniformUpdateFrequency.PER_TICK, "rainStrength", CommonUniforms::getRainStrength).uniform1f(UniformUpdateFrequency.PER_TICK, "wetness", new SmoothedFloat(directives.getWetnessHalfLife(), directives.getDrynessHalfLife(), CommonUniforms::getRainStrength, updateNotifier)).uniform3d(UniformUpdateFrequency.PER_FRAME, "skyColor", CommonUniforms::getSkyColor).uniform1f(UniformUpdateFrequency.PER_FRAME, "dhFarPlane", DHCompat::getFarPlane).uniform1f(UniformUpdateFrequency.PER_FRAME, "dhNearPlane", DHCompat::getNearPlane).uniform1i(UniformUpdateFrequency.PER_FRAME, "dhRenderDistance", DHCompat::getRenderDistance);
    }

    private static boolean isOnGround() {
        return CommonUniforms.client.field_1724 != null && CommonUniforms.client.field_1724.method_24828();
    }

    private static boolean isHurt() {
        if (CommonUniforms.client.field_1724 != null) {
            return CommonUniforms.client.field_1724.field_6235 > 0;
        }
        return false;
    }

    private static boolean isInvisible() {
        if (CommonUniforms.client.field_1724 != null) {
            return CommonUniforms.client.field_1724.method_5767();
        }
        return false;
    }

    private static boolean isBurning() {
        if (CommonUniforms.client.field_1724 != null) {
            return CommonUniforms.client.field_1724.method_5809();
        }
        return false;
    }

    private static boolean isSneaking() {
        if (CommonUniforms.client.field_1724 != null) {
            return CommonUniforms.client.field_1724.method_18276();
        }
        return false;
    }

    private static boolean isSprinting() {
        if (CommonUniforms.client.field_1724 != null) {
            return CommonUniforms.client.field_1724.method_5624();
        }
        return false;
    }

    private static Vector3d getSkyColor() {
        if (CommonUniforms.client.field_1687 == null || CommonUniforms.client.field_1719 == null) {
            return ZERO_VECTOR_3d;
        }
        return JomlConversions.fromVec3(CommonUniforms.client.field_1687.method_23777(CommonUniforms.client.field_1719.method_19538(), CapturedRenderingState.INSTANCE.getTickDelta()));
    }

    static float getBlindness() {
        class_1293 blindness;
        class_1297 cameraEntity = client.method_1560();
        if (cameraEntity instanceof class_1309 && (blindness = ((class_1309)cameraEntity).method_6112(class_1294.field_5919)) != null) {
            if (blindness.method_48559()) {
                return 1.0f;
            }
            return org.joml.Math.clamp((float)0.0f, (float)1.0f, (float)((float)blindness.method_5584() / 20.0f));
        }
        return 0.0f;
    }

    static float getDarknessFactor() {
        class_1293 darkness;
        class_1297 cameraEntity = client.method_1560();
        if (cameraEntity instanceof class_1309 && (darkness = ((class_1309)cameraEntity).method_6112(class_1294.field_38092)) != null) {
            return darkness.method_55653((class_1309)cameraEntity, CapturedRenderingState.INSTANCE.getTickDelta());
        }
        return 0.0f;
    }

    private static float getPlayerMood() {
        if (!(CommonUniforms.client.field_1719 instanceof class_746)) {
            return 0.0f;
        }
        return org.joml.Math.clamp((float)0.0f, (float)1.0f, (float)((class_746)CommonUniforms.client.field_1719).method_26269());
    }

    private static float getConstantMood() {
        if (!(CommonUniforms.client.field_1719 instanceof class_746)) {
            return 0.0f;
        }
        return org.joml.Math.clamp((float)0.0f, (float)1.0f, (float)((LocalPlayerInterface)CommonUniforms.client.field_1719).getCurrentConstantMood());
    }

    static float getRainStrength() {
        if (CommonUniforms.client.field_1687 == null) {
            return 0.0f;
        }
        return org.joml.Math.clamp((float)0.0f, (float)1.0f, (float)CommonUniforms.client.field_1687.method_8430(CapturedRenderingState.INSTANCE.getTickDelta()));
    }

    private static Vector2i getEyeBrightness() {
        if (CommonUniforms.client.field_1719 == null || CommonUniforms.client.field_1687 == null) {
            return ZERO_VECTOR_2i;
        }
        class_243 feet = CommonUniforms.client.field_1719.method_19538();
        class_243 eyes = new class_243(feet.field_1352, CommonUniforms.client.field_1719.method_23320(), feet.field_1350);
        class_2338 eyeBlockPos = class_2338.method_49638((class_2374)eyes);
        int blockLight = CommonUniforms.client.field_1687.method_8314(class_1944.field_9282, eyeBlockPos);
        int skyLight = CommonUniforms.client.field_1687.method_8314(class_1944.field_9284, eyeBlockPos);
        return new Vector2i(blockLight * 16, skyLight * 16);
    }

    private static float getNightVision() {
        float underwaterVisibility;
        class_1297 cameraEntity = client.method_1560();
        if (cameraEntity instanceof class_1309) {
            class_1309 livingEntity = (class_1309)cameraEntity;
            try {
                float nightVisionStrength = class_757.method_3174((class_1309)livingEntity, (float)CapturedRenderingState.INSTANCE.getTickDelta());
                if (nightVisionStrength > 0.0f) {
                    return org.joml.Math.clamp((float)0.0f, (float)1.0f, (float)nightVisionStrength);
                }
            }
            catch (NullPointerException e) {
                return 0.0f;
            }
        }
        if (CommonUniforms.client.field_1724 != null && CommonUniforms.client.field_1724.method_6059(class_1294.field_5927) && (underwaterVisibility = CommonUniforms.client.field_1724.method_3140()) > 0.0f) {
            return org.joml.Math.clamp((float)0.0f, (float)1.0f, (float)underwaterVisibility);
        }
        return 0.0f;
    }

    static int isEyeInWater() {
        class_5636 submersionType = CommonUniforms.client.field_1773.method_19418().method_19334();
        if (submersionType == class_5636.field_27886) {
            return 1;
        }
        if (submersionType == class_5636.field_27885) {
            return 2;
        }
        if (submersionType == class_5636.field_27887) {
            return 3;
        }
        return 0;
    }

    static {
        GbufferPrograms.init();
    }
}

