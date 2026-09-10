/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.batchedentityrendering.impl.wrappers;

import java.util.Objects;
import java.util.Optional;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.irisshaders.batchedentityrendering.mixin.RenderTypeAccessor;
import net.minecraft.class_1921;
import org.jetbrains.annotations.Nullable;

public class TaggingRenderTypeWrapper
extends class_1921
implements WrappableRenderType,
BlendingStateHolder {
    private final int tag;
    private final class_1921 wrapped;

    public TaggingRenderTypeWrapper(String name, class_1921 wrapped, int tag) {
        super(name, wrapped.method_23031(), wrapped.method_23033(), wrapped.method_22722(), wrapped.method_23037(), TaggingRenderTypeWrapper.shouldSortOnUpload(wrapped), () -> ((class_1921)wrapped).method_23516(), () -> ((class_1921)wrapped).method_23518());
        this.tag = tag;
        this.wrapped = wrapped;
    }

    private static boolean shouldSortOnUpload(class_1921 type) {
        return ((RenderTypeAccessor)type).shouldSortOnUpload();
    }

    @Override
    public class_1921 unwrap() {
        return this.wrapped;
    }

    public Optional<class_1921> method_23289() {
        return this.wrapped.method_23289();
    }

    public boolean method_24295() {
        return this.wrapped.method_24295();
    }

    public boolean equals(@Nullable Object object) {
        if (object == null) {
            return false;
        }
        if (object.getClass() != this.getClass()) {
            return false;
        }
        TaggingRenderTypeWrapper other = (TaggingRenderTypeWrapper)object;
        return this.tag == other.tag && Objects.equals(this.wrapped, other.wrapped);
    }

    public int hashCode() {
        return this.wrapped.hashCode() + this.tag + 1;
    }

    public String toString() {
        return "tagged(" + this.tag + "):" + this.wrapped.toString();
    }

    @Override
    public TransparencyType getTransparencyType() {
        return ((BlendingStateHolder)this.wrapped).getTransparencyType();
    }

    @Override
    public void setTransparencyType(TransparencyType transparencyType) {
        ((BlendingStateHolder)this.wrapped).setTransparencyType(transparencyType);
    }
}

