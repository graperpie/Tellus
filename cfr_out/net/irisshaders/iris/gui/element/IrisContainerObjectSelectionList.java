/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_350$class_351
 *  net.minecraft.class_4265
 *  net.minecraft.class_4265$class_4266
 */
package net.irisshaders.iris.gui.element;

import net.minecraft.class_310;
import net.minecraft.class_350;
import net.minecraft.class_4265;

public class IrisContainerObjectSelectionList<E extends class_4265.class_4266<E>>
extends class_4265<E> {
    public IrisContainerObjectSelectionList(class_310 client, int width, int height, int top, int bottom, int left, int right, int itemHeight) {
        super(client, width, height, top, itemHeight);
    }

    protected int method_25329() {
        return this.field_22758 - 6;
    }

    public void select(int entry) {
        this.method_25313((class_350.class_351)((class_4265.class_4266)this.method_25326(entry)));
    }
}

