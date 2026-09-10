/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 */
package net.irisshaders.batchedentityrendering.impl.ordering;

import java.util.List;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.minecraft.class_1921;

public interface RenderOrderManager {
    public void begin(class_1921 var1);

    public void startGroup();

    public boolean maybeStartGroup();

    public boolean isInGroup();

    public void endGroup();

    public void reset();

    public void resetType(TransparencyType var1);

    public List<class_1921> getRenderOrder();
}

