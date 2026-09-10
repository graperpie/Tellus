/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.client.gl.device.CommandList
 *  net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateType
 *  net.caffeinemc.mods.sodium.client.render.chunk.RenderSection
 *  net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager
 *  net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists
 *  net.caffeinemc.mods.sodium.client.render.viewport.Viewport
 *  net.minecraft.class_4184
 *  net.minecraft.class_638
 *  org.jetbrains.annotations.NotNull
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.compat.sodium.mixin;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateType;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.class_4184;
import net.minecraft.class_638;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={RenderSectionManager.class})
public class MixinRenderSectionManagerShadow {
    @Shadow(remap=false)
    @NotNull
    private SortedRenderLists renderLists;
    @Shadow(remap=false)
    @NotNull
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> taskLists;
    @Unique
    @NotNull
    private SortedRenderLists shadowRenderLists = SortedRenderLists.empty();
    @Unique
    @NotNull
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> shadowTaskLists = new EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>>(ChunkUpdateType.class);

    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void create(class_638 level, int renderDistance, CommandList commandList, CallbackInfo ci) {
        for (int var6 = 0; var6 < ChunkUpdateType.values().length; ++var6) {
            ChunkUpdateType type = ChunkUpdateType.values()[var6];
            this.shadowTaskLists.put(type, new ArrayDeque());
        }
    }

    @Redirect(remap=false, method={"createTerrainRenderList"}, at=@At(value="FIELD", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;renderLists:Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/SortedRenderLists;"))
    private void useShadowRenderList(RenderSectionManager instance, SortedRenderLists value) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            this.shadowRenderLists = value;
        } else {
            this.renderLists = value;
        }
    }

    @Redirect(remap=false, method={"createTerrainRenderList"}, at=@At(value="FIELD", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;taskLists:Ljava/util/Map;"))
    private void useShadowTaskrList(RenderSectionManager instance, @NotNull Map<ChunkUpdateType, ArrayDeque<RenderSection>> value) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            this.shadowTaskLists = value;
        } else {
            this.taskLists = value;
        }
    }

    @Inject(method={"update"}, at={@At(value="INVOKE", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;createTerrainRenderList(Lnet/minecraft/client/Camera;Lnet/caffeinemc/mods/sodium/client/render/viewport/Viewport;IZ)V", shift=At.Shift.AFTER)}, cancellable=true)
    private void cancelIfShadow(class_4184 camera, Viewport viewport, boolean spectator, CallbackInfo ci) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            ci.cancel();
        }
    }

    @Redirect(method={"getRenderLists", "getVisibleChunkCount", "renderLayer"}, at=@At(value="FIELD", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;renderLists:Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/SortedRenderLists;"), remap=false)
    private SortedRenderLists useShadowRenderList2(RenderSectionManager instance) {
        return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? this.shadowRenderLists : this.renderLists;
    }

    @Inject(method={"updateChunks"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void doNotUpdateDuringShadow(boolean updateImmediately, CallbackInfo ci) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            ci.cancel();
        }
    }

    @Inject(method={"uploadChunks"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void doNotUploadDuringShadow(CallbackInfo ci) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            ci.cancel();
        }
    }

    @Redirect(method={"resetRenderLists", "submitSectionTasks(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;)V"}, at=@At(value="FIELD", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;taskLists:Ljava/util/Map;"), remap=false)
    @NotNull
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> useShadowTaskList3(RenderSectionManager instance) {
        return ShadowRenderingState.areShadowsCurrentlyBeingRendered() ? this.shadowTaskLists : this.taskLists;
    }

    @Redirect(method={"resetRenderLists"}, at=@At(value="FIELD", target="Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;renderLists:Lnet/caffeinemc/mods/sodium/client/render/chunk/lists/SortedRenderLists;"), remap=false)
    private void useShadowRenderList3(RenderSectionManager instance, SortedRenderLists value) {
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) {
            this.shadowRenderLists = value;
        } else {
            this.renderLists = value;
        }
    }
}

