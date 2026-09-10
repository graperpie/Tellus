/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  net.minecraft.class_285
 */
package net.irisshaders.iris.gl.program;

import com.mojang.blaze3d.platform.GlStateManager;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.program.ProgramImages;
import net.irisshaders.iris.gl.program.ProgramSamplers;
import net.irisshaders.iris.gl.program.ProgramUniforms;
import net.minecraft.class_285;

public final class Program
extends GlResource {
    private final ProgramUniforms uniforms;
    private final ProgramSamplers samplers;
    private final ProgramImages images;

    Program(int program, ProgramUniforms uniforms, ProgramSamplers samplers, ProgramImages images) {
        super(program);
        this.uniforms = uniforms;
        this.samplers = samplers;
        this.images = images;
    }

    public static void unbind() {
        ProgramUniforms.clearActiveUniforms();
        ProgramSamplers.clearActiveSamplers();
        class_285.method_22094((int)0);
    }

    public void use() {
        IrisRenderSystem.memoryBarrier(8232);
        class_285.method_22094((int)this.getGlId());
        this.uniforms.update();
        this.samplers.update();
        this.images.update();
    }

    @Override
    public void destroyInternal() {
        GlStateManager.glDeleteProgram((int)this.getGlId());
    }

    @Deprecated
    public int getProgramId() {
        return this.getGlId();
    }

    public int getActiveImages() {
        return this.images.getActiveImages();
    }
}

