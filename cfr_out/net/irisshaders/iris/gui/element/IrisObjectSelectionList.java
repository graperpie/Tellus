/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_350
 *  net.minecraft.class_350$class_351
 *  net.minecraft.class_6382
 */
package net.irisshaders.iris.gui.element;

import net.minecraft.class_310;
import net.minecraft.class_350;
import net.minecraft.class_6382;

public class IrisObjectSelectionList<E extends class_350.class_351<E>>
extends class_350<E> {
    public IrisObjectSelectionList(class_310 client, int width, int height, int top, int bottom, int left, int right, int itemHeight) {
        super(client, width, height, top, itemHeight);
    }

    protected int method_25329() {
        return this.field_22758 - 6;
    }

    public void select(int entry) {
        this.method_25313(this.method_25326(entry));
    }

    public void method_47399(class_6382 p0) {
    }
}

