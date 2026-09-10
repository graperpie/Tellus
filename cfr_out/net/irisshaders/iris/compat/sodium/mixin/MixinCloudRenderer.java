/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  net.caffeinemc.mods.sodium.api.util.NormI8
 *  net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter
 *  net.caffeinemc.mods.sodium.api.vertex.format.common.ColorVertex
 *  net.caffeinemc.mods.sodium.client.render.immediate.CloudRenderer
 *  net.caffeinemc.mods.sodium.client.render.immediate.CloudRenderer$CloudGeometry
 *  net.minecraft.class_293
 *  net.minecraft.class_5944
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.compat.sodium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ColorVertex;
import net.caffeinemc.mods.sodium.client.render.immediate.CloudRenderer;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.sodium.CloudVertex;
import net.minecraft.class_293;
import net.minecraft.class_5944;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={CloudRenderer.class})
public abstract class MixinCloudRenderer {
    @Unique
    private static final int[] NORMALS = new int[]{NormI8.pack((float)0.0f, (float)-1.0f, (float)0.0f), NormI8.pack((float)0.0f, (float)1.0f, (float)0.0f), NormI8.pack((float)-1.0f, (float)0.0f, (float)0.0f), NormI8.pack((float)1.0f, (float)0.0f, (float)0.0f), NormI8.pack((float)0.0f, (float)0.0f, (float)-1.0f), NormI8.pack((float)0.0f, (float)0.0f, (float)1.0f)};
    @Unique
    private static int computedNormal;
    @Shadow(remap=false)
    @Nullable
    private // Could not load outer class - annotation placement on inner may be incorrect
    @Nullable @Nullable CloudRenderer.CloudGeometry builtGeometry;
    @Unique
    private static boolean hadShadersOn;
    private static final int FACE_MASK_NEG_Y = 1;
    private static final int FACE_MASK_POS_Y = 2;
    private static final int FACE_MASK_NEG_X = 4;
    private static final int FACE_MASK_POS_X = 8;
    private static final int FACE_MASK_NEG_Z = 16;
    private static final int FACE_MASK_POS_Z = 32;

    @Inject(method={"writeVertex"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private static void writeIrisVertex(long buffer, float x, float y, float z, int color, CallbackInfoReturnable<Long> cir) {
        if (IrisApi.getInstance().isShaderPackInUse()) {
            CloudVertex.put(buffer, x, y, z, color, computedNormal);
            cir.setReturnValue((Object)(buffer + 20L));
        }
    }

    @Inject(method={"emitCellGeometryFlat"}, at={@At(value="HEAD")}, remap=false)
    private static void computeNormal2D(VertexBufferWriter writer, int texel, int x, int z, CallbackInfo ci) {
        computedNormal = NORMALS[0];
    }

    @Inject(method={"emitCellGeometryExterior"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/api/util/ColorABGR;mulRGB(II)I", ordinal=0)}, remap=false)
    private static void computeNormal3D(VertexBufferWriter writer, int cellFaces, int cellColor, int cellX, int cellZ, CallbackInfo ci) {
        computedNormal = NORMALS[0];
    }

    @Inject(method={"emitCellGeometryExterior"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/api/util/ColorABGR;mulRGB(II)I", ordinal=1)}, remap=false)
    private static void computeNormal3DUp(VertexBufferWriter writer, int cellFaces, int cellColor, int cellX, int cellZ, CallbackInfo ci) {
        computedNormal = NORMALS[1];
    }

    @Inject(method={"emitCellGeometryExterior"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/immediate/CloudRenderer;writeVertex(JFFFI)J", ordinal=8, remap=false)}, remap=false)
    private static void computeNormal3DNegX(VertexBufferWriter writer, int cellFaces, int cellColor, int cellX, int cellZ, CallbackInfo ci) {
        computedNormal = NORMALS[2];
    }

    @Inject(remap=false, method={"emitCellGeometryExterior"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/immediate/CloudRenderer;writeVertex(JFFFI)J", ordinal=12, remap=false)})
    private static void computeNormal3DPosX(VertexBufferWriter writer, int cellFaces, int cellColor, int cellX, int cellZ, CallbackInfo ci) {
        computedNormal = NORMALS[3];
    }

    @Inject(method={"emitCellGeometryExterior"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/immediate/CloudRenderer;writeVertex(JFFFI)J", ordinal=16)}, remap=false)
    private static void computeNormal3DNegZ(VertexBufferWriter writer, int cellFaces, int cellColor, int cellX, int cellZ, CallbackInfo ci) {
        computedNormal = NORMALS[4];
    }

    @Inject(method={"emitCellGeometryExterior"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/immediate/CloudRenderer;writeVertex(JFFFI)J", ordinal=20)}, remap=false)
    private static void computeNormal3DPosZ(VertexBufferWriter writer, int cellFaces, int cellColor, int cellX, int cellZ, CallbackInfo ci) {
        computedNormal = NORMALS[5];
    }

    @ModifyArg(remap=false, method={"emitCellGeometryExterior"}, at=@At(value="INVOKE", target="Lorg/lwjgl/system/MemoryStack;nmalloc(I)J"))
    private static int allocateNewSize(int size) {
        return IrisApi.getInstance().isShaderPackInUse() ? 480 : size;
    }

    @ModifyArg(remap=false, method={"emitCellGeometryInterior"}, at=@At(value="INVOKE", target="Lorg/lwjgl/system/MemoryStack;nmalloc(I)J"))
    private static int allocateNewSizeInt(int size) {
        return IrisApi.getInstance().isShaderPackInUse() ? 480 : size;
    }

    @ModifyArg(method={"rebuildGeometry"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/Tesselator;begin(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"), index=1)
    private static class_293 rebuild(class_293 p_350837_) {
        return IrisApi.getInstance().isShaderPackInUse() ? IrisVertexFormats.CLOUDS : p_350837_;
    }

    @ModifyArg(method={"emitCellGeometryExterior"}, at=@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/api/vertex/buffer/VertexBufferWriter;push(Lorg/lwjgl/system/MemoryStack;JILcom/mojang/blaze3d/vertex/VertexFormat;)V"), index=3)
    private static class_293 modifyArgIris(class_293 vertexFormatDescription) {
        if (IrisApi.getInstance().isShaderPackInUse()) {
            return IrisVertexFormats.CLOUDS;
        }
        return ColorVertex.FORMAT;
    }

    @ModifyArg(method={"emitCellGeometryInterior"}, at=@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/api/vertex/buffer/VertexBufferWriter;push(Lorg/lwjgl/system/MemoryStack;JILcom/mojang/blaze3d/vertex/VertexFormat;)V"), index=3)
    private static class_293 modifyArgIrisInt(class_293 vertexFormatDescription) {
        if (IrisApi.getInstance().isShaderPackInUse()) {
            return IrisVertexFormats.CLOUDS;
        }
        return ColorVertex.FORMAT;
    }

    @ModifyArg(remap=false, method={"emitCellGeometryFlat"}, at=@At(value="INVOKE", target="Lorg/lwjgl/system/MemoryStack;nmalloc(I)J"))
    private static int allocateNewSize2D(int size) {
        return IrisApi.getInstance().isShaderPackInUse() ? 80 : size;
    }

    @ModifyArg(method={"emitCellGeometryFlat"}, at=@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/api/vertex/buffer/VertexBufferWriter;push(Lorg/lwjgl/system/MemoryStack;JILcom/mojang/blaze3d/vertex/VertexFormat;)V"), index=3)
    private static class_293 modifyArgIris2D(class_293 vertexFormatDescription) {
        if (IrisApi.getInstance().isShaderPackInUse()) {
            return IrisVertexFormats.CLOUDS;
        }
        return ColorVertex.FORMAT;
    }

    @WrapOperation(method={"render"}, at={@At(remap=false, value="INVOKE", target="Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z")})
    private boolean changeGeometry(Object a, Object b, Operation<Boolean> original) {
        return hadShadersOn == Iris.isPackInUseQuick() && (Boolean)original.call(new Object[]{a, b}) != false;
    }

    @ModifyArg(method={"render"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V"), index=2)
    private class_5944 iris$changeProgram(class_5944 p_253993_) {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline)pipeline).getShaderMap().getShader(ShaderKey.CLOUDS_SODIUM);
        }
        return p_253993_;
    }

    @Inject(method={"rebuildGeometry"}, at={@At(remap=false, value="HEAD")}, remap=false)
    private static void changeGeometry2(CallbackInfoReturnable<CloudRenderer.CloudGeometry> cir) {
        hadShadersOn = IrisApi.getInstance().isShaderPackInUse();
    }

    static {
        hadShadersOn = false;
    }
}

