/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  net.minecraft.class_281
 *  net.minecraft.class_284
 *  net.minecraft.class_293
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_5912
 *  net.minecraft.class_5944
 *  org.slf4j.Logger
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import com.google.common.collect.ImmutableSet;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.compat.SkipList;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.blending.DepthColorStorage;
import net.irisshaders.iris.mixinterface.ShaderInstanceInterface;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.ShaderRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ExtendedShader;
import net.irisshaders.iris.pipeline.programs.FallbackShader;
import net.irisshaders.iris.shadows.ShadowRenderer;
import net.minecraft.class_281;
import net.minecraft.class_284;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_5912;
import net.minecraft.class_5944;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_5944.class})
public abstract class MixinShaderInstance
implements ShaderInstanceInterface {
    @Unique
    private static final ImmutableSet<String> ATTRIBUTE_LIST = ImmutableSet.of((Object)"Position", (Object)"Color", (Object)"Normal", (Object)"UV0", (Object)"UV1", (Object)"UV2", (Object[])new String[0]);
    @Shadow
    private static class_5944 field_29485;
    @Shadow
    @Final
    private int field_29493;
    @Shadow
    @Final
    private class_281 field_29467;
    @Shadow
    @Final
    private class_281 field_29468;
    @Unique
    private MethodHandle shouldSkip;

    @Override
    public void setShouldSkip(MethodHandle s) {
        this.shouldSkip = s;
    }

    @Inject(method={"<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;)V"}, at={@At(value="FIELD", target="Lnet/minecraft/client/renderer/ShaderInstance;CHUNK_OFFSET:Lcom/mojang/blaze3d/shaders/Uniform;")}, require=0)
    private void iris$storeSkipFabric(class_5912 resourceProvider, String string, class_293 vertexFormat, CallbackInfo ci) {
        this.shouldSkip = SkipList.shouldSkipList.computeIfAbsent(this.getClass(), x -> {
            try {
                MethodHandle iris$skipDraw = MethodHandles.lookup().findVirtual((Class<?>)x, "iris$skipDraw", MethodType.methodType(Boolean.TYPE));
                Iris.logger.warn("Class " + x.getName() + " has opted out of being rendered with shaders.");
                return iris$skipDraw;
            }
            catch (IllegalAccessException | NoSuchMethodException e) {
                return SkipList.NONE;
            }
        });
        if (Iris.getIrisConfig().shouldSkip(class_2960.method_12829((String)string))) {
            this.shouldSkip = SkipList.ALWAYS;
        }
    }

    public boolean iris$shouldSkipThis() {
        if (Iris.getIrisConfig().shouldAllowUnknownShaders()) {
            if (ShadowRenderer.ACTIVE) {
                return true;
            }
            if (!MixinShaderInstance.shouldOverrideShaders()) {
                return false;
            }
            if (this.shouldSkip == SkipList.NONE) {
                return false;
            }
            if (this.shouldSkip == SkipList.ALWAYS) {
                return true;
            }
            try {
                return this.shouldSkip.invoke((class_5944)this);
            }
            catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return !(this instanceof ExtendedShader) && !(this instanceof FallbackShader) && MixinShaderInstance.shouldOverrideShaders();
    }

    @Unique
    private static boolean shouldOverrideShaders() {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof ShaderRenderingPipeline) {
            return ((ShaderRenderingPipeline)pipeline).shouldOverrideShaders();
        }
        return false;
    }

    @Shadow
    public abstract int method_1270();

    @Redirect(method={"updateLocations"}, at=@At(value="INVOKE", target="Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap=false))
    private void iris$redirectLogSpam(Logger logger, String message, Object arg1, Object arg2) {
        if (this instanceof ExtendedShader || this instanceof FallbackShader) {
            return;
        }
        logger.warn(message, arg1, arg2);
    }

    @Redirect(method={"<init>*"}, require=1, at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/shaders/Uniform;glBindAttribLocation(IILjava/lang/CharSequence;)V"))
    public void iris$redirectBindAttributeLocation(int i, int j, CharSequence charSequence) {
        if (this instanceof ExtendedShader && ATTRIBUTE_LIST.contains((Object)charSequence)) {
            class_284.method_34419((int)i, (int)j, (CharSequence)("iris_" + String.valueOf(charSequence)));
        } else {
            class_284.method_34419((int)i, (int)j, (CharSequence)charSequence);
        }
    }

    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    private void name(class_5912 resourceProvider, String string, class_293 vertexFormat, CallbackInfo ci) {
        GLDebug.nameObject(33506, this.field_29493, string);
        GLDebug.nameObject(33505, this.field_29467.method_34417(), string);
        GLDebug.nameObject(33505, this.field_29468.method_34417(), string);
    }

    @Inject(method={"apply"}, at={@At(value="HEAD")})
    private void iris$lockDepthColorState(CallbackInfo ci) {
        if (field_29485 != null) {
            field_29485.method_34585();
            field_29485 = null;
        }
    }

    @Inject(method={"apply"}, at={@At(value="TAIL")})
    private void onTail(CallbackInfo ci) {
        if (!this.iris$shouldSkipThis()) {
            WorldRenderingPipeline pipeline;
            if (!this.isKnownShader() && MixinShaderInstance.shouldOverrideShaders() && (pipeline = Iris.getPipelineManager().getPipelineNullable()) instanceof IrisRenderingPipeline && !ShadowRenderer.ACTIVE) {
                ((IrisRenderingPipeline)pipeline).bindDefault();
            }
            return;
        }
        DepthColorStorage.disableDepthColor();
    }

    private boolean isKnownShader() {
        return this instanceof ExtendedShader || this instanceof FallbackShader;
    }

    @Inject(method={"clear"}, at={@At(value="HEAD")})
    private void iris$unlockDepthColorState(CallbackInfo ci) {
        if (!this.iris$shouldSkipThis()) {
            WorldRenderingPipeline pipeline;
            if (!this.isKnownShader() && MixinShaderInstance.shouldOverrideShaders() && (pipeline = Iris.getPipelineManager().getPipelineNullable()) instanceof IrisRenderingPipeline) {
                class_310.method_1551().method_1522().method_1235(false);
            }
            return;
        }
        DepthColorStorage.unlockDepthColor();
    }

    @Inject(method={"<init>"}, require=0, at={@At(value="INVOKE", target="Lnet/minecraft/util/GsonHelper;parse(Ljava/io/Reader;)Lcom/google/gson/JsonObject;")})
    public void iris$setupGeometryShader(class_5912 resourceProvider, String string, class_293 vertexFormat, CallbackInfo ci) {
        this.iris$createExtraShaders(resourceProvider, string);
    }

    @Override
    public void iris$createExtraShaders(class_5912 provider, String name) {
    }

    static {
        SkipList.shouldSkipList.put(ExtendedShader.class, SkipList.NONE);
        SkipList.shouldSkipList.put(FallbackShader.class, SkipList.NONE);
    }
}

