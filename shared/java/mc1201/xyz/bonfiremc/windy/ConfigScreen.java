package xyz.bonfiremc.windy;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Scrollable, in-game Windy configuration screen styled after FastMove's config pages. */
public final class ConfigScreen extends Screen {
    private static final int LIST_TOP = 32;
    private static final int LIST_BOTTOM_MARGIN = 36;
    private static final int ROW_HEIGHT = 26;

    private final Screen parent;
    private final List<ConfigRow> rows = new ArrayList<>();
    private WindyConfig workingCopy;
    private double scrollOffset;
    private int contentHeight;

    private ConfigScreen(Screen parent) {
        super(Component.translatable("windy.config.title"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new ConfigScreen(parent);
    }

    @Override
    protected void init() {
        if (workingCopy == null) {
            workingCopy = copyOf(WindyConfig.get());
        }
        rebuildConfigWidgets();
    }

    private void rebuildConfigWidgets() {
        clearWidgets();
        rows.clear();

        addHeader("windy.config.section.general");
        addBoolean("windy.config.spawnWind", "windy.config.spawnWind.tooltip", workingCopy.spawnWind,
                value -> workingCopy.spawnWind = value);
        addBoolean("windy.config.windMustSeeSky", "windy.config.windMustSeeSky.tooltip", workingCopy.windMustSeeSky,
                value -> workingCopy.windMustSeeSky = value);
        addDensityMode();

        addHeader("windy.config.section.constant");
        addInteger("windy.config.constant.minimumWindHeight", "windy.config.constant.minimumWindHeight.tooltip",
                workingCopy.constantWind.minimumWindHeight,
                value -> workingCopy.constantWind.minimumWindHeight = parseInt(value, workingCopy.constantWind.minimumWindHeight));
        addDecimal("windy.config.constant.spawnRateMultiplier", "windy.config.constant.spawnRateMultiplier.tooltip",
                workingCopy.constantWind.spawnRateMultiplier,
                value -> workingCopy.constantWind.spawnRateMultiplier = parseDouble(value, workingCopy.constantWind.spawnRateMultiplier));

        addHeader("windy.config.section.scaling");
        addInteger("windy.config.scaling.minimumWindHeight", "windy.config.scaling.minimumWindHeight.tooltip",
                workingCopy.yLevelScalingWind.minimumWindHeight,
                value -> workingCopy.yLevelScalingWind.minimumWindHeight = parseInt(value, workingCopy.yLevelScalingWind.minimumWindHeight));
        addDecimal("windy.config.scaling.minimumSpawnRateMultiplier", "windy.config.scaling.minimumSpawnRateMultiplier.tooltip",
                workingCopy.yLevelScalingWind.minimumHeightSpawnRateMultiplier,
                value -> workingCopy.yLevelScalingWind.minimumHeightSpawnRateMultiplier = parseDouble(value, workingCopy.yLevelScalingWind.minimumHeightSpawnRateMultiplier));
        addInteger("windy.config.scaling.maximumWindHeight", "windy.config.scaling.maximumWindHeight.tooltip",
                workingCopy.yLevelScalingWind.maximumWindHeight,
                value -> workingCopy.yLevelScalingWind.maximumWindHeight = parseInt(value, workingCopy.yLevelScalingWind.maximumWindHeight));
        addDecimal("windy.config.scaling.maximumSpawnRateMultiplier", "windy.config.scaling.maximumSpawnRateMultiplier.tooltip",
                workingCopy.yLevelScalingWind.maximumHeightSpawnRateMultiplier,
                value -> workingCopy.yLevelScalingWind.maximumHeightSpawnRateMultiplier = parseDouble(value, workingCopy.yLevelScalingWind.maximumHeightSpawnRateMultiplier));

        addHeader("windy.config.section.biome");
        addBoolean("windy.config.biomeWindEnabled", "windy.config.biomeWindEnabled.tooltip", workingCopy.biomeWind.enabled,
                value -> workingCopy.biomeWind.enabled = value);
        addText("windy.config.biomeBlacklist", "windy.config.biomeBlacklist.tooltip",
                formatBlacklist(workingCopy.biomeWind.blacklist), 4096,
                value -> workingCopy.biomeWind.blacklist = parseBlacklist(value));
        addText("windy.config.biomeMultipliers", "windy.config.biomeMultipliers.tooltip",
                formatMultipliers(workingCopy.biomeWind.multipliers), 4096,
                value -> workingCopy.biomeWind.multipliers = parseMultipliers(value));

        contentHeight = rows.size() * ROW_HEIGHT;
        scrollOffset = clampScroll(scrollOffset);

        addRenderableWidget(Button.builder(
                Component.translatable("controls.reset"),
                button -> resetAll()
        ).bounds(width / 2 - 154, height - 28, 150, 20).build());

        addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                button -> saveAndClose()
        ).bounds(width / 2 + 4, height - 28, 150, 20).build());

        updateWidgetPositions();
    }

    private void addHeader(String translationKey) {
        rows.add(ConfigRow.header(Component.translatable(translationKey)
                .copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
    }

    private void addBoolean(String labelKey, String tooltipKey, boolean initialValue, Consumer<Boolean> setter) {
        final boolean[] staged = {initialValue};
        Button button = Button.builder(booleanText(staged[0]), pressed -> {
            staged[0] = !staged[0];
            setter.accept(staged[0]);
            pressed.setMessage(booleanText(staged[0]));
        }).bounds(0, 0, 160, 20).build();
        button.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        rows.add(ConfigRow.value(Component.translatable(labelKey), button, null));
        addRenderableWidget(button);
    }

    private void addDensityMode() {
        Button button = Button.builder(densityModeText(workingCopy.windDensityMode), pressed -> {
            workingCopy.windDensityMode = workingCopy.windDensityMode == WindyConfig.WindDensityMode.CONSTANT
                    ? WindyConfig.WindDensityMode.Y_LEVEL_SCALING
                    : WindyConfig.WindDensityMode.CONSTANT;
            pressed.setMessage(densityModeText(workingCopy.windDensityMode));
        }).bounds(0, 0, 160, 20).build();
        button.setTooltip(Tooltip.create(Component.translatable("windy.config.windDensityMode.tooltip")));
        rows.add(ConfigRow.value(Component.translatable("windy.config.windDensityModeLabel"), button, null));
        addRenderableWidget(button);
    }

    private void addInteger(String labelKey, String tooltipKey, int initialValue, Consumer<String> commit) {
        addEditBox(labelKey, tooltipKey, Integer.toString(initialValue), 32, true, commit);
    }

    private void addDecimal(String labelKey, String tooltipKey, double initialValue, Consumer<String> commit) {
        addEditBox(labelKey, tooltipKey, Double.toString(initialValue), 32, false, commit);
    }

    private void addText(String labelKey, String tooltipKey, String initialValue, int maxLength, Consumer<String> commit) {
        EditBox box = new EditBox(font, 0, 0, 160, 20, Component.translatable(labelKey));
        box.setMaxLength(maxLength);
        box.setValue(initialValue);
        box.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        rows.add(ConfigRow.value(Component.translatable(labelKey), box, () -> commit.accept(box.getValue())));
        addRenderableWidget(box);
    }

    private void addEditBox(String labelKey, String tooltipKey, String initialValue, int maxLength,
                            boolean integerOnly, Consumer<String> commit) {
        EditBox box = new EditBox(font, 0, 0, 160, 20, Component.translatable(labelKey));
        box.setMaxLength(maxLength);
        box.setFilter(integerOnly ? ConfigScreen::isPartialInteger : ConfigScreen::isPartialDecimal);
        box.setValue(initialValue);
        box.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        rows.add(ConfigRow.value(Component.translatable(labelKey), box, () -> commit.accept(box.getValue())));
        addRenderableWidget(box);
    }

    private void resetAll() {
        workingCopy = new WindyConfig();
        scrollOffset = 0.0D;
        rebuildConfigWidgets();
    }

    private void saveAndClose() {
        for (ConfigRow row : rows) {
            if (row.commit != null) row.commit.run();
        }

        // Match the same safety rules used when reading the TOML file.
        if (workingCopy.yLevelScalingWind.maximumWindHeight < workingCopy.yLevelScalingWind.minimumWindHeight) {
            workingCopy.yLevelScalingWind.maximumWindHeight = workingCopy.yLevelScalingWind.minimumWindHeight;
        }

        WindyConfig target = WindyConfig.get();
        target.spawnWind = workingCopy.spawnWind;
        target.windMustSeeSky = workingCopy.windMustSeeSky;
        target.windDensityMode = workingCopy.windDensityMode;
        target.constantWind = workingCopy.constantWind;
        target.yLevelScalingWind = workingCopy.yLevelScalingWind;
        target.biomeWind = workingCopy.biomeWind;
        target.clearBiomeCache();
        WindyConfig.save();
        minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= LIST_TOP && mouseY < height - LIST_BOTTOM_MARGIN && maxScroll() > 0.0D) {
            scrollOffset = clampScroll(scrollOffset - delta * ROW_HEIGHT);
            updateWidgetPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderRows(graphics);
        renderScrollbar(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
    }

    private void renderRows(GuiGraphics graphics) {
        int rowWidth = rowWidth();
        int left = width / 2 - rowWidth / 2;
        int controlWidth = controlWidth(rowWidth);
        int controlX = left + rowWidth - controlWidth - 8;

        for (int i = 0; i < rows.size(); i++) {
            ConfigRow row = rows.get(i);
            int y = rowY(i);
            if (y + ROW_HEIGHT <= LIST_TOP || y >= height - LIST_BOTTOM_MARGIN) continue;

            if (row.header) {
                graphics.drawCenteredString(font, row.label, width / 2, y + 8, 0xFFFF55);
            } else {
                graphics.drawString(font, row.label, left + 8, y + 8, 0xFFFFFF, false);
            }
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        double max = maxScroll();
        if (max <= 0.0D) return;

        int top = LIST_TOP;
        int bottom = height - LIST_BOTTOM_MARGIN;
        int visible = bottom - top;
        int thumbHeight = Math.max(20, (int) Math.round((double) visible * visible / contentHeight));
        int travel = visible - thumbHeight;
        int thumbY = top + (int) Math.round((scrollOffset / max) * travel);
        int x = width / 2 + rowWidth() / 2 + 8;
        graphics.fill(x, top, x + 4, bottom, 0x40202020);
        graphics.fill(x, thumbY, x + 4, thumbY + thumbHeight, 0xFFA0A0A0);
    }

    private void updateWidgetPositions() {
        int rowWidth = rowWidth();
        int left = width / 2 - rowWidth / 2;
        int controlWidth = controlWidth(rowWidth);
        int controlX = left + rowWidth - controlWidth - 8;
        int bottom = height - LIST_BOTTOM_MARGIN;

        for (int i = 0; i < rows.size(); i++) {
            ConfigRow row = rows.get(i);
            if (row.widget == null) continue;
            int y = rowY(i);
            row.widget.setX(controlX);
            row.widget.setY(y + 2);
            row.widget.setWidth(controlWidth);
            boolean visible = y >= LIST_TOP && y + ROW_HEIGHT <= bottom;
            row.widget.visible = visible;
            row.widget.active = visible;
        }
    }

    private int rowY(int index) {
        return LIST_TOP + index * ROW_HEIGHT - (int) Math.round(scrollOffset);
    }

    private int rowWidth() {
        return Math.min(620, Math.max(260, width - 60));
    }

    private static int controlWidth(int rowWidth) {
        return Math.min(190, Math.max(120, rowWidth / 3));
    }

    private double maxScroll() {
        return Math.max(0.0D, contentHeight - (height - LIST_BOTTOM_MARGIN - LIST_TOP));
    }

    private double clampScroll(double value) {
        return Math.max(0.0D, Math.min(maxScroll(), value));
    }

    private static Component booleanText(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private static Component densityModeText(WindyConfig.WindDensityMode mode) {
        return Component.translatable(mode == WindyConfig.WindDensityMode.Y_LEVEL_SCALING
                ? "windy.config.yLevelScaling" : "windy.config.constant");
    }

    private static boolean isPartialInteger(String value) {
        return value.isEmpty() || value.equals("-") || value.matches("-?\\d+");
    }

    private static boolean isPartialDecimal(String value) {
        return value.isEmpty() || value.equals("-") || value.equals(".") || value.equals("-.")
                || value.matches("-?(?:\\d+\\.?\\d*|\\.\\d*)");
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Math.max(-64, Math.min(320, Integer.parseInt(value.trim())));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (Double.isNaN(parsed)) return fallback;
            return Math.max(0.0D, Math.min(100.0D, parsed));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String formatBlacklist(List<String> values) {
        return values == null ? "" : String.join(", ", values);
    }

    private static List<String> parseBlacklist(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split(",")) {
            String value = normalizeRuleKey(part);
            if (!value.isEmpty()) result.add(value);
        }
        return result;
    }

    private static String formatMultipliers(Map<String, Double> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            if (builder.length() > 0) builder.append(", ");
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private static Map<String, Double> parseMultipliers(String raw) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return result;
        for (String part : raw.split(",")) {
            int equals = part.indexOf('=');
            if (equals <= 0 || equals >= part.length() - 1) continue;
            String key = normalizeRuleKey(part.substring(0, equals));
            if (key.isEmpty()) continue;
            try {
                double value = Double.parseDouble(part.substring(equals + 1).trim());
                if (!Double.isNaN(value)) result.put(key, Math.max(0.0D, Math.min(100.0D, value)));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private static String normalizeRuleKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static WindyConfig copyOf(WindyConfig original) {
        WindyConfig copy = new WindyConfig();
        copy.configVersion = original.configVersion;
        copy.spawnWind = original.spawnWind;
        copy.windMustSeeSky = original.windMustSeeSky;
        copy.windDensityMode = original.windDensityMode;

        copy.constantWind = new WindyConfig.ConstantWind();
        copy.constantWind.minimumWindHeight = original.constantWind.minimumWindHeight;
        copy.constantWind.spawnRateMultiplier = original.constantWind.spawnRateMultiplier;

        copy.yLevelScalingWind = new WindyConfig.YLevelScalingWind();
        copy.yLevelScalingWind.minimumWindHeight = original.yLevelScalingWind.minimumWindHeight;
        copy.yLevelScalingWind.minimumHeightSpawnRateMultiplier = original.yLevelScalingWind.minimumHeightSpawnRateMultiplier;
        copy.yLevelScalingWind.maximumWindHeight = original.yLevelScalingWind.maximumWindHeight;
        copy.yLevelScalingWind.maximumHeightSpawnRateMultiplier = original.yLevelScalingWind.maximumHeightSpawnRateMultiplier;

        copy.biomeWind = new WindyConfig.BiomeWind();
        copy.biomeWind.enabled = original.biomeWind.enabled;
        copy.biomeWind.blacklist = new ArrayList<>(original.biomeWind.blacklist);
        copy.biomeWind.multipliers = new LinkedHashMap<>(original.biomeWind.multipliers);
        return copy;
    }

    private static final class ConfigRow {
        private final Component label;
        private final AbstractWidget widget;
        private final Runnable commit;
        private final boolean header;

        private ConfigRow(Component label, AbstractWidget widget, Runnable commit, boolean header) {
            this.label = label;
            this.widget = widget;
            this.commit = commit;
            this.header = header;
        }

        static ConfigRow header(Component label) {
            return new ConfigRow(label, null, null, true);
        }

        static ConfigRow value(Component label, AbstractWidget widget, Runnable commit) {
            return new ConfigRow(label, widget, commit, false);
        }
    }
}
