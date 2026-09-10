/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1058
 *  net.minecraft.class_1059
 *  net.minecraft.class_2960
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package net.irisshaders.iris.mixin.texture;

import java.util.Map;
import net.minecraft.class_1058;
import net.minecraft.class_1059;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={class_1059.class})
public interface TextureAtlasAccessor {
    @Accessor(value="texturesByName")
    public Map<class_2960, class_1058> getTexturesByName();

    @Accessor(value="mipLevel")
    public int getMipLevel();

    @Invoker(value="getWidth")
    public int callGetWidth();

    @Invoker(value="getHeight")
    public int callGetHeight();
}

