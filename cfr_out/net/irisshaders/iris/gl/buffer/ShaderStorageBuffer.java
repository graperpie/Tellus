/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.GlStateManager
 *  org.lwjgl.opengl.GL46C
 *  org.lwjgl.system.MemoryUtil
 */
package net.irisshaders.iris.gl.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import net.irisshaders.iris.gl.GLDebug;
import net.irisshaders.iris.gl.IrisRenderSystem;
import net.irisshaders.iris.gl.buffer.BuiltShaderStorageInfo;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;

public class ShaderStorageBuffer {
    protected final int index;
    protected final BuiltShaderStorageInfo info;
    protected final ByteBuffer content;
    protected int id = IrisRenderSystem.createBuffers();

    public ShaderStorageBuffer(int index, BuiltShaderStorageInfo info) {
        if (info.content() != null) {
            this.content = MemoryUtil.memAlloc((int)info.content().length);
            this.content.put(info.content());
            this.content.flip();
        } else {
            this.content = null;
        }
        GLDebug.nameObject(33504, this.id, "SSBO " + index);
        this.index = index;
        this.info = info;
    }

    public final int getIndex() {
        return this.index;
    }

    public final long getSize() {
        return this.info.size();
    }

    protected void destroy() {
        IrisRenderSystem.bindBufferBase(37074, this.index, 0);
        IrisRenderSystem.deleteBuffers(this.id);
        MemoryUtil.memFree((Buffer)this.content);
    }

    public void bind() {
        IrisRenderSystem.bindBufferBase(37074, this.index, this.id);
    }

    public void resizeIfRelative(int width, int height) {
        if (!this.info.relative()) {
            return;
        }
        IrisRenderSystem.deleteBuffers(this.id);
        int newId = GlStateManager._glGenBuffers();
        GlStateManager._glBindBuffer((int)37074, (int)newId);
        long newWidth = (long)((float)width * this.info.scaleX());
        long newHeight = (long)((float)height * this.info.scaleY());
        long finalSize = newHeight * newWidth * this.info.size();
        IrisRenderSystem.bufferStorage(37074, finalSize, 0);
        IrisRenderSystem.clearBufferSubData(37074, 33321, 0L, finalSize, 6403, 5120, new int[]{0});
        IrisRenderSystem.bindBufferBase(37074, this.index, newId);
        this.id = newId;
    }

    public int getId() {
        return this.id;
    }

    public void createStatic() {
        GlStateManager._glBindBuffer((int)37074, (int)this.getId());
        IrisRenderSystem.bufferStorage(37074, this.info.size(), this.content == null ? 0 : 256);
        if (this.content != null) {
            GL46C.glBufferSubData((int)37074, (long)0L, (ByteBuffer)this.content);
        } else {
            IrisRenderSystem.clearBufferSubData(37074, 33321, 0L, this.info.size(), 6403, 5120, new int[]{0});
        }
        this.bind();
    }
}

