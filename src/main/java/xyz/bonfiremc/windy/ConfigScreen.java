package xyz.bonfiremc.windy;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private WindyConfig workingCopy;
    private Button densityModeButton;
    private Button biomeWindButton;
    private EditBox constantMinimumWindHeightBox;
    private EditBox constantSpawnRateMultiplierBox;
    private EditBox scalingMinimumWindHeightBox;
    private EditBox scalingMinimumSpawnRateMultiplierBox;
    private EditBox scalingMaximumWindHeightBox;
    private EditBox scalingMaximumSpawnRateMultiplierBox;

    private ConfigScreen(Screen parent) {
        super(Component.translatable("windy.config.title"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new ConfigScreen(parent);
    }

    @Override
    protected void init() {
        this.workingCopy = copyOf(WindyConfig.get());
        int centerX = this.width / 2;
        int y = Math.max(32, this.height / 8);
        int fieldWidth = 80;
        int buttonWidth = 240;

        this.addRenderableWidget(CycleButton.onOffBuilder(this.workingCopy.spawnWind)
                .create(centerX - buttonWidth / 2, y, buttonWidth, 20, Component.translatable("windy.config.spawnWind"),
                        (button, value) -> this.workingCopy.spawnWind = value));

        y += 24;
        this.addRenderableWidget(CycleButton.onOffBuilder(this.workingCopy.windMustSeeSky)
                .create(centerX - buttonWidth / 2, y, buttonWidth, 20, Component.translatable("windy.config.windMustSeeSky"),
                        (button, value) -> this.workingCopy.windMustSeeSky = value));

        y += 24;
        this.densityModeButton = this.addRenderableWidget(Button.builder(densityModeComponent(this.workingCopy.windDensityMode), btn -> {
                    this.workingCopy.windDensityMode = this.workingCopy.windDensityMode == WindyConfig.WindDensityMode.CONSTANT
                            ? WindyConfig.WindDensityMode.Y_LEVEL_SCALING
                            : WindyConfig.WindDensityMode.CONSTANT;
                    btn.setMessage(densityModeComponent(this.workingCopy.windDensityMode));
                })
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());

        y += 24;
        this.biomeWindButton = this.addRenderableWidget(Button.builder(biomeWindComponent(this.workingCopy.biomeWind.enabled), btn -> {
                    this.workingCopy.biomeWind.enabled = !this.workingCopy.biomeWind.enabled;
                    btn.setMessage(biomeWindComponent(this.workingCopy.biomeWind.enabled));
                })
                .bounds(centerX - buttonWidth / 2, y, buttonWidth, 20)
                .build());

        y += 38;
        this.constantMinimumWindHeightBox = new EditBox(this.font, centerX - 90, y, fieldWidth, 20, Component.translatable("windy.config.constant.minimumWindHeight"));
        this.constantMinimumWindHeightBox.setValue(Integer.toString(this.workingCopy.constantWind.minimumWindHeight));
        this.addRenderableWidget(this.constantMinimumWindHeightBox);

        this.constantSpawnRateMultiplierBox = new EditBox(this.font, centerX + 10, y, fieldWidth, 20, Component.translatable("windy.config.constant.spawnRateMultiplier"));
        this.constantSpawnRateMultiplierBox.setValue(Double.toString(this.workingCopy.constantWind.spawnRateMultiplier));
        this.addRenderableWidget(this.constantSpawnRateMultiplierBox);

        y += 46;
        this.scalingMinimumWindHeightBox = new EditBox(this.font, centerX - 190, y, fieldWidth, 20, Component.translatable("windy.config.scaling.minimumWindHeight"));
        this.scalingMinimumWindHeightBox.setValue(Integer.toString(this.workingCopy.yLevelScalingWind.minimumWindHeight));
        this.addRenderableWidget(this.scalingMinimumWindHeightBox);

        this.scalingMinimumSpawnRateMultiplierBox = new EditBox(this.font, centerX - 90, y, fieldWidth, 20, Component.translatable("windy.config.scaling.minimumSpawnRateMultiplier"));
        this.scalingMinimumSpawnRateMultiplierBox.setValue(Double.toString(this.workingCopy.yLevelScalingWind.minimumHeightSpawnRateMultiplier));
        this.addRenderableWidget(this.scalingMinimumSpawnRateMultiplierBox);

        this.scalingMaximumWindHeightBox = new EditBox(this.font, centerX + 10, y, fieldWidth, 20, Component.translatable("windy.config.scaling.maximumWindHeight"));
        this.scalingMaximumWindHeightBox.setValue(Integer.toString(this.workingCopy.yLevelScalingWind.maximumWindHeight));
        this.addRenderableWidget(this.scalingMaximumWindHeightBox);

        this.scalingMaximumSpawnRateMultiplierBox = new EditBox(this.font, centerX + 110, y, fieldWidth, 20, Component.translatable("windy.config.scaling.maximumSpawnRateMultiplier"));
        this.scalingMaximumSpawnRateMultiplierBox.setValue(Double.toString(this.workingCopy.yLevelScalingWind.maximumHeightSpawnRateMultiplier));
        this.addRenderableWidget(this.scalingMaximumSpawnRateMultiplierBox);

        y += 42;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> saveAndClose())
                .bounds(centerX - 102, y, 100, 20)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose())
                .bounds(centerX + 2, y, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int top = Math.max(32, this.height / 8);
        guiGraphics.drawCenteredString(this.font, this.title, centerX, 16, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.spawnWind.desc"), centerX, top - 12, 0xA0A0A0);

        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.constant"), centerX, top + 92, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.minimumWindHeight.short"), centerX - 50, top + 113, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.spawnRateMultiplier.short"), centerX + 50, top + 113, 0xA0A0A0);

        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.yLevelScaling"), centerX, top + 138, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.minY.short"), centerX - 150, top + 159, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.minMultiplier.short"), centerX - 50, top + 159, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.maxY.short"), centerX + 50, top + 159, 0xA0A0A0);
        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.maxMultiplier.short"), centerX + 150, top + 159, 0xA0A0A0);

        guiGraphics.drawCenteredString(this.font, Component.translatable("windy.config.advancedJsonNote").withStyle(ChatFormatting.DARK_GRAY), centerX, top + 190, 0x808080);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void saveAndClose() {
        this.workingCopy.constantWind.minimumWindHeight = parseInt(this.constantMinimumWindHeightBox.getValue(), this.workingCopy.constantWind.minimumWindHeight);
        this.workingCopy.constantWind.spawnRateMultiplier = parseDouble(this.constantSpawnRateMultiplierBox.getValue(), this.workingCopy.constantWind.spawnRateMultiplier);
        this.workingCopy.yLevelScalingWind.minimumWindHeight = parseInt(this.scalingMinimumWindHeightBox.getValue(), this.workingCopy.yLevelScalingWind.minimumWindHeight);
        this.workingCopy.yLevelScalingWind.minimumHeightSpawnRateMultiplier = parseDouble(this.scalingMinimumSpawnRateMultiplierBox.getValue(), this.workingCopy.yLevelScalingWind.minimumHeightSpawnRateMultiplier);
        this.workingCopy.yLevelScalingWind.maximumWindHeight = parseInt(this.scalingMaximumWindHeightBox.getValue(), this.workingCopy.yLevelScalingWind.maximumWindHeight);
        this.workingCopy.yLevelScalingWind.maximumHeightSpawnRateMultiplier = parseDouble(this.scalingMaximumSpawnRateMultiplierBox.getValue(), this.workingCopy.yLevelScalingWind.maximumHeightSpawnRateMultiplier);

        WindyConfig target = WindyConfig.get();
        target.spawnWind = this.workingCopy.spawnWind;
        target.windMustSeeSky = this.workingCopy.windMustSeeSky;
        target.windDensityMode = this.workingCopy.windDensityMode;
        target.constantWind = this.workingCopy.constantWind;
        target.yLevelScalingWind = this.workingCopy.yLevelScalingWind;
        target.biomeWind = this.workingCopy.biomeWind;
        target.clearBiomeCache();
        WindyConfig.save();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
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

    private static Component densityModeComponent(WindyConfig.WindDensityMode mode) {
        String key = mode == WindyConfig.WindDensityMode.Y_LEVEL_SCALING ? "yLevelScaling" : "constant";
        return Component.translatable("windy.config.windDensityMode", Component.translatable("windy.config." + key));
    }

    private static Component biomeWindComponent(boolean enabled) {
        return Component.translatable("windy.config.biomeWind", enabled ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    private static int parseInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(-64, Math.min(320, parsed));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Math.max(0.0D, Math.min(100.0D, parsed));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
