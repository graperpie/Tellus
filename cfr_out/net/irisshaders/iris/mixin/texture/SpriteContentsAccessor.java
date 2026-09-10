/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_7764
 *  net.minecraft.class_7764$class_5790
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package net.irisshaders.iris.mixin.texture;

import net.minecraft.class_7764;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={class_7764.class})
public interface SpriteContentsAccessor {
    @Accessor(value="animatedTexture")
    public class_7764.class_5790 getAnimatedTexture();
}

