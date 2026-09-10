/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.Long2ObjectMap
 *  net.minecraft.class_1297
 *  net.minecraft.class_1921
 *  net.minecraft.class_3191
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_4597
 *  net.minecraft.class_4599
 *  net.minecraft.class_4604
 *  net.minecraft.class_638
 *  net.minecraft.class_761
 *  net.minecraft.class_898
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package net.irisshaders.iris.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.class_1297;
import net.minecraft.class_1921;
import net.minecraft.class_3191;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4599;
import net.minecraft.class_4604;
import net.minecraft.class_638;
import net.minecraft.class_761;
import net.minecraft.class_898;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={class_761.class})
public interface LevelRendererAccessor {
    @Accessor(value="entityRenderDispatcher")
    public class_898 getEntityRenderDispatcher();

    @Invoker(value="renderSectionLayer")
    public void invokeRenderSectionLayer(class_1921 var1, double var2, double var4, double var6, Matrix4f var8, Matrix4f var9);

    @Invoker(value="setupRender")
    public void invokeSetupRender(class_4184 var1, class_4604 var2, boolean var3, boolean var4);

    @Invoker(value="renderEntity")
    public void invokeRenderEntity(class_1297 var1, double var2, double var4, double var6, float var8, class_4587 var9, class_4597 var10);

    @Accessor(value="level")
    public class_638 getLevel();

    @Accessor(value="renderBuffers")
    public class_4599 getRenderBuffers();

    @Accessor(value="renderBuffers")
    public void setRenderBuffers(class_4599 var1);

    @Accessor(value="generateClouds")
    public boolean shouldRegenerateClouds();

    @Accessor(value="generateClouds")
    public void setShouldRegenerateClouds(boolean var1);

    @Invoker
    public boolean invokeDoesMobEffectBlockSky(class_4184 var1);

    @Accessor
    public Long2ObjectMap<SortedSet<class_3191>> getDestructionProgress();
}

