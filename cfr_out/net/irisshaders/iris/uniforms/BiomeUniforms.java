/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  net.minecraft.class_1959
 *  net.minecraft.class_1959$class_1963
 *  net.minecraft.class_310
 *  net.minecraft.class_5321
 *  net.minecraft.class_6880
 *  net.minecraft.class_6908
 *  net.minecraft.class_746
 */
package net.irisshaders.iris.uniforms;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;
import net.irisshaders.iris.gl.uniform.FloatSupplier;
import net.irisshaders.iris.gl.uniform.UniformHolder;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.mixinterface.ExtendedBiome;
import net.irisshaders.iris.parsing.BiomeCategories;
import net.minecraft.class_1959;
import net.minecraft.class_310;
import net.minecraft.class_5321;
import net.minecraft.class_6880;
import net.minecraft.class_6908;
import net.minecraft.class_746;

public class BiomeUniforms {
    private static final Object2IntMap<class_5321<class_1959>> biomeMap = new Object2IntOpenHashMap();

    public static Object2IntMap<class_5321<class_1959>> getBiomeMap() {
        return biomeMap;
    }

    public static void addBiomeUniforms(UniformHolder uniforms) {
        uniforms.uniform1i(UniformUpdateFrequency.PER_TICK, "biome", BiomeUniforms.playerI(player -> biomeMap.getInt(player.method_37908().method_23753(player.method_24515()).method_40230().orElse(null)))).uniform1i(UniformUpdateFrequency.PER_TICK, "biome_category", BiomeUniforms.playerI(player -> {
            class_6880 holder = player.method_37908().method_23753(player.method_24515());
            ExtendedBiome extendedBiome = (ExtendedBiome)holder.comp_349();
            if (extendedBiome.getBiomeCategory() == -1) {
                extendedBiome.setBiomeCategory(BiomeUniforms.getBiomeCategory((class_6880<class_1959>)holder).ordinal());
                return extendedBiome.getBiomeCategory();
            }
            return extendedBiome.getBiomeCategory();
        })).uniform1i(UniformUpdateFrequency.PER_TICK, "biome_precipitation", BiomeUniforms.playerI(player -> {
            class_1959.class_1963 precipitation = ((class_1959)player.method_37908().method_23753(player.method_24515()).comp_349()).method_48162(player.method_24515());
            return switch (precipitation) {
                default -> throw new MatchException(null, null);
                case class_1959.class_1963.field_9384 -> 0;
                case class_1959.class_1963.field_9382 -> 1;
                case class_1959.class_1963.field_9383 -> 2;
            };
        })).uniform1f(UniformUpdateFrequency.PER_TICK, "rainfall", BiomeUniforms.playerF(player -> ((ExtendedBiome)player.method_37908().method_23753(player.method_24515()).comp_349()).getDownfall())).uniform1f(UniformUpdateFrequency.PER_TICK, "temperature", BiomeUniforms.playerF(player -> ((class_1959)player.method_37908().method_23753(player.method_24515()).comp_349()).method_8712()));
    }

    private static BiomeCategories getBiomeCategory(class_6880<class_1959> holder) {
        if (holder.method_40220(class_6908.field_37383)) {
            return BiomeCategories.NONE;
        }
        if (holder.method_40220(class_6908.field_36499)) {
            return BiomeCategories.ICY;
        }
        if (holder.method_40220(class_6908.field_36514)) {
            return BiomeCategories.EXTREME_HILLS;
        }
        if (holder.method_40220(class_6908.field_36515)) {
            return BiomeCategories.TAIGA;
        }
        if (holder.method_40220(class_6908.field_36509)) {
            return BiomeCategories.OCEAN;
        }
        if (holder.method_40220(class_6908.field_36516)) {
            return BiomeCategories.JUNGLE;
        }
        if (holder.method_40220(class_6908.field_36517)) {
            return BiomeCategories.FOREST;
        }
        if (holder.method_40220(class_6908.field_36513)) {
            return BiomeCategories.MESA;
        }
        if (holder.method_40220(class_6908.field_36518)) {
            return BiomeCategories.NETHER;
        }
        if (holder.method_40220(class_6908.field_37394)) {
            return BiomeCategories.THE_END;
        }
        if (holder.method_40220(class_6908.field_36510)) {
            return BiomeCategories.BEACH;
        }
        if (holder.method_40220(class_6908.field_36520)) {
            return BiomeCategories.DESERT;
        }
        if (holder.method_40220(class_6908.field_36511)) {
            return BiomeCategories.RIVER;
        }
        if (holder.method_40220(class_6908.field_37378)) {
            return BiomeCategories.SWAMP;
        }
        if (holder.method_40220(class_6908.field_37377)) {
            return BiomeCategories.UNDERGROUND;
        }
        if (holder.method_40220(class_6908.field_37381)) {
            return BiomeCategories.MUSHROOM;
        }
        if (holder.method_40220(class_6908.field_36512)) {
            return BiomeCategories.MOUNTAIN;
        }
        return BiomeCategories.PLAINS;
    }

    static IntSupplier playerI(ToIntFunction<class_746> function) {
        return () -> {
            class_746 player = class_310.method_1551().field_1724;
            if (player == null) {
                return 0;
            }
            return function.applyAsInt(player);
        };
    }

    static FloatSupplier playerF(ToFloatFunction<class_746> function) {
        return () -> {
            class_746 player = class_310.method_1551().field_1724;
            if (player == null) {
                return 0.0f;
            }
            return function.applyAsFloat(player);
        };
    }

    @FunctionalInterface
    public static interface ToFloatFunction<T> {
        public float applyAsFloat(T var1);
    }
}

