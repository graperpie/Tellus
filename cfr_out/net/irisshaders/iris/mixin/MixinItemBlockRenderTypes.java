/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_2680
 *  net.minecraft.class_4696
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.shaderpack.materialmap.BlockMaterialMapping;
import net.irisshaders.iris.shaderpack.materialmap.BlockRenderType;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.class_1921;
import net.minecraft.class_2680;
import net.minecraft.class_4696;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_4696.class})
public class MixinItemBlockRenderTypes {
    @Unique
    private static final class_1921[] LAYER_SET_VANILLA = new class_1921[BlockRenderType.values().length];

    @Inject(method={"getChunkRenderType"}, at={@At(value="HEAD")}, cancellable=true)
    private static void iris$setCustomRenderType(class_2680 arg, CallbackInfoReturnable<class_1921> cir) {
        BlockRenderType type = WorldRenderingSettings.INSTANCE.getBlockTypeIds().get(arg.method_26204());
        if (type != null) {
            cir.setReturnValue((Object)LAYER_SET_VANILLA[type.ordinal()]);
        }
    }

    static {
        for (int i = 0; i < BlockRenderType.values().length; ++i) {
            MixinItemBlockRenderTypes.LAYER_SET_VANILLA[i] = BlockMaterialMapping.convertBlockToRenderType(BlockRenderType.values()[i]);
        }
    }
}

