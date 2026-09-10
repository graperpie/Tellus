/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1074
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  net.minecraft.class_5251
 *  net.minecraft.class_5348
 *  net.minecraft.class_8028
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.gui.element.widget;

import java.util.Optional;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.element.widget.CommentedElementWidget;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElement;
import net.minecraft.class_1074;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import net.minecraft.class_5251;
import net.minecraft.class_5348;
import net.minecraft.class_8028;
import org.jetbrains.annotations.Nullable;

public abstract class BaseOptionElementWidget<T extends OptionMenuElement>
extends CommentedElementWidget<T> {
    protected static final class_2561 SET_TO_DEFAULT = class_2561.method_43471((String)"options.iris.setToDefault").method_27692(class_124.field_1060);
    protected static final class_2561 DIVIDER = class_2561.method_43470((String)": ");
    protected class_5250 unmodifiedLabel;
    protected ShaderPackScreen screen;
    protected NavigationController navigation;
    protected class_2561 trimmedLabel;
    protected class_2561 valueLabel;
    protected boolean usedKeyboard;
    private class_5250 label;
    private boolean isLabelTrimmed;
    private int maxLabelWidth;
    private int valueSectionWidth;

    public BaseOptionElementWidget(T element) {
        super(element);
    }

    @Override
    public void init(ShaderPackScreen screen, NavigationController navigation) {
        this.screen = screen;
        this.navigation = navigation;
        this.valueLabel = null;
        this.trimmedLabel = null;
    }

    protected final void setLabel(class_5250 label) {
        this.label = label.method_27661().method_10852(DIVIDER);
        this.unmodifiedLabel = label;
    }

    protected final void updateRenderParams(int minValueSectionWidth) {
        this.usedKeyboard = this.method_25370();
        if (this.valueLabel == null) {
            this.valueLabel = this.createValueLabel();
        }
        class_327 font = class_310.method_1551().field_1772;
        this.valueSectionWidth = Math.max(minValueSectionWidth, font.method_27525((class_5348)this.valueLabel) + 8);
        this.maxLabelWidth = this.bounds.comp_1196() - 8 - this.valueSectionWidth;
        if (this.trimmedLabel == null || font.method_27525((class_5348)this.label) > this.maxLabelWidth != this.isLabelTrimmed) {
            this.updateLabels();
        }
        this.isLabelTrimmed = font.method_27525((class_5348)this.label) > this.maxLabelWidth;
    }

    protected final void renderOptionWithValue(class_332 guiGraphics, boolean hovered, float sliderPosition, int sliderWidth) {
        GuiUtil.bindIrisWidgetsTexture();
        GuiUtil.drawButton(guiGraphics, this.bounds.comp_1195().comp_1193(), this.bounds.comp_1195().comp_1194(), this.bounds.comp_1196(), this.bounds.comp_1197(), hovered, false);
        GuiUtil.drawButton(guiGraphics, this.bounds.method_48255(class_8028.field_41829) - (this.valueSectionWidth + 2), this.bounds.comp_1195().comp_1194() + 2, this.valueSectionWidth, this.bounds.comp_1197() - 4, false, true);
        if (sliderPosition >= 0.0f) {
            int sliderSpace = this.valueSectionWidth - 4 - sliderWidth;
            int sliderPos = this.bounds.method_48255(class_8028.field_41829) - this.valueSectionWidth + (int)(sliderPosition * (float)sliderSpace);
            GuiUtil.drawButton(guiGraphics, sliderPos, this.bounds.comp_1195().comp_1194() + 4, sliderWidth, this.bounds.comp_1197() - 8, false, false);
        }
        class_327 font = class_310.method_1551().field_1772;
        guiGraphics.method_27535(font, this.trimmedLabel, this.bounds.comp_1195().comp_1193() + 6, this.bounds.comp_1195().comp_1194() + 7, 0xFFFFFF);
        guiGraphics.method_27535(font, this.valueLabel, this.bounds.method_48255(class_8028.field_41829) - 2 - (int)((double)this.valueSectionWidth * 0.5) - (int)((double)font.method_27525((class_5348)this.valueLabel) * 0.5), this.bounds.comp_1195().comp_1194() + 7, 0xFFFFFF);
    }

    protected final void renderOptionWithValue(class_332 guiGraphics, boolean hovered) {
        this.renderOptionWithValue(guiGraphics, hovered, -1.0f, 0);
    }

    protected final void tryRenderTooltip(class_332 guiGraphics, int mouseX, int mouseY, boolean hovered) {
        if (class_437.method_25442()) {
            this.renderTooltip(guiGraphics, SET_TO_DEFAULT, mouseX, mouseY, hovered);
        } else if (this.isLabelTrimmed && !this.screen.isDisplayingComment()) {
            this.renderTooltip(guiGraphics, (class_2561)this.unmodifiedLabel, mouseX, mouseY, hovered);
        }
    }

    protected final void renderTooltip(class_332 guiGraphics, class_2561 text, int mouseX, int mouseY, boolean hovered) {
        if (hovered) {
            ShaderPackScreen.TOP_LAYER_RENDER_QUEUE.add(() -> GuiUtil.drawTextPanel(class_310.method_1551().field_1772, guiGraphics, text, mouseX + 2, mouseY - 16));
        }
    }

    protected final void updateLabels() {
        this.trimmedLabel = this.createTrimmedLabel();
        this.valueLabel = this.createValueLabel();
    }

    protected final class_2561 createTrimmedLabel() {
        class_5250 label = GuiUtil.shortenText(class_310.method_1551().field_1772, this.label.method_27661(), this.maxLabelWidth);
        if (this.isValueModified()) {
            label = label.method_27694(style -> style.method_27703(class_5251.method_27717((int)16763210)));
        }
        return label;
    }

    protected abstract class_2561 createValueLabel();

    public abstract boolean applyNextValue();

    public abstract boolean applyPreviousValue();

    public abstract boolean applyOriginalValue();

    public abstract boolean isValueModified();

    @Nullable
    public abstract String getCommentKey();

    @Override
    public Optional<class_2561> getCommentTitle() {
        return Optional.of(this.unmodifiedLabel);
    }

    @Override
    public Optional<class_2561> getCommentBody() {
        return Optional.ofNullable(this.getCommentKey()).map(key -> class_1074.method_4663((String)key) ? class_2561.method_43471((String)key) : null);
    }

    @Override
    public boolean method_25402(double mx, double my, int button) {
        if (button == 0 || button == 1) {
            boolean refresh = false;
            if (class_437.method_25442()) {
                refresh = this.applyOriginalValue();
            }
            if (!refresh) {
                refresh = button == 0 ? this.applyNextValue() : this.applyPreviousValue();
            }
            if (refresh) {
                this.navigation.refresh();
            }
            GuiUtil.playButtonClickSound();
            return true;
        }
        return super.method_25402(mx, my, button);
    }

    @Override
    public boolean method_25404(int keycode, int scancode, int modifiers) {
        if (keycode == 257) {
            boolean refresh;
            boolean bl = class_437.method_25441() ? this.applyOriginalValue() : (refresh = class_437.method_25442() ? this.applyPreviousValue() : this.applyNextValue());
            if (refresh) {
                this.navigation.refresh();
            }
            GuiUtil.playButtonClickSound();
            return true;
        }
        return false;
    }
}

