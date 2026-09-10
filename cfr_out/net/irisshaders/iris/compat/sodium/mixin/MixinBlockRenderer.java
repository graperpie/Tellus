/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer
 *  net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
 *  net.minecraft.class_1058
 *  net.minecraft.class_1087
 *  net.minecraft.class_2338
 *  net.minecraft.class_2680
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.compat.sodium.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.class_1058;
import net.minecraft.class_1087;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BlockRenderer.class})
public class MixinBlockRenderer {
    @Unique
    private boolean hasOverride;

    @Inject(method={"renderModel"}, at={@At(value="HEAD")})
    private void iris$renderModelHead(class_1087 model, class_2680 state, class_2338 pos, class_2338 origin, CallbackInfo ci) {
        if (WorldRenderingSettings.INSTANCE.getBlockTypeIds().containsKey(state.method_26204())) {
            this.hasOverride = true;
        }
    }

    @Inject(method={"renderModel"}, at={@At(value="TAIL")})
    private void iris$renderModelTail(class_1087 model, class_2680 state, class_2338 pos, class_2338 origin, CallbackInfo ci) {
        this.hasOverride = false;
    }

    @WrapOperation(method={"bufferQuad"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;attemptPassDowngrade(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;")})
    private TerrainRenderPass iris$skipPassDowngrade(BlockRenderer instance, class_1058 textureAtlasSprite, TerrainRenderPass sprite, Operation<TerrainRenderPass> original) {
        if (this.hasOverride) {
            return null;
        }
        return (TerrainRenderPass)original.call(new Object[]{instance, textureAtlasSprite, sprite});
    }
}

