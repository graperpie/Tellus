/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.fabricmc.loader.api.FabricLoader
 *  net.fabricmc.loader.api.ModContainer
 *  net.fabricmc.loader.api.SemanticVersion
 *  net.fabricmc.loader.api.VersionParsingException
 *  net.minecraft.class_1920
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_2680
 *  net.minecraft.class_304
 */
package net.irisshaders.iris.platform;

import java.nio.file.Path;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;
import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import net.minecraft.class_304;

public class IrisFabricHelpers
implements IrisPlatformHelpers {
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public String getVersion() {
        return ((ModContainer)FabricLoader.getInstance().getModContainer("iris").get()).getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public int compareVersions(String currentVersion, String semanticVersion) throws Exception {
        try {
            return SemanticVersion.parse((String)currentVersion).compareTo(SemanticVersion.parse((String)semanticVersion));
        }
        catch (VersionParsingException e) {
            throw new Exception(e);
        }
    }

    @Override
    public class_304 registerKeyBinding(class_304 keyMapping) {
        return KeyBindingHelper.registerKeyBinding((class_304)keyMapping);
    }

    @Override
    public boolean useELS() {
        return false;
    }

    @Override
    public class_2680 getBlockAppearance(class_1920 level, class_2680 state, class_2350 cullFace, class_2338 pos) {
        return state;
    }
}

