package org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable modal popup for removing one string from a provided list.
 */
public class RemoveStringScreen extends Screen {
    private static final int PANEL_COLOR = 0xCC1A1A1A;
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;

    private final Screen previousScreen;
    private final Component fieldTitleText;
    private final List<String> values;
    private final Consumer<String> onRemove;

    private StringObjectSelectionList valueList;

    public RemoveStringScreen(Screen previousScreen, Component fieldTitleText, List<String> values, Consumer<String> onRemove) {
        super(Component.empty());
        this.previousScreen = previousScreen;
        this.fieldTitleText = fieldTitleText;
        this.values = values;
        this.onRemove = onRemove;
    }

    @Override
    protected void init() {
        int panelWidth = 260;
        int panelHeight = 188;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        this.valueList = this.addRenderableWidget(
                new StringObjectSelectionList(this.minecraft, panelWidth - 30, 88, panelY + 44, 18, panelX + 15)
        );
        this.valueList.updateSizeAndPosition(panelWidth - 30, 88, panelX + 15, panelY + 44);
        this.valueList.setValues(this.values);

        int buttonWidth = (panelWidth - 35) / 2;
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                        .pos(panelX + 15, panelY + 142)
                        .width(buttonWidth)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Remove"), btn -> this.tryRemove())
                        .pos(panelX + 20 + buttonWidth, panelY + 142)
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
        int panelHeight = 188;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_COLOR);
        graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, PANEL_BORDER_COLOR);
        if (this.valueList != null) {
            graphics.drawString(this.font, this.fieldTitleText, this.valueList.getX(), this.valueList.getY() - this.font.lineHeight - 5, 0xFFFFFFFF, true);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.valueList != null && this.valueList.isMouseOver(mouseX, mouseY)) {
            this.valueList.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            this.tryRemove();
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

    private void tryRemove() {
        if (this.valueList == null) {
            return;
        }

        String selected = this.valueList.getSelectedValue();
        if (selected == null || selected.isBlank()) {
            return;
        }

        if (this.onRemove != null) {
            this.onRemove.accept(selected);
        }
        this.onClose();
    }

    private static class StringObjectSelectionList extends ObjectSelectionList<StringObjectSelectionList.ValueEntry> {
        StringObjectSelectionList(Minecraft minecraft, int width, int height, int y, int itemHeight, int left) {
            super(minecraft, width, height, y, itemHeight);
        }

        void setValues(List<String> values) {
            this.clearEntries();
            if (values != null) {
                for (String value : values) {
                    if (value != null && !value.isBlank()) {
                        this.addEntry(new ValueEntry(value));
                    }
                }
            }

            if (!this.children().isEmpty()) {
                this.setSelected(this.children().getFirst());
            }
        }

        String getSelectedValue() {
            ValueEntry selected = this.getSelected();
            return selected != null ? selected.text : null;
        }

        @Override
        public int getRowLeft() {
            return this.getX() + 4;
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 14;
        }

        @Override
        protected int scrollBarX() {
            return this.getX() + this.getWidth() - 6;
        }

        class ValueEntry extends Entry<ValueEntry> {
            private final String text;

            ValueEntry(String text) {
                this.text = text;
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
                int textColor = hovered ? 0xFFFFFFFF : 0xFFDDDDDD;
                graphics.drawString(Minecraft.getInstance().font, this.text, this.getContentX() + 2, this.getContentY() + 2, textColor, false);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
                StringObjectSelectionList.this.setSelected(this);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.text);
            }
        }
    }
}

