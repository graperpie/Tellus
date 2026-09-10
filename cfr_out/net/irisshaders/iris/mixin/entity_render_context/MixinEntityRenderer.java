/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntFunction
 *  net.minecraft.class_1297
 *  net.minecraft.class_2561
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_897
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.entity_render_context;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_1297;
import net.minecraft.class_2561;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_897;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_897.class})
public class MixinEntityRenderer<T extends class_1297> {
    @Unique
    private static final NamespacedId NAME_TAG_ID = new NamespacedId("minecraft", "name_tag");
    @Unique
    private int lastId = -100;

    @Inject(method={"renderNameTag"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;getViewYRot(F)F")})
    private void setNameTagId(T entity, class_2561 component, class_4587 poseStack, class_4597 multiBufferSource, int i, float f, CallbackInfo ci) {
        Object2IntFunction<NamespacedId> entityIds = WorldRenderingSettings.INSTANCE.getEntityIds();
        if (entityIds == null) {
            return;
        }
        this.lastId = CapturedRenderingState.INSTANCE.getCurrentRenderedEntity();
        int intId = entityIds.applyAsInt((Object)NAME_TAG_ID);
        CapturedRenderingState.INSTANCE.setCurrentEntity(intId);
    }

    @Inject(method={"renderNameTag"}, at={@At(value="RETURN")})
    private void resetId(T entity, class_2561 component, class_4587 poseStack, class_4597 multiBufferSource, int i, float f, CallbackInfo ci) {
        if (this.lastId != -100) {
            CapturedRenderingState.INSTANCE.setCurrentEntity(this.lastId);
            this.lastId = -100;
        }
    }
}

