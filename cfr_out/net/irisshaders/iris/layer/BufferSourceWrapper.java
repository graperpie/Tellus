/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_4588
 *  net.minecraft.class_4597
 */
package net.irisshaders.iris.layer;

import java.util.function.Function;
import net.irisshaders.batchedentityrendering.impl.Groupable;
import net.minecraft.class_1921;
import net.minecraft.class_4588;
import net.minecraft.class_4597;

public class BufferSourceWrapper
implements class_4597,
Groupable {
    private final class_4597 bufferSource;
    private final Function<class_1921, class_1921> typeChanger;

    public BufferSourceWrapper(class_4597 bufferSource, Function<class_1921, class_1921> typeChanger) {
        this.bufferSource = bufferSource;
        this.typeChanger = typeChanger;
    }

    public class_4597 getOriginal() {
        return this.bufferSource;
    }

    @Override
    public void startGroup() {
        class_4597 class_45972 = this.bufferSource;
        if (class_45972 instanceof Groupable) {
            Groupable groupable = (Groupable)class_45972;
            groupable.startGroup();
        }
    }

    @Override
    public boolean maybeStartGroup() {
        class_4597 class_45972 = this.bufferSource;
        if (class_45972 instanceof Groupable) {
            Groupable groupable = (Groupable)class_45972;
            return groupable.maybeStartGroup();
        }
        return false;
    }

    @Override
    public void endGroup() {
        class_4597 class_45972 = this.bufferSource;
        if (class_45972 instanceof Groupable) {
            Groupable groupable = (Groupable)class_45972;
            groupable.endGroup();
        }
    }

    public class_4588 getBuffer(class_1921 renderType) {
        return this.bufferSource.getBuffer(this.typeChanger.apply(renderType));
    }
}

