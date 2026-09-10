/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_6379
 *  net.minecraft.class_6379$class_6380
 *  net.minecraft.class_6382
 *  net.minecraft.class_8016
 *  net.minecraft.class_8023
 *  net.minecraft.class_8030
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.gui.element.widget;

import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElement;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_6379;
import net.minecraft.class_6382;
import net.minecraft.class_8016;
import net.minecraft.class_8023;
import net.minecraft.class_8030;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractElementWidget<T extends OptionMenuElement>
implements class_364,
class_6379 {
    public static final AbstractElementWidget<OptionMenuElement> EMPTY = new AbstractElementWidget<OptionMenuElement>(null){

        @Override
        public void render(class_332 guiGraphics, int mouseX, int mouseY, float tickDelta, boolean hovered) {
        }

        @Override
        @Nullable
        public class_8016 method_48205(class_8023 pGuiEventListener0) {
            return null;
        }

        @Override
        @NotNull
        public class_8030 method_48202() {
            return class_8030.method_48248();
        }
    };
    protected final T element;
    public class_8030 bounds = class_8030.method_48248();
    private boolean focused;

    public AbstractElementWidget(T element) {
        this.element = element;
    }

    public void init(ShaderPackScreen screen, NavigationController navigation) {
    }

    public abstract void render(class_332 var1, int var2, int var3, float var4, boolean var5);

    public boolean method_25402(double mx, double my, int button) {
        return false;
    }

    public boolean method_25406(double mx, double my, int button) {
        return false;
    }

    public boolean method_25404(int keycode, int scancode, int modifiers) {
        return false;
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

    public class_8030 method_48202() {
        return this.bounds;
    }

    public class_6379.class_6380 method_37018() {
        return class_6379.class_6380.field_33784;
    }

    public void method_37020(class_6382 p0) {
    }
}

