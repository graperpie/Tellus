/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1011
 *  net.minecraft.class_1043
 */
package net.irisshaders.iris.targets.backed;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.IntSupplier;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.shaderpack.texture.CustomTextureData;
import net.minecraft.class_1011;
import net.minecraft.class_1043;

public class NativeImageBackedCustomTexture
extends class_1043
implements TextureAccess {
    public NativeImageBackedCustomTexture(CustomTextureData.PngData textureData) throws IOException {
        super(NativeImageBackedCustomTexture.create(textureData.getContent()));
        if (textureData.getFilteringData().shouldBlur()) {
            IrisRenderSystem.texParameteri(this.method_4624(), 3553, 10241, 9729);
            IrisRenderSystem.texParameteri(this.method_4624(), 3553, 10240, 9729);
        }
        if (textureData.getFilteringData().shouldClamp()) {
            IrisRenderSystem.texParameteri(this.method_4624(), 3553, 10242, 33071);
            IrisRenderSystem.texParameteri(this.method_4624(), 3553, 10243, 33071);
        }
    }

    private static class_1011 create(byte[] content) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(content.length);
        buffer.put(content);
        buffer.flip();
        return class_1011.method_4324((ByteBuffer)buffer);
    }

    public void method_4524() {
        class_1011 image = Objects.requireNonNull(this.method_4525());
        this.method_23207();
        image.method_22619(0, 0, 0, 0, 0, image.method_4307(), image.method_4323(), false, false, false, false);
    }

    @Override
    public TextureType getType() {
        return TextureType.TEXTURE_2D;
    }

    @Override
    public IntSupplier getTextureId() {
        return () -> ((NativeImageBackedCustomTexture)this).method_4624();
    }
}

