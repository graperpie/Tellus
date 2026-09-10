/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 *  net.minecraft.class_3532
 *  net.minecraft.class_4184
 *  net.minecraft.class_5636
 *  net.minecraft.class_761
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.minecraft.class_1297;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4184;
import net.minecraft.class_5636;
import net.minecraft.class_761;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_761.class})
public class MixinLevelRenderer_Sky {
    @Shadow
    @Final
    private class_310 field_4088;

    @Inject(method={"renderSky"}, at={@At(value="HEAD")}, cancellable=true)
    private void preRenderSky(Matrix4f matrix4f, Matrix4f matrix4f2, float f, class_4184 camera, boolean bl, Runnable runnable, CallbackInfo ci) {
        if (Iris.getCurrentPack().isEmpty()) {
            boolean useThickFog;
            class_243 cameraPosition = camera.method_19326();
            class_1297 cameraEntity = camera.method_19331();
            boolean isSubmersed = camera.method_19334() != class_5636.field_27888;
            boolean blockSky = ((LevelRendererAccessor)class_310.method_1551().field_1769).invokeDoesMobEffectBlockSky(camera);
            boolean bl2 = useThickFog = this.field_4088.field_1687.method_28103().method_28110(class_3532.method_15357((double)cameraPosition.method_10216()), class_3532.method_15357((double)cameraPosition.method_10214())) || this.field_4088.field_1705.method_1740().method_1800();
            if (isSubmersed || blockSky || useThickFog) {
                ci.cancel();
            }
        }
    }
}

