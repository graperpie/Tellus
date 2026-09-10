/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.client.render.viewport.Viewport
 *  net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider
 *  net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum
 *  net.minecraft.class_238
 *  net.minecraft.class_4604
 *  org.joml.Matrix4f
 *  org.joml.Vector3d
 */
package net.irisshaders.iris.shadows.frustum.fallback;

import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import net.irisshaders.iris.shadows.frustum.BoxCuller;
import net.minecraft.class_238;
import net.minecraft.class_4604;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class BoxCullingFrustum
extends class_4604
implements Frustum,
ViewportProvider {
    private final BoxCuller boxCuller;
    private final Vector3d position = new Vector3d();

    public BoxCullingFrustum(BoxCuller boxCuller) {
        super(new Matrix4f(), new Matrix4f());
        this.boxCuller = boxCuller;
    }

    public void method_23088(double cameraX, double cameraY, double cameraZ) {
        this.position.set(cameraX, cameraY, cameraZ);
        this.boxCuller.setPosition(cameraX, cameraY, cameraZ);
    }

    public boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return false;
    }

    public boolean method_23093(class_238 box) {
        return !this.boxCuller.isCulled(box);
    }

    public Viewport sodium$createViewport() {
        return new Viewport((Frustum)this, this.position);
    }

    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return !this.boxCuller.isCulledSodium(minX, minY, minZ, maxX, maxY, maxZ);
    }
}

