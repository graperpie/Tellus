/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1267
 *  net.minecraft.class_1928
 *  net.minecraft.class_1934
 *  net.minecraft.class_1940
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_442
 *  net.minecraft.class_5285
 *  net.minecraft.class_5317
 *  net.minecraft.class_7712
 *  net.minecraft.class_8496
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.minecraft.class_1267;
import net.minecraft.class_1928;
import net.minecraft.class_1934;
import net.minecraft.class_1940;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_442;
import net.minecraft.class_5285;
import net.minecraft.class_5317;
import net.minecraft.class_7712;
import net.minecraft.class_8496;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_8496.class})
public class MixinQuickPlayDev {
    @Inject(method={"joinSingleplayerWorld"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$createWorldIfDev(class_310 minecraft, String string, CallbackInfo ci) {
        if (IrisPlatformHelpers.getInstance().isDevelopmentEnvironment()) {
            ci.cancel();
            if (!minecraft.method_1586().method_230(string)) {
                minecraft.method_41735().method_41895(string, new class_1940(string, class_1934.field_9220, false, class_1267.field_5807, true, new class_1928(), class_7712.field_40260), class_5285.method_45541(), class_5317::method_41598, class_310.method_1551().field_1755);
            } else {
                minecraft.method_41735().method_57784(string, () -> minecraft.method_1507((class_437)new class_442()));
            }
        }
    }
}

