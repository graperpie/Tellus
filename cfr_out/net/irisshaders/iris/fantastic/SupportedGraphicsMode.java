/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_5365
 *  net.minecraft.class_7172
 */
package net.irisshaders.iris.fantastic;

import net.irisshaders.iris.Iris;
import net.minecraft.class_5365;
import net.minecraft.class_7172;

public enum SupportedGraphicsMode {
    FAST,
    FANCY;


    public static SupportedGraphicsMode fromVanilla(class_7172<class_5365> status) {
        return switch ((class_5365)status.method_41753()) {
            default -> throw new MatchException(null, null);
            case class_5365.field_25427 -> FAST;
            case class_5365.field_25428 -> FANCY;
            case class_5365.field_25429 -> {
                Iris.logger.warn("Detected Fabulous Graphics being used somehow, changing to Fancy!");
                status.method_41748((Object)class_5365.field_25428);
                yield FANCY;
            }
        };
    }

    public class_5365 toVanilla() {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> class_5365.field_25427;
            case 1 -> class_5365.field_25428;
        };
    }
}

