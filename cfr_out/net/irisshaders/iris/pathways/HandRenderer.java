/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1268
 *  net.minecraft.class_1304
 *  net.minecraft.class_1309
 *  net.minecraft.class_1657
 *  net.minecraft.class_1747
 *  net.minecraft.class_1792
 *  net.minecraft.class_1921
 *  net.minecraft.class_1934
 *  net.minecraft.class_2680
 *  net.minecraft.class_310
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597$class_4598
 *  net.minecraft.class_4696
 *  net.minecraft.class_757
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.mixin.GameRendererAccessor;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.minecraft.class_1268;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1921;
import net.minecraft.class_1934;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4696;
import net.minecraft.class_757;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class HandRenderer {
    public static final HandRenderer INSTANCE = new HandRenderer();
    public static final float DEPTH = 0.125f;
    private final FullyBufferedMultiBufferSource bufferSource = new FullyBufferedMultiBufferSource();
    private boolean ACTIVE;
    private boolean renderingSolid;

    private class_4587 setupGlState(class_757 gameRenderer, class_4184 camera, Matrix4fc modelMatrix, float tickDelta) {
        class_4587 poseStack = new class_4587();
        Matrix4f scaleMatrix = new Matrix4f().scale(1.0f, 1.0f, 0.125f);
        scaleMatrix.mul((Matrix4fc)gameRenderer.method_22973(((GameRendererAccessor)gameRenderer).invokeGetFov(camera, tickDelta, false)));
        gameRenderer.method_22709(scaleMatrix);
        poseStack.method_34426();
        ((GameRendererAccessor)gameRenderer).invokeBobHurt(poseStack, tickDelta);
        if (((Boolean)class_310.method_1551().field_1690.method_42448().method_41753()).booleanValue()) {
            ((GameRendererAccessor)gameRenderer).invokeBobView(poseStack, tickDelta);
        }
        return poseStack;
    }

    private boolean canRender(class_4184 camera, class_757 gameRenderer) {
        return ((GameRendererAccessor)gameRenderer).getRenderHand() && !camera.method_19333() && camera.method_19331() instanceof class_1657 && !((GameRendererAccessor)gameRenderer).getPanoramicMode() && !class_310.method_1551().field_1690.field_1842 && (!(camera.method_19331() instanceof class_1309) || !((class_1309)camera.method_19331()).method_6113()) && class_310.method_1551().field_1761.method_2920() != class_1934.field_9219;
    }

    public boolean isHandTranslucent(class_1268 hand) {
        class_1792 item = class_310.method_1551().field_1724.method_6118(hand == class_1268.field_5810 ? class_1304.field_6171 : class_1304.field_6173).method_7909();
        if (item instanceof class_1747) {
            return class_4696.method_23679((class_2680)((class_1747)item).method_7711().method_9564()) == class_1921.method_23583();
        }
        return false;
    }

    public boolean isAnyHandTranslucent() {
        return this.isHandTranslucent(class_1268.field_5808) || this.isHandTranslucent(class_1268.field_5810);
    }

    public void renderSolid(Matrix4fc modelMatrix, float tickDelta, class_4184 camera, class_757 gameRenderer, WorldRenderingPipeline pipeline) {
        if (!this.canRender(camera, gameRenderer) || !Iris.isPackInUseQuick()) {
            return;
        }
        this.ACTIVE = true;
        class_4587 poseStack = this.setupGlState(gameRenderer, camera, modelMatrix, tickDelta);
        pipeline.setPhase(WorldRenderingPhase.HAND_SOLID);
        poseStack.method_22903();
        class_310.method_1551().method_16011().method_15396("iris_hand");
        this.renderingSolid = true;
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().set((Matrix4fc)poseStack.method_23760().method_23761());
        RenderSystem.applyModelViewMatrix();
        gameRenderer.field_4012.method_22976(tickDelta, new class_4587(), this.bufferSource.getUnflushableWrapper(), class_310.method_1551().field_1724, class_310.method_1551().method_1561().method_23839(camera.method_19331(), tickDelta));
        class_310.method_1551().method_16011().method_15407();
        this.bufferSource.readyUp();
        this.bufferSource.method_22993();
        gameRenderer.method_22709(new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferProjection()));
        poseStack.method_22909();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        this.renderingSolid = false;
        pipeline.setPhase(WorldRenderingPhase.NONE);
        this.ACTIVE = false;
    }

    public void renderTranslucent(Matrix4fc modelMatrix, float tickDelta, class_4184 camera, class_757 gameRenderer, WorldRenderingPipeline pipeline) {
        if (!(this.canRender(camera, gameRenderer) && this.isAnyHandTranslucent() && Iris.isPackInUseQuick())) {
            return;
        }
        this.ACTIVE = true;
        pipeline.setPhase(WorldRenderingPhase.HAND_TRANSLUCENT);
        class_4587 poseStack = this.setupGlState(gameRenderer, camera, modelMatrix, tickDelta);
        poseStack.method_22903();
        class_310.method_1551().method_16011().method_15396("iris_hand_translucent");
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().set((Matrix4fc)poseStack.method_23760().method_23761());
        RenderSystem.applyModelViewMatrix();
        gameRenderer.field_4012.method_22976(tickDelta, new class_4587(), (class_4597.class_4598)this.bufferSource, class_310.method_1551().field_1724, class_310.method_1551().method_1561().method_23839(camera.method_19331(), tickDelta));
        poseStack.method_22909();
        class_310.method_1551().method_16011().method_15407();
        gameRenderer.method_22709(new Matrix4f(CapturedRenderingState.INSTANCE.getGbufferProjection()));
        this.bufferSource.method_22993();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        pipeline.setPhase(WorldRenderingPhase.NONE);
        this.ACTIVE = false;
    }

    public boolean isActive() {
        return this.ACTIVE;
    }

    public boolean isRenderingSolid() {
        return this.renderingSolid;
    }

    public FullyBufferedMultiBufferSource getBufferSource() {
        return this.bufferSource;
    }
}

