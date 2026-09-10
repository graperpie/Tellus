/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.TextureUtil
 *  net.minecraft.class_1044
 *  net.minecraft.class_1058$class_7770
 *  net.minecraft.class_1059
 *  net.minecraft.class_128
 *  net.minecraft.class_129
 *  net.minecraft.class_148
 *  net.minecraft.class_2960
 *  net.minecraft.class_3300
 *  net.minecraft.class_7764$class_5791
 *  net.minecraft.class_7764$class_7765
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.mixin.texture.SpriteContentsAnimatedTextureAccessor;
import net.irisshaders.iris.mixin.texture.SpriteContentsFrameInfoAccessor;
import net.irisshaders.iris.mixin.texture.SpriteContentsTickerAccessor;
import net.irisshaders.iris.pbr.SpriteContentsExtension;
import net.irisshaders.iris.pbr.loader.AtlasPBRLoader;
import net.irisshaders.iris.pbr.texture.PBRAtlasHolder;
import net.irisshaders.iris.pbr.texture.PBRDumpable;
import net.irisshaders.iris.pbr.texture.PBRType;
import net.irisshaders.iris.pbr.texture.TextureAtlasExtension;
import net.irisshaders.iris.pbr.util.TextureManipulationUtil;
import net.minecraft.class_1044;
import net.minecraft.class_1058;
import net.minecraft.class_1059;
import net.minecraft.class_128;
import net.minecraft.class_129;
import net.minecraft.class_148;
import net.minecraft.class_2960;
import net.minecraft.class_3300;
import net.minecraft.class_7764;
import org.jetbrains.annotations.Nullable;

public class PBRAtlasTexture
extends class_1044
implements PBRDumpable {
    protected final class_1059 atlasTexture;
    protected final PBRType type;
    protected final class_2960 id;
    protected final Map<class_2960, AtlasPBRLoader.PBRTextureAtlasSprite> texturesByName = new HashMap<class_2960, AtlasPBRLoader.PBRTextureAtlasSprite>();
    protected final List<class_1058.class_7770> animatedTextures = new ArrayList<class_1058.class_7770>();
    protected int width;
    protected int height;
    protected int mipLevel;

    public PBRAtlasTexture(class_1059 atlasTexture, PBRType type) {
        this.atlasTexture = atlasTexture;
        this.type = type;
        this.id = class_2960.method_60655((String)atlasTexture.method_24106().method_12836(), (String)(atlasTexture.method_24106().method_12832().replace(".png", "") + type.getSuffix() + ".png"));
    }

    public static void syncAnimation(class_7764.class_7765 source, class_7764.class_7765 target) {
        int time;
        SpriteContentsTickerAccessor sourceAccessor = (SpriteContentsTickerAccessor)source;
        List<class_7764.class_5791> sourceFrames = ((SpriteContentsAnimatedTextureAccessor)sourceAccessor.getAnimationInfo()).getFrames();
        int ticks = 0;
        for (int f = 0; f < sourceAccessor.getFrame(); ++f) {
            ticks += ((SpriteContentsFrameInfoAccessor)sourceFrames.get(f)).getTime();
        }
        SpriteContentsTickerAccessor targetAccessor = (SpriteContentsTickerAccessor)target;
        List<class_7764.class_5791> targetFrames = ((SpriteContentsAnimatedTextureAccessor)targetAccessor.getAnimationInfo()).getFrames();
        int cycleTime = 0;
        int frameCount = targetFrames.size();
        for (class_7764.class_5791 frame : targetFrames) {
            cycleTime += ((SpriteContentsFrameInfoAccessor)frame).getTime();
        }
        ticks %= cycleTime;
        int targetFrame = 0;
        while (ticks >= (time = ((SpriteContentsFrameInfoAccessor)targetFrames.get(targetFrame)).getTime())) {
            ++targetFrame;
            ticks -= time;
        }
        targetAccessor.setFrame(targetFrame);
        targetAccessor.setSubFrame(ticks + sourceAccessor.getSubFrame());
    }

    protected static void dumpSpriteNames(Path dir, String fileName, Map<class_2960, AtlasPBRLoader.PBRTextureAtlasSprite> sprites) {
        Path path = dir.resolve(fileName + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(path, new OpenOption[0]);){
            for (Map.Entry entry : sprites.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                AtlasPBRLoader.PBRTextureAtlasSprite sprite = (AtlasPBRLoader.PBRTextureAtlasSprite)((Object)entry.getValue());
                writer.write(String.format(Locale.ROOT, "%s\tx=%d\ty=%d\tw=%d\th=%d%n", entry.getKey(), sprite.method_35806(), sprite.method_35807(), sprite.method_45851().method_45807(), sprite.method_45851().method_45815()));
            }
        }
        catch (IOException e) {
            Iris.logger.warn("Failed to write file {}", path, e);
        }
    }

    public PBRType getType() {
        return this.type;
    }

    public class_2960 getAtlasId() {
        return this.id;
    }

    public void addSprite(AtlasPBRLoader.PBRTextureAtlasSprite sprite) {
        this.texturesByName.put(sprite.method_45851().method_45816(), sprite);
    }

    @Nullable
    public AtlasPBRLoader.PBRTextureAtlasSprite getSprite(class_2960 id) {
        return this.texturesByName.get(id);
    }

    public void clear() {
        this.animatedTextures.forEach(class_1058.class_7770::close);
        this.texturesByName.clear();
        this.animatedTextures.clear();
    }

    public void upload(int atlasWidth, int atlasHeight, int mipLevel) {
        int glId = this.method_4624();
        TextureUtil.prepareImage((int)glId, (int)mipLevel, (int)atlasWidth, (int)atlasHeight);
        TextureManipulationUtil.fillWithColor(glId, mipLevel, this.type.getDefaultValue());
        this.width = atlasWidth;
        this.height = atlasHeight;
        this.mipLevel = mipLevel;
        for (AtlasPBRLoader.PBRTextureAtlasSprite sprite : this.texturesByName.values()) {
            try {
                this.uploadSprite(sprite);
            }
            catch (Throwable throwable) {
                class_128 crashReport = class_128.method_560((Throwable)throwable, (String)"Stitching texture atlas");
                class_129 crashReportCategory = crashReport.method_562("Texture being stitched together");
                crashReportCategory.method_578("Atlas path", (Object)this.id);
                crashReportCategory.method_578("Sprite", (Object)sprite);
                throw new class_148(crashReport);
            }
        }
        PBRAtlasHolder pbrHolder = ((TextureAtlasExtension)this.atlasTexture).getOrCreatePBRHolder();
        switch (this.type) {
            case NORMAL: {
                pbrHolder.setNormalAtlas(this);
                break;
            }
            case SPECULAR: {
                pbrHolder.setSpecularAtlas(this);
            }
        }
    }

    public boolean tryUpload(int atlasWidth, int atlasHeight, int mipLevel) {
        try {
            this.upload(atlasWidth, atlasHeight, mipLevel);
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }

    protected void uploadSprite(AtlasPBRLoader.PBRTextureAtlasSprite sprite) {
        class_1058.class_7770 spriteTicker = sprite.method_33437();
        if (spriteTicker != null) {
            this.animatedTextures.add(spriteTicker);
            class_7764.class_7765 sourceTicker = ((SpriteContentsExtension)sprite.getBaseSprite().method_45851()).getCreatedTicker();
            class_7764.class_7765 targetTicker = ((SpriteContentsExtension)sprite.method_45851()).getCreatedTicker();
            if (sourceTicker != null && targetTicker != null) {
                PBRAtlasTexture.syncAnimation(sourceTicker, targetTicker);
                SpriteContentsTickerAccessor tickerAccessor = (SpriteContentsTickerAccessor)targetTicker;
                SpriteContentsAnimatedTextureAccessor infoAccessor = (SpriteContentsAnimatedTextureAccessor)tickerAccessor.getAnimationInfo();
                infoAccessor.invokeUploadFrame(sprite.method_35806(), sprite.method_35807(), ((SpriteContentsFrameInfoAccessor)infoAccessor.getFrames().get(tickerAccessor.getFrame())).getIndex());
                return;
            }
        }
        sprite.method_4584();
    }

    public void cycleAnimationFrames() {
        this.method_23207();
        for (class_1058.class_7770 ticker : this.animatedTextures) {
            ticker.method_45853();
        }
    }

    public void close() {
        PBRAtlasHolder pbrHolder = ((TextureAtlasExtension)this.atlasTexture).getPBRHolder();
        if (pbrHolder != null) {
            switch (this.type) {
                case NORMAL: {
                    pbrHolder.setNormalAtlas(null);
                    break;
                }
                case SPECULAR: {
                    pbrHolder.setSpecularAtlas(null);
                }
            }
        }
        this.clear();
    }

    public void method_4625(class_3300 manager) {
    }

    public void method_49712(class_2960 id, Path path) {
        String fileName = id.method_36181();
        TextureUtil.writeAsPNG((Path)path, (String)fileName, (int)this.method_4624(), (int)this.mipLevel, (int)this.width, (int)this.height);
        PBRAtlasTexture.dumpSpriteNames(path, fileName, this.texturesByName);
    }

    @Override
    public class_2960 getDefaultDumpLocation() {
        return this.id;
    }
}

