/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer
 *  net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager
 *  net.minecraft.class_2586
 *  net.minecraft.class_310
 *  net.minecraft.class_3191
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_4599
 *  net.minecraft.class_746
 *  net.minecraft.class_824
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.compat.sodium.mixin;

import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_2586;
import net.minecraft.class_310;
import net.minecraft.class_3191;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4599;
import net.minecraft.class_746;
import net.minecraft.class_824;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={SodiumWorldRenderer.class})
public class MixinSodiumWorldRenderer {
    @Unique
    private static boolean renderLightsOnly = false;
    @Unique
    private static int beList = 0;
    @Unique
    private float lastSunAngle;

    @Inject(method={"renderBlockEntity"}, at={@At(value="HEAD")}, cancellable=true)
    private static void checkRenderShadow(class_4587 matrices, class_4599 bufferBuilders, Long2ObjectMap<SortedSet<class_3191>> blockBreakingProgressions, float tickDelta, class_4597.class_4598 immediate, double x, double y, double z, class_824 dispatcher, class_2586 entity, class_746 player, LocalBooleanRef isGlowing, CallbackInfo ci) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            if (renderLightsOnly && entity.method_11010().method_26213() == 0) {
                ci.cancel();
            }
            ++beList;
        }
    }

    @Redirect(method={"setupTerrain"}, remap=false, at=@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;needsUpdate()Z", ordinal=0, remap=false))
    private boolean iris$forceChunkGraphRebuildInShadowPass(RenderSectionManager instance) {
        float sunAngle;
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered() && this.lastSunAngle != (sunAngle = class_310.method_1551().field_1687.method_8442(CapturedRenderingState.INSTANCE.getTickDelta()))) {
            this.lastSunAngle = sunAngle;
            return true;
        }
        return instance.needsUpdate();
    }

    @Redirect(method={"setupTerrain"}, remap=false, at=@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;needsUpdate()Z", ordinal=1, remap=false))
    private boolean iris$forceEndGraphRebuild(RenderSectionManager instance) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            return false;
        }
        return instance.needsUpdate();
    }

    static {
        ShadowRenderingState.setBlockEntityRenderFunction((shadowRenderer, bufferSource, modelView, camera, cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum, lightsOnly) -> {
            renderLightsOnly = lightsOnly;
            SodiumWorldRenderer.instance().renderBlockEntities(modelView, bufferSource, ((LevelRendererAccessor)class_310.method_1551().field_1769).getDestructionProgress(), camera, tickDelta, null);
            int finalBeList = beList;
            beList = 0;
            return finalBeList;
        });
    }
}

