/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1498
 *  net.minecraft.class_2960
 *  net.minecraft.class_4059
 *  net.minecraft.class_4073
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_7923
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.entity_render_context;

import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_1498;
import net.minecraft.class_2960;
import net.minecraft.class_4059;
import net.minecraft.class_4073;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_7923;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_4073.class})
public class MixinHorseArmorLayer {
    @Inject(method={"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/horse/Horse;FFFFFF)V"}, at={@At(value="HEAD")})
    private void changeId(class_4587 pHorseArmorLayer0, class_4597 pMultiBufferSource1, int pInt2, class_1498 pHorse3, float pFloat4, float pFloat5, float pFloat6, float pFloat7, float pFloat8, float pFloat9, CallbackInfo ci) {
        if (WorldRenderingSettings.INSTANCE.getItemIds() == null || !(pHorse3.method_56676().method_7909() instanceof class_4059)) {
            return;
        }
        class_2960 location = class_7923.field_41178.method_10221((Object)pHorse3.method_56676().method_7909());
        CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt((Object)new NamespacedId(location.method_12836(), location.method_12832())));
    }

    @Inject(method={"render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/horse/Horse;FFFFFF)V"}, at={@At(value="TAIL")})
    private void changeId2(CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
    }
}

