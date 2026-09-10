/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_3999
 *  net.minecraft.class_4002
 *  net.minecraft.class_638
 *  net.minecraft.class_708
 *  org.spongepowered.asm.mixin.Mixin
 */
package net.irisshaders.iris.mixin.fantastic;

import net.minecraft.class_3999;
import net.minecraft.class_4002;
import net.minecraft.class_638;
import net.minecraft.class_708;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets={"net.minecraft.client.particle.FireworkParticles$SparkParticle"})
public class MixinFireworkSparkParticle
extends class_708 {
    private MixinFireworkSparkParticle(class_638 level, double x, double y, double z, class_4002 spriteProvider, float upwardsAcceleration) {
        super(level, x, y, z, spriteProvider, upwardsAcceleration);
    }

    public class_3999 method_18122() {
        return class_3999.field_17828;
    }
}

