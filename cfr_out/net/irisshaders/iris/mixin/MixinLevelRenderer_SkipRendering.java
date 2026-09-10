/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.v2.WrapWithCondition
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  net.minecraft.class_1297
 *  net.minecraft.class_1921
 *  net.minecraft.class_2586
 *  net.minecraft.class_4184
 *  net.minecraft.class_4604
 *  net.minecraft.class_638
 *  net.minecraft.class_761
 *  net.minecraft.class_846$class_851
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 */
package net.irisshaders.iris.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.Set;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.class_1297;
import net.minecraft.class_1921;
import net.minecraft.class_2586;
import net.minecraft.class_4184;
import net.minecraft.class_4604;
import net.minecraft.class_638;
import net.minecraft.class_761;
import net.minecraft.class_846;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={class_761.class})
public class MixinLevelRenderer_SkipRendering {
    @Unique
    private static final ObjectArrayList<class_846.class_851> EMPTY_LIST = new ObjectArrayList();

    @WrapWithCondition(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V")})
    private boolean skipSetupRender(class_761 instance, class_4184 camera, class_4604 frustum, boolean bl, boolean bl2) {
        WorldRenderingPipeline worldRenderingPipeline = Iris.getPipelineManager().getPipelineNullable();
        if (worldRenderingPipeline instanceof IrisRenderingPipeline) {
            IrisRenderingPipeline pipeline = (IrisRenderingPipeline)worldRenderingPipeline;
            return !pipeline.skipAllRendering();
        }
        return true;
    }

    @WrapWithCondition(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V")})
    private boolean skipRenderChunks(class_761 instance, class_1921 renderType, double d, double e, double f, Matrix4f matrix4f, Matrix4f matrix4f2) {
        WorldRenderingPipeline worldRenderingPipeline = Iris.getPipelineManager().getPipelineNullable();
        if (worldRenderingPipeline instanceof IrisRenderingPipeline) {
            IrisRenderingPipeline pipeline = (IrisRenderingPipeline)worldRenderingPipeline;
            return !pipeline.skipAllRendering();
        }
        return true;
    }

    @WrapOperation(method={"renderLevel"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientLevel;entitiesForRendering()Ljava/lang/Iterable;")})
    private Iterable<class_1297> skipRenderEntities(class_638 instance, Operation<Iterable<class_1297>> original) {
        IrisRenderingPipeline pipeline;
        WorldRenderingPipeline worldRenderingPipeline = Iris.getPipelineManager().getPipelineNullable();
        if (worldRenderingPipeline instanceof IrisRenderingPipeline && (pipeline = (IrisRenderingPipeline)worldRenderingPipeline).skipAllRendering()) {
            return Collections.emptyList();
        }
        return (Iterable)original.call(new Object[]{instance});
    }

    @WrapOperation(method={"renderLevel"}, at={@At(value="FIELD", target="Lnet/minecraft/client/renderer/LevelRenderer;visibleSections:Lit/unimi/dsi/fastutil/objects/ObjectArrayList;")})
    private ObjectArrayList<class_846.class_851> skipLocalBlockEntities(class_761 instance, Operation<ObjectArrayList<class_846.class_851>> original) {
        IrisRenderingPipeline pipeline;
        WorldRenderingPipeline worldRenderingPipeline = Iris.getPipelineManager().getPipelineNullable();
        if (worldRenderingPipeline instanceof IrisRenderingPipeline && (pipeline = (IrisRenderingPipeline)worldRenderingPipeline).skipAllRendering()) {
            return EMPTY_LIST;
        }
        return (ObjectArrayList)original.call(new Object[]{instance});
    }

    @WrapOperation(method={"renderLevel"}, at={@At(value="FIELD", target="Lnet/minecraft/client/renderer/LevelRenderer;globalBlockEntities:Ljava/util/Set;")})
    private Set<class_2586> skipGlobalBlockEntities(class_761 instance, Operation<Set<class_2586>> original) {
        IrisRenderingPipeline pipeline;
        WorldRenderingPipeline worldRenderingPipeline = Iris.getPipelineManager().getPipelineNullable();
        if (worldRenderingPipeline instanceof IrisRenderingPipeline && (pipeline = (IrisRenderingPipeline)worldRenderingPipeline).skipAllRendering()) {
            return Collections.emptySet();
        }
        return (Set)original.call(new Object[]{instance});
    }
}

