/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1538
 *  net.minecraft.class_1934
 *  net.minecraft.class_2338
 *  net.minecraft.class_239
 *  net.minecraft.class_239$class_240
 *  net.minecraft.class_243
 *  net.minecraft.class_2680
 *  net.minecraft.class_310
 *  net.minecraft.class_3965
 *  net.minecraft.class_5498
 *  net.minecraft.class_638
 *  org.joml.Math
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 *  org.joml.Vector3f
 *  org.joml.Vector4f
 */
package net.irisshaders.iris.uniforms;

import java.util.Objects;
import java.util.stream.StreamSupport;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.helpers.JomlConversions;
import net.irisshaders.iris.mixin.GameRendererAccessor;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CameraUniforms;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1538;
import net.minecraft.class_1934;
import net.minecraft.class_2338;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_5498;
import net.minecraft.class_638;
import org.joml.Math;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class IrisExclusiveUniforms {
    private static final Vector3d ZERO = new Vector3d(0.0);

    public static void addIrisExclusiveUniforms(UniformHolder uniforms) {
        WorldInfoUniforms.addWorldInfoUniforms(uniforms);
        uniforms.uniform1i(UniformUpdateFrequency.PER_TICK, "currentColorSpace", () -> IrisVideoSettings.colorSpace.ordinal());
        uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "thunderStrength", IrisExclusiveUniforms::getThunderStrength);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHealth", IrisExclusiveUniforms::getCurrentHealth);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHealth", IrisExclusiveUniforms::getMaxHealth);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerHunger", IrisExclusiveUniforms::getCurrentHunger);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerHunger", () -> 20);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerArmor", IrisExclusiveUniforms::getCurrentArmor);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerArmor", () -> 50);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "currentPlayerAir", IrisExclusiveUniforms::getCurrentAir);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "maxPlayerAir", IrisExclusiveUniforms::getMaxAir);
        uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "firstPersonCamera", IrisExclusiveUniforms::isFirstPersonCamera);
        uniforms.uniform1b(UniformUpdateFrequency.PER_TICK, "isSpectator", IrisExclusiveUniforms::isSpectator);
        uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "currentSelectedBlockId", IrisExclusiveUniforms::getCurrentSelectedBlockId);
        uniforms.uniform3f(UniformUpdateFrequency.PER_FRAME, "currentSelectedBlockPos", IrisExclusiveUniforms::getCurrentSelectedBlockPos);
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "eyePosition", IrisExclusiveUniforms::getEyePosition);
        uniforms.uniform1f(UniformUpdateFrequency.PER_TICK, "cloudTime", CapturedRenderingState.INSTANCE::getCloudTime);
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "relativeEyePosition", () -> CameraUniforms.getUnshiftedCameraPosition().sub((Vector3dc)IrisExclusiveUniforms.getEyePosition()));
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "playerLookVector", () -> {
            class_1297 patt0$temp = class_310.method_1551().field_1719;
            if (patt0$temp instanceof class_1309) {
                class_1309 livingEntity = (class_1309)patt0$temp;
                return JomlConversions.fromVec3(livingEntity.method_5828(CapturedRenderingState.INSTANCE.getTickDelta()));
            }
            return ZERO;
        });
        uniforms.uniform3d(UniformUpdateFrequency.PER_FRAME, "playerBodyVector", () -> JomlConversions.fromVec3(class_310.method_1551().method_1560().method_5663()));
        Vector4f zero = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
        uniforms.uniform4f(UniformUpdateFrequency.PER_TICK, "lightningBoltPosition", () -> {
            if (class_310.method_1551().field_1687 != null) {
                return StreamSupport.stream(class_310.method_1551().field_1687.method_18112().spliterator(), false).filter(bolt -> bolt instanceof class_1538).findAny().map(bolt -> {
                    Vector3d unshiftedCameraPosition = CameraUniforms.getUnshiftedCameraPosition();
                    class_243 vec3 = bolt.method_30950(class_310.method_1551().method_60646().method_60637(true));
                    return new Vector4f((float)(vec3.field_1352 - unshiftedCameraPosition.x), (float)(vec3.field_1351 - unshiftedCameraPosition.y), (float)(vec3.field_1350 - unshiftedCameraPosition.z), 1.0f);
                }).orElse(zero);
            }
            return zero;
        });
    }

    private static int getCurrentSelectedBlockId() {
        class_2338 blockPos4;
        class_2680 blockState;
        class_239 hitResult = class_310.method_1551().field_1765;
        if (class_310.method_1551().field_1687 != null && ((GameRendererAccessor)class_310.method_1551().field_1773).shouldRenderBlockOutlineA() && hitResult != null && hitResult.method_17783() == class_239.class_240.field_1332 && !(blockState = class_310.method_1551().field_1687.method_8320(blockPos4 = ((class_3965)hitResult).method_17777())).method_26215() && class_310.method_1551().field_1687.method_8621().method_11952(blockPos4)) {
            return WorldRenderingSettings.INSTANCE.getBlockStateIds().getInt((Object)blockState);
        }
        return 0;
    }

    private static Vector3f getCurrentSelectedBlockPos() {
        class_239 hitResult = class_310.method_1551().field_1765;
        if (class_310.method_1551().field_1687 != null && ((GameRendererAccessor)class_310.method_1551().field_1773).shouldRenderBlockOutlineA() && hitResult != null && hitResult.method_17783() == class_239.class_240.field_1332) {
            class_2338 blockPos4 = ((class_3965)hitResult).method_17777();
            return blockPos4.method_46558().method_1020(class_310.method_1551().field_1773.method_19418().method_19326()).method_46409();
        }
        return new Vector3f(-256.0f);
    }

    private static float getThunderStrength() {
        return Math.clamp((float)0.0f, (float)1.0f, (float)class_310.method_1551().field_1687.method_8478(CapturedRenderingState.INSTANCE.getTickDelta()));
    }

    private static float getCurrentHealth() {
        if (class_310.method_1551().field_1724 == null || !class_310.method_1551().field_1761.method_2920().method_8388()) {
            return -1.0f;
        }
        return class_310.method_1551().field_1724.method_6032() / class_310.method_1551().field_1724.method_6063();
    }

    private static float getCurrentHunger() {
        if (class_310.method_1551().field_1724 == null || !class_310.method_1551().field_1761.method_2920().method_8388()) {
            return -1.0f;
        }
        return (float)class_310.method_1551().field_1724.method_7344().method_7586() / 20.0f;
    }

    private static float getCurrentAir() {
        if (class_310.method_1551().field_1724 == null || !class_310.method_1551().field_1761.method_2920().method_8388()) {
            return -1.0f;
        }
        return (float)class_310.method_1551().field_1724.method_5669() / (float)class_310.method_1551().field_1724.method_5748();
    }

    private static float getCurrentArmor() {
        if (class_310.method_1551().field_1724 == null || !class_310.method_1551().field_1761.method_2920().method_8388()) {
            return -1.0f;
        }
        return (float)class_310.method_1551().field_1724.method_6096() / 50.0f;
    }

    private static float getMaxAir() {
        if (class_310.method_1551().field_1724 == null || !class_310.method_1551().field_1761.method_2920().method_8388()) {
            return -1.0f;
        }
        return class_310.method_1551().field_1724.method_5748();
    }

    private static float getMaxHealth() {
        if (class_310.method_1551().field_1724 == null || !class_310.method_1551().field_1761.method_2920().method_8388()) {
            return -1.0f;
        }
        return class_310.method_1551().field_1724.method_6063();
    }

    private static boolean isFirstPersonCamera() {
        return switch (class_310.method_1551().field_1690.method_31044()) {
            case class_5498.field_26665, class_5498.field_26666 -> false;
            default -> true;
        };
    }

    private static boolean isSpectator() {
        return class_310.method_1551().field_1761.method_2920() == class_1934.field_9219;
    }

    private static Vector3d getEyePosition() {
        Objects.requireNonNull(class_310.method_1551().method_1560());
        class_243 pos = class_310.method_1551().method_1560().method_5836(CapturedRenderingState.INSTANCE.getTickDelta());
        return new Vector3d(pos.field_1352, pos.field_1351, pos.field_1350);
    }

    public static class WorldInfoUniforms {
        public static void addWorldInfoUniforms(UniformHolder uniforms) {
            class_638 level = class_310.method_1551().field_1687;
            uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "bedrockLevel", () -> {
                if (level != null) {
                    return level.method_8597().comp_651();
                }
                return 0;
            });
            uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "cloudHeight", () -> {
                if (level != null) {
                    return level.method_28103().method_28108();
                }
                return 192.0;
            });
            uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "heightLimit", () -> {
                if (level != null) {
                    return level.method_8597().comp_652();
                }
                return 256;
            });
            uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "logicalHeightLimit", () -> {
                if (level != null) {
                    return level.method_8597().comp_653();
                }
                return 256;
            });
            uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasCeiling", () -> {
                if (level != null) {
                    return level.method_8597().comp_643();
                }
                return false;
            });
            uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "hasSkylight", () -> {
                if (level != null) {
                    return level.method_8597().comp_642();
                }
                return true;
            });
            uniforms.uniform1f(UniformUpdateFrequency.PER_FRAME, "ambientLight", () -> {
                if (level != null) {
                    return level.method_8597().comp_656();
                }
                return 0.0f;
            });
        }
    }
}

