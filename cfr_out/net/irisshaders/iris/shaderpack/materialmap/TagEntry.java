/*
 * Decompiled with CFR 0.152.
 */
package net.irisshaders.iris.shaderpack.materialmap;

import java.util.Map;
import net.irisshaders.iris.shaderpack.materialmap.Entry;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;

public record TagEntry(NamespacedId id, Map<String, String> propertyPredicates) implements Entry
{
}

