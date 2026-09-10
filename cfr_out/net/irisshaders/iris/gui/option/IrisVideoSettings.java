/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_7172
 *  net.minecraft.class_7172$class_7174
 *  net.minecraft.class_7172$class_7178
 *  net.minecraft.class_7172$class_7277
 *  net.minecraft.class_7172$class_7303
 *  net.minecraft.class_7919
 */
package net.irisshaders.iris.gui.option;

import java.io.IOException;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.option.ShadowDistanceOption;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.class_2561;
import net.minecraft.class_7172;
import net.minecraft.class_7919;

public class IrisVideoSettings {
    private static final class_7919 DISABLED_TOOLTIP = class_7919.method_47407((class_2561)class_2561.method_43471((String)"options.iris.shadowDistance.disabled"));
    private static final class_7919 ENABLED_TOOLTIP = class_7919.method_47407((class_2561)class_2561.method_43471((String)"options.iris.shadowDistance.enabled"));
    public static int shadowDistance = 32;
    public static ColorSpace colorSpace = ColorSpace.SRGB;
    public static final class_7172<Integer> RENDER_DISTANCE = new ShadowDistanceOption<Integer>("options.iris.shadowDistance", (class_7172.class_7277<Integer>)((class_7172.class_7277)mc -> {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        class_7919 tooltip = pipeline != null ? (pipeline.getForcedShadowRenderDistanceChunksForDisplay().isPresent() ? DISABLED_TOOLTIP : ENABLED_TOOLTIP) : ENABLED_TOOLTIP;
        return tooltip;
    }), (class_7172.class_7303<Integer>)((class_7172.class_7303)(arg, d) -> {
        WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline != null) {
            d = pipeline.getForcedShadowRenderDistanceChunksForDisplay().orElse((int)d);
        }
        if ((double)d.intValue() <= 0.0) {
            return class_2561.method_43469((String)"options.generic_value", (Object[])new Object[]{class_2561.method_43471((String)"options.iris.shadowDistance"), "0 (disabled)"});
        }
        return class_2561.method_43469((String)"options.generic_value", (Object[])new Object[]{class_2561.method_43471((String)"options.iris.shadowDistance"), class_2561.method_43469((String)"options.chunks", (Object[])new Object[]{d})});
    }), (class_7172.class_7178<Integer>)new class_7172.class_7174(0, 32), IrisVideoSettings.getOverriddenShadowDistance(shadowDistance), integer -> {
        shadowDistance = integer;
        try {
            Iris.getIrisConfig().save();
        }
        catch (IOException e) {
            Iris.logger.fatal("Failed to save config!", e);
        }
    });

    public static int getOverriddenShadowDistance(int base) {
        return Iris.getPipelineManager().getPipeline().map(pipeline -> pipeline.getForcedShadowRenderDistanceChunksForDisplay().orElse(base)).orElse(base);
    }

    public static boolean isShadowDistanceSliderEnabled() {
        return Iris.getPipelineManager().getPipeline().map(pipeline -> pipeline.getForcedShadowRenderDistanceChunksForDisplay().isEmpty()).orElse(true);
    }
}

