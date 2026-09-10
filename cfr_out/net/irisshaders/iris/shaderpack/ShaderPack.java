/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.google.gson.Gson
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.stream.JsonReader
 *  it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_5250
 *  org.apache.commons.lang3.SystemUtils
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.shaderpack;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.features.FeatureFlags;
import net.irisshaders.iris.gl.buffer.BuiltShaderStorageInfo;
import net.irisshaders.iris.gl.buffer.ShaderStorageInfo;
import net.irisshaders.iris.gl.texture.TextureDefinition;
import net.irisshaders.iris.gl.texture.TextureType;
import net.irisshaders.iris.gui.FeatureMissingErrorScreen;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.pathways.colorspace.ColorSpace;
import net.irisshaders.iris.shaderpack.DimensionId;
import net.irisshaders.iris.shaderpack.IdMap;
import net.irisshaders.iris.shaderpack.ImageInformation;
import net.irisshaders.iris.shaderpack.IrisDefines;
import net.irisshaders.iris.shaderpack.LanguageMap;
import net.irisshaders.iris.shaderpack.error.RusticError;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.include.IncludeGraph;
import net.irisshaders.iris.shaderpack.include.IncludeProcessor;
import net.irisshaders.iris.shaderpack.include.ShaderPackSourceNames;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.option.OrderBackedProperties;
import net.irisshaders.iris.shaderpack.option.ProfileSet;
import net.irisshaders.iris.shaderpack.option.ShaderPackOptions;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuContainer;
import net.irisshaders.iris.shaderpack.option.values.MutableOptionValues;
import net.irisshaders.iris.shaderpack.parsing.BooleanParser;
import net.irisshaders.iris.shaderpack.preprocessor.JcppProcessor;
import net.irisshaders.iris.shaderpack.preprocessor.PropertiesPreprocessor;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.programs.ProgramSetInterface;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import net.irisshaders.iris.shaderpack.texture.CustomTextureData;
import net.irisshaders.iris.shaderpack.texture.TextureFilteringData;
import net.irisshaders.iris.shaderpack.texture.TextureStage;
import net.irisshaders.iris.uniforms.custom.CustomUniforms;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_5250;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;

public class ShaderPack {
    private static final Gson GSON = new Gson();
    public final CustomUniforms.Builder customUniforms;
    private final ProgramSet base;
    private final Map<NamespacedId, ProgramSetInterface> overrides;
    private final IdMap idMap;
    private final LanguageMap languageMap;
    private final EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> customTextureDataMap = new EnumMap(TextureStage.class);
    private final Object2ObjectMap<String, CustomTextureData> irisCustomTextureDataMap = new Object2ObjectOpenHashMap();
    private final CustomTextureData customNoiseTexture;
    private final ShaderPackOptions shaderPackOptions;
    private final OptionMenuContainer menuContainer;
    private final ProfileSet.ProfileResult profile;
    private final String profileInfo;
    private final List<ImageInformation> irisCustomImages;
    private final Set<FeatureFlags> activeFeatures;
    private final Function<AbsolutePackPath, String> sourceProvider;
    private final ShaderProperties shaderProperties;
    private final List<String> dimensionIds;
    private final Int2ObjectArrayMap<BuiltShaderStorageInfo> bufferObjects;
    private Map<NamespacedId, String> dimensionMap;

    public ShaderPack(Path root, ImmutableList<StringPair> environmentDefines, boolean isZip) throws IOException, IllegalStateException {
        this(root, Collections.emptyMap(), environmentDefines, isZip);
    }

    public ShaderPack(Path root, Map<String, String> changedConfigs, ImmutableList<StringPair> environmentDefines, boolean isZip) throws IOException, IllegalStateException {
        List<String> optionalFeatureFlags;
        int i;
        Objects.requireNonNull(root);
        ArrayList<StringPair> envDefines1 = new ArrayList<StringPair>((Collection<StringPair>)environmentDefines);
        envDefines1.addAll((Collection<StringPair>)IrisDefines.createIrisReplacements());
        environmentDefines = ImmutableList.copyOf(envDefines1);
        ImmutableList.Builder starts = ImmutableList.builder();
        ImmutableList<String> potentialFileNames = ShaderPackSourceNames.POTENTIAL_STARTS;
        ShaderPackSourceNames.findPresentSources((ImmutableList.Builder<AbsolutePackPath>)starts, root, AbsolutePackPath.fromAbsolutePath("/"), potentialFileNames);
        this.dimensionIds = new ArrayList<String>();
        this.bufferObjects = new Int2ObjectArrayMap();
        boolean[] hasDimensionIds = new boolean[]{false};
        List dimensionIdCreator = ShaderPack.loadProperties(root, "dimension.properties", (Iterable<StringPair>)environmentDefines).map(dimensionProperties -> {
            hasDimensionIds[0] = !dimensionProperties.isEmpty();
            this.dimensionMap = ShaderPack.parseDimensionMap(dimensionProperties, "dimension.", "dimension.properties");
            return this.parseDimensionIds((Properties)dimensionProperties, "dimension.");
        }).orElse(new ArrayList());
        if (!hasDimensionIds[0]) {
            this.dimensionMap = new Object2ObjectArrayMap();
            if (Files.exists(root.resolve("world0"), new LinkOption[0])) {
                dimensionIdCreator.add("world0");
                this.dimensionMap.putIfAbsent(DimensionId.OVERWORLD, "world0");
                this.dimensionMap.putIfAbsent(new NamespacedId("*", "*"), "world0");
            }
            if (Files.exists(root.resolve("world-1"), new LinkOption[0])) {
                dimensionIdCreator.add("world-1");
                this.dimensionMap.putIfAbsent(DimensionId.NETHER, "world-1");
            }
            if (Files.exists(root.resolve("world1"), new LinkOption[0])) {
                dimensionIdCreator.add("world1");
                this.dimensionMap.putIfAbsent(DimensionId.END, "world1");
            }
        }
        for (String id : dimensionIdCreator) {
            if (!ShaderPackSourceNames.findPresentSources((ImmutableList.Builder<AbsolutePackPath>)starts, root, AbsolutePackPath.fromAbsolutePath("/" + id), potentialFileNames)) continue;
            this.dimensionIds.add(id);
        }
        IncludeGraph graph = new IncludeGraph(root, (ImmutableList<AbsolutePackPath>)starts.build(), isZip);
        if (!graph.getFailures().isEmpty()) {
            throw new IOException(String.join((CharSequence)"\n", (CharSequence[])graph.getFailures().values().stream().map(RusticError::toString).toArray(String[]::new)));
        }
        this.languageMap = new LanguageMap(root.resolve("lang"));
        this.shaderPackOptions = new ShaderPackOptions(graph, changedConfigs);
        graph = this.shaderPackOptions.getIncludes();
        ArrayList<StringPair> finalEnvironmentDefines = new ArrayList<StringPair>(List.copyOf(environmentDefines));
        for (FeatureFlags flag2 : FeatureFlags.values()) {
            if (!flag2.isUsable()) continue;
            finalEnvironmentDefines.add(new StringPair("IRIS_FEATURE_" + flag2.name(), ""));
        }
        this.shaderProperties = ShaderPack.loadProperties(root, "shaders.properties").map(source -> new ShaderProperties((String)source, this.shaderPackOptions, (Iterable<StringPair>)finalEnvironmentDefines)).orElseGet(ShaderProperties::empty);
        for (Int2ObjectMap.Entry shaderStorageInfoEntry : this.shaderProperties.getBufferObjects().int2ObjectEntrySet()) {
            ShaderStorageInfo info = (ShaderStorageInfo)shaderStorageInfoEntry.getValue();
            if (info.name() == null) {
                this.bufferObjects.put(shaderStorageInfoEntry.getIntKey(), (Object)new BuiltShaderStorageInfo(info.size(), info.relative(), info.scaleX(), info.scaleY(), null));
                continue;
            }
            ColorSpace[] path2 = info.name();
            try {
                byte[] data;
                if (path2.startsWith("/")) {
                    path2 = path2.substring(1);
                }
                if ((long)(data = Files.readAllBytes(root.resolve((String)path2))).length > info.size()) {
                    throw new IllegalStateException("Tried to load a shader storage file with no space in the buffer! Increase the buffer size.");
                }
                this.bufferObjects.put(shaderStorageInfoEntry.getIntKey(), (Object)new BuiltShaderStorageInfo(info.size(), info.relative(), info.scaleX(), info.scaleY(), data));
            }
            catch (IOException e) {
                Iris.logger.error("Shader storage buffer with index " + shaderStorageInfoEntry.getIntKey() + " and path " + (String)path2 + " could not be read.", e);
            }
        }
        this.activeFeatures = new HashSet<FeatureFlags>();
        for (i = 0; i < this.shaderProperties.getRequiredFeatureFlags().size(); ++i) {
            this.activeFeatures.add(FeatureFlags.getValue(this.shaderProperties.getRequiredFeatureFlags().get(i)));
        }
        for (i = 0; i < this.shaderProperties.getOptionalFeatureFlags().size(); ++i) {
            this.activeFeatures.add(FeatureFlags.getValue(this.shaderProperties.getOptionalFeatureFlags().get(i)));
        }
        if (!this.activeFeatures.contains((Object)FeatureFlags.SSBO) && !this.shaderProperties.getBufferObjects().isEmpty()) {
            throw new IllegalStateException("An SSBO is being used, but the feature flag for SSBO's hasn't been set! Please set either a requirement or check for the SSBO feature using \"iris.features.required/optional = ssbo\".");
        }
        if (!this.activeFeatures.contains((Object)FeatureFlags.CUSTOM_IMAGES) && !this.shaderProperties.getIrisCustomImages().isEmpty()) {
            throw new IllegalStateException("Custom images are being used, but the feature flag for custom images hasn't been set! Please set either a requirement or check for custom images' feature flag using \"iris.features.required/optional = CUSTOM_IMAGES\".");
        }
        List<FeatureFlags> invalidFlagList = this.shaderProperties.getRequiredFeatureFlags().stream().filter(FeatureFlags::isInvalid).map(FeatureFlags::getValue).collect(Collectors.toList());
        List<String> invalidFeatureFlags = invalidFlagList.stream().map(FeatureFlags::getHumanReadableName).toList();
        if (!invalidFeatureFlags.isEmpty()) {
            if (class_310.method_1551().field_1755 instanceof ShaderPackScreen) {
                class_5250 component = class_2561.method_43469((String)"iris.unsupported.pack.description", (Object[])new Object[]{FeatureFlags.getInvalidStatus(invalidFlagList), invalidFeatureFlags.stream().collect(Collectors.joining(", ", ": ", "."))});
                if (SystemUtils.IS_OS_MAC) {
                    component = component.method_10852((class_2561)class_2561.method_43471((String)"iris.unsupported.pack.macos"));
                }
                class_310.method_1551().method_1507((class_437)new FeatureMissingErrorScreen(class_310.method_1551().field_1755, (class_2561)class_2561.method_43471((String)"iris.unsupported.pack"), (class_2561)component));
            }
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(false);
        }
        ArrayList<StringPair> newEnvDefines = new ArrayList<StringPair>((Collection<StringPair>)environmentDefines);
        if (this.shaderProperties.supportsColorCorrection().orElse(false)) {
            for (ColorSpace space : ColorSpace.values()) {
                newEnvDefines.add(new StringPair("COLOR_SPACE_" + space.name(), String.valueOf(space.ordinal())));
            }
        }
        if (!(optionalFeatureFlags = this.shaderProperties.getOptionalFeatureFlags().stream().filter(flag -> !FeatureFlags.isInvalid(flag)).toList()).isEmpty()) {
            optionalFeatureFlags.forEach(flag -> newEnvDefines.add(new StringPair("IRIS_FEATURE_" + flag, "")));
        }
        environmentDefines = ImmutableList.copyOf(newEnvDefines);
        ProfileSet profiles = ProfileSet.fromTree(this.shaderProperties.getProfiles(), this.shaderPackOptions.getOptionSet());
        this.profile = profiles.scan(this.shaderPackOptions.getOptionSet(), this.shaderPackOptions.getOptionValues());
        ArrayList disabledPrograms = new ArrayList();
        this.profile.current.ifPresent(profile -> disabledPrograms.addAll(profile.disabledPrograms));
        this.shaderProperties.getConditionallyEnabledPrograms().forEach((program, shaderOption) -> {
            if (!BooleanParser.parse(shaderOption, this.shaderPackOptions.getOptionValues())) {
                disabledPrograms.add(program);
            }
        });
        this.menuContainer = new OptionMenuContainer(this.shaderProperties, this.shaderPackOptions, profiles);
        String profileName = this.getCurrentProfileName();
        MutableOptionValues profileOptions = new MutableOptionValues(this.shaderPackOptions.getOptionSet(), this.profile.current.map(p -> p.optionValues).orElse(new HashMap()));
        int userOptionsChanged = this.shaderPackOptions.getOptionValues().getOptionsChanged() - profileOptions.getOptionsChanged();
        this.profileInfo = "Profile: " + profileName + " (+" + userOptionsChanged + " option" + (userOptionsChanged == 1 ? "" : "s") + " changed by user)";
        Iris.logger.info(this.profileInfo);
        IncludeProcessor includeProcessor = new IncludeProcessor(graph);
        ImmutableList finalEnvironmentDefines1 = environmentDefines;
        this.sourceProvider = arg_0 -> ShaderPack.lambda$new$8(disabledPrograms, includeProcessor, (Iterable)finalEnvironmentDefines1, arg_0);
        this.base = new ProgramSet(AbsolutePackPath.fromAbsolutePath("/" + this.dimensionMap.getOrDefault(new NamespacedId("*", "*"), "")), this.sourceProvider, this.shaderProperties, this);
        this.overrides = new HashMap<NamespacedId, ProgramSetInterface>();
        this.idMap = new IdMap(root, this.shaderPackOptions, (Iterable<StringPair>)environmentDefines);
        this.customNoiseTexture = this.shaderProperties.getNoiseTexturePath().map(path -> {
            try {
                return this.readTexture(root, new TextureDefinition.PNGDefinition((String)path));
            }
            catch (IOException e) {
                Iris.logger.error("Unable to read the custom noise texture at " + path, e);
                return null;
            }
        }).orElse(null);
        this.shaderProperties.getCustomTextures().forEach((textureStage, customTexturePropertiesMap) -> {
            Object2ObjectOpenHashMap innerCustomTextureDataMap = new Object2ObjectOpenHashMap();
            customTexturePropertiesMap.forEach((arg_0, arg_1) -> this.lambda$new$10((Object2ObjectMap)innerCustomTextureDataMap, root, arg_0, arg_1));
            this.customTextureDataMap.put((TextureStage)((Object)textureStage), (Object2ObjectMap<String, CustomTextureData>)innerCustomTextureDataMap);
        });
        this.irisCustomImages = this.shaderProperties.getIrisCustomImages();
        this.customUniforms = this.shaderProperties.getCustomUniforms();
        this.shaderProperties.getIrisCustomTextures().forEach((name, texture) -> {
            try {
                this.irisCustomTextureDataMap.put(name, (Object)this.readTexture(root, (TextureDefinition)texture));
            }
            catch (IOException e) {
                Iris.logger.error("Unable to read the custom texture at " + texture.getName(), e);
            }
        });
    }

    private static Optional<Properties> loadProperties(Path shaderPath, String name, Iterable<StringPair> environmentDefines) {
        String fileContents = ShaderPack.readProperties(shaderPath, name);
        if (fileContents == null) {
            return Optional.empty();
        }
        String processed = PropertiesPreprocessor.preprocessSource(fileContents, environmentDefines);
        StringReader propertiesReader = new StringReader(processed);
        OrderBackedProperties properties = new OrderBackedProperties();
        try {
            properties.load(propertiesReader);
        }
        catch (IOException e) {
            Iris.logger.error("Error loading " + name + " at " + String.valueOf(shaderPath), e);
            return Optional.empty();
        }
        return Optional.of(properties);
    }

    private static Map<NamespacedId, String> parseDimensionMap(Properties properties, String keyPrefix, String fileName) {
        Object2ObjectArrayMap overrides = new Object2ObjectArrayMap();
        properties.forEach((BiConsumer<? super Object, ? super Object>)((BiConsumer<Object, Object>)(arg_0, arg_1) -> ShaderPack.lambda$parseDimensionMap$13(keyPrefix, (Map)overrides, arg_0, arg_1)));
        return overrides;
    }

    @Nullable
    private static ProgramSet loadOverrides(boolean has, AbsolutePackPath path, Function<AbsolutePackPath, String> sourceProvider, ShaderProperties shaderProperties, ShaderPack pack) {
        if (has) {
            return new ProgramSet(path, sourceProvider, shaderProperties, pack);
        }
        return null;
    }

    private static Optional<String> loadProperties(Path shaderPath, String name) {
        String fileContents = ShaderPack.readProperties(shaderPath, name);
        if (fileContents == null) {
            return Optional.empty();
        }
        return Optional.of(fileContents);
    }

    private static String readProperties(Path shaderPath, String name) {
        try {
            return Files.readString(shaderPath.resolve(name), StandardCharsets.ISO_8859_1);
        }
        catch (NoSuchFileException e) {
            Iris.logger.debug("An " + name + " file was not found in the current shaderpack");
            return null;
        }
        catch (IOException e) {
            Iris.logger.error("An IOException occurred reading " + name + " from the current shaderpack", e);
            return null;
        }
    }

    private List<String> parseDimensionIds(Properties dimensionProperties, String keyPrefix) {
        ArrayList<String> names = new ArrayList<String>();
        dimensionProperties.forEach((BiConsumer<? super Object, ? super Object>)((BiConsumer<Object, Object>)(keyObject, value) -> {
            String key = (String)keyObject;
            if (!key.startsWith(keyPrefix)) {
                return;
            }
            key = key.substring(keyPrefix.length());
            names.add(key);
        }));
        return names;
    }

    private String getCurrentProfileName() {
        return this.profile.current.map(p -> p.name).orElse("Custom");
    }

    public String getProfileInfo() {
        return this.profileInfo;
    }

    public CustomTextureData readTexture(Path root, TextureDefinition definition) throws IOException {
        CustomTextureData customTextureData;
        String path = definition.getName();
        if (path.contains(":")) {
            String[] parts = path.split(":");
            if (parts.length > 2) {
                Iris.logger.warn("Resource location " + path + " contained more than two parts?");
            }
            customTextureData = parts[0].equals("minecraft") && (parts[1].equals("dynamic/lightmap_1") || parts[1].equals("dynamic/light_map_1")) ? new CustomTextureData.LightmapMarker() : new CustomTextureData.ResourceData(parts[0], parts[1]);
        } else {
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            boolean blur = definition instanceof TextureDefinition.RawDefinition;
            boolean clamp = definition instanceof TextureDefinition.RawDefinition;
            String mcMetaPath = path + ".mcmeta";
            Path mcMetaResolvedPath = root.resolve(mcMetaPath);
            if (Files.exists(mcMetaResolvedPath, new LinkOption[0])) {
                try {
                    JsonObject meta = this.loadMcMeta(mcMetaResolvedPath);
                    if (meta.get("texture") != null) {
                        if (meta.get("texture").getAsJsonObject().get("blur") != null) {
                            blur = meta.get("texture").getAsJsonObject().get("blur").getAsBoolean();
                        }
                        if (meta.get("texture").getAsJsonObject().get("clamp") != null) {
                            clamp = meta.get("texture").getAsJsonObject().get("clamp").getAsBoolean();
                        }
                    }
                }
                catch (IOException e) {
                    Iris.logger.error("Unable to read the custom texture mcmeta at " + mcMetaPath + ", ignoring: " + String.valueOf(e));
                }
            }
            byte[] content = Files.readAllBytes(root.resolve(path));
            if (definition instanceof TextureDefinition.PNGDefinition) {
                customTextureData = new CustomTextureData.PngData(new TextureFilteringData(blur, clamp), content);
            } else if (definition instanceof TextureDefinition.RawDefinition) {
                TextureDefinition.RawDefinition rawDefinition = (TextureDefinition.RawDefinition)definition;
                customTextureData = switch (rawDefinition.getTarget()) {
                    default -> throw new MatchException(null, null);
                    case TextureType.TEXTURE_1D -> new CustomTextureData.RawData1D(content, new TextureFilteringData(blur, clamp), rawDefinition.getInternalFormat(), rawDefinition.getFormat(), rawDefinition.getPixelType(), rawDefinition.getSizeX());
                    case TextureType.TEXTURE_2D -> new CustomTextureData.RawData2D(content, new TextureFilteringData(blur, clamp), rawDefinition.getInternalFormat(), rawDefinition.getFormat(), rawDefinition.getPixelType(), rawDefinition.getSizeX(), rawDefinition.getSizeY());
                    case TextureType.TEXTURE_3D -> new CustomTextureData.RawData3D(content, new TextureFilteringData(blur, clamp), rawDefinition.getInternalFormat(), rawDefinition.getFormat(), rawDefinition.getPixelType(), rawDefinition.getSizeX(), rawDefinition.getSizeY(), rawDefinition.getSizeZ());
                    case TextureType.TEXTURE_RECTANGLE -> new CustomTextureData.RawDataRect(content, new TextureFilteringData(blur, clamp), rawDefinition.getInternalFormat(), rawDefinition.getFormat(), rawDefinition.getPixelType(), rawDefinition.getSizeX(), rawDefinition.getSizeY());
                };
            } else {
                customTextureData = null;
            }
        }
        return customTextureData;
    }

    private JsonObject loadMcMeta(Path mcMetaPath) throws IOException, JsonParseException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(mcMetaPath, new OpenOption[0]), StandardCharsets.UTF_8));){
            JsonReader jsonReader = new JsonReader((Reader)reader);
            JsonObject jsonObject = (JsonObject)GSON.getAdapter(JsonObject.class).read(jsonReader);
            return jsonObject;
        }
    }

    public ProgramSet getProgramSet(NamespacedId dimension) {
        ProgramSetInterface overrides = this.overrides.computeIfAbsent(dimension, dim -> {
            if (this.dimensionMap.containsKey(dimension)) {
                String name = this.dimensionMap.get(dimension);
                if (this.dimensionIds.contains(name)) {
                    return new ProgramSet(AbsolutePackPath.fromAbsolutePath("/" + name), this.sourceProvider, this.shaderProperties, this);
                }
                Iris.logger.error("Attempted to load dimension folder " + name + " for dimension " + String.valueOf(dimension) + ", but it does not exist!");
                return ProgramSetInterface.Empty.INSTANCE;
            }
            return ProgramSetInterface.Empty.INSTANCE;
        });
        if (overrides instanceof ProgramSet) {
            return (ProgramSet)overrides;
        }
        return this.base;
    }

    public IdMap getIdMap() {
        return this.idMap;
    }

    public EnumMap<TextureStage, Object2ObjectMap<String, CustomTextureData>> getCustomTextureDataMap() {
        return this.customTextureDataMap;
    }

    public List<ImageInformation> getIrisCustomImages() {
        return this.irisCustomImages;
    }

    public Object2ObjectMap<String, CustomTextureData> getIrisCustomTextureDataMap() {
        return this.irisCustomTextureDataMap;
    }

    public CustomTextureData getCustomNoiseTexture() {
        return this.customNoiseTexture;
    }

    public LanguageMap getLanguageMap() {
        return this.languageMap;
    }

    public ShaderPackOptions getShaderPackOptions() {
        return this.shaderPackOptions;
    }

    public OptionMenuContainer getMenuContainer() {
        return this.menuContainer;
    }

    public boolean hasFeature(FeatureFlags feature) {
        return this.activeFeatures.contains((Object)feature);
    }

    public Int2ObjectArrayMap<BuiltShaderStorageInfo> getBufferObjects() {
        return this.bufferObjects;
    }

    private static /* synthetic */ void lambda$parseDimensionMap$13(String keyPrefix, Map overrides, Object keyObject, Object valueObject) {
        String key = (String)keyObject;
        String value = (String)valueObject;
        if (!key.startsWith(keyPrefix)) {
            return;
        }
        key = key.substring(keyPrefix.length());
        for (String part : value.split("\\s+")) {
            if (part.equals("*")) {
                overrides.put(new NamespacedId("*", "*"), key);
            }
            overrides.put(new NamespacedId(part), key);
        }
    }

    private /* synthetic */ void lambda$new$10(Object2ObjectMap innerCustomTextureDataMap, Path root, String samplerName, TextureDefinition path) {
        try {
            innerCustomTextureDataMap.put((Object)samplerName, (Object)this.readTexture(root, path));
        }
        catch (IOException e) {
            Iris.logger.error("Unable to read the custom texture at " + String.valueOf(path), e);
        }
    }

    private static /* synthetic */ String lambda$new$8(List disabledPrograms, IncludeProcessor includeProcessor, Iterable finalEnvironmentDefines1, AbsolutePackPath path) {
        String pathString;
        String programString = pathString.substring((pathString = path.getPathString()).indexOf("/") == 0 ? 1 : 0, pathString.lastIndexOf("."));
        if (disabledPrograms.contains(programString)) {
            return null;
        }
        ImmutableList<String> lines = includeProcessor.getIncludedFile(path);
        if (lines == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line);
            builder.append('\n');
        }
        String source = builder.toString();
        source = JcppProcessor.glslPreprocessSource(source, finalEnvironmentDefines1);
        return source;
    }
}

