/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1959
 *  net.minecraft.class_1959$class_5482
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.mixinterface.ExtendedBiome;
import net.minecraft.class_1959;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={class_1959.class}, priority=990)
public class MixinBiome
implements ExtendedBiome {
    @Shadow
    @Final
    private class_1959.class_5482 field_26393;
    @Unique
    private int biomeCategory = -1;

    @Override
    public int getBiomeCategory() {
        return this.biomeCategory;
    }

    @Override
    public void setBiomeCategory(int biomeCategory) {
        this.biomeCategory = biomeCategory;
    }

    @Override
    public float getDownfall() {
        return this.field_26393.comp_846();
    }
}

