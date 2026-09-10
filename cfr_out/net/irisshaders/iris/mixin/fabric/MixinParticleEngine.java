/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_3999
 *  net.minecraft.class_5944
 *  net.minecraft.class_702
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package net.irisshaders.iris.mixin.fabric;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.irisshaders.iris.fantastic.ParticleRenderingPhase;
import net.irisshaders.iris.fantastic.PhasedParticleEngine;
import net.irisshaders.iris.pipeline.programs.ShaderAccess;
import net.minecraft.class_3999;
import net.minecraft.class_5944;
import net.minecraft.class_702;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={class_702.class})
public class MixinParticleEngine
implements PhasedParticleEngine {
    private static final List<class_3999> OPAQUE_PARTICLE_RENDER_TYPES = ImmutableList.of((Object)class_3999.field_17828, (Object)class_3999.field_17830, (Object)class_3999.field_17831, (Object)class_3999.field_17832);
    @Shadow
    @Final
    private static List<class_3999> field_17820;
    @Unique
    private ParticleRenderingPhase phase = ParticleRenderingPhase.EVERYTHING;

    @Redirect(method={"render"}, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;setShader(Ljava/util/function/Supplier;)V", remap=false))
    private void iris$changeParticleShader(Supplier<class_5944> pSupplier0) {
        RenderSystem.setShader(this.phase == ParticleRenderingPhase.TRANSLUCENT ? ShaderAccess::getParticleTranslucentShader : pSupplier0);
    }

    @Redirect(method={"render"}, at=@At(value="FIELD", target="Lnet/minecraft/client/particle/ParticleEngine;RENDER_ORDER:Ljava/util/List;"))
    private List<class_3999> iris$selectParticlesToRender() {
        if (this.phase == ParticleRenderingPhase.TRANSLUCENT) {
            ArrayList<class_3999> toRender = new ArrayList<class_3999>(field_17820);
            toRender.removeAll(OPAQUE_PARTICLE_RENDER_TYPES);
            return toRender;
        }
        if (this.phase == ParticleRenderingPhase.OPAQUE) {
            return OPAQUE_PARTICLE_RENDER_TYPES;
        }
        return field_17820;
    }

    @Override
    public void setParticleRenderingPhase(ParticleRenderingPhase phase) {
        this.phase = phase;
    }
}

