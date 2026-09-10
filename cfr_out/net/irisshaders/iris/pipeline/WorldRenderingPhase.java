/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 */
package net.irisshaders.iris.pipeline;

import net.minecraft.class_1921;

public enum WorldRenderingPhase {
    NONE,
    SKY,
    SUNSET,
    CUSTOM_SKY,
    SUN,
    MOON,
    STARS,
    VOID,
    TERRAIN_SOLID,
    TERRAIN_CUTOUT_MIPPED,
    TERRAIN_CUTOUT,
    ENTITIES,
    BLOCK_ENTITIES,
    DESTROY,
    OUTLINE,
    DEBUG,
    HAND_SOLID,
    TERRAIN_TRANSLUCENT,
    TRIPWIRE,
    PARTICLES,
    CLOUDS,
    RAIN_SNOW,
    WORLD_BORDER,
    HAND_TRANSLUCENT;


    public static WorldRenderingPhase fromTerrainRenderType(class_1921 renderType) {
        if (renderType == class_1921.method_23577()) {
            return TERRAIN_SOLID;
        }
        if (renderType == class_1921.method_23581()) {
            return TERRAIN_CUTOUT;
        }
        if (renderType == class_1921.method_23579()) {
            return TERRAIN_CUTOUT_MIPPED;
        }
        if (renderType == class_1921.method_23583()) {
            return TERRAIN_TRANSLUCENT;
        }
        if (renderType == class_1921.method_29997()) {
            return TRIPWIRE;
        }
        throw new IllegalStateException("Illegal render type!");
    }
}

