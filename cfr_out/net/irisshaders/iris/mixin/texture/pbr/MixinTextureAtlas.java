/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1044
 *  net.minecraft.class_1059
 *  net.minecraft.class_2960
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.texture.pbr;

import net.irisshaders.iris.pbr.texture.PBRAtlasHolder;
import net.irisshaders.iris.pbr.texture.TextureAtlasExtension;
import net.minecraft.class_1044;
import net.minecraft.class_1059;
import net.minecraft.class_2960;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1059.class})
public abstract class MixinTextureAtlas
extends class_1044
implements TextureAtlasExtension {
    @Shadow
    @Final
    private class_2960 field_21749;
    @Unique
    private PBRAtlasHolder pbrHolder;

    @Inject(method={"cycleAnimationFrames()V"}, at={@At(value="TAIL")})
    private void iris$onTailCycleAnimationFrames(CallbackInfo ci) {
        if (this.pbrHolder != null) {
            this.pbrHolder.cycleAnimationFrames();
        }
    }

    @Override
    public PBRAtlasHolder getPBRHolder() {
        return this.pbrHolder;
    }

    @Override
    public PBRAtlasHolder getOrCreatePBRHolder() {
        if (this.pbrHolder == null) {
            this.pbrHolder = new PBRAtlasHolder();
        }
        return this.pbrHolder;
    }
}

