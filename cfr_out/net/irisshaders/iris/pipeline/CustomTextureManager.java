/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMaps
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.minecraft.class_1043
 *  net.minecraft.class_1044
 *  net.minecraft.class_1047
 *  net.minecraft.class_1059
 *  net.minecraft.class_1060
 *  net.minecraft.class_151
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  org.apache.commons.io.FilenameUtils
 */
package net.irisshaders.iris.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.GlResource;
import net.irisshaders.iris.gl.texture.GlTexture;
import net.irisshaders.iris.gl.texture.TextureAccess;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.gl.texture.TextureWrapper;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.mixin.LightTextureAccessor;
import net.irisshaders.iris.pbr.format.TextureFormat;
import net.irisshaders.iris.pbr.format.TextureFormatLoader;
import net.irisshaders.iris.pbr.texture.PBRAtlasTexture;
import net.irisshaders.iris.pbr.texture.PBRTextureHolder;
import net.irisshaders.iris.pbr.texture.PBRTextureManager;
import net.irisshaders.iris.pbr.texture.PBRType;
import net.irisshaders.iris.shaderpack.properties.PackDirectives;
import net.irisshaders.iris.shaderpack.texture.CustomTextureData;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.targets.backed.NativeImageBackedCustomTexture;
import net.irisshaders.iris.targets.backed.NativeImageBackedNoiseTexture;
import net.minecraft.class_1043;
import net.minecraft.class_1044;
import net.minecraft.class_1047;
import net.minecraft.class_1059;
import net.minecraft.class_1060;
import net.minecraft.class_151;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import org.apache.commons.io.FilenameUtils;

public class CustomTextureManager {
    private final EnumMap<TextureStage, Object2ObjectMap<String, TextureAccess>> customTextureIdMap = new EnumMap(TextureStage.class);
    private final Object2ObjectMap<String, TextureAccess> irisCustomTextures = new Object2ObjectOpenHashMap();
    private final TextureAccess noise;
    private final List<class_1044> ownedTextures = new ArrayList<class_1044>();
    private final List<GlTexture> ownedRawTextures = new ArrayList<GlTexture>();

    public CustomTextureManager(PackDirectives packDirectives, EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> customTextureDataMap, Object2ObjectMap<String, CustomTextureData> irisCustomTextureDataMap, CustomTextureData customNoiseTextureData) {
        customTextureDataMap.forEach((textureStage, customTextureStageDataMap) -> {
            Object2ObjectOpenHashMap customTextureIds = new Object2ObjectOpenHashMap();
            customTextureStageDataMap.forEach((arg_0, arg_1) -> this.lambda$new$0((Object2ObjectMap)customTextureIds, textureStage, arg_0, arg_1));
            this.customTextureIdMap.put((TextureStage)((Object)textureStage), (Object2ObjectMap<String, TextureAccess>)customTextureIds);
        });
        irisCustomTextureDataMap.forEach((name, texture) -> {
            try {
                this.irisCustomTextures.put(name, (Object)this.createCustomTexture((CustomTextureData)texture));
            }
            catch (IOException e) {
                Iris.logger.error("Unable to parse the image data for the custom texture on sampler " + name, e);
            }
        });
        if (customNoiseTextureData == null) {
            int noiseTextureResolution = packDirectives.getNoiseTextureResolution();
            NativeImageBackedNoiseTexture texture2 = new NativeImageBackedNoiseTexture(noiseTextureResolution);
            this.ownedTextures.add((class_1044)texture2);
            this.noise = texture2;
        } else {
            try {
                this.noise = this.createCustomTexture(customNoiseTextureData);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private TextureAccess createCustomTexture(CustomTextureData textureData) throws IOException, class_151 {
        if (textureData instanceof CustomTextureData.PngData) {
            NativeImageBackedCustomTexture texture = new NativeImageBackedCustomTexture((CustomTextureData.PngData)textureData);
            this.ownedTextures.add((class_1044)texture);
            return texture;
        }
        if (textureData instanceof CustomTextureData.LightmapMarker) {
            return new TextureWrapper(() -> ((class_1043)((LightTextureAccessor)class_310.method_1551().field_1773.method_22974()).getLightTexture()).method_4624(), TextureType.TEXTURE_2D);
        }
        if (textureData instanceof CustomTextureData.RawData1D) {
            CustomTextureData.RawData1D rawData1D = (CustomTextureData.RawData1D)textureData;
            GlTexture texture = new GlTexture(TextureType.TEXTURE_1D, rawData1D.getSizeX(), 0, 0, rawData1D.getInternalFormat().getGlFormat(), rawData1D.getPixelFormat().getGlFormat(), rawData1D.getPixelType().getGlFormat(), rawData1D.getContent(), rawData1D.getFilteringData());
            this.ownedRawTextures.add(texture);
            return texture;
        }
        if (textureData instanceof CustomTextureData.RawDataRect) {
            CustomTextureData.RawDataRect rawDataRect = (CustomTextureData.RawDataRect)textureData;
            GlTexture texture = new GlTexture(TextureType.TEXTURE_RECTANGLE, rawDataRect.getSizeX(), rawDataRect.getSizeY(), 0, rawDataRect.getInternalFormat().getGlFormat(), rawDataRect.getPixelFormat().getGlFormat(), rawDataRect.getPixelType().getGlFormat(), rawDataRect.getContent(), rawDataRect.getFilteringData());
            this.ownedRawTextures.add(texture);
            return texture;
        }
        if (textureData instanceof CustomTextureData.RawData2D) {
            CustomTextureData.RawData2D rawData2D = (CustomTextureData.RawData2D)textureData;
            GlTexture texture = new GlTexture(TextureType.TEXTURE_2D, rawData2D.getSizeX(), rawData2D.getSizeY(), 0, rawData2D.getInternalFormat().getGlFormat(), rawData2D.getPixelFormat().getGlFormat(), rawData2D.getPixelType().getGlFormat(), rawData2D.getContent(), rawData2D.getFilteringData());
            this.ownedRawTextures.add(texture);
            return texture;
        }
        if (textureData instanceof CustomTextureData.RawData3D) {
            CustomTextureData.RawData3D rawData3D = (CustomTextureData.RawData3D)textureData;
            GlTexture texture = new GlTexture(TextureType.TEXTURE_3D, rawData3D.getSizeX(), rawData3D.getSizeY(), rawData3D.getSizeZ(), rawData3D.getInternalFormat().getGlFormat(), rawData3D.getPixelFormat().getGlFormat(), rawData3D.getPixelType().getGlFormat(), rawData3D.getContent(), rawData3D.getFilteringData());
            this.ownedRawTextures.add(texture);
            return texture;
        }
        if (textureData instanceof CustomTextureData.ResourceData) {
            CustomTextureData.ResourceData resourceData = (CustomTextureData.ResourceData)textureData;
            String namespace = resourceData.getNamespace();
            Object location = resourceData.getLocation();
            int extensionIndex = FilenameUtils.indexOfExtension((String)location);
            Object withoutExtension = extensionIndex != -1 ? ((String)location).substring(0, extensionIndex) : location;
            PBRType pbrType = PBRType.fromFileLocation((String)withoutExtension);
            class_1060 textureManager = class_310.method_1551().method_1531();
            if (pbrType == null) {
                class_2960 textureLocation = class_2960.method_60655((String)namespace, (String)location);
                return new TextureWrapper(() -> {
                    class_1044 texture = textureManager.method_4619(textureLocation);
                    if (texture instanceof class_1059 || texture instanceof PBRAtlasTexture) {
                        int tex = GlStateManagerAccessor.getActiveTexture();
                        int binding = GlStateManagerAccessor.getTEXTURES()[tex].field_5167;
                        texture.method_4527(false, (Integer)class_310.method_1551().field_1690.method_42563().method_41753() > 0);
                        GlStateManager._activeTexture((int)(33984 + tex));
                        GlStateManager._bindTexture((int)binding);
                    }
                    return texture != null ? texture.method_4624() : class_1047.method_4540().method_4624();
                }, TextureType.TEXTURE_2D);
            }
            location = ((String)location).substring(0, extensionIndex - pbrType.getSuffix().length()) + ((String)location).substring(extensionIndex);
            class_2960 textureLocation = class_2960.method_60655((String)namespace, (String)location);
            return new TextureWrapper(() -> {
                class_1044 texture = textureManager.method_4619(textureLocation);
                if (texture != null) {
                    if (texture instanceof class_1059 || texture instanceof PBRAtlasTexture) {
                        int tex = GlStateManagerAccessor.getActiveTexture();
                        int binding = GlStateManagerAccessor.getTEXTURES()[tex].field_5167;
                        texture.method_4527(false, (Integer)class_310.method_1551().field_1690.method_42563().method_41753() > 0);
                        GlStateManager._activeTexture((int)(33984 + tex));
                        GlStateManager._bindTexture((int)binding);
                    }
                    int id = texture.method_4624();
                    PBRTextureHolder pbrHolder = PBRTextureManager.INSTANCE.getOrLoadHolder(id);
                    class_1044 pbrTexture = switch (pbrType) {
                        default -> throw new MatchException(null, null);
                        case PBRType.NORMAL -> pbrHolder.normalTexture();
                        case PBRType.SPECULAR -> pbrHolder.specularTexture();
                    };
                    TextureFormat textureFormat = TextureFormatLoader.getFormat();
                    if (textureFormat != null) {
                        int previousBinding = GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].field_5167;
                        GlStateManager._bindTexture((int)pbrTexture.method_4624());
                        textureFormat.setupTextureParameters(pbrType, pbrTexture);
                        GlStateManager._bindTexture((int)previousBinding);
                    }
                    return pbrTexture.method_4624();
                }
                return class_1047.method_4540().method_4624();
            }, TextureType.TEXTURE_2D);
        }
        throw new IllegalArgumentException("Don't know texture type!");
    }

    public EnumMap<TextureStage, Object2ObjectMap<String, TextureAccess>> getCustomTextureIdMap() {
        return this.customTextureIdMap;
    }

    public Object2ObjectMap<String, TextureAccess> getCustomTextureIdMap(TextureStage stage) {
        return this.customTextureIdMap.getOrDefault((Object)stage, (Object2ObjectMap<String, TextureAccess>)Object2ObjectMaps.emptyMap());
    }

    public Object2ObjectMap<String, TextureAccess> getIrisCustomTextures() {
        return this.irisCustomTextures;
    }

    public TextureAccess getNoiseTexture() {
        return this.noise;
    }

    public void destroy() {
        this.ownedTextures.forEach(class_1044::close);
        this.ownedRawTextures.forEach(GlResource::destroy);
    }

    private /* synthetic */ void lambda$new$0(Object2ObjectMap customTextureIds, TextureStage textureStage, String samplerName, CustomTextureData textureData) {
        try {
            customTextureIds.put((Object)samplerName, (Object)this.createCustomTexture(textureData));
        }
        catch (IOException | class_151 e) {
            Iris.logger.error("Unable to parse the image data for the custom texture on stage " + String.valueOf((Object)textureStage) + ", sampler " + samplerName, e);
        }
    }
}

