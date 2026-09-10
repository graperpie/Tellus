/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI
 *  net.caffeinemc.mods.sodium.client.gui.options.OptionPage
 *  net.minecraft.class_2561
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.compat.sodium.mixin;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.class_2561;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={SodiumOptionsGUI.class})
public class MixinSodiumOptionsGUI
extends class_437 {
    @Shadow(remap=false)
    @Final
    private List<OptionPage> pages;
    @Unique
    private OptionPage shaderPacks;

    protected MixinSodiumOptionsGUI(class_2561 title) {
        super(title);
    }

    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    private void iris$onInit(class_437 prevScreen, CallbackInfo ci) {
        class_5250 shaderPacksTranslated = class_2561.method_43471((String)"options.iris.shaderPackSelection");
        this.shaderPacks = new OptionPage((class_2561)shaderPacksTranslated, ImmutableList.of());
        this.pages.add(this.shaderPacks);
    }

    @Inject(method={"setPage"}, at={@At(value="HEAD")}, remap=false, cancellable=true)
    private void iris$onSetPage(OptionPage page, CallbackInfo ci) {
        if (page == this.shaderPacks) {
            this.field_22787.method_1507((class_437)new ShaderPackScreen(this));
            ci.cancel();
        }
    }
}

