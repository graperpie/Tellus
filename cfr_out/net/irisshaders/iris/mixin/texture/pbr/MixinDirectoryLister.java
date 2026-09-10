/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 *  net.minecraft.class_3298
 *  net.minecraft.class_3300
 *  net.minecraft.class_7948$class_7949
 *  net.minecraft.class_7954
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyArgs
 *  org.spongepowered.asm.mixin.injection.invoke.arg.Args
 */
package net.irisshaders.iris.mixin.texture.pbr;

import java.util.function.BiConsumer;
import net.irisshaders.iris.pbr.texture.PBRType;
import net.minecraft.class_2960;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_7948;
import net.minecraft.class_7954;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value={class_7954.class})
public class MixinDirectoryLister {
    @ModifyArgs(method={"run(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/client/renderer/texture/atlas/SpriteSource$Output;)V"}, at=@At(value="INVOKE", target="Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V", remap=false, ordinal=0))
    private void iris$modifyForEachAction(Args args, class_3300 resourceManager, class_7948.class_7949 output) {
        BiConsumer action = (BiConsumer)args.get(0);
        BiConsumer<class_2960, class_3298> wrappedAction = (location, resource) -> {
            class_2960 baseLocation;
            String basePath = PBRType.removeSuffix(location.method_12832());
            if (basePath != null && resourceManager.method_14486(baseLocation = location.method_45136(basePath)).isPresent()) {
                return;
            }
            action.accept(location, resource);
        };
        args.set(0, wrappedAction);
    }
}

