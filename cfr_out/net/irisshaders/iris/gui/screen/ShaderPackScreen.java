/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_124
 *  net.minecraft.class_156
 *  net.minecraft.class_156$class_158
 *  net.minecraft.class_2561
 *  net.minecraft.class_279
 *  net.minecraft.class_310
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_3675
 *  net.minecraft.class_407
 *  net.minecraft.class_410
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 *  net.minecraft.class_5244
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 *  net.minecraft.class_5481
 *  net.minecraft.class_7919
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.gui.screen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.OldImageButton;
import net.irisshaders.iris.gui.element.ShaderPackOptionList;
import net.irisshaders.iris.gui.element.ShaderPackSelectionList;
import net.irisshaders.iris.gui.element.screen.IrisButton;
import net.irisshaders.iris.gui.element.widget.AbstractElementWidget;
import net.irisshaders.iris.gui.element.widget.CommentedElementWidget;
import net.irisshaders.iris.gui.screen.HudHideable;
import net.irisshaders.iris.mixin.GameRendererAccessor;
import net.irisshaders.iris.platform.IrisPlatformHelpers;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.uniforms.FrameUpdateNotifier;
import net.irisshaders.iris.uniforms.transforms.SmoothedFloat;
import net.minecraft.class_124;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_279;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_3675;
import net.minecraft.class_407;
import net.minecraft.class_410;
import net.minecraft.class_4185;
import net.minecraft.class_437;
import net.minecraft.class_5244;
import net.minecraft.class_5250;
import net.minecraft.class_5348;
import net.minecraft.class_5481;
import net.minecraft.class_7919;
import org.jetbrains.annotations.Nullable;

public class ShaderPackScreen
extends class_437
implements HudHideable {
    public static final Set<Runnable> TOP_LAYER_RENDER_QUEUE = new HashSet<Runnable>();
    private static final class_2561 SELECT_TITLE = class_2561.method_43471((String)"pack.iris.select.title").method_27695(new class_124[]{class_124.field_1080, class_124.field_1056});
    private static final class_2561 CONFIGURE_TITLE = class_2561.method_43471((String)"pack.iris.configure.title").method_27695(new class_124[]{class_124.field_1080, class_124.field_1056});
    private static final int COMMENT_PANEL_WIDTH = 314;
    private static final String development = "Development Environment";
    private final class_437 parent;
    private final class_5250 irisTextComponent;
    private final FrameUpdateNotifier notifier = new FrameUpdateNotifier();
    private ShaderPackSelectionList shaderPackList;
    @Nullable
    private ShaderPackOptionList shaderOptionList = null;
    @Nullable
    private NavigationController navigation = null;
    private class_4185 screenSwitchButton;
    private class_2561 notificationDialog = null;
    private int notificationDialogTimer = 0;
    @Nullable
    private AbstractElementWidget<?> hoveredElement = null;
    private Optional<class_2561> hoveredElementCommentTitle = Optional.empty();
    private List<class_5481> hoveredElementCommentBody = new ArrayList<class_5481>();
    private int hoveredElementCommentTimer = 0;
    private boolean optionMenuOpen = false;
    private boolean dropChanges = false;
    private class_5250 developmentComponent;
    private class_5250 updateComponent;
    private boolean guiHidden = false;
    public final SmoothedFloat blurTransition = new SmoothedFloat(2.0f, 2.0f, () -> {
        if (this.guiHidden) {
            return 0.0f;
        }
        if (this.optionMenuOpen) {
            return 0.1f;
        }
        return this.field_22787.field_1690.method_57703();
    }, this.notifier);
    private float guiButtonHoverTimer = 0.0f;
    private class_4185 openFolderButton;
    private float backgroundInit = 0.0f;
    public final SmoothedFloat listTransition = new SmoothedFloat(1.0f, 1.0f, () -> {
        if (this.guiHidden || this.optionMenuOpen) {
            return 0.0f;
        }
        return this.backgroundInit;
    }, this.notifier);
    public final SmoothedFloat buttonTransition = new SmoothedFloat(1.0f, 1.0f, () -> {
        if (this.guiHidden) {
            return 0.0f;
        }
        return this.backgroundInit;
    }, this.notifier);
    private OldImageButton showHideButton;

    public ShaderPackScreen(class_437 parent) {
        super((class_2561)class_2561.method_43471((String)"options.iris.shaderPackSelection.title"));
        this.parent = parent;
        String irisName = "Iris " + Iris.getVersion();
        if (IrisPlatformHelpers.getInstance().isDevelopmentEnvironment()) {
            this.developmentComponent = class_2561.method_43470((String)development).method_27692(class_124.field_1065);
        }
        this.irisTextComponent = class_2561.method_43470((String)irisName).method_27692(class_124.field_1080);
        if (Iris.getUpdateChecker().getUpdateMessage().isPresent()) {
            this.updateComponent = class_2561.method_43470((String)"New update available!").method_27692(class_124.field_1060).method_27692(class_124.field_1073);
            this.irisTextComponent.method_10852((class_2561)class_2561.method_43470((String)" (outdated)").method_27692(class_124.field_1061));
        }
        this.refreshForChangedPack();
    }

    public void method_25394(class_332 guiGraphics, int mouseX, int mouseY, float delta) {
        this.notifier.onNewFrame();
        this.backgroundInit = 1.0f;
        if (class_437.method_25441() && class_3675.method_15987((long)class_310.method_1551().method_22683().method_4490(), (int)68)) {
            class_310.method_1551().method_1507((class_437)new class_410(option -> {
                Iris.setDebug(option);
                class_310.method_1551().method_1507((class_437)this);
            }, (class_2561)class_2561.method_43470((String)"Shader debug mode toggle"), (class_2561)class_2561.method_43470((String)"Debug mode helps investigate problems and shows shader errors. Would you like to enable it?"), (class_2561)class_2561.method_43470((String)"Yes"), (class_2561)class_2561.method_43470((String)"No")));
        }
        if (class_437.method_25441() && class_3675.method_15987((long)class_310.method_1551().method_22683().method_4490(), (int)71)) {
            class_310.method_1551().method_1507((class_437)new class_410(option -> {
                try {
                    Iris.getIrisConfig().setUnknown(option);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
                class_310.method_1551().method_1507((class_437)this);
            }, (class_2561)class_2561.method_43470((String)"Unknown shader toggle"), (class_2561)class_2561.method_43470((String)"This allows unknown shaders to load in."), (class_2561)class_2561.method_43470((String)"Enable"), (class_2561)class_2561.method_43470((String)"Disable")));
        }
        if (!this.guiHidden) {
            super.method_25394(guiGraphics, mouseX, mouseY, delta);
            if (this.optionMenuOpen && this.shaderOptionList != null) {
                this.shaderOptionList.method_25394(guiGraphics, mouseX, mouseY, delta);
            } else {
                this.shaderPackList.method_25394(guiGraphics, mouseX, mouseY, delta);
            }
        } else {
            this.method_57734(delta);
            this.showHideButton.method_25394(guiGraphics, mouseX, mouseY, delta);
        }
        float previousHoverTimer = this.guiButtonHoverTimer;
        if (previousHoverTimer == this.guiButtonHoverTimer) {
            this.guiButtonHoverTimer = 0.0f;
        }
        if (!this.guiHidden) {
            guiGraphics.method_27534(this.field_22793, this.field_22785, (int)((double)this.field_22789 * 0.5), 8, 0xFFFFFF);
            if (this.notificationDialog != null && this.notificationDialogTimer > 0) {
                guiGraphics.method_27534(this.field_22793, this.notificationDialog, (int)((double)this.field_22789 * 0.5), 21, 0xFFFFFF);
            } else if (this.optionMenuOpen) {
                guiGraphics.method_27534(this.field_22793, CONFIGURE_TITLE, (int)((double)this.field_22789 * 0.5), 21, 0xFFFFFF);
            } else {
                guiGraphics.method_27534(this.field_22793, SELECT_TITLE, (int)((double)this.field_22789 * 0.5), 21, 0xFFFFFF);
            }
            if (this.isDisplayingComment()) {
                int panelHeight = Math.max(50, 18 + this.hoveredElementCommentBody.size() * 10);
                int x = (int)(0.5 * (double)this.field_22789) - 157;
                int y = this.field_22790 - (panelHeight + 4);
                GuiUtil.drawPanel(guiGraphics, x, y, 314, panelHeight);
                guiGraphics.method_27535(this.field_22793, this.hoveredElementCommentTitle.orElse((class_2561)class_2561.method_43473()), x + 4, y + 4, 0xFFFFFF);
                for (int i = 0; i < this.hoveredElementCommentBody.size(); ++i) {
                    guiGraphics.method_35720(this.field_22793, this.hoveredElementCommentBody.get(i), x + 4, y + 16 + i * 10, 0xFFFFFF);
                }
            }
        }
        for (Runnable render : TOP_LAYER_RENDER_QUEUE) {
            render.run();
        }
        TOP_LAYER_RENDER_QUEUE.clear();
        if (this.developmentComponent != null) {
            guiGraphics.method_27535(this.field_22793, (class_2561)this.developmentComponent, 2, this.field_22790 - 10, 0xFFFFFF);
            guiGraphics.method_27535(this.field_22793, (class_2561)this.irisTextComponent, 2, this.field_22790 - 20, 0xFFFFFF);
        } else if (this.updateComponent != null) {
            guiGraphics.method_27535(this.field_22793, (class_2561)this.updateComponent, 2, this.field_22790 - 10, 0xFFFFFF);
            guiGraphics.method_27535(this.field_22793, (class_2561)this.irisTextComponent, 2, this.field_22790 - 20, 0xFFFFFF);
        } else {
            guiGraphics.method_27535(this.field_22793, (class_2561)this.irisTextComponent, 2, this.field_22790 - 10, 0xFFFFFF);
        }
    }

    public boolean method_25402(double d, double e, int i) {
        int widthValue = this.field_22793.method_1727("New update available!");
        if (this.updateComponent != null && d < (double)widthValue && e > (double)(this.field_22790 - 10) && e < (double)this.field_22790) {
            this.field_22787.method_1507((class_437)new class_407(bl -> {
                if (bl) {
                    Iris.getUpdateChecker().getUpdateLink().ifPresent(arg_0 -> ((class_156.class_158)class_156.method_668()).method_670(arg_0));
                }
                this.field_22787.method_1507((class_437)this);
            }, Iris.getUpdateChecker().getUpdateLink().orElse(""), true));
        }
        return super.method_25402(d, e, i);
    }

    protected void method_25426() {
        super.method_25426();
        int bottomCenter = this.field_22789 / 2 - 50;
        int topCenter = this.field_22789 / 2 - 76;
        boolean inWorld = this.field_22787.field_1687 != null;
        this.method_37066((class_364)this.shaderPackList);
        this.method_37066((class_364)this.shaderOptionList);
        this.shaderPackList = new ShaderPackSelectionList(this, this.field_22787, this.field_22789, this.field_22790, 32, this.field_22790 - 58 - 32, 0, this.field_22789);
        if (Iris.getCurrentPack().isPresent() && this.navigation != null) {
            ShaderPack currentPack = Iris.getCurrentPack().get();
            this.shaderOptionList = new ShaderPackOptionList(this, this.navigation, currentPack, this.field_22787, this.field_22789, this.field_22790, 32, this.field_22790 - 58 - 32, 0, this.field_22789);
            this.navigation.setActiveOptionList(this.shaderOptionList);
            this.shaderOptionList.rebuild();
        } else {
            this.optionMenuOpen = false;
            this.shaderOptionList = null;
        }
        this.method_37067();
        if (!this.guiHidden) {
            if (this.optionMenuOpen && this.shaderOptionList != null) {
                this.method_37063((class_364)this.shaderOptionList);
            } else {
                this.method_37063((class_364)this.shaderPackList);
            }
            this.method_37063((class_364)IrisButton.iris$builder(class_5244.field_24334, button -> this.method_25419(), this.buttonTransition).bounds(bottomCenter + 104, this.field_22790 - 27, 100, 20).build());
            this.method_37063((class_364)IrisButton.iris$builder((class_2561)class_2561.method_43471((String)"options.iris.apply"), button -> this.applyChanges(), this.buttonTransition).bounds(bottomCenter, this.field_22790 - 27, 100, 20).build());
            this.method_37063((class_364)IrisButton.iris$builder(class_5244.field_24335, button -> this.dropChangesAndClose(), this.buttonTransition).bounds(bottomCenter - 104, this.field_22790 - 27, 100, 20).build());
            this.openFolderButton = IrisButton.iris$builder((class_2561)class_2561.method_43471((String)"options.iris.openShaderPackFolder"), button -> this.openShaderPackFolder(), this.buttonTransition).bounds(topCenter - 78, this.field_22790 - 51, 152, 20).build();
            this.method_37063((class_364)this.openFolderButton);
            this.screenSwitchButton = (class_4185)this.method_37063((class_364)IrisButton.iris$builder((class_2561)class_2561.method_43471((String)"options.iris.shaderPackList"), button -> {
                this.optionMenuOpen = !this.optionMenuOpen;
                this.applyChanges();
                this.method_25395((class_364)this.shaderPackList.method_25336());
                this.method_25426();
            }, this.buttonTransition).bounds(topCenter + 78, this.field_22790 - 51, 152, 20).build());
            this.refreshScreenSwitchButton();
        }
        if (inWorld) {
            class_5250 showOrHide = this.guiHidden ? class_2561.method_43471((String)"options.iris.gui.show") : class_2561.method_43471((String)"options.iris.gui.hide");
            float endOfLastButton = (float)this.field_22789 / 2.0f + 154.0f;
            float freeSpace = (float)this.field_22789 - endOfLastButton;
            int x = freeSpace > 100.0f ? this.field_22789 - 50 : (freeSpace < 20.0f ? this.field_22789 - 20 : (int)(endOfLastButton + freeSpace / 2.0f) - 10);
            this.showHideButton = new OldImageButton(x, this.field_22790 - 39, 20, 20, this.guiHidden ? 20 : 0, 146, 20, GuiUtil.IRIS_WIDGETS_TEX, 256, 256, button -> {
                this.guiHidden = !this.guiHidden;
                this.method_25426();
            }, (class_2561)showOrHide);
            this.showHideButton.method_47400(class_7919.method_47407((class_2561)showOrHide));
            this.showHideButton.method_47402(Duration.ofSeconds(10L));
            this.method_37063((class_364)this.showHideButton);
        }
        this.hoveredElement = null;
        this.hoveredElementCommentTimer = 0;
    }

    public void refreshForChangedPack() {
        if (Iris.getCurrentPack().isPresent()) {
            ShaderPack currentPack = Iris.getCurrentPack().get();
            this.navigation = new NavigationController(currentPack.getMenuContainer());
            if (this.shaderOptionList != null) {
                this.shaderOptionList.applyShaderPack(currentPack);
                this.shaderOptionList.rebuild();
            }
        } else {
            this.navigation = null;
        }
        this.refreshScreenSwitchButton();
    }

    public void refreshScreenSwitchButton() {
        if (this.screenSwitchButton != null) {
            this.screenSwitchButton.method_25355((class_2561)(this.optionMenuOpen ? class_2561.method_43471((String)"options.iris.shaderPackList") : class_2561.method_43471((String)"options.iris.shaderPackSettings")));
            this.screenSwitchButton.field_22763 = this.optionMenuOpen || this.shaderPackList.getTopButtonRow().shadersEnabled;
        }
    }

    private void processFixedBlur(float tick) {
        class_279 blurEffect = ((GameRendererAccessor)this.field_22787.field_1773).getBlurEffect();
        float g = Math.min((float)this.field_22787.field_1690.method_57703(), this.blurTransition.getAsFloat());
        if (blurEffect != null && g >= 1.0f) {
            blurEffect.method_57799("Radius", g);
            blurEffect.method_1258(tick);
        }
    }

    protected void method_57734(float pScreen0) {
        this.processFixedBlur(pScreen0);
        this.field_22787.method_1522().method_1235(false);
    }

    public void method_25393() {
        super.method_25393();
        if (this.notificationDialogTimer > 0) {
            --this.notificationDialogTimer;
        }
        this.hoveredElementCommentTimer = this.hoveredElement != null ? ++this.hoveredElementCommentTimer : 0;
    }

    public boolean method_25404(int key, int j, int k) {
        if (key == 256) {
            if (this.guiHidden) {
                this.guiHidden = false;
                this.method_25426();
                return true;
            }
            if (this.navigation != null && this.navigation.hasHistory()) {
                this.navigation.back();
                return true;
            }
            if (this.optionMenuOpen) {
                this.optionMenuOpen = false;
                this.method_25426();
                return true;
            }
        } else if (key == 258) {
            if (!this.optionMenuOpen) {
                this.shaderPackList.method_25404(257, 0, 0);
            }
            this.optionMenuOpen = !this.optionMenuOpen;
            this.applyChanges();
            this.method_25426();
            this.method_25395(null);
        } else if (key == 290 && this.showHideButton != null) {
            this.guiHidden = !this.guiHidden;
            this.method_25426();
        }
        return this.guiHidden || super.method_25404(key, j, k);
    }

    public void method_29638(List<Path> paths) {
        if (this.optionMenuOpen) {
            this.onOptionMenuFilesDrop(paths);
        } else {
            this.onPackListFilesDrop(paths);
        }
    }

    public void onPackListFilesDrop(List<Path> paths) {
        List<Path> packs = paths.stream().filter(Iris::isValidShaderpack).toList();
        for (Path pack : packs) {
            String fileName = pack.getFileName().toString();
            try {
                Iris.getShaderpacksDirectoryManager().copyPackIntoDirectory(fileName, pack);
            }
            catch (FileAlreadyExistsException e) {
                this.notificationDialog = class_2561.method_43469((String)"options.iris.shaderPackSelection.copyErrorAlreadyExists", (Object[])new Object[]{fileName}).method_27695(new class_124[]{class_124.field_1056, class_124.field_1061});
                this.notificationDialogTimer = 100;
                this.shaderPackList.refresh();
                return;
            }
            catch (IOException e) {
                Iris.logger.warn("Error copying dragged shader pack", e);
                this.notificationDialog = class_2561.method_43469((String)"options.iris.shaderPackSelection.copyError", (Object[])new Object[]{fileName}).method_27695(new class_124[]{class_124.field_1056, class_124.field_1061});
                this.notificationDialogTimer = 100;
                this.shaderPackList.refresh();
                return;
            }
        }
        this.shaderPackList.refresh();
        if (packs.isEmpty()) {
            if (paths.size() == 1) {
                String fileName = paths.getFirst().getFileName().toString();
                this.notificationDialog = class_2561.method_43469((String)"options.iris.shaderPackSelection.failedAddSingle", (Object[])new Object[]{fileName}).method_27695(new class_124[]{class_124.field_1056, class_124.field_1061});
            } else {
                this.notificationDialog = class_2561.method_43471((String)"options.iris.shaderPackSelection.failedAdd").method_27695(new class_124[]{class_124.field_1056, class_124.field_1061});
            }
        } else if (packs.size() == 1) {
            String packName = packs.getFirst().getFileName().toString();
            this.notificationDialog = class_2561.method_43469((String)"options.iris.shaderPackSelection.addedPack", (Object[])new Object[]{packName}).method_27695(new class_124[]{class_124.field_1056, class_124.field_1054});
            this.shaderPackList.select(packName);
        } else {
            this.notificationDialog = class_2561.method_43469((String)"options.iris.shaderPackSelection.addedPacks", (Object[])new Object[]{packs.size()}).method_27695(new class_124[]{class_124.field_1056, class_124.field_1054});
        }
        this.notificationDialogTimer = 100;
    }

    public void displayNotification(class_2561 component) {
        this.notificationDialog = component;
        this.notificationDialogTimer = 100;
    }

    public void onOptionMenuFilesDrop(List<Path> paths) {
        if (paths.size() != 1) {
            this.notificationDialog = class_2561.method_43471((String)"options.iris.shaderPackOptions.tooManyFiles").method_27695(new class_124[]{class_124.field_1056, class_124.field_1061});
            this.notificationDialogTimer = 100;
            return;
        }
        this.importPackOptions(paths.getFirst());
    }

    public void importPackOptions(Path settingFile) {
        try (InputStream in = Files.newInputStream(settingFile, new OpenOption[0]);){
            Properties properties = new Properties();
            properties.load(in);
            Iris.queueShaderPackOptionsFromProperties(properties);
            this.notificationDialog = class_2561.method_43469((String)"options.iris.shaderPackOptions.importedSettings", (Object[])new Object[]{settingFile.getFileName().toString()}).method_27695(new class_124[]{class_124.field_1056, class_124.field_1054});
            this.notificationDialogTimer = 100;
            if (this.navigation != null) {
                this.navigation.refresh();
            }
        }
        catch (Exception e) {
            Iris.logger.error("Error importing shader settings file \"" + settingFile.toString() + "\"", e);
            this.notificationDialog = class_2561.method_43469((String)"options.iris.shaderPackOptions.failedImport", (Object[])new Object[]{settingFile.getFileName().toString()}).method_27695(new class_124[]{class_124.field_1056, class_124.field_1061});
            this.notificationDialogTimer = 100;
        }
    }

    public void method_25419() {
        if (!this.dropChanges) {
            this.applyChanges();
        } else {
            this.discardChanges();
        }
        try {
            this.shaderPackList.close();
        }
        catch (IOException e) {
            Iris.logger.error("Failed to safely close shaderpack selection!", e);
        }
        this.field_22787.method_1507(this.parent);
    }

    private void dropChangesAndClose() {
        this.dropChanges = true;
        this.method_25419();
    }

    public void applyChanges() {
        String previousPackName;
        ShaderPackSelectionList.BaseEntry base = (ShaderPackSelectionList.BaseEntry)this.shaderPackList.method_25334();
        boolean enabled = this.shaderPackList.getTopButtonRow().shadersEnabled;
        boolean previousShadersEnabled = Iris.getIrisConfig().areShadersEnabled();
        if (enabled != previousShadersEnabled) {
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(enabled);
        }
        if (!(base instanceof ShaderPackSelectionList.ShaderPackEntry)) {
            return;
        }
        ShaderPackSelectionList.ShaderPackEntry entry = (ShaderPackSelectionList.ShaderPackEntry)base;
        this.shaderPackList.setApplied(entry);
        String name = entry.getPackName();
        if (!name.equals(Iris.getCurrentPackName())) {
            Iris.clearShaderPackOptionQueue();
        }
        if (!name.equals(previousPackName = (String)Iris.getIrisConfig().getShaderPackName().orElse(null)) || !Iris.getShaderPackOptionQueue().isEmpty() || Iris.shouldResetShaderPackOptionsOnNextReload()) {
            Iris.getIrisConfig().setShaderPackName(name);
            IrisApi.getInstance().getConfig().setShadersEnabledAndApply(enabled);
        }
        this.refreshForChangedPack();
    }

    private void discardChanges() {
        Iris.clearShaderPackOptionQueue();
    }

    private void openShaderPackFolder() {
        CompletableFuture.runAsync(() -> class_156.method_668().method_673(Iris.getShaderpacksDirectoryManager().getDirectoryUri()));
    }

    public void setElementHoveredStatus(AbstractElementWidget<?> widget, boolean hovered) {
        if (hovered && widget != this.hoveredElement) {
            this.hoveredElement = widget;
            if (widget instanceof CommentedElementWidget) {
                this.hoveredElementCommentTitle = ((CommentedElementWidget)widget).getCommentTitle();
                Optional<class_2561> commentBody = ((CommentedElementWidget)widget).getCommentBody();
                if (commentBody.isEmpty()) {
                    this.hoveredElementCommentBody.clear();
                } else {
                    String rawCommentBody = commentBody.get().getString();
                    if (rawCommentBody.endsWith(".")) {
                        rawCommentBody = rawCommentBody.substring(0, rawCommentBody.length() - 1);
                    }
                    List<class_5250> splitByPeriods = Arrays.stream(rawCommentBody.split("\\. [ ]*")).map(class_2561::method_43470).toList();
                    this.hoveredElementCommentBody = new ArrayList<class_5481>();
                    for (class_5250 text : splitByPeriods) {
                        this.hoveredElementCommentBody.addAll(this.field_22793.method_1728((class_5348)text, 306));
                    }
                }
            } else {
                this.hoveredElementCommentTitle = Optional.empty();
                this.hoveredElementCommentBody.clear();
            }
            this.hoveredElementCommentTimer = 0;
        } else if (!hovered && widget == this.hoveredElement) {
            this.hoveredElement = null;
            this.hoveredElementCommentTitle = Optional.empty();
            this.hoveredElementCommentBody.clear();
            this.hoveredElementCommentTimer = 0;
        }
    }

    public boolean isDisplayingComment() {
        return this.hoveredElementCommentTimer > 20 && this.hoveredElementCommentTitle.isPresent() && !this.hoveredElementCommentBody.isEmpty();
    }

    public class_4185 getBottomRowOption() {
        return this.openFolderButton;
    }
}

