/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_340
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.batchedentityrendering.mixin;

import java.util.List;
import net.irisshaders.batchedentityrendering.impl.BatchingDebugMessageHelper;
import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.iris.Iris;
import net.minecraft.class_310;
import net.minecraft.class_340;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_340.class}, priority=1010)
public abstract class MixinDebugScreenOverlay {
    @Inject(method={"getGameInformation"}, at={@At(value="RETURN")})
    private void batchedentityrendering$appendStats(CallbackInfoReturnable<List<String>> cir) {
        List messages = (List)cir.getReturnValue();
        DrawCallTrackingRenderBuffers drawTracker = (DrawCallTrackingRenderBuffers)class_310.method_1551().method_22940();
        if (Iris.getIrisConfig().areDebugOptionsEnabled()) {
            messages.add("");
            messages.add("[Entity Batching] " + BatchingDebugMessageHelper.getDebugMessage(drawTracker));
        }
    }
}

