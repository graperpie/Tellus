/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_286
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_291
 *  net.minecraft.class_291$class_8555
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_9801
 */
package net.irisshaders.iris.pathways;

import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.helpers.VertexBufferHelper;
import net.minecraft.class_286;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_291;
import net.minecraft.class_293;
import net.minecraft.class_9801;

public class FullScreenQuadRenderer {
    public static final FullScreenQuadRenderer INSTANCE = new FullScreenQuadRenderer();
    private final class_291 quad;

    private FullScreenQuadRenderer() {
        class_287 bufferBuilder = class_289.method_1348().method_60827(class_293.class_5596.field_27382, class_290.field_1585);
        bufferBuilder.method_22912(0.0f, 0.0f, 0.0f).method_22913(0.0f, 0.0f);
        bufferBuilder.method_22912(1.0f, 0.0f, 0.0f).method_22913(1.0f, 0.0f);
        bufferBuilder.method_22912(1.0f, 1.0f, 0.0f).method_22913(1.0f, 1.0f);
        bufferBuilder.method_22912(0.0f, 1.0f, 0.0f).method_22913(0.0f, 1.0f);
        class_9801 meshData = bufferBuilder.method_60794();
        this.quad = new class_291(class_291.class_8555.field_44793);
        this.quad.method_1353();
        this.quad.method_1352(meshData);
        class_289.method_1348().method_60828();
        class_291.method_1354();
    }

    public void render() {
        this.begin();
        this.renderQuad();
        this.end();
    }

    public void begin() {
        ((VertexBufferHelper)this.quad).saveBinding();
        RenderSystem.disableDepthTest();
        class_286.method_34420();
        this.quad.method_1353();
    }

    public void renderQuad() {
        IrisRenderSystem.overridePolygonMode();
        this.quad.method_35665();
        IrisRenderSystem.restorePolygonMode();
    }

    public void end() {
        RenderSystem.enableDepthTest();
        ((VertexBufferHelper)this.quad).restoreBinding();
    }
}

