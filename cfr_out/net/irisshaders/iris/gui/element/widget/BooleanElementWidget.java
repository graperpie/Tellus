/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 */
package net.irisshaders.iris.gui.element.widget;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.element.widget.BaseOptionElementWidget;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.BooleanOption;
import net.irisshaders.iris.shaderpack.option.MergedBooleanOption;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuBooleanOptionElement;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_332;

public class BooleanElementWidget
extends BaseOptionElementWidget<OptionMenuBooleanOptionElement> {
    private static final class_2561 TEXT_TRUE = class_2561.method_43471((String)"label.iris.true").method_27692(class_124.field_1060);
    private static final class_2561 TEXT_FALSE = class_2561.method_43471((String)"label.iris.false").method_27692(class_124.field_1061);
    private static final class_2561 TEXT_TRUE_DEFAULT = class_2561.method_43471((String)"label.iris.true");
    private static final class_2561 TEXT_FALSE_DEFAULT = class_2561.method_43471((String)"label.iris.false");
    private final BooleanOption option;
    private boolean appliedValue;
    private boolean value;
    private boolean defaultValue;

    public BooleanElementWidget(OptionMenuBooleanOptionElement element) {
        super(element);
        this.option = element.option;
    }

    @Override
    public void init(ShaderPackScreen screen, NavigationController navigation) {
        super.init(screen, navigation);
        this.appliedValue = ((OptionMenuBooleanOptionElement)this.element).getAppliedOptionValues().getBooleanValueOrDefault(this.option.getName());
        this.value = ((OptionMenuBooleanOptionElement)this.element).getPendingOptionValues().getBooleanValueOrDefault(this.option.getName());
        this.defaultValue = ((MergedBooleanOption)((OptionMenuBooleanOptionElement)this.element).getAppliedOptionValues().getOptionSet().getBooleanOptions().get((Object)this.option.getName())).getOption().getDefaultValue();
        this.setLabel(GuiUtil.translateOrDefault(class_2561.method_43470((String)this.option.getName()), "option." + this.option.getName(), new Object[0]));
    }

    @Override
    public void render(class_332 guiGraphics, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        this.updateRenderParams(28);
        this.renderOptionWithValue(guiGraphics, hovered || this.method_25370());
        this.tryRenderTooltip(guiGraphics, mouseX, mouseY, hovered);
    }

    @Override
    protected class_2561 createValueLabel() {
        if (this.value == this.defaultValue) {
            return this.value ? TEXT_TRUE_DEFAULT : TEXT_FALSE_DEFAULT;
        }
        return this.value ? TEXT_TRUE : TEXT_FALSE;
    }

    @Override
    public String getCommentKey() {
        return "option." + this.option.getName() + ".comment";
    }

    public String getValue() {
        return Boolean.toString(this.value);
    }

    private void queue() {
        Iris.getShaderPackOptionQueue().put(this.option.getName(), this.getValue());
    }

    @Override
    public boolean applyNextValue() {
        this.value = !this.value;
        this.queue();
        return true;
    }

    @Override
    public boolean applyPreviousValue() {
        return this.applyNextValue();
    }

    @Override
    public boolean applyOriginalValue() {
        this.value = this.option.getDefaultValue();
        this.queue();
        return true;
    }

    @Override
    public boolean isValueModified() {
        return this.value != this.appliedValue;
    }
}

