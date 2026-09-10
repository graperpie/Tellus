/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 */
package net.irisshaders.iris.gui.element.widget;

import java.util.Optional;
import net.irisshaders.iris.gui.element.widget.AbstractElementWidget;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElement;
import net.minecraft.class_2561;

public abstract class CommentedElementWidget<T extends OptionMenuElement>
extends AbstractElementWidget<T> {
    public CommentedElementWidget(T element) {
        super(element);
    }

    public abstract Optional<class_2561> getCommentTitle();

    public abstract Optional<class_2561> getCommentBody();
}

