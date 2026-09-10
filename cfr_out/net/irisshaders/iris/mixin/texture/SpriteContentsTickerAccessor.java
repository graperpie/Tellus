/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_7764$class_5790
 *  net.minecraft.class_7764$class_7765
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package net.irisshaders.iris.mixin.texture;

import net.minecraft.class_7764;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={class_7764.class_7765.class})
public interface SpriteContentsTickerAccessor {
    @Accessor(value="frame")
    public int getFrame();

    @Accessor(value="frame")
    public void setFrame(int var1);

    @Accessor(value="subFrame")
    public int getSubFrame();

    @Accessor(value="subFrame")
    public void setSubFrame(int var1);

    @Accessor(value="animationInfo")
    public class_7764.class_5790 getAnimationInfo();
}

