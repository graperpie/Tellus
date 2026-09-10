/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_4668
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.layer;

import java.util.Objects;
import java.util.Optional;
import net.irisshaders.batchedentityrendering.impl.BlendingStateHolder;
import net.irisshaders.batchedentityrendering.impl.TransparencyType;
import net.irisshaders.batchedentityrendering.impl.WrappableRenderType;
import net.irisshaders.iris.mixin.rendertype.RenderTypeAccessor;
import net.minecraft.class_1921;
import net.minecraft.class_4668;
import org.jetbrains.annotations.Nullable;

public class OuterWrappedRenderType
extends class_1921
implements WrappableRenderType,
BlendingStateHolder {
    private final class_4668 extra;
    private final class_1921 wrapped;

    public OuterWrappedRenderType(String name, class_1921 wrapped, class_4668 extra) {
        super(name, wrapped.method_23031(), wrapped.method_23033(), wrapped.method_22722(), wrapped.method_23037(), OuterWrappedRenderType.shouldSortOnUpload(wrapped), () -> ((class_1921)wrapped).method_23516(), () -> ((class_1921)wrapped).method_23518());
        this.extra = extra;
        this.wrapped = wrapped;
    }

    public static OuterWrappedRenderType wrapExactlyOnce(String name, class_1921 wrapped, class_4668 extra) {
        if (wrapped instanceof OuterWrappedRenderType) {
            wrapped = ((OuterWrappedRenderType)wrapped).unwrap();
        }
        return new OuterWrappedRenderType(name, wrapped, extra);
    }

    private static boolean shouldSortOnUpload(class_1921 type) {
        return ((RenderTypeAccessor)type).shouldSortOnUpload();
    }

    public void method_23516() {
        this.extra.method_23516();
        super.method_23516();
    }

    public void method_23518() {
        super.method_23518();
        this.extra.method_23518();
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
        OuterWrappedRenderType other = (OuterWrappedRenderType)object;
        return Objects.equals(this.wrapped, other.wrapped) && Objects.equals(this.extra, other.extra);
    }

    public int hashCode() {
        return this.wrapped.hashCode() + 1;
    }

    public String toString() {
        return "iris_wrapped:" + this.wrapped.toString();
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

