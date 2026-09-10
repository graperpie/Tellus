/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_4604
 */
package net.irisshaders.iris.shadows.frustum;

import net.minecraft.class_4604;

public class FrustumHolder {
    private class_4604 frustum;
    private String distanceInfo = "(unavailable)";
    private String cullingInfo = "(unavailable)";

    public FrustumHolder setInfo(class_4604 frustum, String distanceInfo, String cullingInfo) {
        this.frustum = frustum;
        this.distanceInfo = distanceInfo;
        this.cullingInfo = cullingInfo;
        return this;
    }

    public class_4604 getFrustum() {
        return this.frustum;
    }

    public String getDistanceInfo() {
        return this.distanceInfo;
    }

    public String getCullingInfo() {
        return this.cullingInfo;
    }
}

