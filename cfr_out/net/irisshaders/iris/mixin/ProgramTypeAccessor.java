/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_281$class_282
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package net.irisshaders.iris.mixin;

import net.minecraft.class_281;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={class_281.class_282.class})
public interface ProgramTypeAccessor {
    @Invoker(value="<init>")
    public static class_281.class_282 createProgramType(String name, int ordinal, String typeName, String extension, int glId) {
        throw new AssertionError();
    }
}

