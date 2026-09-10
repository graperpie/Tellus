/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_315
 *  net.minecraft.class_315$class_5823
 *  net.minecraft.class_7172
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 */
package net.irisshaders.iris.mixin;

import net.minecraft.class_315;
import net.minecraft.class_7172;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={class_315.class})
public abstract class MixinMaxFpsCrashFix {
    @Unique
    private void iris$resetFramerateLimit(class_315.class_5823 instance, String name, class_7172<Integer> option) {
        if ((Integer)option.method_41753() == 0) {
            option.method_41748((Object)120);
        }
        instance.method_42570(name, option);
    }
}

