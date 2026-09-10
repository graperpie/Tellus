/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 */
package net.irisshaders.batchedentityrendering.impl.ordering;

import java.util.LinkedHashSet;
import java.util.List;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.ordering.RenderOrderManager;
import net.minecraft.class_1921;

public class SimpleRenderOrderManager
implements RenderOrderManager {
    private final LinkedHashSet<class_1921> renderTypes = new LinkedHashSet();

    @Override
    public void begin(class_1921 type) {
        this.renderTypes.add(type);
    }

    @Override
    public void startGroup() {
    }

    @Override
    public boolean maybeStartGroup() {
        return false;
    }

    @Override
    public boolean isInGroup() {
        return false;
    }

    @Override
    public void endGroup() {
    }

    @Override
    public void reset() {
        this.renderTypes.clear();
    }

    @Override
    public void resetType(TransparencyType type) {
    }

    @Override
    public List<class_1921> getRenderOrder() {
        return List.copyOf(this.renderTypes);
    }
}

