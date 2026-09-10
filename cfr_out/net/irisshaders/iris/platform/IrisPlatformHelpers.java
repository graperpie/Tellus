/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1920
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_2680
 *  net.minecraft.class_304
 */
package net.irisshaders.iris.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2680;
import net.minecraft.class_304;

public interface IrisPlatformHelpers {
    public static final IrisPlatformHelpers INSTANCE = ServiceLoader.load(IrisPlatformHelpers.class).findFirst().get();

    public static IrisPlatformHelpers getInstance() {
        return INSTANCE;
    }

    public boolean isModLoaded(String var1);

    public String getVersion();

    public boolean isDevelopmentEnvironment();

    public Path getGameDir();

    public Path getConfigDir();

    public int compareVersions(String var1, String var2) throws Exception;

    public class_304 registerKeyBinding(class_304 var1);

    public boolean useELS();

    public class_2680 getBlockAppearance(class_1920 var1, class_2680 var2, class_2350 var3, class_2338 var4);
}

