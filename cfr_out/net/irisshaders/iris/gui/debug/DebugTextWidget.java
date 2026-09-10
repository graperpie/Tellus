/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_5250
 *  net.minecraft.class_6381
 *  net.minecraft.class_6382
 *  net.minecraft.class_7528
 *  net.minecraft.class_7845
 *  net.minecraft.class_7845$class_7939
 *  net.minecraft.class_7847
 *  net.minecraft.class_7852
 *  net.minecraft.class_7940
 *  net.minecraft.class_8021
 */
package net.irisshaders.iris.gui.debug;

import java.util.Objects;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_5250;
import net.minecraft.class_6381;
import net.minecraft.class_6382;
import net.minecraft.class_7528;
import net.minecraft.class_7845;
import net.minecraft.class_7847;
import net.minecraft.class_7852;
import net.minecraft.class_7940;
import net.minecraft.class_8021;

public class DebugTextWidget
extends class_7528 {
    private final class_327 font;
    private final Content content;

    public DebugTextWidget(int i, int j, int k, int l, class_327 arg, Exception exception) {
        super(i, j, k, l, (class_2561)class_2561.method_43473());
        this.font = arg;
        this.content = this.buildContent(exception);
    }

    private Content buildContent(Exception exception) {
        if (exception instanceof ShaderCompileException) {
            ShaderCompileException sce = (ShaderCompileException)exception;
            return this.buildContentShader(sce);
        }
        ContentBuilder lv = new ContentBuilder(this.containerWidth());
        StackTraceElement[] elements = exception.getStackTrace();
        lv.addHeader(this.font, (class_2561)class_2561.method_43470((String)"Error: "));
        Objects.requireNonNull(this.font);
        lv.addSpacer(9);
        if (exception.getMessage() != null) {
            lv.addLine(this.font, (class_2561)class_2561.method_43470((String)exception.getMessage()));
        }
        Objects.requireNonNull(this.font);
        lv.addSpacer(9);
        lv.addHeader(this.font, (class_2561)class_2561.method_43470((String)"Stack trace: "));
        Objects.requireNonNull(this.font);
        lv.addSpacer(9);
        for (int i = 0; i < elements.length; ++i) {
            StackTraceElement element = elements[i];
            if (element == null) continue;
            lv.addLine(this.font, (class_2561)class_2561.method_43470((String)element.toString()));
            if (i >= elements.length - 1) continue;
            Objects.requireNonNull(this.font);
            lv.addSpacer(9);
        }
        return lv.build();
    }

    private Content buildContentShader(ShaderCompileException sce) {
        ContentBuilder lv = new ContentBuilder(this.containerWidth());
        lv.addHeader(this.font, (class_2561)class_2561.method_43470((String)("Shader compile error in " + sce.getFilename() + ": ")));
        Objects.requireNonNull(this.font);
        lv.addSpacer(9);
        lv.addLine(this.font, (class_2561)class_2561.method_43470((String)sce.getError()));
        return lv.build();
    }

    protected int method_44391() {
        return this.content.container().method_25364();
    }

    protected boolean method_44392() {
        return this.method_44391() > this.field_22759;
    }

    protected double method_44393() {
        Objects.requireNonNull(this.font);
        return 9.0;
    }

    protected void method_44389(class_332 arg, int i, int j, float f) {
        int k = this.method_46427() + this.method_44381();
        int l = this.method_46426() + this.method_44381();
        arg.method_51448().method_22903();
        arg.method_51448().method_22904((double)l, (double)k, 0.0);
        this.content.container().method_48206(element -> element.method_25394(arg, i, j, f));
        arg.method_51448().method_22909();
    }

    protected void method_47399(class_6382 arg) {
        arg.method_37034(class_6381.field_33788, this.content.narration());
    }

    private int containerWidth() {
        return this.field_22758 - this.method_44385();
    }

    record Content(class_7845 container, class_2561 narration) {
    }

    static class ContentBuilder {
        private final int width;
        private final class_7845 grid;
        private final class_7845.class_7939 helper;
        private final class_7847 alignHeader;
        private final class_5250 narration = class_2561.method_43473();

        public ContentBuilder(int i) {
            this.width = i;
            this.grid = new class_7845();
            this.grid.method_46458().method_46461();
            this.helper = this.grid.method_47610(1);
            this.helper.method_47612((class_8021)class_7852.method_46512((int)i));
            this.alignHeader = this.helper.method_47611().method_46467().method_46477(32);
        }

        public void addLine(class_327 arg, class_2561 arg2) {
            this.addLine(arg, arg2, 0);
        }

        public void addLine(class_327 arg, class_2561 arg2, int i) {
            this.helper.method_47615((class_8021)new class_7940(this.width, 1, arg2, arg), this.helper.method_47611().method_46475(i));
            this.narration.method_10852(arg2).method_27693("\n");
        }

        public void addHeader(class_327 arg, class_2561 arg2) {
            this.helper.method_47615((class_8021)new class_7940(this.width - 64, 1, arg2, arg).method_48981(true), this.alignHeader);
            this.narration.method_10852(arg2).method_27693("\n");
        }

        public void addSpacer(int i) {
            this.helper.method_47612((class_8021)class_7852.method_46513((int)i));
        }

        public Content build() {
            this.grid.method_48222();
            return new Content(this.grid, (class_2561)this.narration);
        }
    }
}

