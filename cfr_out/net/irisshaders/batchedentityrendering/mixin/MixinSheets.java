/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_4722
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.batchedentityrendering.mixin;

import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.minecraft.class_1921;
import net.minecraft.class_4722;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_4722.class})
public class MixinSheets {
    @Shadow
    @Final
    private static class_1921 field_42070;

    @Inject(method={"<clinit>"}, at={@At(value="TAIL")})
    private static void setSheet(CallbackInfo ci) {
        ((BlendingStateHolder)field_42070).setTransparencyType(TransparencyType.OPAQUE_DECAL);
        ((BlendingStateHolder)class_1921.method_49045()).setTransparencyType(TransparencyType.OPAQUE);
        ((BlendingStateHolder)class_1921.method_49046()).setTransparencyType(TransparencyType.OPAQUE);
        ((BlendingStateHolder)class_1921.method_61157()).setTransparencyType(TransparencyType.DECAL);
    }
}

