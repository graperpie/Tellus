/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiShadowCullingFrustum
 *  com.seibel.distanthorizons.api.objects.math.DhApiMat4f
 *  net.minecraft.class_4604
 *  org.joml.Matrix4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 */
package net.irisshaders.iris.compat.dh.mixin;

import com.seibel.distanthorizons.api.interfaces.override.rendering.IDhApiShadowCullingFrustum;
import com.seibel.distanthorizons.api.objects.math.DhApiMat4f;
import net.irisshaders.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import net.minecraft.class_4604;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={AdvancedShadowCullingFrustum.class})
public class MixinAdvancedShadowCullingFrustum
extends class_4604
implements IDhApiShadowCullingFrustum {
    @Unique
    private int worldMinYDH;
    @Unique
    private int worldMaxYDH;

    public MixinAdvancedShadowCullingFrustum(Matrix4f matrix4f, Matrix4f matrix4f2) {
        super(matrix4f, matrix4f2);
    }

    @Shadow(remap=false)
    protected int isVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        throw new IllegalStateException();
    }

    public void update(int worldMinBlockY, int worldMaxBlockY, DhApiMat4f worldViewProjection) {
        this.worldMinYDH = worldMinBlockY;
        this.worldMaxYDH = worldMaxBlockY;
    }

    public boolean intersects(int lodBlockPosMinX, int lodBlockPosMinZ, int lodBlockWidth, int lodDetailLevel) {
        return this.isVisible(lodBlockPosMinX, this.worldMinYDH, lodBlockPosMinZ, lodBlockPosMinX + lodBlockWidth, this.worldMaxYDH, lodBlockPosMinZ + lodBlockWidth) != 0;
    }
}

