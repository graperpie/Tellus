/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  net.minecraft.class_4184
 *  net.minecraft.class_4604
 *  net.minecraft.class_761
 *  net.minecraft.class_846$class_851
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.shadows;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.class_4184;
import net.minecraft.class_4604;
import net.minecraft.class_761;
import net.minecraft.class_846;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_761.class}, priority=1010)
public abstract class MixinPreventRebuildNearInShadowPass {
    @Shadow
    @Final
    private ObjectArrayList<class_846.class_851> field_45616;

    @Inject(method={"setupRender"}, at={@At(value="TAIL")})
    private void iris$preventRebuildNearInShadowPass(class_4184 camera, class_4604 frustum, boolean bl, boolean bl2, CallbackInfo ci) {
        if (ShadowRenderer.ACTIVE) {
            for (class_846.class_851 chunk : this.field_45616) {
                ShadowRenderer.visibleBlockEntities.addAll(chunk.method_3677().method_3642());
            }
        }
    }
}

