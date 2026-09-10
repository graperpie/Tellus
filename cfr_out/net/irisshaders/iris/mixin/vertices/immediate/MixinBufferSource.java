/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  net.minecraft.class_1921
 *  net.minecraft.class_287
 *  net.minecraft.class_293
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_9799
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.vertices.immediate;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.class_1921;
import net.minecraft.class_287;
import net.minecraft.class_293;
import net.minecraft.class_4597;
import net.minecraft.class_9799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_4597.class_4598.class})
public class MixinBufferSource {
    @WrapOperation(method={"getBuffer"}, at={@At(value="NEW", target="(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)Lcom/mojang/blaze3d/vertex/BufferBuilder;")})
    private class_287 iris$redirectBegin(class_9799 byteBufferBuilder, class_293.class_5596 mode, class_293 vertexFormat, Operation<class_287> original) {
        ImmediateState.skipExtension.set(this.iris$notRenderingLevel());
        class_287 builder = (class_287)original.call(new Object[]{byteBufferBuilder, mode, vertexFormat});
        ImmediateState.skipExtension.set(false);
        return builder;
    }

    @Inject(method={"endBatch(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/RenderType;draw(Lcom/mojang/blaze3d/vertex/MeshData;)V")})
    private void iris$beforeFlushBuffer(class_1921 renderType, class_287 bufferBuilder, CallbackInfo ci) {
        if (this.iris$notRenderingLevel()) {
            ImmediateState.renderWithExtendedVertexFormat = false;
        }
    }

    @Inject(method={"endBatch(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/RenderType;draw(Lcom/mojang/blaze3d/vertex/MeshData;)V", shift=At.Shift.AFTER)})
    private void iris$afterFlushBuffer(class_1921 renderType, class_287 bufferBuilder, CallbackInfo ci) {
        if (this.iris$notRenderingLevel()) {
            ImmediateState.renderWithExtendedVertexFormat = true;
        }
    }

    @Unique
    private boolean iris$notRenderingLevel() {
        return !ImmediateState.isRenderingLevel;
    }
}

