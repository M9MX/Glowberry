package org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Simple reusable confirmation modal.
 */
public class ConfirmScreen extends Screen {
    private static final int PANEL_WIDTH = 290;
    private static final int PANEL_HEIGHT = 130;
    private static final int PANEL_COLOR = 0xCC1A1A1A;
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;

    private final Screen previousScreen;
    private final Component titleText;
    private final Runnable onConfirm;

    public ConfirmScreen(Screen previousScreen, Component titleText, Runnable onConfirm) {
        super(Component.empty());
        this.previousScreen = previousScreen;
        this.titleText = titleText;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        int rowButtonWidth = (PANEL_WIDTH - 35) / 2;
        this.addRenderableWidget(
                Button.builder(Component.literal("Decline"), btn -> this.onClose())
                        .pos(panelX + 15, panelY + 54)
                        .width(rowButtonWidth)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Confirm"), btn -> this.confirmAndClose())
                        .pos(panelX + 20 + rowButtonWidth, panelY + 54)
                        .width(rowButtonWidth)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                        .pos(panelX + 15, panelY + 84)
                        .width(PANEL_WIDTH - 30)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.previousScreen != null) {
            this.previousScreen.render(graphics, -1, -1, partialTick);
        }

        graphics.fill(0, 0, this.width, this.height, 0x66000000);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_COLOR);
        graphics.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_BORDER_COLOR);
        graphics.drawCenteredString(this.font, this.titleText, panelX + PANEL_WIDTH / 2, panelY + 16, 0xFFFFFFFF);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.previousScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void confirmAndClose() {
        if (this.onConfirm != null) {
            this.onConfirm.run();
        }

        if (this.minecraft.screen == this) {
            this.onClose();
        }
    }
}

