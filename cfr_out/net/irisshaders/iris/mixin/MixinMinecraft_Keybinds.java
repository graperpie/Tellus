/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_3695
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.minecraft.class_310;
import net.minecraft.class_3695;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_310.class})
public class MixinMinecraft_Keybinds {
    @Shadow
    private class_3695 field_16240;

    @Inject(method={"tick()V"}, at={@At(value="RETURN")})
    private void iris$onTick(CallbackInfo ci) {
        this.field_16240.method_15396("iris_keybinds");
        Iris.handleKeybinds((class_310)this);
        this.field_16240.method_15407();
    }
}

