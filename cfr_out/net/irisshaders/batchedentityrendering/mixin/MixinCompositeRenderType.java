/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_1921$class_4687
 *  net.minecraft.class_1921$class_4688
 *  net.minecraft.class_293
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_4668
 *  net.minecraft.class_4668$class_4672
 *  net.minecraft.class_4668$class_4685
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.batchedentityrendering.mixin;

import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.mixin.CompositeStateAccessor;
import net.irisshaders.batchedentityrendering.mixin.RenderStateShardAccessor;
import net.minecraft.class_1921;
import net.minecraft.class_293;
import net.minecraft.class_4668;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1921.class_4687.class})
public abstract class MixinCompositeRenderType
extends class_1921
implements BlendingStateHolder {
    @Unique
    private static final String INIT = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;IZZLnet/minecraft/client/renderer/RenderType$CompositeState;)V";
    @Unique
    private TransparencyType transparencyType;

    private MixinCompositeRenderType(String name, class_293 vertexFormat, class_293.class_5596 drawMode, int expectedBufferSize, boolean hasCrumbling, boolean translucent, Runnable startAction, Runnable endAction) {
        super(name, vertexFormat, drawMode, expectedBufferSize, hasCrumbling, translucent, startAction, endAction);
    }

    @Inject(method={"<init>(Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;IZZLnet/minecraft/client/renderer/RenderType$CompositeState;)V"}, at={@At(value="RETURN")})
    private void batchedentityrendering$onCompositeInit(String string, class_293 vertexFormat, class_293.class_5596 mode, int i, boolean bl, boolean bl2, class_1921.class_4688 compositeState, CallbackInfo ci) {
        class_4668.class_4685 transparency = ((CompositeStateAccessor)compositeState).getTransparency();
        class_4668.class_4672 depth = ((CompositeStateAccessor)compositeState).getDepth();
        this.transparencyType = "water_mask".equals(this.field_21363) || depth == class_4668.field_21346 ? TransparencyType.WATER_MASK : ("lines".equals(this.field_21363) ? TransparencyType.LINES : (transparency == RenderStateShardAccessor.getNO_TRANSPARENCY() ? TransparencyType.OPAQUE : (transparency == RenderStateShardAccessor.getGLINT_TRANSPARENCY() || transparency == RenderStateShardAccessor.getCRUMBLING_TRANSPARENCY() ? TransparencyType.DECAL : TransparencyType.GENERAL_TRANSPARENT)));
    }

    @Override
    public TransparencyType getTransparencyType() {
        return this.transparencyType;
    }

    @Override
    public void setTransparencyType(TransparencyType type) {
        this.transparencyType = type;
    }
}

