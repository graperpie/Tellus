/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_315
 *  net.minecraft.class_4063
 *  net.minecraft.class_7172
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin.sky;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.properties.CloudSetting;
import net.minecraft.class_315;
import net.minecraft.class_4063;
import net.minecraft.class_7172;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_315.class}, priority=1010)
public class MixinOptions_CloudsOverride {
    @Shadow
    @Final
    private class_7172<Integer> field_1870;

    @Inject(method={"getCloudsType"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$overrideCloudsType(CallbackInfoReturnable<class_4063> cir) {
        if ((Integer)this.field_1870.method_41753() < 4) {
            return;
        }
        Iris.getPipelineManager().getPipeline().ifPresent(p -> {
            CloudSetting setting = p.getCloudSetting();
            switch (setting) {
                case OFF: {
                    cir.setReturnValue((Object)class_4063.field_18162);
                    return;
                }
                case FAST: {
                    cir.setReturnValue((Object)class_4063.field_18163);
                    return;
                }
                case FANCY: {
                    cir.setReturnValue((Object)class_4063.field_18164);
                }
            }
        });
    }
}

