/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_4599
 */
package net.irisshaders.iris.shadows;

import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4599;

public class ShadowRenderingState {
    private static BlockEntityRenderFunction function = ShadowRenderer::renderBlockEntities;

    public static boolean areShadowsCurrentlyBeingRendered() {
        return ShadowRenderer.ACTIVE;
    }

    public static void setBlockEntityRenderFunction(BlockEntityRenderFunction function) {
        ShadowRenderingState.function = function;
    }

    public static int renderBlockEntities(ShadowRenderer shadowRenderer, class_4599 bufferSource, class_4587 modelView, class_4184 camera, double cameraX, double cameraY, double cameraZ, float tickDelta, boolean hasEntityFrustum, boolean lightsOnly) {
        return function.renderBlockEntities(shadowRenderer, bufferSource, modelView, camera, cameraX, cameraY, cameraZ, tickDelta, hasEntityFrustum, lightsOnly);
    }

    public static int getRenderDistance() {
        return ShadowRenderer.renderDistance;
    }

    public static interface BlockEntityRenderFunction {
        public int renderBlockEntities(ShadowRenderer var1, class_4599 var2, class_4587 var3, class_4184 var4, double var5, double var7, double var9, float var11, boolean var12, boolean var13);
    }
}

