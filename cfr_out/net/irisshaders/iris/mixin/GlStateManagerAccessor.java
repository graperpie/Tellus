/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  com.mojang.blaze3d.platform.GlStateManager$class_1017
 *  com.mojang.blaze3d.platform.GlStateManager$class_1022
 *  com.mojang.blaze3d.platform.GlStateManager$class_1026
 *  com.mojang.blaze3d.platform.GlStateManager$class_1039
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package net.irisshaders.iris.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={GlStateManager.class}, remap=false)
public interface GlStateManagerAccessor {
    @Accessor(value="BLEND")
    public static GlStateManager.class_1017 getBLEND() {
        throw new UnsupportedOperationException("Not accessed");
    }

    @Accessor(value="COLOR_MASK")
    public static GlStateManager.class_1022 getCOLOR_MASK() {
        throw new UnsupportedOperationException("Not accessed");
    }

    @Accessor(value="DEPTH")
    public static GlStateManager.class_1026 getDEPTH() {
        throw new UnsupportedOperationException("Not accessed");
    }

    @Accessor(value="activeTexture")
    public static int getActiveTexture() {
        throw new UnsupportedOperationException("Not accessed");
    }

    @Accessor(value="TEXTURES")
    public static GlStateManager.class_1039[] getTEXTURES() {
        throw new UnsupportedOperationException("Not accessed");
    }
}

