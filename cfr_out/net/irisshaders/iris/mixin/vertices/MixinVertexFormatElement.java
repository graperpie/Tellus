/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_296
 *  net.minecraft.class_296$class_298
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin.vertices;

import net.minecraft.class_296;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_296.class})
public class MixinVertexFormatElement {
    @Inject(method={"supportsUsage"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$fixGenericAttributes(int index, class_296.class_298 type, CallbackInfoReturnable<Boolean> cir) {
        if (type == class_296.class_298.field_20782) {
            cir.setReturnValue((Object)true);
        }
    }
}

