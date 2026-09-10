/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1060
 *  net.minecraft.class_3300
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin.texture;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.irisshaders.iris.pbr.format.TextureFormatLoader;
import net.irisshaders.iris.pbr.texture.PBRTextureManager;
import net.minecraft.class_1060;
import net.minecraft.class_3300;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1060.class})
public class MixinTextureManager {
    @Inject(method={"method_18167", "lambda$reload$5"}, at={@At(value="TAIL")}, require=1)
    private void iris$onTailReloadLambda(class_3300 resourceManager, Executor applyExecutor, CompletableFuture<?> future, Void void1, CallbackInfo ci) {
        TextureFormatLoader.reload(resourceManager);
        PBRTextureManager.INSTANCE.clear();
    }

    @Inject(method={"_dumpAllSheets(Ljava/nio/file/Path;)V"}, at={@At(value="RETURN")})
    private void iris$onInnerDumpTextures(Path path, CallbackInfo ci) {
        PBRTextureManager.INSTANCE.dumpTextures(path);
    }

    @Inject(method={"close()V"}, at={@At(value="TAIL")}, remap=false)
    private void iris$onTailClose(CallbackInfo ci) {
        PBRTextureManager.INSTANCE.close();
    }
}

