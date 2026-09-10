/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.Iterables
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_350$class_351
 *  net.minecraft.class_3532
 *  net.minecraft.class_364
 *  net.minecraft.class_4265$class_4266
 *  net.minecraft.class_437
 *  net.minecraft.class_525
 *  net.minecraft.class_5250
 *  net.minecraft.class_5251
 *  net.minecraft.class_5348
 *  net.minecraft.class_6379
 *  net.minecraft.class_8028
 *  net.minecraft.class_8030
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.gui.element;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.FileDialogUtil;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.NavigationController;
import net.irisshaders.iris.gui.element.IrisContainerObjectSelectionList;
import net.irisshaders.iris.gui.element.IrisElementRow;
import net.irisshaders.iris.gui.element.widget.AbstractElementWidget;
import net.irisshaders.iris.gui.element.widget.OptionMenuConstructor;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuContainer;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_350;
import net.minecraft.class_3532;
import net.minecraft.class_364;
import net.minecraft.class_4265;
import net.minecraft.class_437;
import net.minecraft.class_525;
import net.minecraft.class_5250;
import net.minecraft.class_5251;
import net.minecraft.class_5348;
import net.minecraft.class_6379;
import net.minecraft.class_8028;
import net.minecraft.class_8030;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShaderPackOptionList
extends IrisContainerObjectSelectionList<BaseEntry> {
    private static final class_2960 MENU_LIST_BACKGROUND = class_2960.method_60656((String)"textures/gui/menu_background.png");
    private final List<AbstractElementWidget<?>> elementWidgets = new ArrayList();
    private final ShaderPackScreen screen;
    private final NavigationController navigation;
    private OptionMenuContainer container;

    public ShaderPackOptionList(ShaderPackScreen screen, NavigationController navigation, ShaderPack pack, class_310 client, int width, int height, int top, int bottom, int left, int right) {
        super(client, width, bottom, top, bottom, left, right, 24);
        this.navigation = navigation;
        this.screen = screen;
        this.applyShaderPack(pack);
    }

    public void applyShaderPack(ShaderPack pack) {
        this.container = pack.getMenuContainer();
    }

    public void rebuild() {
        this.method_25339();
        this.method_25307(0.0);
        OptionMenuConstructor.constructAndApplyToScreen(this.container, this.screen, this, this.navigation);
    }

    public void refresh() {
        this.elementWidgets.forEach(widget -> widget.init(this.screen, this.navigation));
    }

    public int method_25322() {
        return Math.min(400, this.field_22758 - 12);
    }

    protected void method_57715(class_332 pAbstractSelectionList0) {
        if (this.screen.listTransition.getAsFloat() < 0.02f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)this.screen.listTransition.getAsFloat());
        pAbstractSelectionList0.method_25290(MENU_LIST_BACKGROUND, this.method_46426(), this.method_46427() + 3, (float)this.method_55442(), (float)(this.method_55443() + (int)this.method_25341()), this.method_25368(), this.method_25364(), 32, 32);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    protected void method_57713(class_332 pAbstractSelectionList0) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)this.screen.listTransition.getAsFloat());
        pAbstractSelectionList0.method_25290(class_525.field_49895, this.method_46426(), this.method_46427() + 2, 0.0f, 0.0f, this.method_25368(), 2, 32, 2);
        pAbstractSelectionList0.method_25290(class_525.field_49896, this.method_46426(), this.method_55443(), 0.0f, 0.0f, this.method_25368(), 2, 32, 2);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.disableBlend();
    }

    protected boolean method_53812(int i) {
        return i == 0 || i == 1;
    }

    public void addHeader(class_2561 text, boolean backButton) {
        this.method_25321((class_350.class_351)new HeaderEntry(this, this.screen, this.navigation, text, backButton));
    }

    public void addWidgets(int columns, List<AbstractElementWidget<?>> elements) {
        this.elementWidgets.addAll(elements);
        ArrayList<AbstractElementWidget<Object>> row = new ArrayList();
        for (AbstractElementWidget<?> element : elements) {
            row.add(element);
            if (row.size() < columns) continue;
            this.method_25321((class_350.class_351)new ElementRowEntry(this.screen, this.navigation, row));
            row = new ArrayList();
        }
        if (!row.isEmpty()) {
            while (row.size() < columns) {
                row.add(AbstractElementWidget.EMPTY);
            }
            this.method_25321((class_350.class_351)new ElementRowEntry(this.screen, this.navigation, row));
        }
    }

    public NavigationController getNavigation() {
        return this.navigation;
    }

    public class HeaderEntry
    extends BaseEntry {
        public static final class_2561 BACK_BUTTON_TEXT = class_2561.method_43470((String)"< ").method_10852((class_2561)class_2561.method_43471((String)"options.iris.back").method_27692(class_124.field_1056));
        public static final class_5250 RESET_BUTTON_TEXT_INACTIVE = class_2561.method_43471((String)"options.iris.reset").method_27692(class_124.field_1080);
        public static final class_5250 RESET_BUTTON_TEXT_ACTIVE = class_2561.method_43471((String)"options.iris.reset").method_27692(class_124.field_1054);
        public static final class_5250 RESET_HOLD_SHIFT_TOOLTIP = class_2561.method_43471((String)"options.iris.reset.tooltip.holdShift").method_27692(class_124.field_1065);
        public static final class_5250 RESET_TOOLTIP = class_2561.method_43471((String)"options.iris.reset.tooltip").method_27692(class_124.field_1061);
        public static final class_5250 IMPORT_TOOLTIP = class_2561.method_43471((String)"options.iris.importSettings.tooltip").method_27694(style -> style.method_27703(class_5251.method_27717((int)5089023)));
        public static final class_5250 EXPORT_TOOLTIP = class_2561.method_43471((String)"options.iris.exportSettings.tooltip").method_27694(style -> style.method_27703(class_5251.method_27717((int)16547133)));
        private static final int MIN_SIDE_BUTTON_WIDTH = 42;
        private static final int BUTTON_HEIGHT = 16;
        private final ShaderPackScreen screen;
        @Nullable
        private final IrisElementRow backButton;
        private final IrisElementRow utilityButtons = new IrisElementRow();
        private final IrisElementRow.TextButtonElement resetButton;
        private final IrisElementRow.IconButtonElement importButton;
        private final IrisElementRow.IconButtonElement exportButton;
        private final class_2561 text;

        public HeaderEntry(ShaderPackOptionList this$0, ShaderPackScreen screen, NavigationController navigation, class_2561 text, boolean hasBackButton) {
            super(navigation);
            this.backButton = hasBackButton ? new IrisElementRow().add(new IrisElementRow.TextButtonElement(BACK_BUTTON_TEXT, this::backButtonClicked), Math.max(42, class_310.method_1551().field_1772.method_27525((class_5348)BACK_BUTTON_TEXT) + 8)) : null;
            this.resetButton = new IrisElementRow.TextButtonElement((class_2561)RESET_BUTTON_TEXT_INACTIVE, this::resetButtonClicked);
            this.importButton = new IrisElementRow.IconButtonElement(GuiUtil.Icon.IMPORT, GuiUtil.Icon.IMPORT_COLORED, this::importSettingsButtonClicked);
            this.exportButton = new IrisElementRow.IconButtonElement(GuiUtil.Icon.EXPORT, GuiUtil.Icon.EXPORT_COLORED, this::exportSettingsButtonClicked);
            this.utilityButtons.add(this.importButton, 15).add(this.exportButton, 15).add(this.resetButton, Math.max(42, class_310.method_1551().field_1772.method_27525((class_5348)RESET_BUTTON_TEXT_INACTIVE) + 8));
            this.screen = screen;
            this.text = text;
        }

        public void method_25343(class_332 guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean shiftDown;
            guiGraphics.method_25294(x - 3, y + entryHeight - 2, x + entryWidth, y + entryHeight - 1, 0x66BEBEBE);
            class_327 font = class_310.method_1551().field_1772;
            ShaderPackOptionList.method_49605((class_332)guiGraphics, (class_327)font, (class_2561)this.text, (int)(x + (int)((double)entryWidth * 0.5)), (int)(x + 5), (int)(y + 5), (int)(x + entryWidth - 10 - this.utilityButtons.getWidth()), (int)(y + 15), (int)0xFFFFFF);
            GuiUtil.bindIrisWidgetsTexture();
            if (this.backButton != null) {
                this.backButton.render(guiGraphics, x, y, 16, mouseX, mouseY, tickDelta, hovered);
            }
            this.resetButton.disabled = !(shiftDown = class_437.method_25442()) && !this.resetButton.method_25370();
            this.resetButton.text = !this.resetButton.disabled ? RESET_BUTTON_TEXT_ACTIVE : RESET_BUTTON_TEXT_INACTIVE;
            this.utilityButtons.renderRightAligned(guiGraphics, x + entryWidth - 3, y, 16, mouseX, mouseY, tickDelta, hovered);
            if (this.resetButton.isHovered() || this.resetButton.method_25370()) {
                class_5250 tooltip = !this.resetButton.disabled ? RESET_TOOLTIP : RESET_HOLD_SHIFT_TOOLTIP;
                this.queueBottomRightAnchoredTooltip(guiGraphics, this.resetButton.method_48202().method_48255(class_8028.field_41829), this.resetButton.method_48202().comp_1195().comp_1194(), font, (class_2561)tooltip);
            }
            if (this.importButton.isHovered() || this.importButton.method_25370()) {
                this.queueBottomRightAnchoredTooltip(guiGraphics, this.importButton.method_48202().method_48255(class_8028.field_41829), this.importButton.method_48202().comp_1195().comp_1194(), font, (class_2561)IMPORT_TOOLTIP);
            }
            if (this.exportButton.isHovered() || this.exportButton.method_25370()) {
                this.queueBottomRightAnchoredTooltip(guiGraphics, this.exportButton.method_48202().method_48255(class_8028.field_41829), this.exportButton.method_48202().comp_1195().comp_1194(), font, (class_2561)EXPORT_TOOLTIP);
            }
        }

        private void queueBottomRightAnchoredTooltip(class_332 guiGraphics, int x, int y, class_327 font, class_2561 text) {
            ShaderPackScreen.TOP_LAYER_RENDER_QUEUE.add(() -> GuiUtil.drawTextPanel(font, guiGraphics, text, x - (font.method_27525((class_5348)text) + 10), y - 16));
        }

        public List<? extends class_364> method_25396() {
            if (this.backButton != null) {
                return ImmutableList.copyOf((Iterable)Iterables.concat(this.utilityButtons.children(), this.backButton.children()));
            }
            return ImmutableList.copyOf(this.utilityButtons.children());
        }

        public boolean method_25402(double mouseX, double mouseY, int button) {
            boolean backButtonResult = this.backButton != null && this.backButton.mouseClicked(mouseX, mouseY, button);
            boolean utilButtonResult = this.utilityButtons.mouseClicked(mouseX, mouseY, button);
            return backButtonResult || utilButtonResult;
        }

        public boolean method_25404(int keycode, int scancode, int modifiers) {
            if (this.backButton != null && this.backButton.keyPressed(keycode, scancode, modifiers)) {
                return true;
            }
            return this.utilityButtons.keyPressed(keycode, scancode, modifiers);
        }

        public List<? extends class_6379> method_37025() {
            return ImmutableList.of();
        }

        private boolean backButtonClicked(IrisElementRow.TextButtonElement button) {
            this.navigation.back();
            GuiUtil.playButtonClickSound();
            return true;
        }

        private boolean resetButtonClicked(IrisElementRow.TextButtonElement button) {
            if (class_437.method_25442()) {
                Iris.resetShaderPackOptionsOnNextReload();
                this.screen.applyChanges();
                GuiUtil.playButtonClickSound();
                return true;
            }
            return false;
        }

        private boolean importSettingsButtonClicked(IrisElementRow.IconButtonElement button) {
            GuiUtil.playButtonClickSound();
            if (Iris.getCurrentPack().isEmpty()) {
                return false;
            }
            if (class_310.method_1551().method_22683().method_4498()) {
                this.screen.displayNotification((class_2561)class_2561.method_43471((String)"options.iris.mustDisableFullscreen").method_27692(class_124.field_1061).method_27692(class_124.field_1067));
                return false;
            }
            ShaderPackScreen originalScreen = this.screen;
            FileDialogUtil.fileSelectDialog(FileDialogUtil.DialogType.OPEN, "Import Shader Settings from File", Iris.getShaderpacksDirectory().resolve(Iris.getCurrentPackName() + ".txt"), "Shader Pack Settings (.txt)", "*.txt").whenComplete((path, err) -> {
                if (err != null) {
                    Iris.logger.error("Error selecting shader settings from file", (Throwable)err);
                    return;
                }
                if (class_310.method_1551().field_1755 == originalScreen) {
                    path.ifPresent(originalScreen::importPackOptions);
                }
            });
            return true;
        }

        private boolean exportSettingsButtonClicked(IrisElementRow.IconButtonElement button) {
            GuiUtil.playButtonClickSound();
            if (Iris.getCurrentPack().isEmpty()) {
                return false;
            }
            if (class_310.method_1551().method_22683().method_4498()) {
                this.screen.displayNotification((class_2561)class_2561.method_43471((String)"options.iris.mustDisableFullscreen").method_27692(class_124.field_1061).method_27692(class_124.field_1067));
                return false;
            }
            FileDialogUtil.fileSelectDialog(FileDialogUtil.DialogType.SAVE, "Export Shader Settings to File", Iris.getShaderpacksDirectory().resolve(Iris.getCurrentPackName() + ".txt"), "Shader Pack Settings (.txt)", "*.txt").whenComplete((path, err) -> {
                if (err != null) {
                    Iris.logger.error("Error selecting file to export shader settings", (Throwable)err);
                    return;
                }
                path.ifPresent(p -> {
                    Properties toSave = new Properties();
                    Path sourceTxtPath = Iris.getShaderpacksDirectory().resolve(Iris.getCurrentPackName() + ".txt");
                    if (Files.exists(sourceTxtPath, new LinkOption[0])) {
                        try (InputStream in2 = Files.newInputStream(sourceTxtPath, new OpenOption[0]);){
                            toSave.load(in2);
                        }
                        catch (IOException in2) {
                            // empty catch block
                        }
                    }
                    try (OutputStream out = Files.newOutputStream(p, new OpenOption[0]);){
                        toSave.store(out, null);
                    }
                    catch (IOException e) {
                        Iris.logger.error("Error saving properties to \"" + String.valueOf(p) + "\"", e);
                    }
                });
            });
            return true;
        }
    }

    public static class ElementRowEntry
    extends BaseEntry {
        private final List<AbstractElementWidget<?>> widgets;
        private final ShaderPackScreen screen;
        private int cachedWidth;
        private int cachedPosX;

        public ElementRowEntry(ShaderPackScreen screen, NavigationController navigation, List<AbstractElementWidget<?>> widgets) {
            super(navigation);
            this.screen = screen;
            this.widgets = widgets;
        }

        public void method_25343(class_332 guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.cachedWidth = entryWidth;
            this.cachedPosX = x;
            int totalWidthWithoutMargins = entryWidth - 2 * (this.widgets.size() - 1);
            float singleWidgetWidth = (float)(totalWidthWithoutMargins -= 3) / (float)this.widgets.size();
            for (int i = 0; i < this.widgets.size(); ++i) {
                AbstractElementWidget<?> widget = this.widgets.get(i);
                boolean widgetHovered = hovered && this.getHoveredWidget(mouseX) == i || this.method_25399() == widget;
                widget.bounds = new class_8030(x + (int)((singleWidgetWidth + 2.0f) * (float)i), y, (int)singleWidgetWidth, entryHeight + 2);
                widget.render(guiGraphics, mouseX, mouseY, tickDelta, widgetHovered);
                this.screen.setElementHoveredStatus(widget, widgetHovered);
            }
        }

        public int getHoveredWidget(int mouseX) {
            float positionAcrossWidget = (float)class_3532.method_15340((int)(mouseX - this.cachedPosX), (int)0, (int)this.cachedWidth) / (float)this.cachedWidth;
            return class_3532.method_15340((int)((int)Math.floor((float)this.widgets.size() * positionAcrossWidget)), (int)0, (int)(this.widgets.size() - 1));
        }

        public boolean method_25402(double mouseX, double mouseY, int button) {
            return this.widgets.get(this.getHoveredWidget((int)mouseX)).method_25402(mouseX, mouseY, button);
        }

        public boolean method_25406(double mouseX, double mouseY, int button) {
            return this.widgets.get(this.getHoveredWidget((int)mouseX)).method_25406(mouseX, mouseY, button);
        }

        @NotNull
        public List<? extends class_364> method_25396() {
            return ImmutableList.copyOf(this.widgets);
        }

        @NotNull
        public List<? extends class_6379> method_37025() {
            return ImmutableList.copyOf(this.widgets);
        }
    }

    public static abstract class BaseEntry
    extends class_4265.class_4266<BaseEntry> {
        protected final NavigationController navigation;

        protected BaseEntry(NavigationController navigation) {
            this.navigation = navigation;
        }
    }
}

