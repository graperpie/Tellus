/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntFunction
 *  net.minecraft.class_1268
 *  net.minecraft.class_1657
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_746
 *  net.minecraft.class_7923
 *  org.jetbrains.annotations.NotNull
 *  org.joml.Vector3f
 */
package net.irisshaders.iris.uniforms;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import net.irisshaders.iris.api.v0.item.IrisItemLightProvider;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.shaderpack.IdMap;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.minecraft.class_1268;
import net.minecraft.class_1657;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_746;
import net.minecraft.class_7923;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public final class IdMapUniforms {
    private IdMapUniforms() {
    }

    public static void addIdMapUniforms(FrameUpdateNotifier notifier, UniformHolder uniforms, IdMap idMap, boolean isOldHandLight) {
        HeldItemSupplier mainHandSupplier = new HeldItemSupplier(class_1268.field_5808, idMap.getItemIdMap(), isOldHandLight);
        HeldItemSupplier offHandSupplier = new HeldItemSupplier(class_1268.field_5810, idMap.getItemIdMap(), false);
        notifier.addListener(mainHandSupplier::update);
        notifier.addListener(offHandSupplier::update);
        uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "heldItemId", mainHandSupplier::getIntID).uniform1i(UniformUpdateFrequency.PER_FRAME, "heldItemId2", offHandSupplier::getIntID).uniform1i(UniformUpdateFrequency.PER_FRAME, "heldBlockLightValue", mainHandSupplier::getLightValue).uniform1i(UniformUpdateFrequency.PER_FRAME, "heldBlockLightValue2", offHandSupplier::getLightValue).uniform3f(UniformUpdateFrequency.PER_FRAME, "heldBlockLightColor", mainHandSupplier::getLightColor).uniform3f(UniformUpdateFrequency.PER_FRAME, "heldBlockLightColor2", offHandSupplier::getLightColor);
    }

    private static class HeldItemSupplier {
        private final class_1268 hand;
        private final Object2IntFunction<NamespacedId> itemIdMap;
        private final boolean applyOldHandLight;
        private int intID;
        private int lightValue;
        private Vector3f lightColor;

        HeldItemSupplier(class_1268 hand, Object2IntFunction<NamespacedId> itemIdMap, boolean shouldApplyOldHandLight) {
            this.hand = hand;
            this.itemIdMap = itemIdMap;
            this.applyOldHandLight = shouldApplyOldHandLight && hand == class_1268.field_5808;
        }

        private void invalidate() {
            this.intID = -1;
            this.lightValue = 0;
            this.lightColor = IrisItemLightProvider.DEFAULT_LIGHT_COLOR;
        }

        public void update() {
            class_746 player = class_310.method_1551().field_1724;
            if (player == null) {
                this.invalidate();
                return;
            }
            class_1799 heldStack = player.method_5998(this.hand);
            if (heldStack == null) {
                this.invalidate();
                return;
            }
            class_1792 heldItem = heldStack.method_7909();
            if (heldItem == null) {
                this.invalidate();
                return;
            }
            class_2960 heldItemId = class_7923.field_41178.method_10221((Object)heldItem);
            this.intID = this.itemIdMap.applyAsInt((Object)new NamespacedId(heldItemId.method_12836(), heldItemId.method_12832()));
            IrisItemLightProvider lightProvider = (IrisItemLightProvider)heldItem;
            this.lightValue = lightProvider.getLightEmission((class_1657)class_310.method_1551().field_1724, heldStack);
            if (this.applyOldHandLight) {
                lightProvider = this.applyOldHandLighting(player, lightProvider);
            }
            this.lightColor = lightProvider.getLightColor((class_1657)class_310.method_1551().field_1724, heldStack);
        }

        private IrisItemLightProvider applyOldHandLighting(@NotNull class_746 player, IrisItemLightProvider existing) {
            class_1799 offHandStack = player.method_5998(class_1268.field_5810);
            if (offHandStack == null) {
                return existing;
            }
            class_1792 offHandItem = offHandStack.method_7909();
            if (offHandItem == null) {
                return existing;
            }
            IrisItemLightProvider lightProvider = (IrisItemLightProvider)offHandItem;
            int newEmission = lightProvider.getLightEmission((class_1657)class_310.method_1551().field_1724, offHandStack);
            if (this.lightValue < newEmission) {
                this.lightValue = newEmission;
                return lightProvider;
            }
            return existing;
        }

        public int getIntID() {
            return this.intID;
        }

        public int getLightValue() {
            return this.lightValue;
        }

        public Vector3f getLightColor() {
            return this.lightColor;
        }
    }
}

