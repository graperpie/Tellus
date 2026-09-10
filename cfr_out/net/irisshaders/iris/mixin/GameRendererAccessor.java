/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_279
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_757
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package net.irisshaders.iris.mixin;

import net.minecraft.class_279;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={class_757.class})
public interface GameRendererAccessor {
    @Accessor
    public class_279 getBlurEffect();

    @Accessor
    public boolean getRenderHand();

    @Accessor
    public boolean getPanoramicMode();

    @Invoker
    public void invokeBobView(class_4587 var1, float var2);

    @Invoker
    public void invokeBobHurt(class_4587 var1, float var2);

    @Invoker
    public double invokeGetFov(class_4184 var1, float var2, boolean var3);

    @Invoker(value="shouldRenderBlockOutline")
    public boolean shouldRenderBlockOutlineA();
}

