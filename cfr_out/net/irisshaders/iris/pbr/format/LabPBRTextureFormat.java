/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.format;

import java.util.Objects;
import net.irisshaders.iris.pbr.format.TextureFormat;
import net.irisshaders.iris.pbr.mipmap.ChannelMipmapGenerator;
import net.irisshaders.iris.pbr.mipmap.CustomMipmapGenerator;
import net.irisshaders.iris.pbr.mipmap.DiscreteBlendFunction;
import net.irisshaders.iris.pbr.mipmap.LinearBlendFunction;
import net.irisshaders.iris.pbr.texture.PBRType;
import org.jetbrains.annotations.Nullable;

public record LabPBRTextureFormat(String name, @Nullable String version) implements TextureFormat
{
    public static final ChannelMipmapGenerator SPECULAR_MIPMAP_GENERATOR = new ChannelMipmapGenerator(LinearBlendFunction.INSTANCE, new DiscreteBlendFunction(v -> v < 230 ? 0 : v - 229), new DiscreteBlendFunction(v -> v < 65 ? 0 : 1), new DiscreteBlendFunction(v -> v < 255 ? 0 : 1));

    @Override
    public boolean canInterpolateValues(PBRType pbrType) {
        return pbrType != PBRType.SPECULAR;
    }

    @Override
    @Nullable
    public CustomMipmapGenerator getMipmapGenerator(PBRType pbrType) {
        if (pbrType == PBRType.SPECULAR) {
            return SPECULAR_MIPMAP_GENERATOR;
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        LabPBRTextureFormat other = (LabPBRTextureFormat)obj;
        return Objects.equals(this.name, other.name) && Objects.equals(this.version, other.version);
    }
}

