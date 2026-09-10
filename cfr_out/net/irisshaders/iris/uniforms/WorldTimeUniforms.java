/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_310
 *  net.minecraft.class_638
 */
package net.irisshaders.iris.uniforms;

import java.util.Objects;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.shaderpack.DimensionId;
import net.minecraft.class_310;
import net.minecraft.class_638;

public final class WorldTimeUniforms {
    private WorldTimeUniforms() {
    }

    public static void addWorldTimeUniforms(UniformHolder uniforms) {
        uniforms.uniform1i(UniformUpdateFrequency.PER_TICK, "worldTime", WorldTimeUniforms::getWorldDayTime).uniform1i(UniformUpdateFrequency.PER_TICK, "worldDay", WorldTimeUniforms::getWorldDay).uniform1i(UniformUpdateFrequency.PER_TICK, "moonPhase", () -> WorldTimeUniforms.getWorld().method_30273());
    }

    static int getWorldDayTime() {
        long timeOfDay = WorldTimeUniforms.getWorld().method_8532();
        if (Iris.getCurrentDimension() == DimensionId.END || Iris.getCurrentDimension() == DimensionId.NETHER) {
            return (int)(timeOfDay % 24000L);
        }
        long dayTime = WorldTimeUniforms.getWorld().method_8597().comp_641().orElse(timeOfDay % 24000L);
        return (int)dayTime;
    }

    private static int getWorldDay() {
        long timeOfDay = WorldTimeUniforms.getWorld().method_8532();
        long day = timeOfDay / 24000L;
        return (int)day;
    }

    private static class_638 getWorld() {
        return Objects.requireNonNull(class_310.method_1551().field_1687);
    }
}

