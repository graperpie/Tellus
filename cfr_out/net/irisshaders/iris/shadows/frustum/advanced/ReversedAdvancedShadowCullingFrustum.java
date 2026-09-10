/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum
 *  net.minecraft.class_238
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 */
package net.irisshaders.iris.shadows.frustum.advanced;

import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import net.irisshaders.iris.shadows.frustum.BoxCuller;
import net.irisshaders.iris.shadows.frustum.advanced.AdvancedShadowCullingFrustum;
import net.minecraft.class_238;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

public class ReversedAdvancedShadowCullingFrustum
extends AdvancedShadowCullingFrustum
implements Frustum {
    private final BoxCuller distanceCuller;

    public ReversedAdvancedShadowCullingFrustum(Matrix4fc modelViewProjection, Matrix4fc shadowProjection, Vector3f shadowLightVectorFromOrigin, BoxCuller voxelCuller, BoxCuller distanceCuller) {
        super(modelViewProjection, shadowProjection, shadowLightVectorFromOrigin, voxelCuller);
        this.distanceCuller = distanceCuller;
    }

    @Override
    public void method_23088(double cameraX, double cameraY, double cameraZ) {
        if (this.distanceCuller != null) {
            this.distanceCuller.setPosition(cameraX, cameraY, cameraZ);
        }
        super.method_23088(cameraX, cameraY, cameraZ);
    }

    @Override
    public boolean method_23093(class_238 aabb) {
        if (this.distanceCuller != null && this.distanceCuller.isCulled(aabb)) {
            return false;
        }
        if (this.boxCuller != null && !this.boxCuller.isCulled(aabb)) {
            return true;
        }
        return this.isVisible(aabb.field_1323, aabb.field_1322, aabb.field_1321, aabb.field_1320, aabb.field_1325, aabb.field_1324) != 0;
    }

    @Override
    public int fastAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        if (this.distanceCuller != null && this.distanceCuller.isCulled(minX, minY, minZ, maxX, maxY, maxZ)) {
            return 0;
        }
        if (this.boxCuller != null && !this.boxCuller.isCulled(minX, minY, minZ, maxX, maxY, maxZ)) {
            return 2;
        }
        return this.isVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        if (this.distanceCuller != null && this.distanceCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ)) {
            return false;
        }
        if (this.boxCuller != null && !this.boxCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ)) {
            return true;
        }
        return this.checkCornerVisibility(minX, minY, minZ, maxX, maxY, maxZ) > 0;
    }
}

