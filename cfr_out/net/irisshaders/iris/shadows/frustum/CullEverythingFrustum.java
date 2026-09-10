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
package net.irisshaders.iris.shadows.frustum;

import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import net.minecraft.class_238;
import net.minecraft.class_4604;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class CullEverythingFrustum
extends class_4604
implements ViewportProvider,
Frustum {
    private final Vector3d position = new Vector3d();

    public CullEverythingFrustum() {
        super(new Matrix4f(), new Matrix4f());
    }

    public boolean canDetermineInvisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return false;
    }

    public boolean method_23093(class_238 box) {
        return false;
    }

    public void method_23088(double d, double e, double f) {
        this.position.set(d, e, f);
    }

    public Viewport sodium$createViewport() {
        return new Viewport((Frustum)this, this.position);
    }

    public boolean testAab(float v, float v1, float v2, float v3, float v4, float v5) {
        return false;
    }
}

