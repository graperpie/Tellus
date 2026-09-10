/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1079
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package net.irisshaders.iris.mixin.texture;

import net.minecraft.class_1079;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={class_1079.class})
public interface AnimationMetadataSectionAccessor {
    @Accessor(value="frameWidth")
    public int getFrameWidth();

    @Mutable
    @Accessor(value="frameWidth")
    public void setFrameWidth(int var1);

    @Accessor(value="frameHeight")
    public int getFrameHeight();

    @Mutable
    @Accessor(value="frameHeight")
    public void setFrameHeight(int var1);
}

