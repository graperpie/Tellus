/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.texture;

import net.irisshaders.iris.pbr.texture.PBRSpriteHolder;
import org.jetbrains.annotations.Nullable;

public interface SpriteContentsExtension {
    @Nullable
    public PBRSpriteHolder getPBRHolder();

    public PBRSpriteHolder getOrCreatePBRHolder();
}

