/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.pbr.texture;

import net.irisshaders.iris.pbr.texture.PBRAtlasHolder;
import org.jetbrains.annotations.Nullable;

public interface TextureAtlasExtension {
    @Nullable
    public PBRAtlasHolder getPBRHolder();

    public PBRAtlasHolder getOrCreatePBRHolder();
}

