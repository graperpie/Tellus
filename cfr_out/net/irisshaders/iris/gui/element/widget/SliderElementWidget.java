/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_3532
 *  net.minecraft.class_437
 *  net.minecraft.class_5348
 *  net.minecraft.class_8027
 *  net.minecraft.class_8028
 */
package net.irisshaders.iris.gui.element.widget;

import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.element.widget.StringElementWidget;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuStringOptionElement;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_437;
import net.minecraft.class_5348;
import net.minecraft.class_8027;
import net.minecraft.class_8028;

public class SliderElementWidget
extends StringElementWidget {
    private static final int PREVIEW_SLIDER_WIDTH = 4;
    private static final int ACTIVE_SLIDER_WIDTH = 6;
    private boolean mouseDown = false;

    public SliderElementWidget(OptionMenuStringOptionElement element) {
        super(element);
    }

    @Override
    public void render(class_332 guiGraphics, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        this.updateRenderParams(35);
        if (!hovered && !this.method_25370()) {
            if (this.usedKeyboard) {
                this.usedKeyboard = false;
                this.mouseDown = false;
            }
            this.renderOptionWithValue(guiGraphics, false, (float)this.valueIndex / (float)(this.valueCount - 1), 4);
        } else {
            this.renderSlider(guiGraphics);
        }
        if (this.usedKeyboard) {
            if (class_437.method_25442()) {
                this.renderTooltip(guiGraphics, SET_TO_DEFAULT, this.bounds.method_48255(class_8028.field_41829), this.bounds.comp_1195().comp_1194(), hovered);
            } else if (!this.screen.isDisplayingComment()) {
                this.renderTooltip(guiGraphics, (class_2561)this.unmodifiedLabel, this.bounds.method_48255(class_8028.field_41829), this.bounds.comp_1195().comp_1194(), hovered);
            }
        } else if (class_437.method_25442()) {
            this.renderTooltip(guiGraphics, SET_TO_DEFAULT, mouseX, mouseY, hovered);
        } else if (!this.screen.isDisplayingComment()) {
            this.renderTooltip(guiGraphics, (class_2561)this.unmodifiedLabel, mouseX, mouseY, hovered);
        }
        if (this.usedKeyboard && !this.method_25370()) {
            this.usedKeyboard = false;
            this.onReleased();
        }
        if (this.mouseDown && !this.usedKeyboard) {
            if (!hovered) {
                this.onReleased();
            }
            this.whileDragging(mouseX);
        }
    }

    private void renderSlider(class_332 guiGraphics) {
        GuiUtil.bindIrisWidgetsTexture();
        GuiUtil.drawButton(guiGraphics, this.bounds.comp_1195().comp_1193(), this.bounds.comp_1195().comp_1194(), this.bounds.comp_1196(), this.bounds.comp_1197(), this.method_25370(), false);
        GuiUtil.drawButton(guiGraphics, this.bounds.comp_1195().comp_1193() + 2, this.bounds.comp_1195().comp_1194() + 2, this.bounds.comp_1196() - 4, this.bounds.comp_1197() - 4, false, true);
        int sliderSpace = this.bounds.comp_1196() - 8 - 6;
        int sliderPos = this.bounds.comp_1195().comp_1193() + 4 + (int)((float)this.valueIndex / (float)(this.valueCount - 1) * (float)sliderSpace);
        GuiUtil.drawButton(guiGraphics, sliderPos, this.bounds.comp_1195().comp_1194() + 4, 6, this.bounds.comp_1197() - 8, this.mouseDown, false);
        class_327 font = class_310.method_1551().field_1772;
        guiGraphics.method_27535(font, this.valueLabel, this.bounds.method_48254(class_8027.field_41822) - (int)((double)font.method_27525((class_5348)this.valueLabel) * 0.5), this.bounds.comp_1195().comp_1194() + 7, 0xFFFFFF);
    }

    private void whileDragging(int mouseX) {
        float mousePositionAcrossWidget = class_3532.method_15363((float)((float)(mouseX - (this.bounds.comp_1195().comp_1193() + 4)) / (float)(this.bounds.comp_1196() - 8)), (float)0.0f, (float)1.0f);
        int newValueIndex = Math.min(this.valueCount - 1, (int)(mousePositionAcrossWidget * (float)this.valueCount));
        if (this.valueIndex != newValueIndex) {
            this.valueIndex = newValueIndex;
            this.updateLabels();
        }
    }

    private void onReleased() {
        this.mouseDown = false;
        this.queue();
        this.navigation.refresh();
        GuiUtil.playButtonClickSound();
    }

    @Override
    public boolean method_25402(double mx, double my, int button) {
        if (button == 0) {
            if (class_437.method_25442()) {
                if (this.applyOriginalValue()) {
                    this.navigation.refresh();
                }
                GuiUtil.playButtonClickSound();
                return true;
            }
            this.mouseDown = true;
            GuiUtil.playButtonClickSound();
            return true;
        }
        return false;
    }

    @Override
    public boolean method_25404(int keycode, int scancode, int modifiers) {
        if (keycode == 257) {
            if (class_437.method_25442()) {
                if (this.applyOriginalValue()) {
                    this.navigation.refresh();
                }
                GuiUtil.playButtonClickSound();
                return true;
            }
            this.mouseDown = !this.mouseDown;
            this.usedKeyboard = true;
            GuiUtil.playButtonClickSound();
            return true;
        }
        if (this.mouseDown && this.usedKeyboard) {
            if (keycode == 263) {
                this.valueIndex = Math.max(0, this.valueIndex - 1);
                this.updateLabels();
                return true;
            }
            if (keycode == 262) {
                this.valueIndex = Math.min(this.valueCount - 1, this.valueIndex + 1);
                this.updateLabels();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean method_25406(double mx, double my, int button) {
        if (button == 0) {
            this.onReleased();
            return true;
        }
        return super.method_25406(mx, my, button);
    }
}

