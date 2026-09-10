/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 *  org.joml.Vector3d
 */
package net.irisshaders.iris.helpers;

import net.minecraft.class_243;
import org.joml.Vector3d;

public class JomlConversions {
    public static Vector3d fromVec3(class_243 vec) {
        return new Vector3d(vec.method_10216(), vec.method_10214(), vec.method_10215());
    }
}

