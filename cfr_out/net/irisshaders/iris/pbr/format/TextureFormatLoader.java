/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2960
 *  net.minecraft.class_3298
 *  net.minecraft.class_3300
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.format;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.pbr.format.TextureFormat;
import net.irisshaders.iris.pbr.format.TextureFormatRegistry;
import net.minecraft.class_2960;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import org.jetbrains.annotations.Nullable;

public class TextureFormatLoader {
    public static final class_2960 LOCATION = class_2960.method_60656((String)"optifine/texture.properties");
    private static TextureFormat format;

    @Nullable
    public static TextureFormat getFormat() {
        return format;
    }

    public static void reload(class_3300 resourceManager) {
        TextureFormat newFormat = TextureFormatLoader.loadFormat(resourceManager);
        boolean didFormatChange = !Objects.equals(format, newFormat);
        format = newFormat;
        if (didFormatChange) {
            TextureFormatLoader.onFormatChange();
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Nullable
    private static TextureFormat loadFormat(class_3300 resourceManager) {
        Optional resource = resourceManager.method_14486(LOCATION);
        if (!resource.isPresent()) return null;
        try (InputStream stream2222 = ((class_3298)resource.get()).method_14482();){
            Properties properties = new Properties();
            properties.load(stream2222);
            String format = properties.getProperty("format");
            if (format == null) return null;
            if (format.isEmpty()) return null;
            String[] splitFormat = format.split("/");
            if (splitFormat.length <= 0) return null;
            String name = splitFormat[0];
            TextureFormat.Factory factory = TextureFormatRegistry.INSTANCE.getFactory(name);
            if (factory != null) {
                String version = splitFormat.length > 1 ? splitFormat[1] : null;
                TextureFormat textureFormat = factory.createFormat(name, version);
                return textureFormat;
            }
            Iris.logger.warn("Invalid texture format '" + name + "' in file '" + String.valueOf(LOCATION) + "'");
            return null;
        }
        catch (FileNotFoundException stream2222) {
            return null;
        }
        catch (Exception e) {
            Iris.logger.error("Failed to load texture format from file '" + String.valueOf(LOCATION) + "'", e);
        }
        return null;
    }

    private static void onFormatChange() {
        try {
            Iris.reload();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

