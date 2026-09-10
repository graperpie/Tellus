/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1058
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.texture;

import net.minecraft.class_1058;
import org.jetbrains.annotations.Nullable;

public class PBRSpriteHolder {
    protected class_1058 normalSprite;
    protected class_1058 specularSprite;

    @Nullable
    public class_1058 getNormalSprite() {
        return this.normalSprite;
    }

    public void setNormalSprite(class_1058 sprite) {
        this.normalSprite = sprite;
    }

    @Nullable
    public class_1058 getSpecularSprite() {
        return this.specularSprite;
    }

    public void setSpecularSprite(class_1058 sprite) {
        this.specularSprite = sprite;
    }

    public void close() {
        if (this.normalSprite != null) {
            this.normalSprite.method_45851().close();
        }
        if (this.specularSprite != null) {
            this.specularSprite.method_45851().close();
        }
    }
}

