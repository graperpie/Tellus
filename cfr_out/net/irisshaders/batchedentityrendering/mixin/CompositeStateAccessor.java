/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1921$class_4688
 *  net.minecraft.class_4668$class_4672
 *  net.minecraft.class_4668$class_4685
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package net.irisshaders.batchedentityrendering.mixin;

import net.minecraft.class_1921;
import net.minecraft.class_4668;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={class_1921.class_4688.class})
public interface CompositeStateAccessor {
    @Accessor(value="transparencyState")
    public class_4668.class_4685 getTransparency();

    @Accessor(value="depthTestState")
    public class_4668.class_4672 getDepth();
}

