/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntFunction
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.minecraft.class_1297
 *  net.minecraft.class_1299
 *  net.minecraft.class_1641
 *  net.minecraft.class_1657
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_7923
 *  net.minecraft.class_898
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.entity_render_context;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.irisshaders.iris.layer.BufferSourceWrapper;
import net.irisshaders.iris.layer.EntityRenderStateShard;
import net.irisshaders.iris.layer.OuterWrappedRenderType;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1641;
import net.minecraft.class_1657;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_7923;
import net.minecraft.class_898;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_898.class})
public class MixinEntityRenderDispatcher {
    @Unique
    private static final NamespacedId CURRENT_PLAYER = new NamespacedId("minecraft", "current_player");
    @Unique
    private static final NamespacedId CONVERTING_VILLAGER = new NamespacedId("minecraft", "zombie_villager_converting");
    @Unique
    private static final Object2ObjectMap<class_1299<?>, NamespacedId> ENTITY_IDS = new Object2ObjectOpenHashMap();

    @ModifyVariable(method={"render"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift=At.Shift.AFTER), allow=1, require=1, argsOnly=true)
    private class_4597 iris$beginEntityRender(class_4597 bufferSource, class_1297 entity) {
        class_1641 zombie;
        Object2IntFunction<NamespacedId> entityIds = WorldRenderingSettings.INSTANCE.getEntityIds();
        if (entityIds == null || !ImmediateState.isRenderingLevel) {
            return bufferSource;
        }
        int intId = entity instanceof class_1641 && (zombie = (class_1641)entity).method_7198() && WorldRenderingSettings.INSTANCE.hasVillagerConversionId() ? entityIds.applyAsInt((Object)CONVERTING_VILLAGER) : (entity instanceof class_1657 && class_310.method_1551().method_1560() == entity ? (entityIds.containsKey((Object)CURRENT_PLAYER) ? entityIds.getInt((Object)CURRENT_PLAYER) : entityIds.applyAsInt((Object)((NamespacedId)ENTITY_IDS.computeIfAbsent((Object)entity.method_5864(), k -> {
            class_2960 entityId = class_7923.field_41177.method_10221((Object)entity.method_5864());
            return new NamespacedId(entityId.method_12836(), entityId.method_12832());
        })))) : entityIds.applyAsInt((Object)((NamespacedId)ENTITY_IDS.computeIfAbsent((Object)entity.method_5864(), k -> {
            class_2960 entityId = class_7923.field_41177.method_10221((Object)entity.method_5864());
            return new NamespacedId(entityId.method_12836(), entityId.method_12832());
        }))));
        CapturedRenderingState.INSTANCE.setCurrentEntity(intId);
        return new BufferSourceWrapper(bufferSource, renderType -> OuterWrappedRenderType.wrapExactlyOnce("iris:entity", renderType, EntityRenderStateShard.INSTANCE));
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V")})
    private void iris$endEntityRender(class_1297 entity, double x, double y, double z, float yaw, float tickDelta, class_4587 poseStack, class_4597 bufferSource, int light, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentEntity(0);
        CapturedRenderingState.INSTANCE.setCurrentRenderedItem(0);
    }
}

