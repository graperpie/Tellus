/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_315
 *  net.minecraft.class_339
 *  net.minecraft.class_7172
 *  net.minecraft.class_7172$class_7178
 *  net.minecraft.class_7172$class_7277
 *  net.minecraft.class_7172$class_7303
 */
package net.irisshaders.iris.gui.option;

import java.util.function.Consumer;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.minecraft.class_315;
import net.minecraft.class_339;
import net.minecraft.class_7172;

public class ShadowDistanceOption<T>
extends class_7172<T> {
    public ShadowDistanceOption(String string, class_7172.class_7277<T> arg, class_7172.class_7303<T> arg2, class_7172.class_7178<T> arg3, T object, Consumer<T> consumer) {
        super(string, arg, arg2, arg3, object, consumer);
    }

    public class_339 method_18520(class_315 options, int x, int y, int width) {
        class_339 widget = super.method_18520(options, x, y, width);
        widget.field_22763 = IrisVideoSettings.isShadowDistanceSliderEnabled();
        return widget;
    }
}

