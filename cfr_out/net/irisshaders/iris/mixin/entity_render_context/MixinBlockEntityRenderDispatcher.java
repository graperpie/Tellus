/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  net.minecraft.class_2586
 *  net.minecraft.class_2680
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_824
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.entity_render_context;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.irisshaders.iris.layer.BlockEntityRenderStateShard;
import net.irisshaders.iris.layer.BufferSourceWrapper;
import net.irisshaders.iris.layer.OuterWrappedRenderType;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.class_2586;
import net.minecraft.class_2680;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_824;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_824.class})
public class MixinBlockEntityRenderDispatcher {
    @Unique
    private static final String RUN_REPORTED = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;tryRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Ljava/lang/Runnable;)V";

    @ModifyVariable(method={"render"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/level/block/entity/BlockEntityType;isValid(Lnet/minecraft/world/level/block/state/BlockState;)Z"), allow=1, require=1, argsOnly=true)
    private class_4597 iris$wrapBufferSource(class_4597 bufferSource, class_2586 blockEntity) {
        class_2680 state = blockEntity.method_11010();
        Object2IntMap<class_2680> blockStateIds = WorldRenderingSettings.INSTANCE.getBlockStateIds();
        if (blockStateIds == null || !ImmediateState.isRenderingLevel) {
            return bufferSource;
        }
        int intId = blockStateIds.getOrDefault((Object)state, -1);
        CapturedRenderingState.INSTANCE.setCurrentBlockEntity(intId);
        return new BufferSourceWrapper(bufferSource, renderType -> OuterWrappedRenderType.wrapExactlyOnce("iris:block_entity", renderType, BlockEntityRenderStateShard.INSTANCE));
    }

    @Inject(method={"render"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;tryRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Ljava/lang/Runnable;)V", shift=At.Shift.AFTER)})
    private void iris$afterRender(class_2586 blockEntity, float tickDelta, class_4587 matrix, class_4597 bufferSource, CallbackInfo ci) {
        CapturedRenderingState.INSTANCE.setCurrentBlockEntity(0);
    }
}

