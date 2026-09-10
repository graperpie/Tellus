/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_293
 *  net.minecraft.class_296
 *  net.minecraft.class_296$class_297
 *  net.minecraft.class_296$class_298
 */
package net.irisshaders.iris.vertices;

import net.irisshaders.iris.Iris;
import net.minecraft.class_293;
import net.minecraft.class_296;

public class IrisVertexFormats {
    public static final class_296 ENTITY_ELEMENT;
    public static final class_296 ENTITY_ID_ELEMENT;
    public static final class_296 MID_TEXTURE_ELEMENT;
    public static final class_296 TANGENT_ELEMENT;
    public static final class_296 MID_BLOCK_ELEMENT;
    public static final class_293 TERRAIN;
    public static final class_293 ENTITY;
    public static final class_293 GLYPH;
    public static final class_293 CLOUDS;

    private static void debug(class_293 format) {
        Iris.logger.info("Vertex format: " + String.valueOf(format) + " with byte size " + format.method_1362());
        int byteIndex = 0;
        for (class_296 element : format.method_1357()) {
            Iris.logger.info(String.valueOf(element) + " @ " + byteIndex + " is " + String.valueOf(element.comp_2844()) + " " + String.valueOf(element.comp_2845()));
            byteIndex += element.method_60847();
        }
    }

    private static int getNextVertexFormatElementId() {
        int id = 0;
        while (class_296.method_60844((int)id) != null) {
            if (++id < 32) continue;
            throw new RuntimeException("Too many mods registering VertexFormatElements");
        }
        return id;
    }

    static {
        int LAST_UV = 0;
        for (int i = 0; i < 32; ++i) {
            class_296 element = class_296.method_60844((int)i);
            if (element == null || element.comp_2845() != class_296.class_298.field_1636) continue;
            LAST_UV = Math.max(LAST_UV, element.comp_2843());
        }
        ENTITY_ELEMENT = class_296.method_60845((int)IrisVertexFormats.getNextVertexFormatElementId(), (int)0, (class_296.class_297)class_296.class_297.field_1625, (class_296.class_298)class_296.class_298.field_20782, (int)2);
        ENTITY_ID_ELEMENT = class_296.method_60845((int)IrisVertexFormats.getNextVertexFormatElementId(), (int)(LAST_UV + 1), (class_296.class_297)class_296.class_297.field_1622, (class_296.class_298)class_296.class_298.field_1636, (int)3);
        MID_TEXTURE_ELEMENT = class_296.method_60845((int)IrisVertexFormats.getNextVertexFormatElementId(), (int)0, (class_296.class_297)class_296.class_297.field_1623, (class_296.class_298)class_296.class_298.field_20782, (int)2);
        TANGENT_ELEMENT = class_296.method_60845((int)IrisVertexFormats.getNextVertexFormatElementId(), (int)0, (class_296.class_297)class_296.class_297.field_1621, (class_296.class_298)class_296.class_298.field_20782, (int)4);
        MID_BLOCK_ELEMENT = class_296.method_60845((int)IrisVertexFormats.getNextVertexFormatElementId(), (int)0, (class_296.class_297)class_296.class_297.field_1621, (class_296.class_298)class_296.class_298.field_20782, (int)3);
        TERRAIN = class_293.method_60833().method_60842("Position", class_296.field_52107).method_60842("Color", class_296.field_52108).method_60842("UV0", class_296.field_52109).method_60842("UV2", class_296.field_52112).method_60842("Normal", class_296.field_52113).method_60841(1).method_60842("mc_Entity", ENTITY_ELEMENT).method_60842("mc_midTexCoord", MID_TEXTURE_ELEMENT).method_60842("at_tangent", TANGENT_ELEMENT).method_60842("at_midBlock", MID_BLOCK_ELEMENT).method_60841(1).method_60840();
        ENTITY = class_293.method_60833().method_60842("Position", class_296.field_52107).method_60842("Color", class_296.field_52108).method_60842("UV0", class_296.field_52109).method_60842("UV1", class_296.field_52111).method_60842("UV2", class_296.field_52112).method_60842("Normal", class_296.field_52113).method_60841(1).method_60842("iris_Entity", ENTITY_ID_ELEMENT).method_60842("mc_midTexCoord", MID_TEXTURE_ELEMENT).method_60842("at_tangent", TANGENT_ELEMENT).method_60840();
        GLYPH = class_293.method_60833().method_60842("Position", class_296.field_52107).method_60842("Color", class_296.field_52108).method_60842("UV0", class_296.field_52109).method_60842("UV2", class_296.field_52112).method_60842("Normal", class_296.field_52113).method_60841(1).method_60842("iris_Entity", ENTITY_ID_ELEMENT).method_60842("mc_midTexCoord", MID_TEXTURE_ELEMENT).method_60842("at_tangent", TANGENT_ELEMENT).method_60841(1).method_60840();
        CLOUDS = class_293.method_60833().method_60842("Position", class_296.field_52107).method_60842("Color", class_296.field_52108).method_60842("Normal", class_296.field_52113).method_60841(1).method_60840();
    }
}

