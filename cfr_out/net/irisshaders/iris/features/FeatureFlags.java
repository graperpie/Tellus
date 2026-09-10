/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1074
 *  org.apache.commons.lang3.StringUtils
 */
package net.irisshaders.iris.features;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.minecraft.class_1074;
import org.apache.commons.lang3.StringUtils;

public enum FeatureFlags {
    SEPARATE_HARDWARE_SAMPLERS(() -> true, () -> true),
    HIGHER_SHADOWCOLOR(() -> true, () -> true),
    CUSTOM_IMAGES(() -> true, IrisRenderSystem::supportsImageLoadStore),
    PER_BUFFER_BLENDING(() -> true, IrisRenderSystem::supportsBufferBlending),
    COMPUTE_SHADERS(() -> true, IrisRenderSystem::supportsCompute),
    TESSELLATION_SHADERS(() -> true, IrisRenderSystem::supportsTesselation),
    ENTITY_TRANSLUCENT(() -> true, () -> true),
    REVERSED_CULLING(() -> true, () -> true),
    BLOCK_EMISSION_ATTRIBUTE(() -> true, () -> true),
    CAN_DISABLE_WEATHER(() -> true, () -> true),
    SSBO(() -> true, IrisRenderSystem::supportsSSBO),
    UNKNOWN(() -> false, () -> false);

    private final BooleanSupplier irisRequirement;
    private final BooleanSupplier hardwareRequirement;

    private FeatureFlags(BooleanSupplier irisRequirement, BooleanSupplier hardwareRequirement) {
        this.irisRequirement = irisRequirement;
        this.hardwareRequirement = hardwareRequirement;
    }

    public static String getInvalidStatus(List<FeatureFlags> invalidFeatureFlags) {
        FeatureFlags[] flags;
        boolean unsupportedHardware = false;
        boolean unsupportedIris = false;
        for (FeatureFlags flag : flags = invalidFeatureFlags.toArray(new FeatureFlags[0])) {
            unsupportedIris |= !flag.irisRequirement.getAsBoolean();
            unsupportedHardware |= !flag.hardwareRequirement.getAsBoolean();
        }
        if (unsupportedIris) {
            if (unsupportedHardware) {
                return class_1074.method_4662((String)"iris.unsupported.irisorpc", (Object[])new Object[0]);
            }
            return class_1074.method_4662((String)"iris.unsupported.iris", (Object[])new Object[0]);
        }
        if (unsupportedHardware) {
            return class_1074.method_4662((String)"iris.unsupported.pc", (Object[])new Object[0]);
        }
        return null;
    }

    public static boolean isInvalid(String name) {
        try {
            return !FeatureFlags.valueOf(name.toUpperCase(Locale.US)).isUsable();
        }
        catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static FeatureFlags getValue(String value) {
        if (value.equalsIgnoreCase("TESSELATION_SHADERS")) {
            value = "TESSELLATION_SHADERS";
        }
        try {
            return FeatureFlags.valueOf(value.toUpperCase(Locale.US));
        }
        catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public String getHumanReadableName() {
        return StringUtils.capitalize((String)this.name().replace("_", " ").toLowerCase());
    }

    public boolean isUsable() {
        return this.irisRequirement.getAsBoolean() && this.hardwareRequirement.getAsBoolean();
    }
}

