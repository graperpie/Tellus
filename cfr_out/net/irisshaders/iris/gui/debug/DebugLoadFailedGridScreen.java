/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_339
 *  net.minecraft.class_364
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 *  net.minecraft.class_7843
 *  net.minecraft.class_7845
 *  net.minecraft.class_7847
 *  net.minecraft.class_8021
 *  org.apache.commons.lang3.exception.ExceptionUtils
 */
package net.irisshaders.iris.gui.debug;

import java.io.IOException;
import java.util.Objects;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.debug.DebugTextWidget;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_339;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_7843;
import net.minecraft.class_7845;
import net.minecraft.class_7847;
import net.minecraft.class_8021;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class DebugLoadFailedGridScreen
extends class_437 {
    private final Exception exception;
    private final class_437 parent;

    public DebugLoadFailedGridScreen(class_437 parent, class_2561 arg, Exception exception) {
        super(arg);
        this.parent = parent;
        this.exception = exception;
    }

    protected void method_25426() {
        super.method_25426();
        class_7845 widget = new class_7845();
        class_7847 layoutSettings = widget.method_46457().method_46472().method_46467();
        class_7847 layoutSettings4 = widget.method_46457().method_46472().method_46471(30).method_46467();
        class_7847 layoutSettings2 = widget.method_46457().method_46472().method_46471(30).method_46461();
        class_7847 layoutSettings3 = widget.method_46457().method_46472().method_46471(30).method_46470();
        int numWidgets = 0;
        Objects.requireNonNull(this.field_22793);
        widget.method_46454((class_8021)new DebugTextWidget(0, 0, this.field_22789 - 80, 9 * 15, this.field_22793, this.exception), ++numWidgets, 0, 1, 2, layoutSettings);
        widget.method_46454((class_8021)class_4185.method_46430((class_2561)class_2561.method_43471((String)"menu.returnToGame"), arg2 -> this.field_22787.method_1507(this.parent)).method_46432(100).method_46431(), ++numWidgets, 0, 1, 2, layoutSettings2);
        widget.method_46454((class_8021)class_4185.method_46430((class_2561)class_2561.method_43470((String)"Reload pack"), arg2 -> {
            class_310.method_1551().method_1507(this.parent);
            try {
                Iris.reload();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).method_46432(100).method_46431(), numWidgets, 0, 1, 2, layoutSettings3);
        widget.method_46454((class_8021)class_4185.method_46430((class_2561)class_2561.method_43470((String)"Copy error"), arg2 -> this.field_22787.field_1774.method_1455(ExceptionUtils.getStackTrace((Throwable)this.exception))).method_46432(100).method_46431(), numWidgets, 0, 1, 2, layoutSettings4);
        widget.method_48222();
        class_7843.method_46442((class_8021)widget, (int)0, (int)0, (int)this.field_22789, (int)this.field_22790);
        widget.method_48206(x$0 -> {
            class_339 cfr_ignored_0 = (class_339)this.method_37063((class_364)x$0);
        });
    }
}

