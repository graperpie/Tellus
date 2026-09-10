/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_238
 */
package net.irisshaders.iris.shadows.frustum;

import net.minecraft.class_238;

public class BoxCuller {
    private final double maxDistance;
    private double minAllowedX;
    private double maxAllowedX;
    private double minAllowedY;
    private double maxAllowedY;
    private double minAllowedZ;
    private double maxAllowedZ;

    public BoxCuller(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public void setPosition(double cameraX, double cameraY, double cameraZ) {
        this.minAllowedX = cameraX - this.maxDistance;
        this.maxAllowedX = cameraX + this.maxDistance;
        this.minAllowedY = cameraY - this.maxDistance;
        this.maxAllowedY = cameraY + this.maxDistance;
        this.minAllowedZ = cameraZ - this.maxDistance;
        this.maxAllowedZ = cameraZ + this.maxDistance;
    }

    public boolean isCulled(class_238 aabb) {
        return this.isCulled((float)aabb.field_1323, (float)aabb.field_1322, (float)aabb.field_1321, (float)aabb.field_1320, (float)aabb.field_1325, (float)aabb.field_1324);
    }

    public boolean isCulled(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (maxX < this.minAllowedX || minX > this.maxAllowedX) {
            return true;
        }
        if (maxY < this.minAllowedY || minY > this.maxAllowedY) {
            return true;
        }
        return maxZ < this.minAllowedZ || minZ > this.maxAllowedZ;
    }

    public boolean isCulledSodium(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (maxX < -this.maxDistance || minX > this.maxDistance) {
            return true;
        }
        if (maxY < -this.maxDistance || minY > this.maxDistance) {
            return true;
        }
        return maxZ < -this.maxDistance || minZ > this.maxDistance;
    }

    public String toString() {
        return "Box Culling active; max distance " + this.maxDistance;
    }
}

