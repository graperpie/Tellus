/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_2558
 *  net.minecraft.class_2558$class_2559
 *  net.minecraft.class_2561
 *  net.minecraft.class_2568
 *  net.minecraft.class_2568$class_5247
 *  net.minecraft.class_2678
 *  net.minecraft.class_310
 *  net.minecraft.class_634
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.shader.ShaderCompileException;
import net.minecraft.class_124;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_2568;
import net.minecraft.class_2678;
import net.minecraft.class_310;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_634.class})
public class MixinClientPacketListener {
    @Inject(method={"handleLogin"}, at={@At(value="TAIL")})
    private void iris$showUpdateMessage(class_2678 a, CallbackInfo ci) {
        if (class_310.method_1551().field_1724 == null) {
            return;
        }
        Iris.getUpdateChecker().getUpdateMessage().ifPresent(msg -> class_310.method_1551().field_1724.method_7353(msg, false));
        Iris.getStoredError().ifPresent(e -> class_310.method_1551().field_1724.method_7353((class_2561)class_2561.method_43471((String)(e instanceof ShaderCompileException ? "iris.load.failure.shader" : "iris.load.failure.generic")).method_10852((class_2561)class_2561.method_43470((String)"Copy Info").method_27694(arg -> arg.method_30938(Boolean.valueOf(true)).method_10977(class_124.field_1078).method_10958(new class_2558(class_2558.class_2559.field_21462, e.getMessage())).method_10949(new class_2568(class_2568.class_5247.field_24342, (Object)class_2561.method_43471((String)"chat.copy.click"))))), false));
        if (Iris.loadedIncompatiblePack()) {
            class_310.method_1551().field_1705.method_34001(10, 70, 140);
            Iris.logger.warn("Incompatible pack for DH!");
            class_310.method_1551().field_1724.method_7353((class_2561)class_2561.method_43470((String)"This pack doesn't have DH support.").method_27695(new class_124[]{class_124.field_1067, class_124.field_1061}), false);
            class_310.method_1551().field_1724.method_7353((class_2561)class_2561.method_43470((String)"Distant Horizons (DH) chunks won't show up. This isn't a bug, get another shader.").method_27692(class_124.field_1061), false);
        }
    }
}

