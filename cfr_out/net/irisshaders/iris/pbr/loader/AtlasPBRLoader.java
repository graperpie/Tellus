/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1011
 *  net.minecraft.class_1058
 *  net.minecraft.class_1059
 *  net.minecraft.class_1079
 *  net.minecraft.class_2960
 *  net.minecraft.class_3270
 *  net.minecraft.class_3298
 *  net.minecraft.class_3300
 *  net.minecraft.class_3532
 *  net.minecraft.class_7368
 *  net.minecraft.class_7764
 *  net.minecraft.class_7771
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.mixin.texture.AnimationMetadataSectionAccessor;
import net.irisshaders.iris.mixin.texture.TextureAtlasAccessor;
import net.irisshaders.iris.pbr.format.TextureFormat;
import net.irisshaders.iris.pbr.format.TextureFormatLoader;
import net.irisshaders.iris.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.pbr.mipmap.ChannelMipmapGenerator;
import net.irisshaders.iris.pbr.mipmap.CustomMipmapGenerator;
import net.irisshaders.iris.pbr.mipmap.LinearBlendFunction;
import net.irisshaders.iris.pbr.texture.PBRAtlasTexture;
import net.irisshaders.iris.pbr.texture.PBRSpriteHolder;
import net.irisshaders.iris.pbr.texture.PBRType;
import net.irisshaders.iris.pbr.texture.SpriteContentsExtension;
import net.irisshaders.iris.pbr.util.ImageManipulationUtil;
import net.minecraft.class_1011;
import net.minecraft.class_1058;
import net.minecraft.class_1059;
import net.minecraft.class_1079;
import net.minecraft.class_2960;
import net.minecraft.class_3270;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import net.minecraft.class_3532;
import net.minecraft.class_7368;
import net.minecraft.class_7764;
import net.minecraft.class_7771;
import org.jetbrains.annotations.Nullable;

public class AtlasPBRLoader
implements PBRTextureLoader<class_1059> {
    public static final ChannelMipmapGenerator LINEAR_MIPMAP_GENERATOR = new ChannelMipmapGenerator(LinearBlendFunction.INSTANCE, LinearBlendFunction.INSTANCE, LinearBlendFunction.INSTANCE, LinearBlendFunction.INSTANCE);

    @Override
    public void load(class_1059 atlas, class_3300 resourceManager, PBRTextureLoader.PBRTextureConsumer pbrTextureConsumer) {
        TextureAtlasAccessor atlasAccessor = (TextureAtlasAccessor)atlas;
        int atlasWidth = atlasAccessor.callGetWidth();
        int atlasHeight = atlasAccessor.callGetHeight();
        int mipLevel = atlasAccessor.getMipLevel();
        PBRAtlasTexture normalAtlas = null;
        PBRAtlasTexture specularAtlas = null;
        for (class_1058 sprite : ((TextureAtlasAccessor)atlas).getTexturesByName().values()) {
            PBRSpriteHolder pbrSpriteHolder;
            PBRTextureAtlasSprite normalSprite = this.createPBRSprite(sprite, resourceManager, atlas, atlasWidth, atlasHeight, mipLevel, PBRType.NORMAL);
            PBRTextureAtlasSprite specularSprite = this.createPBRSprite(sprite, resourceManager, atlas, atlasWidth, atlasHeight, mipLevel, PBRType.SPECULAR);
            if (normalSprite != null) {
                if (normalAtlas == null) {
                    normalAtlas = new PBRAtlasTexture(atlas, PBRType.NORMAL);
                }
                normalAtlas.addSprite(normalSprite);
                pbrSpriteHolder = ((SpriteContentsExtension)sprite.method_45851()).getOrCreatePBRHolder();
                pbrSpriteHolder.setNormalSprite(normalSprite);
            }
            if (specularSprite == null) continue;
            if (specularAtlas == null) {
                specularAtlas = new PBRAtlasTexture(atlas, PBRType.SPECULAR);
            }
            specularAtlas.addSprite(specularSprite);
            pbrSpriteHolder = ((SpriteContentsExtension)sprite.method_45851()).getOrCreatePBRHolder();
            pbrSpriteHolder.setSpecularSprite(specularSprite);
        }
        if (normalAtlas != null && normalAtlas.tryUpload(atlasWidth, atlasHeight, mipLevel)) {
            pbrTextureConsumer.acceptNormalTexture(normalAtlas);
        }
        if (specularAtlas != null && specularAtlas.tryUpload(atlasWidth, atlasHeight, mipLevel)) {
            pbrTextureConsumer.acceptSpecularTexture(specularAtlas);
        }
    }

    @Nullable
    protected PBRTextureAtlasSprite createPBRSprite(class_1058 sprite, class_3300 resourceManager, class_1059 atlas, int atlasWidth, int atlasHeight, int mipLevel, PBRType pbrType) {
        class_1011 nativeImage;
        class_7368 animationMetadata;
        class_2960 spriteName = sprite.method_45851().method_45816();
        class_2960 pbrImageLocation = this.getPBRImageLocation(spriteName, pbrType);
        Optional optionalResource = resourceManager.method_14486(pbrImageLocation);
        if (optionalResource.isEmpty()) {
            return null;
        }
        class_3298 resource = (class_3298)optionalResource.get();
        try {
            animationMetadata = resource.method_14481();
        }
        catch (Exception e) {
            Iris.logger.error("Unable to parse metadata from {}", pbrImageLocation, e);
            return null;
        }
        try (InputStream stream = resource.method_14482();){
            nativeImage = class_1011.method_4309((InputStream)stream);
        }
        catch (IOException e) {
            Iris.logger.error("Using missing texture, unable to load {}", pbrImageLocation, e);
            return null;
        }
        int imageWidth = nativeImage.method_4307();
        int imageHeight = nativeImage.method_4323();
        class_1079 metadataSection = animationMetadata.method_43041((class_3270)class_1079.field_5337).orElse(class_1079.field_21768);
        class_7771 frameSize = metadataSection.method_24143(imageWidth, imageHeight);
        int frameWidth = frameSize.comp_1049();
        int frameHeight = frameSize.comp_1050();
        if (!class_3532.method_48117((int)imageWidth, (int)frameWidth) || !class_3532.method_48117((int)imageHeight, (int)frameHeight)) {
            Iris.logger.error("Image {} size {},{} is not multiple of frame size {},{}", pbrImageLocation, imageWidth, imageHeight, frameWidth, frameHeight);
            nativeImage.close();
            return null;
        }
        int targetFrameWidth = sprite.method_45851().method_45807();
        int targetFrameHeight = sprite.method_45851().method_45815();
        if (frameWidth != targetFrameWidth || frameHeight != targetFrameHeight) {
            try {
                int targetImageWidth = imageWidth / frameWidth * targetFrameWidth;
                int targetImageHeight = imageHeight / frameHeight * targetFrameHeight;
                class_1011 scaledImage = targetImageWidth % imageWidth == 0 && targetImageHeight % imageHeight == 0 ? ImageManipulationUtil.scaleNearestNeighbor(nativeImage, targetImageWidth, targetImageHeight) : ImageManipulationUtil.scaleBilinear(nativeImage, targetImageWidth, targetImageHeight);
                nativeImage.close();
                nativeImage = scaledImage;
                frameWidth = targetFrameWidth;
                frameHeight = targetFrameHeight;
                if (metadataSection != class_1079.field_21768) {
                    AnimationMetadataSectionAccessor animationAccessor = (AnimationMetadataSectionAccessor)metadataSection;
                    int internalFrameWidth = animationAccessor.getFrameWidth();
                    int internalFrameHeight = animationAccessor.getFrameHeight();
                    if (internalFrameWidth != -1) {
                        animationAccessor.setFrameWidth(frameWidth);
                    }
                    if (internalFrameHeight != -1) {
                        animationAccessor.setFrameHeight(frameHeight);
                    }
                }
            }
            catch (Exception e) {
                Iris.logger.error("Something bad happened trying to load PBR texture " + spriteName.method_12832() + pbrType.getSuffix() + "!", e);
                throw e;
            }
        }
        class_2960 pbrSpriteName = class_2960.method_60655((String)spriteName.method_12836(), (String)(spriteName.method_12832() + pbrType.getSuffix()));
        PBRSpriteContents pbrSpriteContents = new PBRSpriteContents(pbrSpriteName, new class_7771(frameWidth, frameHeight), nativeImage, animationMetadata, pbrType);
        pbrSpriteContents.method_45808(mipLevel);
        return new PBRTextureAtlasSprite(pbrSpriteName, pbrSpriteContents, atlasWidth, atlasHeight, sprite.method_35806(), sprite.method_35807(), sprite);
    }

    protected class_2960 getPBRImageLocation(class_2960 spriteName, PBRType pbrType) {
        String path = pbrType.appendSuffix(spriteName.method_12832());
        if (path.startsWith("optifine/cit/")) {
            return class_2960.method_60655((String)spriteName.method_12836(), (String)(path + ".png"));
        }
        return class_2960.method_60655((String)spriteName.method_12836(), (String)("textures/" + path + ".png"));
    }

    public static class PBRTextureAtlasSprite
    extends class_1058 {
        protected final class_1058 baseSprite;

        protected PBRTextureAtlasSprite(class_2960 location, PBRSpriteContents contents, int atlasWidth, int atlasHeight, int x, int y, class_1058 baseSprite) {
            super(location, (class_7764)contents, atlasWidth, atlasHeight, x, y);
            this.baseSprite = baseSprite;
        }

        public class_1058 getBaseSprite() {
            return this.baseSprite;
        }
    }

    protected static class PBRSpriteContents
    extends class_7764
    implements CustomMipmapGenerator.Provider {
        protected final PBRType pbrType;

        public PBRSpriteContents(class_2960 name, class_7771 size, class_1011 image, class_7368 metadata, PBRType pbrType) {
            super(name, size, image, metadata);
            this.pbrType = pbrType;
        }

        @Override
        public CustomMipmapGenerator getMipmapGenerator() {
            CustomMipmapGenerator generator;
            TextureFormat format = TextureFormatLoader.getFormat();
            if (format != null && (generator = format.getMipmapGenerator(this.pbrType)) != null) {
                return generator;
            }
            return LINEAR_MIPMAP_GENERATOR;
        }
    }
}

