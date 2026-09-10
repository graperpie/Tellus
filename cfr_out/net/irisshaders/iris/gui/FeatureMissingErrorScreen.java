/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 *  net.minecraft.class_5244
 *  net.minecraft.class_5489
 */
package net.irisshaders.iris.gui;

import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5244;
import net.minecraft.class_5489;

public class FeatureMissingErrorScreen
extends class_437 {
    private final class_437 parent;
    private final class_2561 messageTemp;
    private class_5489 message;

    public FeatureMissingErrorScreen(class_437 parent, class_2561 title, class_2561 message) {
        super(title);
        this.parent = parent;
        this.messageTemp = message;
    }

    protected void method_25426() {
        super.method_25426();
        this.message = class_5489.method_61133((class_327)this.field_22793, (int)(this.field_22789 - 50), (class_2561[])new class_2561[]{this.messageTemp});
        this.method_37063((class_364)class_4185.method_46430((class_2561)class_5244.field_24339, arg -> this.field_22787.method_1507(this.parent)).method_46434(this.field_22789 / 2 - 100, 140, 200, 20).method_46431());
    }

    public void method_25394(class_332 guiGraphics, int mouseX, int mouseY, float delta) {
        this.method_25420(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 90, 0xFFFFFF);
        this.message.method_30889(guiGraphics, this.field_22789 / 2, 110, 9, 0xFFFFFF);
        super.method_25394(guiGraphics, mouseX, mouseY, delta);
    }
}

