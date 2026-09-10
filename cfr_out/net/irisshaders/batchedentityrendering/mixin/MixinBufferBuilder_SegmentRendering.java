/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics
 *  net.minecraft.class_287
 *  net.minecraft.class_293
 *  net.minecraft.class_9799
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.batchedentityrendering.mixin;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.irisshaders.batchedentityrendering.impl.BufferBuilderExt;
import net.minecraft.class_287;
import net.minecraft.class_293;
import net.minecraft.class_9799;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_287.class}, priority=1010)
public class MixinBufferBuilder_SegmentRendering
implements BufferBuilderExt {
    @Final
    @Shadow
    private class_9799 field_52071;
    @Final
    @Shadow
    private class_293 field_1565;
    @Shadow
    private int field_1554;
    @Shadow
    @Final
    private int field_52074;
    @Unique
    private boolean dupeNextVertex;
    @Unique
    private boolean dupeNextVertexAfter;

    @Override
    public void splitStrip() {
        if (this.field_1554 == 0) {
            return;
        }
        this.duplicateLastVertex();
        this.dupeNextVertexAfter = true;
        this.dupeNextVertex = false;
    }

    @Unique
    private void duplicateLastVertex() {
        long l = this.field_52071.method_60808(this.field_52074);
        MemoryIntrinsics.copyMemory((long)(l - (long)this.field_52074), (long)l, (int)this.field_52074);
        ++this.field_1554;
    }

    @Inject(method={"endLastVertex"}, at={@At(value="RETURN")})
    private void batchedentityrendering$onNext(CallbackInfo ci) {
        if (this.dupeNextVertexAfter) {
            this.dupeNextVertexAfter = false;
            this.dupeNextVertex = true;
            return;
        }
        if (this.dupeNextVertex) {
            this.dupeNextVertex = false;
            this.duplicateLastVertex();
        }
    }
}

