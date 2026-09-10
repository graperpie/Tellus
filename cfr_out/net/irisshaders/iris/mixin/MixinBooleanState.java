/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_1018
 *  org.lwjgl.opengl.GL11
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.gl.BooleanStateExtended;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={GlStateManager.class_1018.class})
public class MixinBooleanState
implements BooleanStateExtended {
    @Shadow
    public boolean field_5051;
    @Shadow
    @Final
    private int field_5050;
    @Unique
    private boolean stateUnknown;

    @Inject(method={"setEnabled"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$setUnknownState(boolean enable, CallbackInfo ci) {
        if (this.stateUnknown) {
            ci.cancel();
            this.field_5051 = enable;
            this.stateUnknown = false;
            if (enable) {
                GL11.glEnable((int)this.field_5050);
            } else {
                GL11.glDisable((int)this.field_5050);
            }
        }
    }

    @Override
    public void setUnknownState() {
        this.stateUnknown = true;
    }
}

