/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_638
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 */
package net.irisshaders.iris.mixin.vertices.block_rendering;

import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.class_638;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value={class_638.class})
public class MixinClientLevel {
    @ModifyVariable(method={"getShade"}, at=@At(value="HEAD"), argsOnly=true)
    private boolean iris$maybeDisableDirectionalShading(boolean shaded) {
        if (WorldRenderingSettings.INSTANCE.shouldDisableDirectionalShading()) {
            return false;
        }
        return shaded;
    }
}

