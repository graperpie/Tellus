/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  net.minecraft.class_761
 *  net.minecraft.class_846$class_851
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Mutable
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 */
package net.irisshaders.iris.mixin.shadows;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.irisshaders.iris.shadows.CullingDataCache;
import net.minecraft.class_761;
import net.minecraft.class_846;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={class_761.class})
public class MixinLevelRenderer
implements CullingDataCache {
    @Shadow
    @Final
    @Mutable
    private ObjectArrayList<class_846.class_851> field_45616;
    @Unique
    private ObjectArrayList<class_846.class_851> savedRenderChunks = new ObjectArrayList(69696);
    @Shadow
    private double field_4115;
    @Shadow
    private double field_4064;
    @Unique
    private double savedLastCameraX;
    @Unique
    private double savedLastCameraY;
    @Unique
    private double savedLastCameraZ;
    @Unique
    private double savedLastCameraPitch;
    @Unique
    private double savedLastCameraYaw;

    @Override
    public void saveState() {
        this.swap();
    }

    @Override
    public void restoreState() {
        this.swap();
    }

    @Unique
    private void swap() {
        ObjectArrayList<class_846.class_851> tmpList = this.field_45616;
        this.field_45616 = this.savedRenderChunks;
        this.savedRenderChunks = tmpList;
        double tmp = this.field_4115;
        this.field_4115 = this.savedLastCameraPitch;
        this.savedLastCameraPitch = tmp;
        tmp = this.field_4064;
        this.field_4064 = this.savedLastCameraYaw;
        this.savedLastCameraYaw = tmp;
    }
}

