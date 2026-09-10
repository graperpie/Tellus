/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  net.minecraft.class_1044
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_8215
 *  org.jetbrains.annotations.NotNull
 */
package net.irisshaders.iris.pbr.texture;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.nio.file.Path;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.state.StateUpdateNotifiers;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.irisshaders.iris.pbr.TextureTracker;
import net.irisshaders.iris.pbr.loader.PBRTextureLoader;
import net.irisshaders.iris.pbr.loader.PBRTextureLoaderRegistry;
import net.irisshaders.iris.pbr.texture.PBRDumpable;
import net.irisshaders.iris.pbr.texture.PBRTextureHolder;
import net.irisshaders.iris.pbr.texture.PBRType;
import net.irisshaders.iris.targets.backed.NativeImageBackedSingleColorTexture;
import net.minecraft.class_1044;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_8215;
import org.jetbrains.annotations.NotNull;

public class PBRTextureManager {
    public static final PBRTextureManager INSTANCE = new PBRTextureManager();
    private static Runnable normalTextureChangeListener;
    private static Runnable specularTextureChangeListener;
    private final Int2ObjectMap<PBRTextureHolder> holders = new Int2ObjectOpenHashMap();
    private final PBRTextureConsumerImpl consumer = new PBRTextureConsumerImpl();
    private NativeImageBackedSingleColorTexture defaultNormalTexture;
    private NativeImageBackedSingleColorTexture defaultSpecularTexture;
    private final PBRTextureHolder defaultHolder = new PBRTextureHolder(){

        @Override
        @NotNull
        public class_1044 normalTexture() {
            return PBRTextureManager.this.defaultNormalTexture;
        }

        @Override
        @NotNull
        public class_1044 specularTexture() {
            return PBRTextureManager.this.defaultSpecularTexture;
        }
    };

    private PBRTextureManager() {
    }

    private static void dumpTexture(class_8215 dumpable, class_2960 id, Path path) {
        try {
            dumpable.method_49712(id, path);
        }
        catch (IOException e) {
            Iris.logger.error("Failed to dump texture {}", id, e);
        }
    }

    private static void closeTexture(class_1044 texture) {
        try {
            texture.close();
        }
        catch (Exception exception) {
            // empty catch block
        }
        texture.method_4528();
    }

    public static void notifyPBRTexturesChanged() {
        if (normalTextureChangeListener != null) {
            normalTextureChangeListener.run();
        }
        if (specularTextureChangeListener != null) {
            specularTextureChangeListener.run();
        }
    }

    public void init() {
        this.defaultNormalTexture = new NativeImageBackedSingleColorTexture(PBRType.NORMAL.getDefaultValue());
        this.defaultSpecularTexture = new NativeImageBackedSingleColorTexture(PBRType.SPECULAR.getDefaultValue());
    }

    public PBRTextureHolder getHolder(int id) {
        PBRTextureHolder holder = (PBRTextureHolder)this.holders.get(id);
        if (holder == null) {
            return this.defaultHolder;
        }
        return holder;
    }

    public PBRTextureHolder getOrLoadHolder(int id) {
        PBRTextureHolder holder = (PBRTextureHolder)this.holders.get(id);
        if (holder == null) {
            holder = this.loadHolder(id);
            this.holders.put(id, (Object)holder);
        }
        return holder;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private PBRTextureHolder loadHolder(int id) {
        Class<?> clazz;
        PBRTextureLoader<?> loader;
        class_1044 texture = TextureTracker.INSTANCE.getTexture(id);
        if (texture != null && (loader = PBRTextureLoaderRegistry.INSTANCE.getLoader(clazz = texture.getClass())) != null) {
            int previousTextureBinding = GlStateManagerAccessor.getTEXTURES()[GlStateManagerAccessor.getActiveTexture()].field_5167;
            this.consumer.clear();
            try {
                loader.load(texture, class_310.method_1551().method_1478(), this.consumer);
                PBRTextureHolder pBRTextureHolder = this.consumer.toHolder();
                return pBRTextureHolder;
            }
            catch (Exception e) {
                Iris.logger.debug("Failed to load PBR textures for texture " + id, e);
            }
            finally {
                GlStateManager._bindTexture((int)previousTextureBinding);
            }
        }
        return this.defaultHolder;
    }

    public void onDeleteTexture(int id) {
        PBRTextureHolder holder = (PBRTextureHolder)this.holders.remove(id);
        if (holder != null) {
            this.closeHolder(holder);
        }
    }

    public void dumpTextures(Path path) {
        for (PBRTextureHolder holder : this.holders.values()) {
            if (holder == this.defaultHolder) continue;
            this.dumpHolder(holder, path);
        }
    }

    private void dumpHolder(PBRTextureHolder holder, Path path) {
        PBRDumpable dumpable;
        class_1044 normalTexture = holder.normalTexture();
        class_1044 specularTexture = holder.specularTexture();
        if (normalTexture != this.defaultNormalTexture && normalTexture instanceof PBRDumpable) {
            dumpable = (PBRDumpable)normalTexture;
            PBRTextureManager.dumpTexture(dumpable, dumpable.getDefaultDumpLocation(), path);
        }
        if (specularTexture != this.defaultSpecularTexture && specularTexture instanceof PBRDumpable) {
            dumpable = (PBRDumpable)specularTexture;
            PBRTextureManager.dumpTexture(dumpable, dumpable.getDefaultDumpLocation(), path);
        }
    }

    public void clear() {
        for (PBRTextureHolder holder : this.holders.values()) {
            if (holder == this.defaultHolder) continue;
            this.closeHolder(holder);
        }
        this.holders.clear();
    }

    public void close() {
        this.clear();
        this.defaultNormalTexture.close();
        this.defaultSpecularTexture.close();
    }

    private void closeHolder(PBRTextureHolder holder) {
        class_1044 normalTexture = holder.normalTexture();
        class_1044 specularTexture = holder.specularTexture();
        if (normalTexture != this.defaultNormalTexture) {
            PBRTextureManager.closeTexture(normalTexture);
        }
        if (specularTexture != this.defaultSpecularTexture) {
            PBRTextureManager.closeTexture(specularTexture);
        }
    }

    static {
        StateUpdateNotifiers.normalTextureChangeNotifier = listener -> {
            normalTextureChangeListener = listener;
        };
        StateUpdateNotifiers.specularTextureChangeNotifier = listener -> {
            specularTextureChangeListener = listener;
        };
    }

    private class PBRTextureConsumerImpl
    implements PBRTextureLoader.PBRTextureConsumer {
        private class_1044 normalTexture;
        private class_1044 specularTexture;
        private boolean changed;

        private PBRTextureConsumerImpl() {
        }

        @Override
        public void acceptNormalTexture(@NotNull class_1044 texture) {
            this.normalTexture = texture;
            this.changed = true;
        }

        @Override
        public void acceptSpecularTexture(@NotNull class_1044 texture) {
            this.specularTexture = texture;
            this.changed = true;
        }

        public void clear() {
            this.normalTexture = PBRTextureManager.this.defaultNormalTexture;
            this.specularTexture = PBRTextureManager.this.defaultSpecularTexture;
            this.changed = false;
        }

        public PBRTextureHolder toHolder() {
            if (this.changed) {
                return new PBRTextureHolderImpl(this.normalTexture, this.specularTexture);
            }
            return PBRTextureManager.this.defaultHolder;
        }
    }

    private record PBRTextureHolderImpl(class_1044 normalTexture, class_1044 specularTexture) implements PBRTextureHolder
    {
    }
}

