/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1011
 *  net.minecraft.class_4725
 *  net.minecraft.class_7764
 *  net.minecraft.class_7764$class_7765
 *  net.minecraft.class_7768
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin.texture;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pbr.SpriteContentsExtension;
import net.irisshaders.iris.pbr.mipmap.CustomMipmapGenerator;
import net.minecraft.class_1011;
import net.minecraft.class_4725;
import net.minecraft.class_7764;
import net.minecraft.class_7768;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_7764.class})
public class MixinSpriteContents
implements SpriteContentsExtension {
    @Unique
    @Nullable
    private class_7764.class_7765 createdTicker;

    @Redirect(method={"increaseMipLevel"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/texture/MipmapGenerator;generateMipLevels([Lcom/mojang/blaze3d/platform/NativeImage;I)[Lcom/mojang/blaze3d/platform/NativeImage;"))
    private class_1011[] iris$redirectMipmapGeneration(class_1011[] nativeImages, int mipLevel) {
        CustomMipmapGenerator.Provider provider;
        CustomMipmapGenerator generator;
        MixinSpriteContents mixinSpriteContents = this;
        if (mixinSpriteContents instanceof CustomMipmapGenerator.Provider && (generator = (provider = (CustomMipmapGenerator.Provider)((Object)mixinSpriteContents)).getMipmapGenerator()) != null) {
            try {
                return generator.generateMipLevels(nativeImages, mipLevel);
            }
            catch (Exception e) {
                Iris.logger.error("ERROR MIPMAPPING", e);
            }
        }
        return class_4725.method_24102((class_1011[])nativeImages, (int)mipLevel);
    }

    @Inject(method={"createTicker()Lnet/minecraft/client/renderer/texture/SpriteTicker;"}, at={@At(value="RETURN")})
    private void onReturnCreateTicker(CallbackInfoReturnable<class_7768> cir) {
        class_7768 ticker = (class_7768)cir.getReturnValue();
        if (ticker instanceof class_7764.class_7765) {
            class_7764.class_7765 innerTicker;
            this.createdTicker = innerTicker = (class_7764.class_7765)ticker;
        }
    }

    @Override
    @Nullable
    public class_7764.class_7765 getCreatedTicker() {
        return this.createdTicker;
    }
}

