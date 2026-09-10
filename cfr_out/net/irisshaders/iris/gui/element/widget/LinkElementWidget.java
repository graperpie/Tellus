/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1074
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 *  net.minecraft.class_8027
 *  net.minecraft.class_8028
 */
package net.irisshaders.iris.gui.element.widget;

import java.util.Optional;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.element.widget.CommentedElementWidget;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuLinkElement;
import net.minecraft.class_1074;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_5250;
import net.minecraft.class_5348;
import net.minecraft.class_8027;
import net.minecraft.class_8028;

public class LinkElementWidget
extends CommentedElementWidget<OptionMenuLinkElement> {
    private static final class_2561 ARROW = class_2561.method_43470((String)">");
    private final String targetScreenId;
    private final class_5250 label;
    private NavigationController navigation;
    private class_5250 trimmedLabel = null;
    private boolean isLabelTrimmed = false;

    public LinkElementWidget(OptionMenuLinkElement element) {
        super(element);
        this.targetScreenId = element.targetScreenId;
        this.label = GuiUtil.translateOrDefault(class_2561.method_43470((String)element.targetScreenId), "screen." + element.targetScreenId, new Object[0]);
    }

    @Override
    public void init(ShaderPackScreen screen, NavigationController navigation) {
        this.navigation = navigation;
    }

    @Override
    public void render(class_332 guiGraphics, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        GuiUtil.bindIrisWidgetsTexture();
        GuiUtil.drawButton(guiGraphics, this.bounds.comp_1195().comp_1193(), this.bounds.comp_1195().comp_1194(), this.bounds.comp_1196(), this.bounds.comp_1197(), hovered || this.method_25370(), false);
        class_327 font = class_310.method_1551().field_1772;
        int maxLabelWidth = this.bounds.comp_1196() - 9;
        if (font.method_27525((class_5348)this.label) > maxLabelWidth) {
            this.isLabelTrimmed = true;
        }
        if (this.trimmedLabel == null) {
            this.trimmedLabel = GuiUtil.shortenText(font, this.label, maxLabelWidth);
        }
        int labelWidth = font.method_27525((class_5348)this.trimmedLabel);
        guiGraphics.method_27535(font, (class_2561)this.trimmedLabel, this.bounds.method_48254(class_8027.field_41822) - (int)((double)labelWidth * 0.5) - (int)(0.5 * (double)Math.max(labelWidth - (this.bounds.comp_1196() - 18), 0)), this.bounds.comp_1195().comp_1194() + 7, 0xFFFFFF);
        guiGraphics.method_27535(font, ARROW, this.bounds.method_48255(class_8028.field_41829) - 9, this.bounds.comp_1195().comp_1194() + 7, 0xFFFFFF);
        if (hovered && this.isLabelTrimmed) {
            ShaderPackScreen.TOP_LAYER_RENDER_QUEUE.add(() -> GuiUtil.drawTextPanel(font, guiGraphics, (class_2561)this.label, mouseX + 2, mouseY - 16));
        }
    }

    @Override
    public boolean method_25402(double mx, double my, int button) {
        if (button == 0) {
            this.navigation.open(this.targetScreenId);
            GuiUtil.playButtonClickSound();
            return true;
        }
        return super.method_25402(mx, my, button);
    }

    @Override
    public boolean method_25404(int keyCode, int pInt1, int pInt2) {
        if (keyCode == 257) {
            this.navigation.open(this.targetScreenId);
            GuiUtil.playButtonClickSound();
            return true;
        }
        return super.method_25404(keyCode, pInt1, pInt2);
    }

    @Override
    public Optional<class_2561> getCommentTitle() {
        return Optional.of(this.label);
    }

    @Override
    public Optional<class_2561> getCommentBody() {
        String translation = "screen." + this.targetScreenId + ".comment";
        return Optional.ofNullable(class_1074.method_4663((String)translation) ? class_2561.method_43471((String)translation) : null);
    }
}

