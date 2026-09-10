/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1011
 *  net.minecraft.class_156
 *  net.minecraft.class_310
 *  org.apache.commons.io.FilenameUtils
 */
package net.irisshaders.iris.pbr.util;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import net.minecraft.class_1011;
import net.minecraft.class_156;
import net.minecraft.class_310;
import org.apache.commons.io.FilenameUtils;

public class TextureExporter {
    public static void exportTextures(String directory, String filename, int textureId, int mipLevel, int width, int height) {
        String extension = FilenameUtils.getExtension((String)filename);
        String baseName = filename.substring(0, filename.length() - extension.length() - 1);
        for (int level = 0; level <= mipLevel; ++level) {
            TextureExporter.exportTexture(directory, baseName + "_" + level + "." + extension, textureId, level, width >> level, height >> level);
        }
    }

    public static void exportTexture(String directory, String filename, int textureId, int level, int width, int height) {
        class_1011 nativeImage = new class_1011(width, height, false);
        RenderSystem.bindTexture((int)textureId);
        nativeImage.method_4327(level, false);
        File dir = new File(class_310.method_1551().field_1697, directory);
        dir.mkdirs();
        File file = new File(dir, filename);
        class_156.method_27958().execute(() -> {
            try {
                nativeImage.method_4325(file);
            }
            catch (Exception exception) {
            }
            finally {
                nativeImage.close();
            }
        });
    }
}

