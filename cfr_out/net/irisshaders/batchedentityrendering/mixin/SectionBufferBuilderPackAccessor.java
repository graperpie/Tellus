/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921
 *  net.minecraft.class_750
 *  net.minecraft.class_9799
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package net.irisshaders.batchedentityrendering.mixin;

import java.util.Map;
import net.minecraft.class_1921;
import net.minecraft.class_750;
import net.minecraft.class_9799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={class_750.class})
public interface SectionBufferBuilderPackAccessor {
    @Accessor
    public Map<class_1921, class_9799> getBuffers();
}

