/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.minecraft.class_1304
 *  net.minecraft.class_1309
 *  net.minecraft.class_1738
 *  net.minecraft.class_1741
 *  net.minecraft.class_2960
 *  net.minecraft.class_3883
 *  net.minecraft.class_3887
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_572
 *  net.minecraft.class_6880
 *  net.minecraft.class_7923
 *  net.minecraft.class_8053
 *  net.minecraft.class_8054
 *  net.minecraft.class_970
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.entity_render_context;

import com.llamalad7.mixinextras.sugar.Local;
import net.irisshaders.iris.helpers.EntityState;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1738;
import net.minecraft.class_1741;
import net.minecraft.class_2960;
import net.minecraft.class_3883;
import net.minecraft.class_3887;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_572;
import net.minecraft.class_6880;
import net.minecraft.class_7923;
import net.minecraft.class_8053;
import net.minecraft.class_8054;
import net.minecraft.class_970;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_970.class})
public abstract class MixinHumanoidArmorLayer<T extends class_1309, M extends class_572<T>, A extends class_572<T>>
extends class_3887<T, M> {
    public MixinHumanoidArmorLayer(class_3883<T, M> pRenderLayer0) {
        super(pRenderLayer0);
    }

    @Inject(method={"Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;)V"}, require=0, at={@At(value="INVOKE", target="Lnet/minecraft/client/model/HumanoidModel;copyPropertiesTo(Lnet/minecraft/client/model/HumanoidModel;)V")})
    private void changeId(class_4587 pHumanoidArmorLayer0, class_4597 pMultiBufferSource1, T pLivingEntity2, class_1304 pEquipmentSlot3, int pInt4, A pHumanoidModel5, CallbackInfo ci, @Local class_1738 lvArmorItem8) {
        if (WorldRenderingSettings.INSTANCE.getItemIds() == null) {
            return;
        }
        class_2960 location = class_7923.field_41178.method_10221((Object)lvArmorItem8);
        CapturedRenderingState.INSTANCE.setCurrentRenderedItem(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt((Object)new NamespacedId(location.method_12836(), location.method_12832())));
    }

    @Inject(method={"renderTrim(Lnet/minecraft/core/Holder;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/HumanoidModel;Z)V"}, at={@At(value="HEAD")})
    private void changeTrimTemp(class_6880<class_1741> holder, class_4587 poseStack, class_4597 multiBufferSource, int i, class_8053 armorTrim, A humanoidModel, boolean bl, CallbackInfo ci) {
        if (WorldRenderingSettings.INSTANCE.getItemIds() == null) {
            return;
        }
        EntityState.interposeItemId(WorldRenderingSettings.INSTANCE.getItemIds().applyAsInt((Object)new NamespacedId("minecraft", "trim_" + ((class_8054)armorTrim.method_48431().comp_349()).comp_1208())));
    }

    @Inject(method={"renderTrim(Lnet/minecraft/core/Holder;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/item/armortrim/ArmorTrim;Lnet/minecraft/client/model/HumanoidModel;Z)V"}, at={@At(value="TAIL")})
    private void changeTrimTemp2(class_6880<class_1741> holder, class_4587 poseStack, class_4597 multiBufferSource, int i, class_8053 armorTrim, A humanoidModel, boolean bl, CallbackInfo ci) {
        EntityState.restoreItemId();
    }

    @Inject(method={"renderArmorPiece"}, at={@At(value="TAIL")})
    private void changeId2(CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
    }
}

