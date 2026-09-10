/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_281$class_282
 *  org.apache.commons.lang3.ArrayUtils
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.Shadow
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.gl.program.IrisProgramTypes;
import net.irisshaders.iris.mixin.ProgramTypeAccessor;
import net.minecraft.class_281;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={class_281.class_282.class})
public class MixinProgramType {
    @Shadow
    @Final
    @Mutable
    private static class_281.class_282[] field_1532;

    static {
        int baseOrdinal = field_1532.length;
        IrisProgramTypes.GEOMETRY = ProgramTypeAccessor.createProgramType("GEOMETRY", baseOrdinal, "geometry", ".gsh", 36313);
        IrisProgramTypes.TESS_CONTROL = ProgramTypeAccessor.createProgramType("TESS_CONTROL", baseOrdinal + 1, "tess_control", ".tcs", 36488);
        IrisProgramTypes.TESS_EVAL = ProgramTypeAccessor.createProgramType("TESS_EVAL", baseOrdinal + 2, "tess_eval", ".tes", 36487);
        field_1532 = (class_281.class_282[])ArrayUtils.addAll((Object[])field_1532, (Object[])new class_281.class_282[]{IrisProgramTypes.GEOMETRY, IrisProgramTypes.TESS_CONTROL, IrisProgramTypes.TESS_EVAL});
    }
}

