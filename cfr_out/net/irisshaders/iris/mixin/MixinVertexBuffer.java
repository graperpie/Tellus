/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_291
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.helpers.VertexBufferHelper;
import net.minecraft.class_291;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_291.class})
public class MixinVertexBuffer
implements VertexBufferHelper {
    @Unique
    private static class_291 current;
    @Unique
    private static class_291 saved;

    @Inject(method={"unbind()V"}, at={@At(value="HEAD")})
    private static void unbindHelper(CallbackInfo ci) {
        current = null;
    }

    @Shadow
    public void method_1353() {
        throw new IllegalStateException("not shadowed");
    }

    @Inject(method={"bind()V"}, at={@At(value="HEAD")})
    private void bindHelper(CallbackInfo ci) {
        current = (class_291)this;
    }

    @Override
    public void saveBinding() {
        saved = current;
    }

    @Override
    public void restoreBinding() {
        if (saved != null) {
            saved.method_1353();
        } else {
            class_291.method_1354();
        }
    }
}

