/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1087
 *  net.minecraft.class_1747
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_2960
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_5634
 *  net.minecraft.class_7923
 *  net.minecraft.class_811
 *  net.minecraft.class_918
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.entity_render_context;

import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_1087;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_5634;
import net.minecraft.class_7923;
import net.minecraft.class_811;
import net.minecraft.class_918;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_918.class}, priority=1010)
public abstract class MixinItemRenderer {
    @Unique
    private int previousBeValue;

    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void changeId(class_1799 pItemRenderer0, class_811 pItemTransforms$TransformType1, boolean pBoolean2, class_4587 pPoseStack3, class_4597 pMultiBufferSource4, int pInt5, int pInt6, class_1087 pBakedModel7, CallbackInfo ci) {
        this.iris$setupId(pItemRenderer0);
    }

    /*
     * Enabled aggressive block sorting
     */
    @Unique
    private void iris$setupId(class_1799 pItemRenderer0) {
        if (WorldRenderingSettings.INSTANCE.getItemIds() == null) {
            return;
        }
        class_1792 class_17922 = pItemRenderer0.method_7909();
        if (class_17922 instanceof class_1747) {
            class_1747 blockItem = (class_1747)class_17922;
            if (!(pItemRenderer0.method_7909() instanceof class_5634)) {
                if (WorldRenderingSettings.INSTANCE.getBlockStateIds() == null) {
                    return;
                }
                this.previousBeValue = CapturedRenderingState.INSTANCE.getCurrentRenderedBlockEntity();
                CapturedRenderingState.INSTANCE.setCurrentBlockEntity(1);
                CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getBlockStateIds().getOrDefault((Object)blockItem.method_7711().method_9564(), 0));
                return;
            }
        }
        class_2960 location = class_7923.field_41178.method_10221((Object)pItemRenderer0.method_7909());
        CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt((Object)new NamespacedId(location.method_12836(), location.method_12832())));
    }

    @Inject(method={"render"}, at={@At(value="RETURN")})
    private void changeId3(CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
        CapturedRenderingState.INSTANCE.setCurrentBlockEntity(this.previousBeValue);
        this.previousBeValue = 0;
    }
}

