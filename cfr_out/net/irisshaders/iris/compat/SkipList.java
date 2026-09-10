/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 */
package net.irisshaders.iris.compat;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Map;

public class SkipList {
    public static Map<Class<?>, MethodHandle> shouldSkipList = new Object2ObjectOpenHashMap();
    public static final MethodHandle NONE = MethodHandles.constant(Integer.class, 2);
    public static final MethodHandle ALWAYS = MethodHandles.constant(Integer.class, 1);
}

