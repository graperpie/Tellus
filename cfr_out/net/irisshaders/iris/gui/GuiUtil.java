/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1074
 *  net.minecraft.class_1109
 *  net.minecraft.class_1113
 *  net.minecraft.class_1921
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_3417
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 *  net.minecraft.class_6880
 */
package net.irisshaders.iris.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_1074;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_1921;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3417;
import net.minecraft.class_5250;
import net.minecraft.class_5348;
import net.minecraft.class_6880;

public final class GuiUtil {
    public static final class_2960 IRIS_WIDGETS_TEX = class_2960.method_60655((String)"iris", (String)"textures/gui/widgets.png");
    private static final class_2561 ELLIPSIS = class_2561.method_43470((String)"...");

    private GuiUtil() {
    }

    private static class_310 client() {
        return class_310.method_1551();
    }

    public static void bindIrisWidgetsTexture() {
        RenderSystem.setShaderTexture((int)0, (class_2960)IRIS_WIDGETS_TEX);
    }

    public static void drawButton(class_332 guiGraphics, int x, int y, int width, int height, boolean hovered, boolean disabled) {
        int halfWidth = width / 2;
        int halfHeight = height / 2;
        int vOffset = disabled ? 46 : (hovered ? 86 : 66);
        RenderSystem.enableBlend();
        guiGraphics.method_25290(IRIS_WIDGETS_TEX, x, y, 0.0f, (float)vOffset, halfWidth, halfHeight, 256, 256);
        guiGraphics.method_25290(IRIS_WIDGETS_TEX, x + halfWidth, y, (float)(200 - (width - halfWidth)), (float)vOffset, width - halfWidth, halfHeight, 256, 256);
        guiGraphics.method_25290(IRIS_WIDGETS_TEX, x, y + halfHeight, 0.0f, (float)(vOffset + (20 - (height - halfHeight))), halfWidth, height - halfHeight, 256, 256);
        guiGraphics.method_25290(IRIS_WIDGETS_TEX, x + halfWidth, y + halfHeight, (float)(200 - (width - halfWidth)), (float)(vOffset + (20 - (height - halfHeight))), width - halfWidth, height - halfHeight, 256, 256);
    }

    public static void drawPanel(class_332 guiGraphics, int x, int y, int width, int height) {
        int borderColor = -555819298;
        int innerColor = -570425344;
        guiGraphics.method_51739(class_1921.method_51785(), x, y, x + width, y + 1, borderColor);
        guiGraphics.method_51739(class_1921.method_51785(), x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.method_51739(class_1921.method_51785(), x, y + 1, x + 1, y + height - 1, borderColor);
        guiGraphics.method_51739(class_1921.method_51785(), x + width - 1, y + 1, x + width, y + height - 1, borderColor);
        guiGraphics.method_51739(class_1921.method_51785(), x + 1, y + 1, x + width - 1, y + height - 1, innerColor);
    }

    public static void drawTextPanel(class_327 font, class_332 guiGraphics, class_2561 text, int x, int y) {
        GuiUtil.drawPanel(guiGraphics, x, y, font.method_27525((class_5348)text) + 8, 16);
        guiGraphics.method_27535(font, text, x + 4, y + 4, 0xFFFFFF);
    }

    public static class_5250 shortenText(class_327 font, class_5250 text, int width) {
        if (font.method_27525((class_5348)text) > width) {
            return class_2561.method_43470((String)font.method_27523(text.getString(), width - font.method_27525((class_5348)ELLIPSIS))).method_10852(ELLIPSIS).method_10862(text.method_10866());
        }
        return text;
    }

    public static class_5250 translateOrDefault(class_5250 defaultText, String translationDesc, Object ... format) {
        if (class_1074.method_4663((String)translationDesc)) {
            return class_2561.method_43469((String)translationDesc, (Object[])format);
        }
        return defaultText;
    }

    public static void playButtonClickSound() {
        GuiUtil.client().method_1483().method_4873((class_1113)class_1109.method_47978((class_6880)class_3417.field_15015, (float)1.0f));
    }

    public static class Icon {
        public static final Icon SEARCH = new Icon(0, 0, 7, 8);
        public static final Icon CLOSE = new Icon(7, 0, 5, 6);
        public static final Icon REFRESH = new Icon(12, 0, 10, 10);
        public static final Icon EXPORT = new Icon(22, 0, 7, 8);
        public static final Icon EXPORT_COLORED = new Icon(29, 0, 7, 8);
        public static final Icon IMPORT = new Icon(22, 8, 7, 8);
        public static final Icon IMPORT_COLORED = new Icon(29, 8, 7, 8);
        private final int u;
        private final int v;
        private final int width;
        private final int height;

        public Icon(int u, int v, int width, int height) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
        }

        public void draw(class_332 guiGraphics, int x, int y) {
            RenderSystem.enableBlend();
            guiGraphics.method_25290(IRIS_WIDGETS_TEX, x, y, (float)this.u, (float)this.v, this.width, this.height, 256, 256);
        }

        public int getWidth() {
            return this.width;
        }

        public int getHeight() {
            return this.height;
        }
    }
}

