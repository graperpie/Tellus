/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_9799
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.vertices.MojangBufferAccessor;
import net.minecraft.class_9799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={class_9799.class})
public class MixinByteBufferBuilder
implements MojangBufferAccessor {
    @Shadow
    long field_52082;

    @Override
    public long getPointer() {
        return this.field_52082;
    }
}

