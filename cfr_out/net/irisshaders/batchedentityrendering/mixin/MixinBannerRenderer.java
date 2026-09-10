/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1767
 *  net.minecraft.class_1921
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_4730
 *  net.minecraft.class_630
 *  net.minecraft.class_823
 *  net.minecraft.class_9307
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.batchedentityrendering.mixin;

import net.irisshaders.batchedentityrendering.impl.Groupable;
import net.irisshaders.batchedentityrendering.impl.wrappers.TaggingRenderTypeWrapper;
import net.irisshaders.iris.layer.BufferSourceWrapper;
import net.minecraft.class_1767;
import net.minecraft.class_1921;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4730;
import net.minecraft.class_630;
import net.minecraft.class_823;
import net.minecraft.class_9307;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_823.class})
public class MixinBannerRenderer {
    @Unique
    private static final String RENDER_PATTERNS = "Lnet/minecraft/client/renderer/blockentity/BannerRenderer;renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLjava/util/List;Z)V";
    @Unique
    private static Groupable groupableToEnd;
    @Unique
    private static int index;

    @ModifyVariable(method={"renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;Z)V"}, at=@At(value="HEAD"), argsOnly=true)
    private static class_4597 iris$wrapBufferSource(class_4597 multiBufferSource) {
        if (multiBufferSource instanceof Groupable) {
            Groupable groupable = (Groupable)multiBufferSource;
            boolean started = groupable.maybeStartGroup();
            if (started) {
                groupableToEnd = groupable;
            }
            index = 0;
            return new BufferSourceWrapper(multiBufferSource, type -> new TaggingRenderTypeWrapper(type.toString(), (class_1921)type, index++));
        }
        return multiBufferSource;
    }

    @Inject(method={"renderPatterns(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/model/geom/ModelPart;Lnet/minecraft/client/resources/model/Material;ZLnet/minecraft/world/item/DyeColor;Lnet/minecraft/world/level/block/entity/BannerPatternLayers;Z)V"}, at={@At(value="RETURN")})
    private static void iris$endRenderingCanvas(class_4587 poseStack, class_4597 multiBufferSource, int i, int j, class_630 modelPart, class_4730 material, boolean bl, class_1767 dyeColor, class_9307 bannerPatternLayers, boolean bl2, CallbackInfo ci) {
        if (groupableToEnd != null) {
            groupableToEnd.endGroup();
            groupableToEnd = null;
            index = 0;
        }
    }
}

