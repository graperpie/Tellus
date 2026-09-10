/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_290
 *  net.minecraft.class_293
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.vertices;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.vertices.ImmediateState;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.class_290;
import net.minecraft.class_293;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_293.class})
public class MixinVertexFormat {
    @Inject(method={"setupBufferState"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$onSetupBufferState(CallbackInfo ci) {
        if (Iris.isPackInUseQuick() && ImmediateState.renderWithExtendedVertexFormat) {
            if (this == class_290.field_1590) {
                IrisVertexFormats.TERRAIN.method_22649();
                ci.cancel();
            } else if (this == class_290.field_20888) {
                IrisVertexFormats.GLYPH.method_22649();
                ci.cancel();
            } else if (this == class_290.field_1580) {
                IrisVertexFormats.ENTITY.method_22649();
                ci.cancel();
            }
        }
    }

    @Inject(method={"clearBufferState"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$onClearBufferState(CallbackInfo ci) {
        if (Iris.isPackInUseQuick() && ImmediateState.renderWithExtendedVertexFormat) {
            if (this == class_290.field_1590) {
                IrisVertexFormats.TERRAIN.method_22651();
                ci.cancel();
            } else if (this == class_290.field_20888) {
                IrisVertexFormats.GLYPH.method_22651();
                ci.cancel();
            } else if (this == class_290.field_1580) {
                IrisVertexFormats.ENTITY.method_22651();
                ci.cancel();
            }
        }
    }
}

