/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonParser
 *  com.google.gson.JsonSyntaxException
 *  net.minecraft.class_2558
 *  net.minecraft.class_2558$class_2559
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_5250
 *  org.apache.commons.io.FileUtils
 *  org.apache.commons.io.IOUtils
 *  org.apache.commons.lang3.time.DateUtils
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.IrisConfig;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.minecraft.class_2558;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_5250;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.Nullable;

public class UpdateChecker {
    private final String currentVersion;
    private CompletableFuture<UpdateInfo> info;
    private CompletableFuture<BetaInfo> betaInfo;
    private boolean shouldShowUpdateMessage;
    private boolean shouldShowBetaUpdateMessage;
    private boolean usedIrisInstaller;

    public UpdateChecker(String currentVersion) {
        this.currentVersion = currentVersion;
        if (Objects.equals(System.getProperty("iris.installer", "false"), "true")) {
            this.usedIrisInstaller = true;
        }
    }

    public void checkForUpdates(IrisConfig irisConfig) {
        if (irisConfig.shouldDisableUpdateMessage()) {
            this.shouldShowUpdateMessage = false;
            return;
        }
        this.info = CompletableFuture.supplyAsync(() -> {
            try {
                File updateFile = IrisPlatformHelpers.getInstance().getGameDir().resolve("irisUpdateInfo.json").toFile();
                if (DateUtils.isSameDay((Date)new Date(), (Date)new Date(updateFile.lastModified()))) {
                    UpdateInfo updateInfo;
                    Iris.logger.warn("[Iris Update Check] Cached update file detected, using that!");
                    try {
                        updateInfo = (UpdateInfo)new Gson().fromJson(FileUtils.readFileToString((File)updateFile, (Charset)StandardCharsets.UTF_8), UpdateInfo.class);
                    }
                    catch (JsonSyntaxException | NullPointerException e) {
                        Iris.logger.error("[Iris Update Check] Cached file invalid, will delete!", e);
                        Files.delete(updateFile.toPath());
                        return null;
                    }
                    try {
                        if (IrisPlatformHelpers.INSTANCE.compareVersions(this.currentVersion, updateInfo.semanticVersion) >= 0) return null;
                        this.shouldShowUpdateMessage = true;
                        Iris.logger.warn("[Iris Update Check] New update detected, showing update message!");
                        return updateInfo;
                    }
                    catch (Exception e) {
                        Iris.logger.error("[Iris Update Check] Caught a VersionParsingException while parsing semantic versions!", e);
                    }
                }
                InputStream in = new URL("https://github.com/IrisShaders/Iris-Update-Index/releases/latest/download/updateIndex.json").openStream();
                try {
                    String updateIndex;
                    try {
                        updateIndex = JsonParser.parseReader((Reader)new InputStreamReader(in)).getAsJsonObject().get(StandardMacros.getMcVersion()).getAsString();
                    }
                    catch (NullPointerException e) {
                        Iris.logger.warn("[Iris Update Check] This version doesn't have an update index, skipping.");
                        UpdateInfo updateInfo = null;
                        if (in == null) return updateInfo;
                        in.close();
                        return updateInfo;
                    }
                    String json = IOUtils.toString((URL)new URL(updateIndex), (Charset)StandardCharsets.UTF_8);
                    UpdateInfo updateInfo = (UpdateInfo)new Gson().fromJson(json, UpdateInfo.class);
                    BufferedWriter writer = new BufferedWriter(new FileWriter(updateFile));
                    writer.write(json);
                    writer.close();
                    try {
                        if (IrisPlatformHelpers.INSTANCE.compareVersions(this.currentVersion, updateInfo.semanticVersion) < 0) {
                            this.shouldShowUpdateMessage = true;
                            Iris.logger.info("[Iris Update Check] New update detected, showing update message!");
                            UpdateInfo updateInfo2 = updateInfo;
                            return updateInfo2;
                        }
                        UpdateInfo updateInfo3 = null;
                        return updateInfo3;
                    }
                    catch (Exception e) {
                        Iris.logger.error("[Iris Update Check] Caught a VersionParsingException while parsing semantic versions!", e);
                        return null;
                    }
                }
                finally {
                    if (in != null) {
                        try {
                            in.close();
                        }
                        catch (Throwable throwable) {
                            Throwable throwable2;
                            throwable2.addSuppressed(throwable);
                        }
                    }
                }
            }
            catch (FileNotFoundException e) {
                Iris.logger.warn("[Iris Update Check] Unable to download " + e.getMessage());
                return null;
            }
            catch (IOException e) {
                Iris.logger.warn("[Iris Update Check] Failed to get update info!", e);
            }
            return null;
        });
    }

    private void checkBetaUpdates() {
        this.betaInfo = CompletableFuture.supplyAsync(() -> {
            try (InputStream in = URI.create("https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Files/master/betaTag.json").toURL().openStream();){
                BetaInfo updateInfo = (BetaInfo)new Gson().fromJson((JsonElement)JsonParser.parseReader((Reader)new InputStreamReader(in)).getAsJsonObject(), BetaInfo.class);
                if (0 < updateInfo.betaVersion && "".equalsIgnoreCase(updateInfo.betaTag)) {
                    this.shouldShowUpdateMessage = true;
                    Iris.logger.info("[Iris Beta Update Check] New update detected, showing update message!");
                    BetaInfo betaInfo2 = updateInfo;
                    return betaInfo2;
                }
                BetaInfo betaInfo = null;
                return betaInfo;
            }
            catch (FileNotFoundException e) {
                Iris.logger.warn("[Iris Beta Update Check] Unable to download " + e.getMessage());
                return null;
            }
            catch (IOException e) {
                Iris.logger.warn("[Iris Beta Update Check] Failed to get update info!", e);
            }
            return null;
        });
    }

    @Nullable
    public UpdateInfo getUpdateInfo() {
        if (this.info != null && this.info.isDone()) {
            try {
                return this.info.get();
            }
            catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Nullable
    public Optional<BetaInfo> getBetaInfo() {
        if (this.betaInfo != null && this.betaInfo.isDone()) {
            try {
                return Optional.ofNullable(this.betaInfo.get());
            }
            catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    public Optional<class_2561> getUpdateMessage() {
        if (this.shouldShowUpdateMessage) {
            UpdateInfo info = this.getUpdateInfo();
            if (info == null) {
                return Optional.empty();
            }
            String languageCode = class_310.method_1551().field_1690.field_1883.toLowerCase(Locale.ROOT);
            String originalText = info.updateInfo.containsKey(languageCode) ? info.updateInfo.get(languageCode) : info.updateInfo.get("en_us");
            String[] textParts = originalText.split("\\{link}");
            if (textParts.length > 1) {
                class_5250 component1 = class_2561.method_43470((String)textParts[0]);
                class_5250 component2 = class_2561.method_43470((String)textParts[1]);
                class_5250 link = class_2561.method_43470((String)(this.usedIrisInstaller ? "the Iris Installer" : info.modHost)).method_27694(arg -> arg.method_10958(new class_2558(class_2558.class_2559.field_11749, this.usedIrisInstaller ? info.installer : info.modDownload)).method_30938(Boolean.valueOf(true)));
                return Optional.of(component1.method_10852((class_2561)link).method_10852((class_2561)component2));
            }
            class_5250 link = class_2561.method_43470((String)(this.usedIrisInstaller ? "the Iris Installer" : info.modHost)).method_27694(arg -> arg.method_10958(new class_2558(class_2558.class_2559.field_11749, this.usedIrisInstaller ? info.installer : info.modDownload)).method_30938(Boolean.valueOf(true)));
            return Optional.of(class_2561.method_43470((String)textParts[0]).method_10852((class_2561)link));
        }
        return Optional.empty();
    }

    public Optional<String> getUpdateLink() {
        if (this.shouldShowUpdateMessage) {
            UpdateInfo info = this.getUpdateInfo();
            return Optional.of(this.usedIrisInstaller ? info.installer : info.modDownload);
        }
        return Optional.empty();
    }

    public static class UpdateInfo {
        public String semanticVersion;
        public Map<String, String> updateInfo;
        public String modHost;
        public String modDownload;
        public String installer;
    }

    public static class BetaInfo {
        public String betaTag;
        public int betaVersion;
    }
}

