/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_2561
 *  net.minecraft.class_437
 *  net.minecraft.class_446
 *  net.minecraft.class_7172
 *  net.minecraft.class_7172$class_7178
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 */
package net.irisshaders.iris.mixin.gui;

import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.class_2561;
import net.minecraft.class_437;
import net.minecraft.class_446;
import net.minecraft.class_7172;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value={class_446.class})
public abstract class MixinVideoSettingsScreen
extends class_437 {
    protected MixinVideoSettingsScreen(class_2561 title) {
        super(title);
    }

    @ModifyArg(method={"addOptions"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/gui/components/OptionsList;addSmall([Lnet/minecraft/client/OptionInstance;)V"), index=0)
    private class_7172<?>[] iris$addShaderPackScreenButton(class_7172<?>[] $$0) {
        class_7172[] options = new class_7172[$$0.length + 2];
        System.arraycopy($$0, 0, options, 0, $$0.length);
        options[options.length - 2] = new class_7172("options.iris.shaderPackSelection", class_7172.method_42717((class_2561)class_2561.method_43473()), (arg, object) -> class_2561.method_43473(), (class_7172.class_7178)class_7172.field_38278, (Object)true, parent -> this.field_22787.method_1507((class_437)new ShaderPackScreen(this)));
        options[options.length - 1] = IrisVideoSettings.RENDER_DISTANCE;
        return options;
    }
}

