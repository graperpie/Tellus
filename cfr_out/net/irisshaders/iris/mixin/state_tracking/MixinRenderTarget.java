/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_276
 *  net.minecraft.class_310
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.state_tracking;

import net.irisshaders.iris.Iris;
import net.minecraft.class_276;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_276.class})
public class MixinRenderTarget {
    @Inject(method={"bindWrite(Z)V"}, at={@At(value="RETURN")})
    private void iris$onBindFramebuffer(boolean bl, CallbackInfo ci) {
        boolean mainBound = this == class_310.method_1551().method_1522();
        Iris.getPipelineManager().getPipeline().ifPresent(pipeline -> pipeline.setIsMainBound(mainBound));
    }
}

