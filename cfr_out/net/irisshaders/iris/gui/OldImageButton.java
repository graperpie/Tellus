/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 *  net.minecraft.class_4185
 *  net.minecraft.class_4185$class_4241
 *  net.minecraft.class_5244
 */
package net.irisshaders.iris.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_4185;
import net.minecraft.class_5244;

public class OldImageButton
extends class_4185 {
    protected final class_2960 resourceLocation;
    protected final int xTexStart;
    protected final int yTexStart;
    protected final int yDiffTex;
    protected final int textureWidth;
    protected final int textureHeight;

    public OldImageButton(int pImageButton0, int pInt1, int pInt2, int pInt3, int pInt4, int pInt5, class_2960 pResourceLocation6, class_4185.class_4241 pButton$OnPress7) {
        this(pImageButton0, pInt1, pInt2, pInt3, pInt4, pInt5, pInt3, pResourceLocation6, 256, 256, pButton$OnPress7);
    }

    public OldImageButton(int pImageButton0, int pInt1, int pInt2, int pInt3, int pInt4, int pInt5, int pInt6, class_2960 pResourceLocation7, class_4185.class_4241 pButton$OnPress8) {
        this(pImageButton0, pInt1, pInt2, pInt3, pInt4, pInt5, pInt6, pResourceLocation7, 256, 256, pButton$OnPress8);
    }

    public OldImageButton(int pImageButton0, int pInt1, int pInt2, int pInt3, int pInt4, int pInt5, int pInt6, class_2960 pResourceLocation7, int pInt8, int pInt9, class_4185.class_4241 pButton$OnPress10) {
        this(pImageButton0, pInt1, pInt2, pInt3, pInt4, pInt5, pInt6, pResourceLocation7, pInt8, pInt9, pButton$OnPress10, class_5244.field_39003);
    }

    public OldImageButton(int pImageButton0, int pInt1, int pInt2, int pInt3, int pInt4, int pInt5, int pInt6, class_2960 pResourceLocation7, int pInt8, int pInt9, class_4185.class_4241 pButton$OnPress10, class_2561 pComponent11) {
        super(pImageButton0, pInt1, pInt2, pInt3, pComponent11, pButton$OnPress10, field_40754);
        this.textureWidth = pInt8;
        this.textureHeight = pInt9;
        this.xTexStart = pInt4;
        this.yTexStart = pInt5;
        this.yDiffTex = pInt6;
        this.resourceLocation = pResourceLocation7;
    }

    public void method_48579(class_332 pImageButton0, int pInt1, int pInt2, float pFloat3) {
        this.renderTexture(pImageButton0, this.resourceLocation, this.method_46426(), this.method_46427(), this.xTexStart, this.yTexStart, this.yDiffTex, this.field_22758, this.field_22759, this.textureWidth, this.textureHeight);
    }

    public void renderTexture(class_332 pAbstractWidget0, class_2960 pResourceLocation1, int pInt2, int pInt3, int pInt4, int pInt5, int pInt6, int pInt7, int pInt8, int pInt9, int pInt10) {
        int lvInt12 = pInt5;
        if (!this.method_37303()) {
            lvInt12 = pInt5 + pInt6 * 2;
        } else if (this.method_25367()) {
            lvInt12 = pInt5 + pInt6;
        }
        RenderSystem.enableDepthTest();
        pAbstractWidget0.method_25290(pResourceLocation1, pInt2, pInt3, (float)pInt4, (float)lvInt12, pInt7, pInt8, pInt9, pInt10);
    }
}

