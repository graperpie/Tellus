/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager$class_1040
 *  com.mojang.blaze3d.systems.RenderSystem
 *  org.joml.Vector2f
 */
package net.irisshaders.iris.uniforms;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.uniform.DynamicUniformHolder;
import org.joml.Vector2f;

public class VanillaUniforms {
    public static void addVanillaUniforms(DynamicUniformHolder uniforms) {
        Vector2f cachedScreenSize = new Vector2f();
        uniforms.uniform1f("iris_LineWidth", RenderSystem::getShaderLineWidth, listener -> {});
        uniforms.uniform2f("iris_ScreenSize", () -> cachedScreenSize.set((float)GlStateManager.class_1040.method_35332(), (float)GlStateManager.class_1040.method_35333()), listener -> {});
    }
}

