/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.compat.sodium.mixin;

import java.io.IOException;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
import net.irisshaders.iris.Iris;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={SodiumGameOptions.class})
public class MixinSodiumGameOptions {
    @Inject(method={"writeToDisk"}, at={@At(value="RETURN")}, remap=false)
    private static void iris$writeIrisConfig(CallbackInfo ci) {
        try {
            if (Iris.getIrisConfig() != null) {
                Iris.getIrisConfig().save();
            }
        }
        catch (IOException e) {
            Iris.logger.error("Failed to save Iris config file", e);
        }
    }
}

