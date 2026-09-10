/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1078
 *  net.minecraft.class_2477
 *  net.minecraft.class_3298
 *  net.minecraft.class_3300
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package net.irisshaders.iris.mixin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.shaderpack.LanguageMap;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.minecraft.class_1078;
import net.minecraft.class_2477;
import net.minecraft.class_3298;
import net.minecraft.class_3300;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_1078.class}, priority=990)
public class MixinClientLanguage {
    @Unique
    private static final String LOAD = "Lnet/minecraft/client/resources/language/ClientLanguage;loadFrom(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/List;)Lnet/minecraft/client/resources/language/ClientLanguage;";
    @Unique
    private static final List<String> languageCodes = new ArrayList<String>();
    @Shadow
    @Final
    private Map<String, String> field_5330;

    @Inject(method={"appendFrom"}, at={@At(value="HEAD")})
    private static void injectFrom(String string, List<class_3298> list, Map<String, String> map, CallbackInfo ci) {
        String json = String.format(Locale.ROOT, "lang/%s.json", string);
        if (Iris.class.getResource("/assets/iris/" + json) != null) {
            class_2477.method_29425((InputStream)Iris.class.getResourceAsStream("/assets/iris/" + json), map::put);
        }
    }

    @Inject(method={"loadFrom"}, at={@At(value="HEAD")})
    private static void check(class_3300 resourceManager, List<String> definitions, boolean bl, CallbackInfoReturnable<class_1078> cir) {
        languageCodes.clear();
        new LinkedList<String>(definitions).descendingIterator().forEachRemaining(languageCodes::add);
    }

    @Inject(method={"getOrDefault"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$addLanguageEntries(String key, String value, CallbackInfoReturnable<String> cir) {
        String override = this.iris$lookupOverriddenEntry(key);
        if (override != null) {
            cir.setReturnValue((Object)override);
        }
    }

    @Inject(method={"has"}, at={@At(value="HEAD")}, cancellable=true)
    private void iris$addLanguageEntriesToTranslationChecks(String key, CallbackInfoReturnable<Boolean> cir) {
        String override = this.iris$lookupOverriddenEntry(key);
        if (override != null) {
            cir.setReturnValue((Object)true);
        }
    }

    @Unique
    private String iris$lookupOverriddenEntry(String key) {
        ShaderPack pack = Iris.getCurrentPack().orElse(null);
        if (pack == null) {
            return null;
        }
        LanguageMap languageMap = pack.getLanguageMap();
        if (this.field_5330.containsKey(key)) {
            return null;
        }
        for (String code : languageCodes) {
            String translation;
            Map<String, String> translations = languageMap.getTranslations(code);
            if (translations == null || (translation = translations.get(key)) == null) continue;
            return translation;
        }
        return null;
    }
}

