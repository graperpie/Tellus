/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 */
package net.irisshaders.iris.gui.element.screen;

import net.minecraft.class_2561;

public record ElementWidgetScreenData(class_2561 heading, boolean backButton) {
    public static final ElementWidgetScreenData EMPTY = new ElementWidgetScreenData((class_2561)class_2561.method_43473(), true);
}

