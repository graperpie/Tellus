/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1044
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_542
 *  org.apache.commons.io.IOUtils
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import java.io.IOException;
import java.io.InputStream;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.irisshaders.iris.shaderpack.texture.CustomTextureData;
import net.irisshaders.iris.shaderpack.texture.TextureFilteringData;
import net.irisshaders.iris.targets.backed.NativeImageBackedCustomTexture;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_542;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_310.class})
public class MixinMinecraft_Images {
    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void iris$setupImages(class_542 arg, CallbackInfo ci) {
        if (!IrisPlatformHelpers.getInstance().isModLoaded("fabric-resource-loader-v0")) {
            try {
                class_310.method_1551().method_1531().method_4616(class_2960.method_60655((String)"iris", (String)"textures/gui/widgets.png"), (class_1044)new NativeImageBackedCustomTexture(new CustomTextureData.PngData(new TextureFilteringData(false, false), IOUtils.toByteArray((InputStream)Iris.class.getResourceAsStream("/assets/iris/textures/gui/widgets.png")))));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

