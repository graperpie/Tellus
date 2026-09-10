/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntFunction
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats
 *  net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType
 *  net.minecraft.class_2248
 *  net.minecraft.class_2680
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.shaderpack.materialmap;

import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.irisshaders.iris.shaderpack.materialmap.BlockRenderType;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.minecraft.class_2248;
import net.minecraft.class_2680;
import org.jetbrains.annotations.Nullable;

public class WorldRenderingSettings {
    public static final WorldRenderingSettings INSTANCE = new WorldRenderingSettings();
    private boolean reloadRequired = false;
    private Object2IntMap<class_2680> blockStateIds = null;
    private Map<class_2248, BlockRenderType> blockTypeIds = null;
    private Object2IntFunction<NamespacedId> entityIds;
    private Object2IntFunction<NamespacedId> itemIds;
    private float ambientOcclusionLevel = 1.0f;
    private boolean disableDirectionalShading = false;
    private boolean hasVillagerConversionId = false;
    private boolean useSeparateAo = false;
    private boolean separateEntityDraws = false;
    private boolean voxelizeLightBlocks = false;
    private ChunkVertexType chunkVertexFormat = ChunkMeshFormats.COMPACT;

    public boolean isReloadRequired() {
        return this.reloadRequired;
    }

    public void clearReloadRequired() {
        this.reloadRequired = false;
    }

    @Nullable
    public Object2IntMap<class_2680> getBlockStateIds() {
        return this.blockStateIds;
    }

    public void setBlockStateIds(Object2IntMap<class_2680> blockStateIds) {
        if (this.blockStateIds != null && this.blockStateIds.equals(blockStateIds)) {
            return;
        }
        this.reloadRequired = true;
        this.blockStateIds = blockStateIds;
    }

    public Map<class_2248, BlockRenderType> getBlockTypeIds() {
        return this.blockTypeIds;
    }

    public void setBlockTypeIds(Map<class_2248, BlockRenderType> blockTypeIds) {
        if (this.blockTypeIds != null && this.blockTypeIds.equals(blockTypeIds)) {
            return;
        }
        this.reloadRequired = true;
        this.blockTypeIds = blockTypeIds;
    }

    @Nullable
    public Object2IntFunction<NamespacedId> getEntityIds() {
        return this.entityIds;
    }

    public void setEntityIds(Object2IntFunction<NamespacedId> entityIds) {
        this.entityIds = entityIds;
        this.hasVillagerConversionId = entityIds.containsKey((Object)new NamespacedId("minecraft", "zombie_villager_converting"));
    }

    @Nullable
    public Object2IntFunction<NamespacedId> getItemIds() {
        return this.itemIds;
    }

    public void setItemIds(Object2IntFunction<NamespacedId> itemIds) {
        this.itemIds = itemIds;
    }

    public float getAmbientOcclusionLevel() {
        return this.ambientOcclusionLevel;
    }

    public void setAmbientOcclusionLevel(float ambientOcclusionLevel) {
        if (ambientOcclusionLevel == this.ambientOcclusionLevel) {
            return;
        }
        this.reloadRequired = true;
        this.ambientOcclusionLevel = ambientOcclusionLevel;
    }

    public boolean shouldDisableDirectionalShading() {
        return this.disableDirectionalShading;
    }

    public void setDisableDirectionalShading(boolean disableDirectionalShading) {
        if (disableDirectionalShading == this.disableDirectionalShading) {
            return;
        }
        this.reloadRequired = true;
        this.disableDirectionalShading = disableDirectionalShading;
    }

    public boolean shouldUseSeparateAo() {
        return this.useSeparateAo;
    }

    public void setUseSeparateAo(boolean useSeparateAo) {
        if (useSeparateAo == this.useSeparateAo) {
            return;
        }
        this.reloadRequired = true;
        this.useSeparateAo = useSeparateAo;
    }

    public ChunkVertexType getVertexFormat() {
        return this.chunkVertexFormat;
    }

    public void setVertexFormat(ChunkVertexType chunkVertexFormat) {
        if (chunkVertexFormat == this.chunkVertexFormat) {
            return;
        }
        this.reloadRequired = true;
        this.chunkVertexFormat = chunkVertexFormat;
    }

    public boolean shouldVoxelizeLightBlocks() {
        return this.voxelizeLightBlocks;
    }

    public void setVoxelizeLightBlocks(boolean voxelizeLightBlocks) {
        if (voxelizeLightBlocks == this.voxelizeLightBlocks) {
            return;
        }
        this.reloadRequired = true;
        this.voxelizeLightBlocks = voxelizeLightBlocks;
    }

    public boolean shouldSeparateEntityDraws() {
        return this.separateEntityDraws;
    }

    public void setSeparateEntityDraws(boolean separateEntityDraws) {
        this.separateEntityDraws = separateEntityDraws;
    }

    public boolean hasVillagerConversionId() {
        return this.hasVillagerConversionId;
    }
}

