/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_156
 *  net.minecraft.class_1921
 *  net.minecraft.class_1921$class_4688
 *  net.minecraft.class_290
 *  net.minecraft.class_293
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_4668
 *  net.minecraft.class_4668$class_4683
 *  net.minecraft.class_4668$class_5939
 *  net.minecraft.class_4668$class_5942
 */
package net.irisshaders.iris.pathways;

import java.util.function.Function;
import net.irisshaders.iris.layer.InnerWrappedRenderType;
import net.irisshaders.iris.layer.LightningRenderStateShard;
import net.irisshaders.iris.pipeline.programs.ShaderAccess;
import net.minecraft.class_156;
import net.minecraft.class_1921;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_4668;

public class LightningHandler
extends class_1921 {
    public static final class_1921 IRIS_LIGHTNING = new InnerWrappedRenderType("iris_lightning2", (class_1921)class_1921.method_24049((String)"iris_lightning", (class_293)class_290.field_1576, (class_293.class_5596)class_293.class_5596.field_27382, (int)256, (boolean)false, (boolean)true, (class_1921.class_4688)class_1921.class_4688.method_23598().method_34578(field_29429).method_23616(field_21349).method_23615(field_21367).method_23610(field_25282).method_23617(false)), new LightningRenderStateShard());
    public static final Function<class_2960, class_1921> MEKANISM_FLAME = class_156.method_34866(resourceLocation -> {
        class_1921.class_4688 state = class_1921.class_4688.method_23598().method_34578(new class_4668.class_5942(ShaderAccess::getMekanismFlameShader)).method_34577((class_4668.class_5939)new class_4668.class_4683(resourceLocation, false, false)).method_23615(field_21370).method_23617(true);
        return LightningHandler.method_24049((String)"mek_flame", (class_293)class_290.field_1575, (class_293.class_5596)class_293.class_5596.field_27382, (int)256, (boolean)true, (boolean)false, (class_1921.class_4688)state);
    });
    public static final class_1921 MEKASUIT = LightningHandler.method_24049((String)"mekasuit", (class_293)class_290.field_1580, (class_293.class_5596)class_293.class_5596.field_27382, (int)131072, (boolean)true, (boolean)false, (class_1921.class_4688)class_1921.class_4688.method_23598().method_34578(new class_4668.class_5942(ShaderAccess::getMekasuitShader)).method_34577((class_4668.class_5939)field_21377).method_23608(field_21383).method_23611(field_21385).method_23617(true));
    public static final Function<class_2960, class_1921> SPS = class_156.method_34866(r -> LightningHandler.method_24049((String)"sps", (class_293)class_290.field_1575, (class_293.class_5596)class_293.class_5596.field_27382, (int)1536, (boolean)true, (boolean)false, (class_1921.class_4688)class_1921.class_4688.method_23598().method_34578(new class_4668.class_5942(ShaderAccess::getSPSShader)).method_34577((class_4668.class_5939)new class_4668.class_4683(r, false, false)).method_23615(class_4668.field_21367).method_23617(true)));

    public LightningHandler(String pRenderType0, class_293 pVertexFormat1, class_293.class_5596 pVertexFormat$Mode2, int pInt3, boolean pBoolean4, boolean pBoolean5, Runnable pRunnable6, Runnable pRunnable7) {
        super(pRenderType0, pVertexFormat1, pVertexFormat$Mode2, pInt3, pBoolean4, pBoolean5, pRunnable6, pRunnable7);
    }
}

