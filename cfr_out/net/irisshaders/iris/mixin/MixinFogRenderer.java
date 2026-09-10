/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_4184
 *  net.minecraft.class_5636
 *  net.minecraft.class_638
 *  net.minecraft.class_6880
 *  net.minecraft.class_6908
 *  net.minecraft.class_746
 *  net.minecraft.class_758
 *  net.minecraft.class_758$class_4596
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_1297;
import net.minecraft.class_4184;
import net.minecraft.class_5636;
import net.minecraft.class_638;
import net.minecraft.class_6880;
import net.minecraft.class_6908;
import net.minecraft.class_746;
import net.minecraft.class_758;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_758.class})
public class MixinFogRenderer {
    @Shadow
    private static float field_4034;
    @Shadow
    private static float field_4033;
    @Shadow
    private static float field_4032;

    @Inject(method={"setupFog"}, at={@At(value="HEAD")})
    private static void iris$setupLegacyWaterFog(class_4184 camera, class_758.class_4596 $$1, float $$2, boolean $$3, float $$4, CallbackInfo ci) {
        if (camera.method_19334() == class_5636.field_27886) {
            class_1297 entity = camera.method_19331();
            float density = 0.05f;
            if (entity instanceof class_746) {
                class_746 localPlayer = (class_746)entity;
                density -= localPlayer.method_3140() * localPlayer.method_3140() * 0.03f;
                class_6880 biome = localPlayer.method_37908().method_23753(localPlayer.method_24515());
                if (biome.method_40220(class_6908.field_37378)) {
                    density += 0.005f;
                }
            }
            CapturedRenderingState.INSTANCE.setFogDensity(density);
        } else {
            CapturedRenderingState.INSTANCE.setFogDensity(-1.0f);
        }
    }

    @Inject(method={"setupColor"}, at={@At(value="TAIL")})
    private static void render(class_4184 camera, float tickDelta, class_638 level, int i, float f, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setFogColor(field_4034, field_4033, field_4032);
    }
}

