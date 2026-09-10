/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.class_2960
 */
package net.irisshaders.iris.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.minecraft.class_2960;

public class IrisConfig {
    private static final String COMMENT = "This file stores configuration options for Iris, such as the currently active shaderpack";
    private final Path propertiesPath;
    private final Path excludedPath;
    private String shaderPackName = null;
    private boolean enableShaders = true;
    private boolean allowUnknownShaders = false;
    private boolean enableDebugOptions = false;
    private List<class_2960> shadersToSkip = new ArrayList<class_2960>();
    private boolean disableUpdateMessage = false;
    private static Gson GSON = new Gson();

    public IrisConfig(Path propertiesPath, Path excluded) {
        this.propertiesPath = propertiesPath;
        this.excludedPath = excluded;
    }

    public void initialize() throws IOException {
        this.load();
        if (!Files.exists(this.propertiesPath, new LinkOption[0])) {
            this.save();
        }
    }

    public boolean isInternal() {
        return false;
    }

    public Optional<String> getShaderPackName() {
        return Optional.ofNullable(this.shaderPackName);
    }

    public void setShaderPackName(String name) {
        this.shaderPackName = name == null || name.equals("(internal)") || name.isEmpty() ? null : name;
    }

    public boolean areShadersEnabled() {
        return this.enableShaders;
    }

    public boolean areDebugOptionsEnabled() {
        return this.enableDebugOptions;
    }

    public boolean shouldDisableUpdateMessage() {
        return this.disableUpdateMessage;
    }

    public void setDebugEnabled(boolean enabled) {
        this.enableDebugOptions = enabled;
    }

    public void setShadersEnabled(boolean enabled) {
        this.enableShaders = enabled;
    }

    public void load() throws IOException {
        if (Files.exists(this.excludedPath, new LinkOption[0])) {
            JsonArray json = JsonParser.parseString((String)Files.readString(this.excludedPath)).getAsJsonObject().getAsJsonArray("excluded");
            for (int i = 0; i < json.size(); ++i) {
                class_2960 resource = class_2960.method_12829((String)json.get(i).getAsString());
                if (resource == null) {
                    Iris.logger.warn("Unknown shader " + json.get(i).getAsString());
                }
                this.shadersToSkip.add(resource);
            }
        } else {
            JsonObject defaultV = new JsonObject();
            JsonArray array = new JsonArray();
            array.add("put:valuesHere");
            defaultV.add("excluded", (JsonElement)array);
            Files.writeString(this.excludedPath, (CharSequence)GSON.toJson((JsonElement)defaultV), new OpenOption[0]);
        }
        if (!Files.exists(this.propertiesPath, new LinkOption[0])) {
            return;
        }
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(this.propertiesPath, new OpenOption[0]);){
            properties.load(is);
        }
        this.shaderPackName = properties.getProperty("shaderPack");
        this.enableShaders = !"false".equals(properties.getProperty("enableShaders"));
        this.allowUnknownShaders = "true".equals(properties.getProperty("allowUnknownShaders"));
        this.enableDebugOptions = "true".equals(properties.getProperty("enableDebugOptions"));
        this.disableUpdateMessage = "true".equals(properties.getProperty("disableUpdateMessage"));
        try {
            IrisVideoSettings.shadowDistance = Integer.parseInt(properties.getProperty("maxShadowRenderDistance", "32"));
            IrisVideoSettings.colorSpace = ColorSpace.valueOf(properties.getProperty("colorSpace", "SRGB"));
        }
        catch (IllegalArgumentException e) {
            Iris.logger.error("Shadow distance setting reset; value is invalid.");
            IrisVideoSettings.shadowDistance = 32;
            IrisVideoSettings.colorSpace = ColorSpace.SRGB;
            this.save();
        }
        if (this.shaderPackName != null && (this.shaderPackName.equals("(internal)") || this.shaderPackName.isEmpty())) {
            this.shaderPackName = null;
        }
    }

    public void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("shaderPack", this.getShaderPackName().orElse(""));
        properties.setProperty("enableShaders", this.enableShaders ? "true" : "false");
        properties.setProperty("allowUnknownShaders", this.allowUnknownShaders ? "true" : "false");
        properties.setProperty("enableDebugOptions", this.enableDebugOptions ? "true" : "false");
        properties.setProperty("disableUpdateMessage", this.disableUpdateMessage ? "true" : "false");
        properties.setProperty("maxShadowRenderDistance", String.valueOf(IrisVideoSettings.shadowDistance));
        properties.setProperty("colorSpace", IrisVideoSettings.colorSpace.name());
        try (OutputStream os = Files.newOutputStream(this.propertiesPath, new OpenOption[0]);){
            properties.store(os, COMMENT);
        }
    }

    public boolean shouldAllowUnknownShaders() {
        return this.allowUnknownShaders;
    }

    public boolean shouldSkip(class_2960 value) {
        return this.shadersToSkip.contains(value);
    }

    public void setUnknown(boolean b) throws IOException {
        this.allowUnknownShaders = b;
        this.save();
    }
}

