/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_4599
 *  net.minecraft.class_4618
 *  net.minecraft.class_750
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.batchedentityrendering.mixin;

import net.irisshaders.batchedentityrendering.impl.DrawCallTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingBuffer;
import net.irisshaders.batchedentityrendering.impl.MemoryTrackingRenderBuffers;
import net.irisshaders.batchedentityrendering.impl.RenderBuffersExt;
import net.irisshaders.batchedentityrendering.mixin.BufferSourceAccessor;
import net.irisshaders.batchedentityrendering.mixin.OutlineBufferSourceAccessor;
import net.irisshaders.batchedentityrendering.mixin.SectionBufferBuilderPackAccessor;
import net.minecraft.class_4597;
import net.minecraft.class_4599;
import net.minecraft.class_4618;
import net.minecraft.class_750;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_4599.class})
public class MixinRenderBuffers
implements RenderBuffersExt,
MemoryTrackingRenderBuffers,
DrawCallTrackingRenderBuffers {
    @Unique
    private final FullyBufferedMultiBufferSource buffered = new FullyBufferedMultiBufferSource();
    @Unique
    private final class_4618 outlineBufferSource = new class_4618((class_4597.class_4598)this.buffered);
    @Unique
    private int begins = 0;
    @Unique
    private int maxBegins = 0;
    @Shadow
    @Final
    private class_4597.class_4598 field_46901;
    @Shadow
    @Final
    private class_4597.class_4598 field_20959;
    @Shadow
    @Final
    private class_750 field_20956;

    @Inject(method={"bufferSource"}, at={@At(value="HEAD")}, cancellable=true)
    private void batchedentityrendering$replaceBufferSource(CallbackInfoReturnable<class_4597.class_4598> cir) {
        if (this.begins == 0) {
            return;
        }
        cir.setReturnValue((Object)this.buffered);
    }

    @Inject(method={"crumblingBufferSource"}, at={@At(value="HEAD")}, cancellable=true)
    private void batchedentityrendering$replaceCrumblingBufferSource(CallbackInfoReturnable<class_4597.class_4598> cir) {
        if (this.begins == 0) {
            return;
        }
        cir.setReturnValue((Object)this.buffered.getUnflushableWrapper());
    }

    @Inject(method={"outlineBufferSource"}, at={@At(value="HEAD")}, cancellable=true)
    private void batchedentityrendering$replaceOutlineBufferSource(CallbackInfoReturnable<class_4618> provider) {
        if (this.begins == 0) {
            return;
        }
        provider.setReturnValue((Object)this.outlineBufferSource);
    }

    @Override
    public void beginLevelRendering() {
        if (this.begins == 0) {
            this.buffered.assertWrapStackEmpty();
        }
        ++this.begins;
        this.maxBegins = Math.max(this.begins, this.maxBegins);
    }

    @Override
    public void endLevelRendering() {
        --this.begins;
        if (this.begins == 0) {
            this.buffered.assertWrapStackEmpty();
        }
    }

    @Override
    public long getEntityBufferAllocatedSize() {
        return this.buffered.getAllocatedSize();
    }

    @Override
    public long getMiscBufferAllocatedSize() {
        return ((MemoryTrackingBuffer)this.field_46901).getAllocatedSize();
    }

    @Override
    public int getMaxBegins() {
        return this.maxBegins;
    }

    @Override
    public void freeAndDeleteBuffers() {
        this.buffered.freeAndDeleteBuffer();
        ((SectionBufferBuilderPackAccessor)this.field_20956).getBuffers().values().forEach(bufferBuilder -> ((MemoryTrackingBuffer)bufferBuilder).freeAndDeleteBuffer());
        ((BufferSourceAccessor)this.field_46901).getFixedBuffers().forEach((renderType, bufferBuilder) -> ((MemoryTrackingBuffer)bufferBuilder).freeAndDeleteBuffer());
        ((BufferSourceAccessor)this.field_46901).getFixedBuffers().clear();
        ((MemoryTrackingBuffer)((OutlineBufferSourceAccessor)this.outlineBufferSource).getOutlineBufferSource()).freeAndDeleteBuffer();
    }

    @Override
    public int getDrawCalls() {
        return this.buffered.getDrawCalls();
    }

    @Override
    public int getRenderTypes() {
        return this.buffered.getRenderTypes();
    }

    @Override
    public void resetDrawCounts() {
        this.buffered.resetDrawCalls();
    }
}

