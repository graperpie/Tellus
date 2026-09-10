/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4668
 *  net.minecraft.class_4668$class_4685
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package net.irisshaders.iris.mixin.rendertype;

import net.minecraft.class_4668;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={class_4668.class})
public interface RenderStateShardAccessor {
    @Accessor(value="TRANSLUCENT_TRANSPARENCY")
    public static class_4668.class_4685 getTranslucentTransparency() {
        throw new AssertionError();
    }

    @Accessor(value="name")
    public String getName();
}

