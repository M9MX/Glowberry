package org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Reusable modal popup for collecting a single string value.
 */
public class AddStringScreen extends Screen {
    private static final int PANEL_COLOR = 0xCC1A1A1A;
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;

    private final Screen previousScreen;
    private final Component fieldTitleText;
    private final Component inputHint;
    private final Consumer<String> onCreate;
    private final int maxLength;

    private EditBox nameBox;

    public AddStringScreen(Screen previousScreen, Component fieldTitleText, Component inputHint, int maxLength, Consumer<String> onCreate) {
        super(Component.empty());
        this.previousScreen = previousScreen;
        this.fieldTitleText = fieldTitleText;
        this.inputHint = inputHint;
        this.onCreate = onCreate;
        this.maxLength = Math.max(1, maxLength);
    }

    @Override
    protected void init() {
        int panelWidth = 260;
        int panelHeight = 128;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        this.nameBox = this.addRenderableWidget(new EditBox(
                this.font,
                panelX + 15,
                panelY + 44,
                panelWidth - 30,
                20,
                Component.literal("Name")
        ));
        this.nameBox.setMaxLength(this.maxLength);
        this.nameBox.setFilter(text -> text.matches("[a-zA-Z0-9_ -]*"));
        this.nameBox.setHint(this.inputHint);
        this.setFocused(this.nameBox);

        int buttonWidth = (panelWidth - 35) / 2;
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                        .pos(panelX + 15, panelY + 83)
                        .width(buttonWidth)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Create"), btn -> this.tryCreate())
                        .pos(panelX + 20 + buttonWidth, panelY + 83)
                        .width(buttonWidth)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.previousScreen != null) {
            this.previousScreen.render(graphics, -1, -1, partialTick);
        }

        graphics.fill(0, 0, this.width, this.height, 0x55000000);

        int panelWidth = 260;
        int panelHeight = 128;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_COLOR);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, PANEL_BORDER_COLOR);
        if (this.nameBox != null) {
            graphics.drawString(this.font, this.fieldTitleText, this.nameBox.getX(), this.nameBox.getY() - this.font.lineHeight - 8, 0xFFFFFFFF, true);
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
            this.tryCreate();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void tryCreate() {
        String value = this.nameBox != null ? this.nameBox.getValue().trim() : "";
        if (value.isEmpty()) {
            return;
        }

        if (this.onCreate != null) {
            this.onCreate.accept(value);
        }
        this.onClose();
    }
}

