/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 */
package net.irisshaders.iris.gui.element.widget;

import java.util.Optional;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.element.widget.BaseOptionElementWidget;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.OptionSet;
import net.irisshaders.iris.shaderpack.option.Profile;
import net.irisshaders.iris.shaderpack.option.ProfileSet;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuProfileElement;
import net.irisshaders.iris.shaderpack.option.values.OptionValues;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_5250;
import net.minecraft.class_5348;

public class ProfileElementWidget
extends BaseOptionElementWidget<OptionMenuProfileElement> {
    private static final class_5250 PROFILE_LABEL = class_2561.method_43471((String)"options.iris.profile");
    private static final class_5250 PROFILE_CUSTOM = class_2561.method_43471((String)"options.iris.profile.custom").method_27692(class_124.field_1054);
    private Profile next;
    private Profile previous;
    private class_2561 profileLabel;

    public ProfileElementWidget(OptionMenuProfileElement element) {
        super(element);
    }

    @Override
    public void init(ShaderPackScreen screen, NavigationController navigation) {
        super.init(screen, navigation);
        this.setLabel(PROFILE_LABEL);
        ProfileSet profiles = ((OptionMenuProfileElement)this.element).profiles;
        OptionSet options = ((OptionMenuProfileElement)this.element).options;
        OptionValues pendingValues = ((OptionMenuProfileElement)this.element).getPendingOptionValues();
        ProfileSet.ProfileResult result = profiles.scan(options, pendingValues);
        this.next = result.next;
        this.previous = result.previous;
        Optional<String> profileName = result.current.map(p -> p.name);
        this.profileLabel = (class_2561)profileName.map(name -> GuiUtil.translateOrDefault(class_2561.method_43470((String)name), "profile." + name, new Object[0])).orElse(PROFILE_CUSTOM);
    }

    @Override
    public void render(class_332 guiGraphics, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        this.updateRenderParams(this.bounds.comp_1196() - (class_310.method_1551().field_1772.method_27525((class_5348)PROFILE_LABEL) + 16));
        this.renderOptionWithValue(guiGraphics, hovered || this.method_25370());
    }

    @Override
    protected class_2561 createValueLabel() {
        return this.profileLabel;
    }

    @Override
    public Optional<class_2561> getCommentTitle() {
        return Optional.of(PROFILE_LABEL);
    }

    @Override
    public String getCommentKey() {
        return "profile.comment";
    }

    @Override
    public boolean applyNextValue() {
        if (this.next == null) {
            return false;
        }
        Iris.queueShaderPackOptionsFromProfile(this.next);
        return true;
    }

    @Override
    public boolean applyPreviousValue() {
        if (this.previous == null) {
            return false;
        }
        Iris.queueShaderPackOptionsFromProfile(this.previous);
        return true;
    }

    @Override
    public boolean applyOriginalValue() {
        return false;
    }

    @Override
    public boolean isValueModified() {
        return false;
    }
}

