package org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.function.Consumer;

/**
 * Credits:
 * https://github.com/QWERTZexe/ModernConfig
 * https://modrinth.com/mod/modernconfig
 * License: THIRD_PARTY_LICENSES/ModernConfig
 */
public class ColorPickerScreen extends Screen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 232;
    private static final int PANEL_COLOR = 0xCC1A1A1A;
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;

    private static final int PICKER_WIDTH = 200;
    private static final int PICKER_HEIGHT = 150;
    private static final int HUE_BAR_WIDTH = 20;
    private static final int PREVIEW_SIZE = 34;
    private static final int CACHE_BLOCK_SIZE = 4;

    private final Screen previousScreen;
    private final Consumer<Integer> onColorSelected;

    private int currentColor;
    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float brightness = 1.0f;

    private boolean draggingSaturationBrightness = false;
    private boolean draggingHue = false;
    private boolean syncingHex = false;

    private EditBox hexInput;

    private final int[][] saturationBrightnessCache = new int[PICKER_WIDTH / CACHE_BLOCK_SIZE][PICKER_HEIGHT / CACHE_BLOCK_SIZE];
    private final int[] hueBarCache = new int[PICKER_HEIGHT / CACHE_BLOCK_SIZE];
    private float lastCachedHue = -1.0f;

    public ColorPickerScreen(Screen previousScreen, int initialColor, Consumer<Integer> onColorSelected) {
        super(Component.literal("Color Picker"));
        this.previousScreen = previousScreen;
        this.currentColor = initialColor & 0xFFFFFF;
        this.onColorSelected = onColorSelected;
        updateHSVFromColor();
        precalculateHueBar();
    }

    @Override
    protected void init() {
        int panelX = getPanelX();
        int panelY = getPanelY();
        int previewX = getPreviewX();
        int hexInputY = getPreviewY() + PREVIEW_SIZE + 16;

        this.hexInput = this.addRenderableWidget(new EditBox(
                this.font,
                previewX,
                hexInputY,
                47,
                20,
                Component.literal("Hex")
        ));
        this.hexInput.setMaxLength(7);
        this.hexInput.setFilter(text -> text.matches("#?[0-9A-Fa-f]{0,6}"));
        this.hexInput.setValue(formatHex(this.currentColor));
        this.hexInput.setResponder(this::onHexInputChanged);

        int buttonY = panelY + PANEL_HEIGHT - 30;
        int buttonWidth = (PANEL_WIDTH - 35) / 2;

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                        .pos(panelX + 15, buttonY)
                        .width(buttonWidth)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Apply"), btn -> applyAndClose())
                        .pos(panelX + 20 + buttonWidth, buttonY)
                        .width(buttonWidth)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.previousScreen != null) {
            this.previousScreen.render(graphics, -1, -1, partialTick);
        }

        graphics.fill(0, 0, this.width, this.height, 0x66000000);

        int panelX = getPanelX();
        int panelY = getPanelY();
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_COLOR);
        graphics.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_BORDER_COLOR);

        int pickerX = getPickerX();
        int pickerY = getPickerY();
        int hueX = getHueBarX();
        int previewX = getPreviewX();
        int previewY = getPreviewY();

        graphics.drawCenteredString(this.font, "Select Color", this.width / 2, pickerY - this.font.lineHeight - 6, 0xFFFFFFFF);

        drawSaturationBrightnessPicker(graphics, pickerX, pickerY);
        drawHueBar(graphics, hueX, pickerY);

        graphics.drawString(this.font, "Preview", previewX, previewY - this.font.lineHeight - 4, 0xFFFFFFFF, true);
        graphics.fill(previewX, previewY, previewX + PREVIEW_SIZE, previewY + PREVIEW_SIZE, 0xFF000000);
        graphics.fill(previewX + 1, previewY + 1, previewX + PREVIEW_SIZE - 1, previewY + PREVIEW_SIZE - 1, 0xFF000000 | this.currentColor);

        if (this.hexInput != null) {
            graphics.drawString(this.font, "HEX CODE:", this.hexInput.getX(), this.hexInput.getY() - this.font.lineHeight - 3, 0xFFFFFFFF, true);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }

        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            applyAndClose();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            double mouseX = event.x();
            double mouseY = event.y();

            if (insideRect(mouseX, mouseY, getPickerX(), getPickerY(), PICKER_WIDTH, PICKER_HEIGHT)) {
                this.draggingSaturationBrightness = true;
                updateSaturationBrightness(mouseX, mouseY);
                return true;
            }

            if (insideRect(mouseX, mouseY, getHueBarX(), getPickerY(), HUE_BAR_WIDTH, PICKER_HEIGHT)) {
                this.draggingHue = true;
                updateHue(mouseY);
                return true;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.draggingSaturationBrightness = false;
            this.draggingHue = false;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (this.draggingSaturationBrightness) {
                updateSaturationBrightness(event.x(), event.y());
                return true;
            }

            if (this.draggingHue) {
                updateHue(event.y());
                return true;
            }
        }

        return super.mouseDragged(event, offsetX, offsetY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.previousScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void applyAndClose() {
        if (this.onColorSelected != null) {
            this.onColorSelected.accept(this.currentColor);
        }
        this.onClose();
    }

    private void onHexInputChanged(String value) {
        if (this.syncingHex) {
            return;
        }

        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        if (hex.matches("[0-9A-Fa-f]{6}")) {
            try {
                this.currentColor = Integer.parseInt(hex, 16) & 0xFFFFFF;
                updateHSVFromColor();
            } catch (NumberFormatException ignored) {
                // Ignore malformed value.
            }
        }
    }

    private void updateHSVFromColor() {
        float[] hsv = new float[3];
        Color.RGBtoHSB((this.currentColor >> 16) & 0xFF, (this.currentColor >> 8) & 0xFF, this.currentColor & 0xFF, hsv);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.brightness = hsv[2];
    }

    private void updateColorFromHSV() {
        int rgb = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        this.currentColor = rgb & 0xFFFFFF;

        if (this.hexInput != null) {
            this.syncingHex = true;
            this.hexInput.setValue(formatHex(this.currentColor));
            this.syncingHex = false;
        }
    }

    private void updateSaturationBrightness(double mouseX, double mouseY) {
        this.saturation = Mth.clamp((float) (mouseX - getPickerX()) / PICKER_WIDTH, 0.0f, 1.0f);
        this.brightness = Mth.clamp(1.0f - (float) (mouseY - getPickerY()) / PICKER_HEIGHT, 0.0f, 1.0f);
        updateColorFromHSV();
    }

    private void updateHue(double mouseY) {
        this.hue = Mth.clamp((float) (mouseY - getPickerY()) / PICKER_HEIGHT, 0.0f, 1.0f);
        updateColorFromHSV();
    }

    private void drawSaturationBrightnessPicker(GuiGraphics graphics, int startX, int startY) {
        if (Math.abs(this.lastCachedHue - this.hue) > 0.001f) {
            int cacheWidth = this.saturationBrightnessCache.length;
            int cacheHeight = this.saturationBrightnessCache[0].length;
            for (int x = 0; x < cacheWidth; x++) {
                for (int y = 0; y < cacheHeight; y++) {
                    float s = (float) x / (cacheWidth - 1);
                    float b = 1.0f - (float) y / (cacheHeight - 1);
                    this.saturationBrightnessCache[x][y] = Color.HSBtoRGB(this.hue, s, b) & 0xFFFFFF;
                }
            }
            this.lastCachedHue = this.hue;
        }

        for (int x = 0; x < this.saturationBrightnessCache.length; x++) {
            for (int y = 0; y < this.saturationBrightnessCache[0].length; y++) {
                int pixelX = startX + x * CACHE_BLOCK_SIZE;
                int pixelY = startY + y * CACHE_BLOCK_SIZE;
                int color = this.saturationBrightnessCache[x][y];
                graphics.fill(pixelX, pixelY, pixelX + CACHE_BLOCK_SIZE, pixelY + CACHE_BLOCK_SIZE, 0xFF000000 | color);
            }
        }

        int indicatorX = (int) (startX + this.saturation * PICKER_WIDTH);
        int indicatorY = (int) (startY + (1.0f - this.brightness) * PICKER_HEIGHT);
        graphics.fill(indicatorX - 3, indicatorY - 3, indicatorX + 3, indicatorY + 3, 0xFF000000);
        graphics.fill(indicatorX - 2, indicatorY - 2, indicatorX + 2, indicatorY + 2, 0xFFFFFFFF);
    }

    private void drawHueBar(GuiGraphics graphics, int startX, int startY) {
        for (int y = 0; y < this.hueBarCache.length; y++) {
            int pixelY = startY + y * CACHE_BLOCK_SIZE;
            graphics.fill(startX, pixelY, startX + HUE_BAR_WIDTH, pixelY + CACHE_BLOCK_SIZE, 0xFF000000 | this.hueBarCache[y]);
        }

        int indicatorY = (int) (startY + this.hue * PICKER_HEIGHT);
        graphics.fill(startX - 2, indicatorY - 1, startX + HUE_BAR_WIDTH + 2, indicatorY + 1, 0xFFFFFFFF);
    }

    private void precalculateHueBar() {
        for (int y = 0; y < this.hueBarCache.length; y++) {
            float h = (float) y / (this.hueBarCache.length - 1);
            this.hueBarCache[y] = Color.HSBtoRGB(h, 1.0f, 1.0f) & 0xFFFFFF;
        }
    }

    private int getPanelX() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int getPanelY() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private int getPickerX() {
        return getPanelX() + 16;
    }

    private int getPickerY() {
        return getPanelY() + 42;
    }

    private int getHueBarX() {
        return getPickerX() + PICKER_WIDTH + 8;
    }

    private int getPreviewX() {
        return getHueBarX() + HUE_BAR_WIDTH + 12;
    }

    private int getPreviewY() {
        return getPickerY() + 10;
    }

    private static boolean insideRect(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static String formatHex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }
}

