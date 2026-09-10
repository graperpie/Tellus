/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 *  net.minecraft.class_1747
 *  net.minecraft.class_1792
 *  net.minecraft.class_1799
 *  org.joml.Vector3f
 */
package net.irisshaders.iris.api.v0.item;

import net.minecraft.class_1657;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import org.joml.Vector3f;

public interface IrisItemLightProvider {
    public static final Vector3f DEFAULT_LIGHT_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);

    default public int getLightEmission(class_1657 player, class_1799 stack) {
        class_1792 class_17922 = stack.method_7909();
        if (class_17922 instanceof class_1747) {
            class_1747 item = (class_1747)class_17922;
            return item.method_7711().method_9564().method_26213();
        }
        return 0;
    }

    default public Vector3f getLightColor(class_1657 player, class_1799 stack) {
        return DEFAULT_LIGHT_COLOR;
    }
}

