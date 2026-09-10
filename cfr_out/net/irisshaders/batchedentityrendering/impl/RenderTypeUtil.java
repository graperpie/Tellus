/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_293$class_5596
 */
package net.irisshaders.batchedentityrendering.impl;

import net.minecraft.class_1921;
import net.minecraft.class_293;

public class RenderTypeUtil {
    public static boolean isTriangleStripDrawMode(class_1921 renderType) {
        return renderType.method_23033() == class_293.class_5596.field_27380;
    }
}

