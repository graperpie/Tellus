/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  net.minecraft.class_2680
 *  net.minecraft.class_846$class_851$class_4578
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 */
package net.irisshaders.iris.mixin.vertices.block_rendering;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.minecraft.class_2680;
import net.minecraft.class_846;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={class_846.class_851.class_4578.class}, priority=999)
public class MixinChunkRebuildTask {
    @Unique
    private final Object2IntMap<class_2680> blockStateIds = this.getBlockStateIds();

    @Unique
    private Object2IntMap<class_2680> getBlockStateIds() {
        return WorldRenderingSettings.INSTANCE.getBlockStateIds();
    }
}

