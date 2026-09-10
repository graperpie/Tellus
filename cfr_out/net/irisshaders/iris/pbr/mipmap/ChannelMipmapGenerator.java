/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_5253$class_8045
 */
package net.irisshaders.iris.pbr.mipmap;

import net.irisshaders.iris.pbr.mipmap.AbstractMipmapGenerator;
import net.minecraft.class_5253;

public class ChannelMipmapGenerator
extends AbstractMipmapGenerator {
    protected final BlendFunction redFunc;
    protected final BlendFunction greenFunc;
    protected final BlendFunction blueFunc;
    protected final BlendFunction alphaFunc;

    public ChannelMipmapGenerator(BlendFunction redFunc, BlendFunction greenFunc, BlendFunction blueFunc, BlendFunction alphaFunc) {
        this.redFunc = redFunc;
        this.greenFunc = greenFunc;
        this.blueFunc = blueFunc;
        this.alphaFunc = alphaFunc;
    }

    @Override
    public int blend(int c0, int c1, int c2, int c3) {
        return class_5253.class_8045.method_48344((int)this.alphaFunc.blend(class_5253.class_8045.method_48342((int)c0), class_5253.class_8045.method_48342((int)c1), class_5253.class_8045.method_48342((int)c2), class_5253.class_8045.method_48342((int)c3)), (int)this.blueFunc.blend(class_5253.class_8045.method_48347((int)c0), class_5253.class_8045.method_48347((int)c1), class_5253.class_8045.method_48347((int)c2), class_5253.class_8045.method_48347((int)c3)), (int)this.greenFunc.blend(class_5253.class_8045.method_48346((int)c0), class_5253.class_8045.method_48346((int)c1), class_5253.class_8045.method_48346((int)c2), class_5253.class_8045.method_48346((int)c3)), (int)this.redFunc.blend(class_5253.class_8045.method_48345((int)c0), class_5253.class_8045.method_48345((int)c1), class_5253.class_8045.method_48345((int)c2), class_5253.class_8045.method_48345((int)c3)));
    }

    public static interface BlendFunction {
        public int blend(int var1, int var2, int var3, int var4);
    }
}

