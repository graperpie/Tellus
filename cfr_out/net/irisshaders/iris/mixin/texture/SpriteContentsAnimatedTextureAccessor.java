/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_7764$class_5790
 *  net.minecraft.class_7764$class_5791
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package net.irisshaders.iris.mixin.texture;

import java.util.List;
import net.minecraft.class_7764;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={class_7764.class_5790.class})
public interface SpriteContentsAnimatedTextureAccessor {
    @Accessor(value="frames")
    public List<class_7764.class_5791> getFrames();

    @Invoker(value="uploadFrame")
    public void invokeUploadFrame(int var1, int var2, int var3);
}

