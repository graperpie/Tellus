/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 */
package net.irisshaders.batchedentityrendering.impl.ordering;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.irisshaders.batchedentityrendering.impl.ordering.RenderOrderManager;
import net.minecraft.class_1921;

public class TranslucencyRenderOrderManager
implements RenderOrderManager {
    private final EnumMap<TransparencyType, LinkedHashSet<class_1921>> renderTypes = new EnumMap(TransparencyType.class);

    public TranslucencyRenderOrderManager() {
        for (TransparencyType type : TransparencyType.values()) {
            this.renderTypes.put(type, new LinkedHashSet());
        }
    }

    private static TransparencyType getTransparencyType(class_1921 type) {
        while (type instanceof WrappableRenderType) {
            type = ((WrappableRenderType)type).unwrap();
        }
        if (type instanceof BlendingStateHolder) {
            return ((BlendingStateHolder)type).getTransparencyType();
        }
        return TransparencyType.GENERAL_TRANSPARENT;
    }

    @Override
    public void begin(class_1921 type) {
        this.renderTypes.get((Object)TranslucencyRenderOrderManager.getTransparencyType(type)).add(type);
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

    public int getGroupId() {
        return 0;
    }

    @Override
    public void endGroup() {
    }

    @Override
    public void reset() {
        this.renderTypes.forEach((type, set) -> set.clear());
    }

    @Override
    public void resetType(TransparencyType type) {
        this.renderTypes.get((Object)type).clear();
    }

    @Override
    public List<class_1921> getRenderOrder() {
        int layerCount = 0;
        for (LinkedHashSet<class_1921> set : this.renderTypes.values()) {
            layerCount += set.size();
        }
        ArrayList<class_1921> allRenderTypes = new ArrayList<class_1921>(layerCount);
        for (LinkedHashSet<class_1921> set : this.renderTypes.values()) {
            allRenderTypes.addAll(set);
        }
        return allRenderTypes;
    }
}

