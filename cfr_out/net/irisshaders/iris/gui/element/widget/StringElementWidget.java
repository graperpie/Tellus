/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.minecraft.class_2477
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_5250
 *  net.minecraft.class_5251
 *  net.minecraft.class_8028
 */
package net.irisshaders.iris.gui.element.widget;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.element.widget.BaseOptionElementWidget;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.StringOption;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuStringOptionElement;
import net.minecraft.class_2477;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_5250;
import net.minecraft.class_5251;
import net.minecraft.class_8028;

public class StringElementWidget
extends BaseOptionElementWidget<OptionMenuStringOptionElement> {
    protected final StringOption option;
    protected String appliedValue;
    protected int valueCount;
    protected int valueIndex;
    protected class_5250 prefix;
    protected class_5250 suffix;

    public StringElementWidget(OptionMenuStringOptionElement element) {
        super(element);
        this.option = element.option;
    }

    @Override
    public void init(ShaderPackScreen screen, NavigationController navigation) {
        super.init(screen, navigation);
        String actualPendingValue = ((OptionMenuStringOptionElement)this.element).getPendingOptionValues().getStringValueOrDefault(this.option.getName());
        this.appliedValue = ((OptionMenuStringOptionElement)this.element).getAppliedOptionValues().getStringValueOrDefault(this.option.getName());
        this.prefix = class_2561.method_43470((String)(class_2477.method_10517().method_4678("prefix." + this.option.getName()) ? class_2477.method_10517().method_48307("prefix." + this.option.getName()) : ""));
        this.suffix = class_2561.method_43470((String)(class_2477.method_10517().method_4678("suffix." + this.option.getName()) ? class_2477.method_10517().method_48307("suffix." + this.option.getName()) : ""));
        this.setLabel(GuiUtil.translateOrDefault(class_2561.method_43470((String)this.option.getName()), "option." + this.option.getName(), new Object[0]));
        ImmutableList<String> values = this.option.getAllowedValues();
        this.valueCount = values.size();
        this.valueIndex = values.indexOf(actualPendingValue);
    }

    @Override
    public void render(class_332 guiGraphics, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        this.updateRenderParams(0);
        this.renderOptionWithValue(guiGraphics, hovered || this.method_25370());
        if (this.usedKeyboard) {
            this.tryRenderTooltip(guiGraphics, this.bounds.method_48255(class_8028.field_41829), this.bounds.comp_1195().comp_1194(), hovered);
        } else {
            this.tryRenderTooltip(guiGraphics, mouseX, mouseY, hovered);
        }
    }

    private void increment(int amount) {
        this.valueIndex = Math.max(this.valueIndex, 0);
        this.valueIndex = Math.floorMod(this.valueIndex + amount, this.valueCount);
    }

    @Override
    protected class_2561 createValueLabel() {
        return this.prefix.method_27661().method_10852((class_2561)GuiUtil.translateOrDefault(class_2561.method_43470((String)this.getValue()).method_10852((class_2561)this.suffix), "value." + this.option.getName() + "." + this.getValue(), new Object[0])).method_27694(style -> style.method_27703(class_5251.method_27717((int)0x6688FF)));
    }

    @Override
    public String getCommentKey() {
        return "option." + this.option.getName() + ".comment";
    }

    public String getValue() {
        if (this.valueIndex < 0) {
            return this.appliedValue;
        }
        return (String)this.option.getAllowedValues().get(this.valueIndex);
    }

    protected void queue() {
        Iris.getShaderPackOptionQueue().put(this.option.getName(), this.getValue());
    }

    @Override
    public boolean applyNextValue() {
        this.increment(1);
        this.queue();
        return true;
    }

    @Override
    public boolean applyPreviousValue() {
        this.increment(-1);
        this.queue();
        return true;
    }

    @Override
    public boolean applyOriginalValue() {
        this.valueIndex = this.option.getAllowedValues().indexOf((Object)this.option.getDefaultValue());
        this.queue();
        return true;
    }

    @Override
    public boolean isValueModified() {
        return !this.appliedValue.equals(this.getValue());
    }
}

