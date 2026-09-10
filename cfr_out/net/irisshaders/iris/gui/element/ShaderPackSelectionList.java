/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.class_124
 *  net.minecraft.class_156
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_350$class_351
 *  net.minecraft.class_364
 *  net.minecraft.class_407
 *  net.minecraft.class_437
 *  net.minecraft.class_525
 *  net.minecraft.class_5250
 *  net.minecraft.class_5348
 *  net.minecraft.class_8016
 *  net.minecraft.class_8023
 *  net.minecraft.class_8030
 *  org.jetbrains.annotations.Nullable
 */
package net.irisshaders.iris.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gui.GuiUtil;
import net.irisshaders.iris.gui.element.IrisElementRow;
import net.irisshaders.iris.gui.element.IrisObjectSelectionList;
import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.class_124;
import net.minecraft.class_156;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_350;
import net.minecraft.class_364;
import net.minecraft.class_407;
import net.minecraft.class_437;
import net.minecraft.class_525;
import net.minecraft.class_5250;
import net.minecraft.class_5348;
import net.minecraft.class_8016;
import net.minecraft.class_8023;
import net.minecraft.class_8030;
import org.jetbrains.annotations.Nullable;

public class ShaderPackSelectionList
extends IrisObjectSelectionList<BaseEntry> {
    private static final class_2561 PACK_LIST_LABEL = class_2561.method_43471((String)"pack.iris.list.label").method_27695(new class_124[]{class_124.field_1056, class_124.field_1080});
    private static final class_2960 MENU_LIST_BACKGROUND = class_2960.method_60656((String)"textures/gui/menu_background.png");
    private final ShaderPackScreen screen;
    private final TopButtonRowEntry topButtonRow;
    private final WatchService watcher;
    private final WatchKey key;
    private final PinnedEntry downloadButton;
    private boolean keyValid;
    private ShaderPackEntry applied = null;

    public ShaderPackSelectionList(ShaderPackScreen screen, class_310 client, int width, int height, int top, int bottom, int left, int right) {
        super(client, width, bottom, top + 4, bottom, left, right, 20);
        WatchKey key1;
        WatchService watcher1;
        this.screen = screen;
        this.topButtonRow = new TopButtonRowEntry(this, Iris.getIrisConfig().areShadersEnabled());
        this.downloadButton = new PinnedEntry((class_2561)class_2561.method_43470((String)"Download Shaders"), () -> this.field_22740.method_1507((class_437)new class_407(bl -> {
            if (bl) {
                class_156.method_668().method_670("https://modrinth.com/shaders");
            }
            this.field_22740.method_1507((class_437)this.screen);
        }, "https://modrinth.com/shaders", true)), this);
        try {
            watcher1 = FileSystems.getDefault().newWatchService();
            key1 = Iris.getShaderpacksDirectory().register(watcher1, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            this.keyValid = true;
        }
        catch (IOException e) {
            Iris.logger.error("Couldn't register file watcher!", e);
            watcher1 = null;
            key1 = null;
            this.keyValid = false;
        }
        this.key = key1;
        this.watcher = watcher1;
        this.refresh();
    }

    public boolean method_25404(int pContainerEventHandler0, int pInt1, int pInt2) {
        if (pContainerEventHandler0 == 265 && this.method_25336() == this.method_48200()) {
            return true;
        }
        return super.method_25404(pContainerEventHandler0, pInt1, pInt2);
    }

    public void method_48579(class_332 pAbstractSelectionList0, int pInt1, int pInt2, float pFloat3) {
        if (this.keyValid) {
            for (WatchEvent<?> event : this.key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                this.refresh();
                break;
            }
            this.keyValid = this.key.reset();
        }
        super.method_48579(pAbstractSelectionList0, pInt1, pInt2, pFloat3);
    }

    public void close() throws IOException {
        if (this.key != null) {
            this.key.cancel();
        }
        if (this.watcher != null) {
            this.watcher.close();
        }
    }

    protected void method_57715(class_332 pAbstractSelectionList0) {
        if (this.screen.listTransition.getAsFloat() < 0.02f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)this.screen.listTransition.getAsFloat());
        pAbstractSelectionList0.method_25290(MENU_LIST_BACKGROUND, this.method_46426(), this.method_46427() - 2, (float)this.method_55442(), (float)(this.method_55443() + (int)this.method_25341()), this.method_25368(), this.method_25364(), 32, 32);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    protected void method_57713(class_332 pAbstractSelectionList0) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)this.screen.listTransition.getAsFloat());
        pAbstractSelectionList0.method_25290(class_525.field_49895, this.method_46426(), this.method_46427() - 2, 0.0f, 0.0f, this.method_25368(), 2, 32, 2);
        pAbstractSelectionList0.method_25290(class_525.field_49896, this.method_46426(), this.method_55443(), 0.0f, 0.0f, this.method_25368(), 2, 32, 2);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.disableBlend();
    }

    public int method_25322() {
        return Math.min(308, this.field_22758 - 50);
    }

    protected int method_25337(int index) {
        return super.method_25337(index) + 2;
    }

    public void refresh() {
        List<String> names;
        this.method_25339();
        try {
            names = Iris.getShaderpacksDirectoryManager().enumerate();
        }
        catch (Throwable e) {
            Iris.logger.error("Error reading files while constructing selection UI", e);
            this.addLabelEntries(new class_2561[]{class_2561.method_43473(), class_2561.method_43470((String)"There was an error reading your shaderpacks directory").method_27695(new class_124[]{class_124.field_1061, class_124.field_1067}), class_2561.method_43473(), class_2561.method_43470((String)"Check your logs for more information."), class_2561.method_43470((String)"Please file an issue report including a log file."), class_2561.method_43470((String)"If you are able to identify the file causing this, please include it in your report as well."), class_2561.method_43470((String)"Note that this might be an issue with folder permissions; ensure those are correct first.")});
            return;
        }
        this.method_25321(this.topButtonRow);
        if (names.isEmpty()) {
            this.method_25321(this.downloadButton);
        }
        this.topButtonRow.allowEnableShadersButton = !names.isEmpty();
        int index = 0;
        for (String name : names) {
            this.addPackEntry(++index, name);
        }
        this.addLabelEntries(PACK_LIST_LABEL);
    }

    public void addPackEntry(int index, String name) {
        ShaderPackEntry entry = new ShaderPackEntry(index, this, name);
        Iris.getIrisConfig().getShaderPackName().ifPresent(currentPackName -> {
            if (name.equals(currentPackName)) {
                this.method_25313(entry);
                this.method_25395((class_364)entry);
                this.method_25324(entry);
                this.setApplied(entry);
            }
        });
        this.method_25321(entry);
    }

    public void addLabelEntries(class_2561 ... lines) {
        for (class_2561 text : lines) {
            this.method_25321(new LabelEntry(text));
        }
    }

    public void select(String name) {
        for (int i = 0; i < this.method_25340(); ++i) {
            BaseEntry entry = (BaseEntry)this.method_25326(i);
            if (!(entry instanceof ShaderPackEntry) || !((ShaderPackEntry)entry).packName.equals(name)) continue;
            this.method_25313(entry);
            return;
        }
    }

    public ShaderPackEntry getApplied() {
        return this.applied;
    }

    public void setApplied(ShaderPackEntry entry) {
        this.applied = entry;
    }

    public TopButtonRowEntry getTopButtonRow() {
        return this.topButtonRow;
    }

    public class ShaderPackEntry
    extends BaseEntry {
        private final String packName;
        private final ShaderPackSelectionList list;
        private final int index;
        private class_8030 bounds = class_8030.method_48248();
        private boolean focused;

        public ShaderPackEntry(int index, ShaderPackSelectionList list, String packName) {
            this.packName = packName;
            this.list = list;
            this.index = index;
        }

        public class_8030 method_48202() {
            return this.bounds;
        }

        public boolean isApplied() {
            return this.list.getApplied() == this;
        }

        public boolean isSelected() {
            return this.list.method_25334() == this;
        }

        public String getPackName() {
            return this.packName;
        }

        public void method_25343(class_332 guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.bounds = new class_8030(x, y, entryWidth, entryHeight);
            class_327 font = class_310.method_1551().field_1772;
            int color = 0xFFFFFF;
            Object name = this.packName;
            if (hovered) {
                GuiUtil.bindIrisWidgetsTexture();
                GuiUtil.drawButton(guiGraphics, x - 2, y - 2, entryWidth, entryHeight + 4, hovered, false);
            }
            boolean shadersEnabled = this.list.getTopButtonRow().shadersEnabled;
            if (font.method_27525((class_5348)class_2561.method_43470((String)name).method_27692(class_124.field_1067)) > this.list.method_25322() - 3) {
                name = font.method_27523((String)name, this.list.method_25322() - 8) + "...";
            }
            class_5250 text = class_2561.method_43470((String)name);
            if (this.method_25405(mouseX, mouseY)) {
                text = text.method_27692(class_124.field_1067);
            }
            if (shadersEnabled && this.isApplied()) {
                color = 16773731;
            }
            if (!shadersEnabled && !this.method_25405(mouseX, mouseY)) {
                color = 0xA2A2A2;
            }
            guiGraphics.method_27534(font, (class_2561)text, x + entryWidth / 2 - 2, y + (entryHeight - 11) / 2, color);
        }

        public boolean method_25402(double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            return this.doThing();
        }

        public boolean method_25404(int keycode, int pInt1, int pInt2) {
            if (keycode != 257) {
                return false;
            }
            return this.doThing();
        }

        private boolean doThing() {
            boolean didAnything = false;
            if (!this.list.getTopButtonRow().shadersEnabled) {
                this.list.getTopButtonRow().setShadersEnabled(true);
                didAnything = true;
            }
            if (!this.isSelected()) {
                this.list.select(this.index);
                didAnything = true;
            }
            ShaderPackSelectionList.this.screen.method_25395((class_364)ShaderPackSelectionList.this.screen.getBottomRowOption());
            return didAnything;
        }

        @Nullable
        public class_8016 method_48205(class_8023 pGuiEventListener0) {
            return !this.method_25370() ? class_8016.method_48193((class_364)this) : null;
        }

        public boolean method_25370() {
            return this.list.method_25336() == this;
        }
    }

    public static class TopButtonRowEntry
    extends BaseEntry {
        private static final class_2561 NONE_PRESENT_LABEL = class_2561.method_43471((String)"options.iris.shaders.nonePresent").method_27692(class_124.field_1080);
        private static final class_2561 SHADERS_DISABLED_LABEL = class_2561.method_43471((String)"options.iris.shaders.disabled");
        private static final class_2561 SHADERS_ENABLED_LABEL = class_2561.method_43471((String)"options.iris.shaders.enabled");
        private final ShaderPackSelectionList list;
        public boolean allowEnableShadersButton = true;
        public boolean shadersEnabled;

        public TopButtonRowEntry(ShaderPackSelectionList list, boolean shadersEnabled) {
            this.list = list;
            this.shadersEnabled = shadersEnabled;
        }

        public void setShadersEnabled(boolean shadersEnabled) {
            this.shadersEnabled = shadersEnabled;
            this.list.screen.refreshScreenSwitchButton();
        }

        public void method_25343(class_332 guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            GuiUtil.bindIrisWidgetsTexture();
            GuiUtil.drawButton(guiGraphics, x - 2, y - 2, entryWidth, entryHeight + 2, hovered, !this.allowEnableShadersButton);
            guiGraphics.method_27534(class_310.method_1551().field_1772, this.getEnableDisableLabel(), x + entryWidth / 2 - 2, y + (entryHeight - 11) / 2, 0xFFFFFF);
        }

        private class_2561 getEnableDisableLabel() {
            return this.allowEnableShadersButton ? (this.shadersEnabled ? SHADERS_ENABLED_LABEL : SHADERS_DISABLED_LABEL) : NONE_PRESENT_LABEL;
        }

        public boolean method_25402(double mouseX, double mouseY, int button) {
            if (this.allowEnableShadersButton) {
                this.setShadersEnabled(!this.shadersEnabled);
                GuiUtil.playButtonClickSound();
                return true;
            }
            return false;
        }

        public boolean method_25404(int keycode, int scancode, int modifiers) {
            if (keycode == 257 && this.allowEnableShadersButton) {
                this.setShadersEnabled(!this.shadersEnabled);
                GuiUtil.playButtonClickSound();
                return true;
            }
            return false;
        }

        @Nullable
        public class_8016 method_48205(class_8023 pGuiEventListener0) {
            return !this.method_25370() ? class_8016.method_48193((class_364)this) : null;
        }

        public boolean method_25370() {
            return this.list.method_25336() == this;
        }

        public static class EnableShadersButtonElement
        extends IrisElementRow.TextButtonElement {
            private int centerX;

            public EnableShadersButtonElement(class_2561 text, Function<IrisElementRow.TextButtonElement, Boolean> onClick) {
                super(text, onClick);
            }

            @Override
            public void renderLabel(class_332 guiGraphics, int x, int y, int width, int height, int mouseX, int mouseY, float tickDelta, boolean hovered) {
                int textX = this.centerX - (int)((double)this.font.method_27525((class_5348)this.text) * 0.5);
                int textY = y + (int)((double)(height - 8) * 0.5);
                guiGraphics.method_27535(this.font, this.text, textX, textY, 0xFFFFFF);
            }
        }
    }

    private static class PinnedEntry
    extends BaseEntry {
        public final boolean allowPressButton = true;
        private final class_2561 label;
        private final Runnable onClick;

        public PinnedEntry(class_2561 label, Runnable onClick, ShaderPackSelectionList list) {
            this.label = label;
            this.onClick = onClick;
        }

        public void method_25343(class_332 guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            GuiUtil.bindIrisWidgetsTexture();
            GuiUtil.drawButton(guiGraphics, x - 2, y - 2, entryWidth, entryHeight + 2, hovered, false);
            guiGraphics.method_27534(class_310.method_1551().field_1772, this.label, x + entryWidth / 2 - 2, y + (entryHeight - 11) / 2, 0xFFFFFF);
        }

        public boolean method_25402(double mouseX, double mouseY, int button) {
            Objects.requireNonNull(this);
            GuiUtil.playButtonClickSound();
            this.onClick.run();
            return false;
        }

        public boolean method_25404(int keycode, int scancode, int modifiers) {
            if (keycode == 257) {
                Objects.requireNonNull(this);
                GuiUtil.playButtonClickSound();
                this.onClick.run();
                return false;
            }
            return false;
        }
    }

    public static class LabelEntry
    extends BaseEntry {
        private final class_2561 label;

        public LabelEntry(class_2561 label) {
            this.label = label;
        }

        public void method_25343(class_332 guiGraphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            guiGraphics.method_27534(class_310.method_1551().field_1772, this.label, x + entryWidth / 2 - 2, y + (entryHeight - 11) / 2, 0xC2C2C2);
        }
    }

    public static abstract class BaseEntry
    extends class_350.class_351<BaseEntry> {
        protected BaseEntry() {
        }
    }
}

