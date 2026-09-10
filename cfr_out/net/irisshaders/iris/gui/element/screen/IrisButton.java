/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_4185
 *  net.minecraft.class_4185$class_4241
 *  net.minecraft.class_4185$class_7841
 *  net.minecraft.class_7919
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.gui.element.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.uniform.FloatSupplier;
import net.irisshaders.iris.gui.GuiUtil;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_4185;
import net.minecraft.class_7919;
import org.jetbrains.annotations.Nullable;

public class IrisButton
extends class_4185 {
    private final FloatSupplier alphaSupplier;

    public IrisButton(int pButton0, int pInt1, int pInt2, int pInt3, class_2561 pComponent4, class_4185.class_4241 pButton$OnPress5, class_4185.class_7841 pButton$CreateNarration6, FloatSupplier alpha) {
        super(pButton0, pInt1, pInt2, pInt3, pComponent4, pButton$OnPress5, pButton$CreateNarration6);
        this.alphaSupplier = alpha;
    }

    public static Builder iris$builder(class_2561 pComponent0, class_4185.class_4241 pButton$OnPress1, FloatSupplier alpha) {
        return new Builder(pComponent0, pButton$OnPress1, alpha);
    }

    protected void method_48579(class_332 guiGraphics, int pInt1, int pInt2, float pFloat3) {
        class_310 lvMinecraft5 = class_310.method_1551();
        guiGraphics.method_51422(1.0f, 1.0f, 1.0f, this.method_25367() ? this.alphaSupplier.getAsFloat() * 1.8f : this.alphaSupplier.getAsFloat());
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        GuiUtil.bindIrisWidgetsTexture();
        GuiUtil.drawButton(guiGraphics, this.method_46426(), this.method_46427(), this.method_25368(), this.method_25364(), this.method_25367(), this.field_22763);
        guiGraphics.method_51422(1.0f, 1.0f, 1.0f, this.alphaSupplier.getAsFloat());
        int lvInt6 = this.field_22763 ? 0xFFFFFF : 0xA0A0A0;
        this.method_48589(guiGraphics, lvMinecraft5.field_1772, lvInt6 | class_3532.method_15386((float)(this.alphaSupplier.getAsFloat() * 255.0f)) << 24);
        guiGraphics.method_51422(1.0f, 1.0f, 1.0f, 1.0f);
    }

    static /* synthetic */ class_4185.class_7841 access$000() {
        return field_40754;
    }

    public static class Builder {
        private final class_2561 message;
        private final class_4185.class_4241 onPress;
        private final FloatSupplier alpha;
        @Nullable
        private class_7919 tooltip;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private class_4185.class_7841 createNarration = IrisButton.access$000();

        public Builder(class_2561 pButton$Builder0, class_4185.class_4241 pButton$OnPress1, FloatSupplier alpha) {
            this.message = pButton$Builder0;
            this.onPress = pButton$OnPress1;
            this.alpha = alpha;
        }

        public Builder pos(int pButton$Builder0, int pInt1) {
            this.x = pButton$Builder0;
            this.y = pInt1;
            return this;
        }

        public Builder width(int pButton$Builder0) {
            this.width = pButton$Builder0;
            return this;
        }

        public Builder size(int pButton$Builder0, int pInt1) {
            this.width = pButton$Builder0;
            this.height = pInt1;
            return this;
        }

        public Builder bounds(int pButton$Builder0, int pInt1, int pInt2, int pInt3) {
            return this.pos(pButton$Builder0, pInt1).size(pInt2, pInt3);
        }

        public Builder tooltip(@Nullable class_7919 pButton$Builder0) {
            this.tooltip = pButton$Builder0;
            return this;
        }

        public Builder createNarration(class_4185.class_7841 pButton$Builder0) {
            this.createNarration = pButton$Builder0;
            return this;
        }

        public IrisButton build() {
            IrisButton lvButton1 = new IrisButton(this.x, this.y, this.width, this.height, this.message, this.onPress, this.createNarration, this.alpha);
            lvButton1.method_47400(this.tooltip);
            return lvButton1;
        }
    }
}

