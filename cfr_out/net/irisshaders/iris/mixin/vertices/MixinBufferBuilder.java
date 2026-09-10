/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_287
 *  net.minecraft.class_290
 *  net.minecraft.class_293
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_296
 *  net.minecraft.class_4588
 *  net.minecraft.class_9799
 *  org.joml.Vector3f
 *  org.lwjgl.system.MemoryUtil
 *  org.spongepowered.asm.mixin.Dynamic
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin.vertices;

import java.util.Arrays;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.BufferBuilderPolygonView;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.irisshaders.iris.vertices.ImmediateState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.irisshaders.iris.vertices.MojangBufferAccessor;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import net.minecraft.class_287;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_296;
import net.minecraft.class_4588;
import net.minecraft.class_9799;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_287.class})
public abstract class MixinBufferBuilder
implements class_4588,
BlockSensitiveBufferBuilder {
    @Unique
    private final BufferBuilderPolygonView polygon = new BufferBuilderPolygonView();
    @Unique
    private final Vector3f normal = new Vector3f();
    @Unique
    private final long[] vertexOffsets = new long[4];
    @Shadow
    private int field_52077;
    @Unique
    private boolean skipEndVertexOnce;
    @Shadow
    @Final
    private class_293.class_5596 field_52073;
    @Shadow
    @Final
    private class_293 field_1565;
    @Shadow
    @Final
    private int[] field_52076;
    @Shadow
    @Final
    private boolean field_21594;
    @Shadow
    private long field_52072;
    @Shadow
    private int field_1554;
    @Unique
    private boolean extending;
    @Unique
    private boolean injectNormalAndUV1;
    @Unique
    private int iris$vertexCount;
    @Unique
    private int currentBlock = -1;
    @Unique
    private byte currentRenderType = (byte)-1;
    @Unique
    private int currentLocalPosX;
    @Unique
    private int currentLocalPosY;
    @Unique
    private int currentLocalPosZ;
    @Shadow
    @Final
    private class_9799 field_52071;

    @Shadow
    public abstract class_4588 method_22914(float var1, float var2, float var3);

    @Shadow
    protected abstract long method_60798(class_296 var1);

    @ModifyVariable(method={"<init>"}, at=@At(value="FIELD", target="Lcom/mojang/blaze3d/vertex/VertexFormatElement;POSITION:Lcom/mojang/blaze3d/vertex/VertexFormatElement;", ordinal=0), argsOnly=true)
    private class_293 iris$extendFormat(class_293 format) {
        boolean iris$isTerrain = false;
        this.injectNormalAndUV1 = false;
        if (ImmediateState.skipExtension.get().booleanValue() || !Iris.isPackInUseQuick()) {
            return format;
        }
        if (format == class_290.field_1590 || format == IrisVertexFormats.TERRAIN) {
            this.extending = true;
            iris$isTerrain = true;
            this.injectNormalAndUV1 = false;
            return IrisVertexFormats.TERRAIN;
        }
        if (format == class_290.field_1580 || format == IrisVertexFormats.ENTITY) {
            this.extending = true;
            iris$isTerrain = false;
            this.injectNormalAndUV1 = false;
            return IrisVertexFormats.ENTITY;
        }
        if (format == class_290.field_20888 || format == IrisVertexFormats.GLYPH) {
            this.extending = true;
            iris$isTerrain = false;
            this.injectNormalAndUV1 = true;
            return IrisVertexFormats.GLYPH;
        }
        return format;
    }

    @Redirect(method={"addVertex(FFFIFFIIFFF)V"}, at=@At(value="FIELD", target="Lcom/mojang/blaze3d/vertex/BufferBuilder;fastFormat:Z"))
    private boolean fastFormat(class_287 instance) {
        return this.field_21594 && !this.extending;
    }

    @Inject(method={"addVertex(FFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"}, at={@At(value="RETURN")})
    private void injectMidBlock(float x, float y, float z, CallbackInfoReturnable<class_4588> cir) {
        if ((this.field_52077 & IrisVertexFormats.MID_BLOCK_ELEMENT.method_60843()) != 0) {
            long midBlockOffset = this.method_60798(IrisVertexFormats.MID_BLOCK_ELEMENT);
            MemoryUtil.memPutInt((long)midBlockOffset, (int)ExtendedDataHelper.computeMidBlock(x, y, z, this.currentLocalPosX, this.currentLocalPosY, this.currentLocalPosZ));
            byte currentBlockEmission = -1;
            MemoryUtil.memPutByte((long)(midBlockOffset + 3L), (byte)currentBlockEmission);
        }
        if ((this.field_52077 & IrisVertexFormats.ENTITY_ELEMENT.method_60843()) != 0) {
            offset = this.method_60798(IrisVertexFormats.ENTITY_ELEMENT);
            MemoryUtil.memPutShort((long)offset, (short)((short)this.currentBlock));
            MemoryUtil.memPutShort((long)(offset + 2L), (short)this.currentRenderType);
        } else if ((this.field_52077 & IrisVertexFormats.ENTITY_ID_ELEMENT.method_60843()) != 0) {
            offset = this.method_60798(IrisVertexFormats.ENTITY_ID_ELEMENT);
            MemoryUtil.memPutShort((long)offset, (short)((short)CapturedRenderingState.INSTANCE.getCurrentRenderedEntity()));
            MemoryUtil.memPutShort((long)(offset + 2L), (short)((short)CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity()));
            MemoryUtil.memPutShort((long)(offset + 4L), (short)((short)CapturedRenderingState.INSTANCE.getCurrentRenderedItem()));
        }
    }

    @Inject(method={"push"}, at={@At(value="TAIL")}, remap=false, require=0)
    @Dynamic(value="Used to skip endLastVertex if the last push was made by Sodium")
    private void iris$skipSodiumChange(CallbackInfo ci) {
        this.skipEndVertexOnce = true;
    }

    @Inject(method={"endLastVertex"}, at={@At(value="HEAD")})
    private void iris$beforeNext(CallbackInfo ci) {
        if (this.field_1554 == 0 || !this.extending) {
            return;
        }
        this.field_52077 &= ~IrisVertexFormats.MID_TEXTURE_ELEMENT.method_60843();
        this.field_52077 &= ~IrisVertexFormats.TANGENT_ELEMENT.method_60843();
        if (this.injectNormalAndUV1 && this.field_52077 != (this.field_52077 & ~class_296.field_52113.method_60843())) {
            this.method_22914(0.0f, 1.0f, 0.0f);
        }
        if (this.skipEndVertexOnce) {
            this.skipEndVertexOnce = false;
            return;
        }
        if (this.field_52073 != class_293.class_5596.field_27382 && this.field_52073 != class_293.class_5596.field_27379) {
            return;
        }
        this.vertexOffsets[this.iris$vertexCount] = this.field_52072 - ((MojangBufferAccessor)this.field_52071).getPointer();
        ++this.iris$vertexCount;
        if (this.field_52073 == class_293.class_5596.field_27382 && this.iris$vertexCount == 4 || this.field_52073 == class_293.class_5596.field_27379 && this.iris$vertexCount == 3) {
            this.fillExtendedData(this.iris$vertexCount);
        }
    }

    @Override
    public void beginBlock(int block, byte renderType, byte blockEmission, int localPosX, int localPosY, int localPosZ) {
        this.currentBlock = block;
        this.currentRenderType = renderType;
        this.currentLocalPosX = localPosX;
        this.currentLocalPosY = localPosY;
        this.currentLocalPosZ = localPosZ;
    }

    @Override
    public void endBlock() {
        this.currentBlock = -1;
        this.currentRenderType = (byte)-1;
        this.currentLocalPosX = 0;
        this.currentLocalPosY = 0;
        this.currentLocalPosZ = 0;
    }

    @Unique
    private void fillExtendedData(int vertexAmount) {
        this.iris$vertexCount = 0;
        int stride = this.field_1565.method_1362();
        this.polygon.setup(((MojangBufferAccessor)this.field_52071).getPointer(), this.vertexOffsets, stride, vertexAmount);
        float midU = 0.0f;
        float midV = 0.0f;
        for (int vertex = 0; vertex < vertexAmount; ++vertex) {
            midU += this.polygon.u(vertex);
            midV += this.polygon.v(vertex);
        }
        midU /= (float)vertexAmount;
        midV /= (float)vertexAmount;
        int midTexOffset = this.field_52076[IrisVertexFormats.MID_TEXTURE_ELEMENT.comp_2842()];
        int normalOffset = this.field_52076[class_296.field_52113.comp_2842()];
        int tangentOffset = this.field_52076[IrisVertexFormats.TANGENT_ELEMENT.comp_2842()];
        if (vertexAmount == 3) {
            for (int vertex = 0; vertex < vertexAmount; ++vertex) {
                long newPointer = ((MojangBufferAccessor)this.field_52071).getPointer() + this.vertexOffsets[vertex];
                int vertexNormal = MemoryUtil.memGetInt((long)(newPointer + (long)normalOffset));
                int tangent = NormalHelper.computeTangentSmooth(NormI8.unpackX(vertexNormal), NormI8.unpackY(vertexNormal), NormI8.unpackZ(vertexNormal), this.polygon);
                MemoryUtil.memPutFloat((long)(newPointer + (long)midTexOffset), (float)midU);
                MemoryUtil.memPutFloat((long)(newPointer + (long)midTexOffset + 4L), (float)midV);
                MemoryUtil.memPutInt((long)(newPointer + (long)tangentOffset), (int)tangent);
            }
        } else {
            boolean recalculateNormal = ImmediateState.isRenderingLevel;
            NormalHelper.computeFaceNormal(this.normal, this.polygon);
            int packedNormal = 0;
            if (recalculateNormal) {
                packedNormal = NormI8.pack(this.normal.x, this.normal.y, this.normal.z, 0.0f);
            }
            int tangent = NormalHelper.computeTangent(this.normal.x, this.normal.y, this.normal.z, this.polygon);
            for (int vertex = 0; vertex < vertexAmount; ++vertex) {
                long newPointer = ((MojangBufferAccessor)this.field_52071).getPointer() + this.vertexOffsets[vertex];
                MemoryUtil.memPutFloat((long)(newPointer + (long)midTexOffset), (float)midU);
                MemoryUtil.memPutFloat((long)(newPointer + (long)midTexOffset + 4L), (float)midV);
                if (recalculateNormal) {
                    MemoryUtil.memPutInt((long)(newPointer + (long)normalOffset), (int)packedNormal);
                }
                MemoryUtil.memPutInt((long)(newPointer + (long)tangentOffset), (int)tangent);
            }
        }
        Arrays.fill(this.vertexOffsets, 0L);
    }
}

