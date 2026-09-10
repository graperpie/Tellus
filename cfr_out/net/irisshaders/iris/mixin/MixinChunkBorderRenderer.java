/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.minecraft.class_4588
 *  net.minecraft.class_862
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.Slice
 */
package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.class_4588;
import net.minecraft.class_862;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value={class_862.class})
public class MixinChunkBorderRenderer {
    private class_4588 fakeConsumer = new class_4588(this){

        public class_4588 method_22912(float x, float y, float z) {
            return this;
        }

        public class_4588 method_1336(int red, int green, int blue, int alpha) {
            return this;
        }

        public class_4588 method_22913(float u, float v) {
            return this;
        }

        public class_4588 method_60796(int u, int v) {
            return this;
        }

        public class_4588 method_22921(int u, int v) {
            return this;
        }

        public class_4588 method_22914(float normalX, float normalY, float normalZ) {
            return this;
        }
    };

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), slice=@Slice(from=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/PoseStack$Pose;pose()Lorg/joml/Matrix4f;"), to=@At(value="FIELD", target="Lnet/minecraft/client/renderer/debug/ChunkBorderRenderer;CELL_BORDER:I", ordinal=0)))
    private class_4588 isCameraChunk(class_4588 instance, Matrix4f pose, float x, float y, float z, @Local(ordinal=0) int k, @Local(ordinal=1) int l) {
        if (k != 0 && k != 16 || l != 0 && l != 16) {
            return instance.method_22918(pose, x, y, z);
        }
        return this.fakeConsumer;
    }

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(Lorg/joml/Matrix4f;FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), slice=@Slice(from=@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel;getMinBuildHeight()I", ordinal=1), to=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer(Lnet/minecraft/client/renderer/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal=1)))
    private class_4588 isSubChunkBorder(class_4588 instance, Matrix4f pose, float x, float y, float z, @Local(ordinal=0) int k) {
        if (k % 16 != 0) {
            return instance.method_22918(pose, x, y, z);
        }
        return this.fakeConsumer;
    }
}

