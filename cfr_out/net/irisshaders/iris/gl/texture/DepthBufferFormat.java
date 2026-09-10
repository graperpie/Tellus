/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.gl.texture;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public enum DepthBufferFormat {
    DEPTH(false),
    DEPTH16(false),
    DEPTH24(false),
    DEPTH32(false),
    DEPTH32F(false),
    DEPTH_STENCIL(true),
    DEPTH24_STENCIL8(true),
    DEPTH32F_STENCIL8(true);

    private final boolean combinedStencil;

    private DepthBufferFormat(boolean combinedStencil) {
        this.combinedStencil = combinedStencil;
    }

    @Nullable
    public static DepthBufferFormat fromGlEnum(int glenum) {
        return switch (glenum) {
            case 6402 -> DEPTH;
            case 33189 -> DEPTH16;
            case 33190 -> DEPTH24;
            case 33191 -> DEPTH32;
            case 36012 -> DEPTH32F;
            case 34041 -> DEPTH_STENCIL;
            case 35056 -> DEPTH24_STENCIL8;
            case 36013 -> DEPTH32F_STENCIL8;
            default -> null;
        };
    }

    public static DepthBufferFormat fromGlEnumOrDefault(int glenum) {
        DepthBufferFormat format = DepthBufferFormat.fromGlEnum(glenum);
        return Objects.requireNonNullElse(format, DEPTH);
    }

    public int getGlInternalFormat() {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> 6402;
            case 1 -> 33189;
            case 2 -> 33190;
            case 3 -> 33191;
            case 4 -> 36012;
            case 5 -> 34041;
            case 6 -> 35056;
            case 7 -> 36013;
        };
    }

    public int getGlType() {
        return this.isCombinedStencil() ? 34041 : 6402;
    }

    public int getGlFormat() {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0, 1 -> 5123;
            case 2, 3 -> 5125;
            case 4 -> 5126;
            case 5, 6 -> 34042;
            case 7 -> 36269;
        };
    }

    public boolean isCombinedStencil() {
        return this.combinedStencil;
    }
}

