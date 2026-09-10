/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_5348
 *  net.minecraft.class_8016
 *  net.minecraft.class_8023
 *  net.minecraft.class_8030
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.gui.element;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.irisshaders.iris.gui.GuiUtil;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_5348;
import net.minecraft.class_8016;
import net.minecraft.class_8023;
import net.minecraft.class_8030;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IrisElementRow {
    private final Map<Element, Integer> elements = new HashMap<Element, Integer>();
    private final List<Element> orderedElements = new ArrayList<Element>();
    private final int spacing;
    private int x;
    private int y;
    private int width;
    private int height;

    public IrisElementRow(int spacing) {
        this.spacing = spacing;
    }

    public IrisElementRow() {
        this(1);
    }

    public int getWidth() {
        return this.width;
    }

    public IrisElementRow add(Element element, int width) {
        if (!this.orderedElements.contains(element)) {
            this.orderedElements.add(element);
        }
        this.elements.put(element, width);
        this.width += width + this.spacing;
        return this;
    }

    public void setWidth(Element element, int width) {
        if (!this.elements.containsKey(element)) {
            return;
        }
        this.width -= this.elements.get(element) + 2;
        this.add(element, width);
    }

    public void render(class_332 guiGraphics, int x, int y, int height, int mouseX, int mouseY, float tickDelta, boolean rowHovered) {
        this.x = x;
        this.y = y;
        this.height = height;
        int currentX = x;
        for (Element element : this.orderedElements) {
            int currentWidth = this.elements.get(element);
            element.render(guiGraphics, currentX, y, currentWidth, height, mouseX, mouseY, tickDelta, rowHovered && this.sectionHovered(currentX, currentWidth, mouseX, mouseY));
            currentX += currentWidth + this.spacing;
        }
    }

    public void renderRightAligned(class_332 guiGraphics, int x, int y, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        this.render(guiGraphics, x - this.width, y, height, mouseX, mouseY, tickDelta, hovered);
    }

    private boolean sectionHovered(int sectionX, int sectionWidth, double mx, double my) {
        return mx > (double)sectionX && mx < (double)(sectionX + sectionWidth) && my > (double)this.y && my < (double)(this.y + this.height);
    }

    private Optional<Element> getHovered(double mx, double my) {
        int currentX = this.x;
        for (Element element : this.orderedElements) {
            int currentWidth = this.elements.get(element);
            if (this.sectionHovered(currentX, currentWidth, mx, my)) {
                return Optional.of(element);
            }
            currentX += currentWidth + this.spacing;
        }
        return Optional.empty();
    }

    private Optional<Element> getFocused() {
        return this.orderedElements.stream().filter(Element::method_25370).findFirst();
    }

    public boolean mouseClicked(double mx, double my, int button) {
        return this.getHovered(mx, my).map(element -> element.method_25402(mx, my, button)).orElse(false);
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return this.getHovered(mx, my).map(element -> element.method_25406(mx, my, button)).orElse(false);
    }

    public boolean keyPressed(int keycode, int scancode, int modifiers) {
        return this.getFocused().map(element -> element.method_25404(keycode, scancode, modifiers)).orElse(false);
    }

    public List<? extends class_364> children() {
        return ImmutableList.copyOf(this.orderedElements);
    }

    public static abstract class Element
    implements class_364 {
        public boolean disabled = false;
        private boolean hovered = false;
        private boolean focused;
        private class_8030 bounds = class_8030.method_48248();

        public void render(class_332 guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
            this.bounds = new class_8030(x, y, width, height);
            GuiUtil.bindIrisWidgetsTexture();
            GuiUtil.drawButton(guiGraphics, x, y, width, height, this.isHovered() || this.method_25370(), this.disabled);
            this.hovered = hovered;
            this.renderLabel(guiGraphics, x, y, width, height, mouseX, mouseY, tickDelta, hovered);
        }

        public abstract void renderLabel(class_332 var1, int var2, int var3, int var4, int var5, int var6, int var7, float var8, boolean var9);

        public boolean isHovered() {
            return this.hovered;
        }

        public boolean method_25370() {
            return this.focused;
        }

        public void method_25365(boolean focused) {
            this.focused = focused;
        }

        @Nullable
        public class_8016 method_48205(class_8023 pGuiEventListener0) {
            return !this.method_25370() ? class_8016.method_48193((class_364)this) : null;
        }

        @NotNull
        public class_8030 method_48202() {
            return this.bounds;
        }
    }

    public static class TextButtonElement
    extends ButtonElement<TextButtonElement> {
        protected final class_327 font;
        public class_2561 text;

        public TextButtonElement(class_2561 text, Function<TextButtonElement, Boolean> onClick) {
            super(onClick);
            this.font = class_310.method_1551().field_1772;
            this.text = text;
        }

        @Override
        public void renderLabel(class_332 guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
            int textX = x + (int)((double)(width - this.font.method_27525((class_5348)this.text)) * 0.5);
            int textY = y + (int)((double)(height - 8) * 0.5);
            guiGraphics.method_27535(this.font, this.text, textX, textY, 0xFFFFFF);
        }
    }

    public static class IconButtonElement
    extends ButtonElement<IconButtonElement> {
        public final GuiUtil.Icon icon;
        public final GuiUtil.Icon hoveredIcon;

        public IconButtonElement(GuiUtil.Icon icon, GuiUtil.Icon hoveredIcon, Function<IconButtonElement, Boolean> onClick) {
            super(onClick);
            this.icon = icon;
            this.hoveredIcon = hoveredIcon;
        }

        public IconButtonElement(GuiUtil.Icon icon, Function<IconButtonElement, Boolean> onClick) {
            this(icon, icon, onClick);
        }

        @Override
        public void renderLabel(class_332 guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
            int iconX = x + (int)((double)(width - this.icon.getWidth()) * 0.5);
            int iconY = y + (int)((double)(height - this.icon.getHeight()) * 0.5);
            GuiUtil.bindIrisWidgetsTexture();
            if (!this.disabled && (hovered || this.method_25370())) {
                this.hoveredIcon.draw(guiGraphics, iconX, iconY);
            } else {
                this.icon.draw(guiGraphics, iconX, iconY);
            }
        }
    }

    public static abstract class ButtonElement<T extends ButtonElement<T>>
    extends Element {
        private final Function<T, Boolean> onClick;

        protected ButtonElement(Function<T, Boolean> onClick) {
            this.onClick = onClick;
        }

        public boolean method_25402(double mx, double my, int button) {
            if (this.disabled) {
                return false;
            }
            if (button == 0) {
                return this.onClick.apply(this);
            }
            return super.method_25402(mx, my, button);
        }

        public boolean method_25404(int keycode, int scancode, int modifiers) {
            if (keycode == 257) {
                return this.onClick.apply(this);
            }
            return false;
        }
    }
}

