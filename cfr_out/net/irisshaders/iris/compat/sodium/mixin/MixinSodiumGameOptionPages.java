/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.caffeinemc.mods.sodium.client.gui.SodiumGameOptionPages
 *  net.caffeinemc.mods.sodium.client.gui.options.Option
 *  net.caffeinemc.mods.sodium.client.gui.options.OptionFlag
 *  net.caffeinemc.mods.sodium.client.gui.options.OptionGroup$Builder
 *  net.caffeinemc.mods.sodium.client.gui.options.OptionImpact
 *  net.caffeinemc.mods.sodium.client.gui.options.OptionImpl
 *  net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter
 *  net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl
 *  net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl
 *  net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage
 *  net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage
 *  net.minecraft.class_2561
 *  net.minecraft.class_315
 *  net.minecraft.class_5365
 *  net.minecraft.class_7172
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.Slice
 */
package net.irisshaders.iris.compat.sodium.mixin;

import java.io.IOException;
import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptionPages;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.OptionFlag;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.gui.options.control.CyclingControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.caffeinemc.mods.sodium.client.gui.options.storage.OptionStorage;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.fantastic.SupportedGraphicsMode;
import net.irisshaders.iris.gui.option.IrisVideoSettings;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.minecraft.class_2561;
import net.minecraft.class_315;
import net.minecraft.class_5365;
import net.minecraft.class_7172;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value={SodiumGameOptionPages.class})
public class MixinSodiumGameOptionPages {
    @Shadow(remap=false)
    @Final
    private static MinecraftOptionsStorage vanillaOpts;

    @Redirect(method={"general"}, remap=false, slice=@Slice(from=@At(value="CONSTANT", args={"stringValue=options.renderDistance"}), to=@At(value="CONSTANT", args={"stringValue=options.simulationDistance"})), at=@At(value="INVOKE", remap=false, target="net/caffeinemc/mods/sodium/client/gui/options/OptionGroup$Builder.add (Lnet/caffeinemc/mods/sodium/client/gui/options/Option;)Lnet/caffeinemc/mods/sodium/client/gui/options/OptionGroup$Builder;"), allow=1)
    private static OptionGroup.Builder iris$addMaxShadowDistanceOption(OptionGroup.Builder builder, Option<?> candidate) {
        builder.add(candidate);
        builder.add(MixinSodiumGameOptionPages.createMaxShadowDistanceSlider(vanillaOpts));
        return builder;
    }

    @Redirect(method={"quality"}, remap=false, slice=@Slice(from=@At(value="CONSTANT", args={"stringValue=options.graphics"}), to=@At(value="CONSTANT", args={"stringValue=options.renderClouds"})), at=@At(value="INVOKE", remap=false, target="net/caffeinemc/mods/sodium/client/gui/options/OptionGroup$Builder.add (Lnet/caffeinemc/mods/sodium/client/gui/options/Option;)Lnet/caffeinemc/mods/sodium/client/gui/options/OptionGroup$Builder;"), allow=1)
    private static OptionGroup.Builder iris$addColorSpaceOption(OptionGroup.Builder builder, Option<?> candidate) {
        builder.add(candidate);
        builder.add(MixinSodiumGameOptionPages.createColorSpaceButton(vanillaOpts));
        return builder;
    }

    @ModifyArg(method={"quality"}, remap=false, slice=@Slice(from=@At(value="CONSTANT", args={"stringValue=options.graphics"}), to=@At(value="CONSTANT", args={"stringValue=options.renderClouds"})), at=@At(value="INVOKE", remap=false, target="net/caffeinemc/mods/sodium/client/gui/options/OptionGroup$Builder.add (Lnet/caffeinemc/mods/sodium/client/gui/options/Option;)Lnet/caffeinemc/mods/sodium/client/gui/options/OptionGroup$Builder;"), allow=1)
    private static Option<?> iris$replaceGraphicsQualityButton(Option<?> candidate) {
        if (!Iris.getIrisConfig().areShadersEnabled()) {
            return candidate;
        }
        return MixinSodiumGameOptionPages.createLimitedVideoSettingsButton(vanillaOpts);
    }

    @Unique
    private static OptionImpl<class_315, Integer> createMaxShadowDistanceSlider(MinecraftOptionsStorage vanillaOpts) {
        return OptionImpl.createBuilder(Integer.TYPE, (OptionStorage)vanillaOpts).setName((class_2561)class_2561.method_43471((String)"options.iris.shadowDistance")).setTooltip((class_2561)class_2561.method_43471((String)"options.iris.shadowDistance.sodium_tooltip")).setControl(option -> new SliderControl((Option)option, 0, 32, 1, MixinSodiumGameOptionPages.translateVariableOrDisabled("options.chunks", "Disabled"))).setBinding((options, value) -> {
            IrisVideoSettings.shadowDistance = value;
            try {
                Iris.getIrisConfig().save();
            }
            catch (IOException e) {
                Iris.logger.error("Failed to save Iris config!", e);
            }
        }, options -> IrisVideoSettings.getOverriddenShadowDistance(IrisVideoSettings.shadowDistance)).setImpact(OptionImpact.HIGH).setEnabled(IrisVideoSettings::isShadowDistanceSliderEnabled).build();
    }

    @Unique
    private static OptionImpl<class_315, ColorSpace> createColorSpaceButton(MinecraftOptionsStorage vanillaOpts) {
        return OptionImpl.createBuilder(ColorSpace.class, (OptionStorage)vanillaOpts).setName((class_2561)class_2561.method_43471((String)"options.iris.colorSpace")).setTooltip((class_2561)class_2561.method_43471((String)"options.iris.colorSpace.sodium_tooltip")).setControl(option -> new CyclingControl((Option)option, ColorSpace.class, new class_2561[]{class_2561.method_43470((String)"sRGB"), class_2561.method_43470((String)"DCI_P3"), class_2561.method_43470((String)"Display P3"), class_2561.method_43470((String)"REC2020"), class_2561.method_43470((String)"Adobe RGB")})).setBinding((options, value) -> {
            IrisVideoSettings.colorSpace = value;
            try {
                Iris.getIrisConfig().save();
            }
            catch (IOException e) {
                Iris.logger.error("Failed to save Iris config!", e);
            }
        }, options -> IrisVideoSettings.colorSpace).setImpact(OptionImpact.LOW).setEnabled(() -> true).build();
    }

    @Unique
    private static ControlValueFormatter translateVariableOrDisabled(String key, String disabled) {
        return v -> v == 0 ? class_2561.method_43470((String)disabled) : class_2561.method_43469((String)key, (Object[])new Object[]{v});
    }

    @Unique
    private static OptionImpl<class_315, SupportedGraphicsMode> createLimitedVideoSettingsButton(MinecraftOptionsStorage vanillaOpts) {
        return OptionImpl.createBuilder(SupportedGraphicsMode.class, (OptionStorage)vanillaOpts).setName((class_2561)class_2561.method_43471((String)"options.graphics")).setTooltip((class_2561)class_2561.method_43471((String)"sodium.options.graphics_quality.tooltip")).setControl(option -> new CyclingControl((Option)option, SupportedGraphicsMode.class, new class_2561[]{class_2561.method_43471((String)"options.graphics.fast"), class_2561.method_43471((String)"options.graphics.fancy")})).setBinding((opts, value) -> opts.method_42534().method_41748((Object)value.toVanilla()), opts -> SupportedGraphicsMode.fromVanilla((class_7172<class_5365>)opts.method_42534())).setImpact(OptionImpact.HIGH).setFlags(new OptionFlag[]{OptionFlag.REQUIRES_RENDERER_RELOAD}).build();
    }
}

