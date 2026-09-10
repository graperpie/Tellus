/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_1059
 *  net.minecraft.class_1060
 *  net.minecraft.class_287
 *  net.minecraft.class_289
 *  net.minecraft.class_290
 *  net.minecraft.class_293$class_5596
 *  net.minecraft.class_2960
 *  net.minecraft.class_3999
 */
package net.irisshaders.iris.fantastic;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_1059;
import net.minecraft.class_1060;
import net.minecraft.class_287;
import net.minecraft.class_289;
import net.minecraft.class_290;
import net.minecraft.class_293;
import net.minecraft.class_2960;
import net.minecraft.class_3999;

public class IrisParticleRenderTypes {
    public static final class_3999 OPAQUE_TERRAIN = new class_3999(){

        public class_287 method_18130(class_289 bufferBuilder, class_1060 textureManager) {
            RenderSystem.disableBlend();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.setShaderTexture((int)0, (class_2960)class_1059.field_5275);
            return bufferBuilder.method_60827(class_293.class_5596.field_27382, class_290.field_1584);
        }

        public String toString() {
            return "OPAQUE_TERRAIN_SHEET";
        }
    };
}

