/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1792
 *  org.spongepowered.asm.mixin.Mixin
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.api.v0.item.IrisItemLightProvider;
import net.minecraft.class_1792;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={class_1792.class})
public class MixinItem
implements IrisItemLightProvider {
}

