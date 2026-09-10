/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1104
 *  net.minecraft.class_4897
 *  net.minecraft.class_746
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package net.irisshaders.iris.mixin;

import java.util.List;
import net.irisshaders.iris.mixinterface.BiomeAmbienceInterface;
import net.irisshaders.iris.mixinterface.LocalPlayerInterface;
import net.minecraft.class_1104;
import net.minecraft.class_4897;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={class_746.class})
public class MixinLocalPlayer
implements LocalPlayerInterface {
    @Shadow
    @Final
    private List<class_1104> field_3933;

    @Override
    public float getCurrentConstantMood() {
        for (class_1104 ambientSoundHandler : this.field_3933) {
            if (!(ambientSoundHandler instanceof class_4897)) continue;
            return ((BiomeAmbienceInterface)ambientSoundHandler).getConstantMood();
        }
        return 0.0f;
    }
}

