/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4184
 *  net.minecraft.class_757
 *  net.minecraft.class_761
 *  net.minecraft.class_765
 *  net.minecraft.class_9779
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.vertices.immediate;

import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.class_4184;
import net.minecraft.class_757;
import net.minecraft.class_761;
import net.minecraft.class_765;
import net.minecraft.class_9779;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_761.class}, priority=999)
public class MixinLevelRenderer {
    @Inject(method={"renderLevel"}, at={@At(value="HEAD")})
    private void iris$immediateStateBeginLevelRender(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        ImmediateState.isRenderingLevel = true;
    }

    @Inject(method={"renderLevel"}, at={@At(value="RETURN")})
    private void iris$immediateStateEndLevelRender(class_9779 deltaTracker, boolean bl, class_4184 camera, class_757 gameRenderer, class_765 lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        ImmediateState.isRenderingLevel = false;
    }
}

