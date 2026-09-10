/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMaps
 *  it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
 *  net.minecraft.class_1921
 *  net.minecraft.class_2246
 *  net.minecraft.class_2248
 *  net.minecraft.class_2680
 *  net.minecraft.class_2689
 *  net.minecraft.class_2769
 *  net.minecraft.class_2960
 *  net.minecraft.class_5321
 *  net.minecraft.class_6862
 *  net.minecraft.class_6885$class_6888
 *  net.minecraft.class_7923
 */
package net.irisshaders.iris.shaderpack.materialmap;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.irisshaders.iris.shaderpack.materialmap.BlockEntry;
import net.irisshaders.iris.shaderpack.materialmap.BlockRenderType;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.materialmap.TagEntry;
import net.minecraft.class_1921;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2680;
import net.minecraft.class_2689;
import net.minecraft.class_2769;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_6862;
import net.minecraft.class_6885;
import net.minecraft.class_7923;

public class BlockMaterialMapping {
    public static Object2IntMap<class_2680> createBlockStateIdMap(Int2ObjectLinkedOpenHashMap<List<BlockEntry>> blockPropertiesMap, Int2ObjectLinkedOpenHashMap<List<TagEntry>> tagPropertiesMap) {
        Object2IntLinkedOpenHashMap blockStateIds = new Object2IntLinkedOpenHashMap();
        blockStateIds.defaultReturnValue(-1);
        blockPropertiesMap.forEach((arg_0, arg_1) -> BlockMaterialMapping.lambda$createBlockStateIdMap$0((Object2IntMap)blockStateIds, arg_0, arg_1));
        tagPropertiesMap.forEach((arg_0, arg_1) -> BlockMaterialMapping.lambda$createBlockStateIdMap$1((Object2IntMap)blockStateIds, arg_0, arg_1));
        return blockStateIds;
    }

    private static void addTag(TagEntry tagEntry, Object2IntMap<class_2680> idMap, int intId) {
        List<class_6862> compatibleTags = class_7923.field_41175.method_40273().filter(t -> t.comp_327().method_12836().equalsIgnoreCase(tagEntry.id().getNamespace()) && t.comp_327().method_12832().equalsIgnoreCase(tagEntry.id().getName())).toList();
        if (compatibleTags.isEmpty()) {
            if (IrisPlatformHelpers.getInstance().isDevelopmentEnvironment()) {
                Iris.logger.warn("Failed to find the tag " + String.valueOf(tagEntry.id()));
            }
        } else if (compatibleTags.size() > 1) {
            Iris.logger.fatal("You've broke the system; congrats. More than one tag matched " + String.valueOf(tagEntry.id()));
        } else {
            ((class_6885.class_6888)class_7923.field_41175.method_40266(compatibleTags.getFirst()).get()).forEach(block -> {
                Map<String, String> propertyPredicates = tagEntry.propertyPredicates();
                if (propertyPredicates.isEmpty()) {
                    for (class_2680 state : ((class_2248)block.comp_349()).method_9595().method_11662()) {
                        idMap.putIfAbsent((Object)state, intId);
                    }
                    return;
                }
                LinkedHashMap properties = new LinkedHashMap();
                class_2689 stateManager = ((class_2248)block.comp_349()).method_9595();
                propertyPredicates.forEach((key, value) -> {
                    class_2769 property = stateManager.method_11663(key);
                    if (property == null) {
                        Iris.logger.warn("Error while parsing the block ID map entry for tag \"block." + intId + "\":");
                        Iris.logger.warn("- The block " + String.valueOf(((class_5321)block.method_40230().get()).method_29177()) + " has no property with the name " + key + ", ignoring!");
                        return;
                    }
                    properties.put((class_2769<?>)property, (String)value);
                });
                for (class_2680 state : stateManager.method_11662()) {
                    if (!BlockMaterialMapping.checkState(state, properties)) continue;
                    idMap.putIfAbsent((Object)state, intId);
                }
            });
        }
    }

    public static Map<class_2248, BlockRenderType> createBlockTypeMap(Map<NamespacedId, BlockRenderType> blockPropertiesMap) {
        if (blockPropertiesMap.isEmpty()) {
            return Object2ObjectMaps.emptyMap();
        }
        Reference2ReferenceOpenHashMap blockTypeIds = new Reference2ReferenceOpenHashMap();
        blockPropertiesMap.forEach((arg_0, arg_1) -> BlockMaterialMapping.lambda$createBlockTypeMap$5((Map)blockTypeIds, arg_0, arg_1));
        return blockTypeIds;
    }

    public static class_1921 convertBlockToRenderType(BlockRenderType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            default -> throw new MatchException(null, null);
            case BlockRenderType.SOLID -> class_1921.method_23577();
            case BlockRenderType.CUTOUT -> class_1921.method_23581();
            case BlockRenderType.CUTOUT_MIPPED -> class_1921.method_23579();
            case BlockRenderType.TRANSLUCENT -> class_1921.method_23583();
        };
    }

    private static void addBlockStates(BlockEntry entry, Object2IntMap<class_2680> idMap, int intId) {
        class_2960 resourceLocation;
        NamespacedId id = entry.id();
        try {
            resourceLocation = class_2960.method_60655((String)id.getNamespace(), (String)id.getName());
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to get entry for " + intId, exception);
        }
        class_2248 block = (class_2248)class_7923.field_41175.method_10223(resourceLocation);
        if (block == class_2246.field_10124) {
            return;
        }
        Map<String, String> propertyPredicates = entry.propertyPredicates();
        if (propertyPredicates.isEmpty()) {
            for (class_2680 state : block.method_9595().method_11662()) {
                idMap.putIfAbsent((Object)state, intId);
            }
            return;
        }
        LinkedHashMap properties = new LinkedHashMap();
        class_2689 stateManager = block.method_9595();
        propertyPredicates.forEach((key, value) -> {
            class_2769 property = stateManager.method_11663(key);
            if (property == null) {
                Iris.logger.warn("Error while parsing the block ID map entry for \"block." + intId + "\":");
                Iris.logger.warn("- The block " + String.valueOf(resourceLocation) + " has no property with the name " + key + ", ignoring!");
                return;
            }
            properties.put((class_2769<?>)property, (String)value);
        });
        for (class_2680 state : stateManager.method_11662()) {
            if (!BlockMaterialMapping.checkState(state, properties)) continue;
            idMap.putIfAbsent((Object)state, intId);
        }
    }

    private static boolean checkState(class_2680 state, Map<class_2769<?>, String> expectedValues) {
        for (Map.Entry<class_2769<?>, String> condition : expectedValues.entrySet()) {
            String actualValue;
            class_2769<?> property = condition.getKey();
            String expectedValue = condition.getValue();
            if (expectedValue.equals(actualValue = property.method_11901(state.method_11654(property)))) continue;
            return false;
        }
        return true;
    }

    private static /* synthetic */ void lambda$createBlockTypeMap$5(Map blockTypeIds, NamespacedId id, BlockRenderType blockType) {
        class_2960 resourceLocation = class_2960.method_60655((String)id.getNamespace(), (String)id.getName());
        class_2248 block = (class_2248)class_7923.field_41175.method_10223(resourceLocation);
        blockTypeIds.put(block, blockType);
    }

    private static /* synthetic */ void lambda$createBlockStateIdMap$1(Object2IntMap blockStateIds, Integer intId, List entries) {
        for (TagEntry entry : entries) {
            BlockMaterialMapping.addTag(entry, (Object2IntMap<class_2680>)blockStateIds, intId);
        }
    }

    private static /* synthetic */ void lambda$createBlockStateIdMap$0(Object2IntMap blockStateIds, Integer intId, List entries) {
        for (BlockEntry entry : entries) {
            BlockMaterialMapping.addBlockStates(entry, (Object2IntMap<class_2680>)blockStateIds, intId);
        }
    }
}

