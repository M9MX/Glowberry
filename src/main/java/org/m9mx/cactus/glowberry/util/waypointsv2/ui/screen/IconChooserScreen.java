package org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Item-based icon chooser for WaypointsV2.
 */
public class IconChooserScreen extends Screen {
    private static final int PANEL_WIDTH = 440;
    private static final int PANEL_HEIGHT = 300;
    private static final int PANEL_COLOR = 0xCC1A1A1A;
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;

    private static final int GRID_CELL_SIZE = 22;
    private static final int LOAD_BATCH_SIZE = 100;
    private static final int MAX_LOADED_RESULTS = 500;

    private final Screen previousScreen;
    private final Consumer<Item> onIconSelected;
    private final Item initiallySelected;

    private EditBox searchBox;

    private List<Item> allItems = new ArrayList<>();
    private List<Item> filteredItems = new ArrayList<>();
    private int loadedStartIndex = 0;
    private int loadedEndIndex = 0;
    private int topRowAbsolute = 0;
    private Item selectedItem;

    public IconChooserScreen(Screen previousScreen, Item currentItem, Consumer<Item> onIconSelected) {
        super(Component.literal("Choose Icon"));
        this.previousScreen = previousScreen;
        this.initiallySelected = currentItem != null ? currentItem : Items.COMPASS;
        this.selectedItem = this.initiallySelected;
        this.onIconSelected = onIconSelected;
    }

    @Override
    protected void init() {
        int panelX = getPanelX();
        int panelY = getPanelY();

        this.searchBox = this.addRenderableWidget(new EditBox(
                this.font,
                panelX + 15,
                panelY + 28,
                PANEL_WIDTH - 30,
                20,
                Component.literal("Search")
        ));
        this.searchBox.setMaxLength(80);
        this.searchBox.setHint(Component.literal("Search items..."));
        this.searchBox.setResponder(this::applyFilter);

        int buttonY = panelY + PANEL_HEIGHT - 30;
        int buttonWidth = (PANEL_WIDTH - 35) / 2;
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                        .pos(panelX + 15, buttonY)
                        .width(buttonWidth)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(Component.literal("Use Selected"), btn -> useSelectedAndClose())
                        .pos(panelX + 20 + buttonWidth, buttonY)
                        .width(buttonWidth)
                        .build()
        );

        loadAllItems();
        applyFilter("");
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
        graphics.drawCenteredString(this.font, "Choose Icon", this.width / 2, panelY + 10, 0xFFFFFFFF);

        int gridX = getGridX();
        int gridY = getGridY();
        int gridWidth = getGridWidth();
        int gridHeight = getGridHeight();
        graphics.fill(gridX, gridY, gridX + gridWidth, gridY + gridHeight, 0x55222222);
        graphics.renderOutline(gridX, gridY, gridWidth, gridHeight, 0xFF555555);

        renderGrid(graphics, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            useSelectedAndClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (handleGridClick(event.x(), event.y())) {
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInsideGrid(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int maxTopRow = getMaxTopRow();
        if (scrollY < 0) {
            this.topRowAbsolute = Math.min(this.topRowAbsolute + 1, maxTopRow);
        } else if (scrollY > 0) {
            this.topRowAbsolute = Math.max(this.topRowAbsolute - 1, 0);
        }

        updateLoadedWindowForViewport();
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.previousScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int cols = getGridColumns();
        int visibleRows = getVisibleRows();
        int gridX = getGridX();
        int gridY = getGridY();

        Item hoveredItem = null;

        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = (this.topRowAbsolute + row) * cols + col;
                if (index >= this.filteredItems.size()) {
                    break;
                }

                if (index < this.loadedStartIndex || index >= this.loadedEndIndex) {
                    continue;
                }

                Item item = this.filteredItems.get(index);
                int cellX = gridX + col * GRID_CELL_SIZE;
                int cellY = gridY + row * GRID_CELL_SIZE;

                boolean selected = item == this.selectedItem;
                graphics.fill(cellX, cellY, cellX + GRID_CELL_SIZE, cellY + GRID_CELL_SIZE, selected ? 0xFF666666 : 0xAA2A2A2A);
                graphics.renderOutline(cellX, cellY, GRID_CELL_SIZE, GRID_CELL_SIZE, selected ? 0xFFFFFFFF : 0xFF3A3A3A);
                graphics.renderItem(new ItemStack(item), cellX + 3, cellY + 3);

                if (mouseX >= cellX && mouseX < cellX + GRID_CELL_SIZE && mouseY >= cellY && mouseY < cellY + GRID_CELL_SIZE) {
                    hoveredItem = item;
                }
            }
        }

        if (hoveredItem != null) {
            String hoverText = hoveredItem.getDefaultInstance().getHoverName().getString();
            int textWidth = this.font.width(hoverText);
            int tooltipX = mouseX + 10;
            int tooltipY = mouseY + 10;
            graphics.fill(tooltipX - 3, tooltipY - 3, tooltipX + textWidth + 3, tooltipY + this.font.lineHeight + 3, 0xCC000000);
            graphics.drawString(this.font, hoverText, tooltipX, tooltipY, 0xFFFFFFFF, false);
        }
    }

    private boolean handleGridClick(double mouseX, double mouseY) {
        if (!isInsideGrid(mouseX, mouseY)) {
            return false;
        }

        int col = (int) ((mouseX - getGridX()) / GRID_CELL_SIZE);
        int row = (int) ((mouseY - getGridY()) / GRID_CELL_SIZE);
        if (col < 0 || row < 0 || col >= getGridColumns() || row >= getVisibleRows()) {
            return false;
        }

        int index = (this.topRowAbsolute + row) * getGridColumns() + col;
        if (index < 0 || index >= this.filteredItems.size()) {
            return false;
        }

        this.selectedItem = this.filteredItems.get(index);
        return true;
    }

    private void useSelectedAndClose() {
        if (this.selectedItem != null && this.onIconSelected != null) {
            this.onIconSelected.accept(this.selectedItem);
        }
        this.onClose();
    }

    private void loadAllItems() {
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        items.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        this.allItems = items;
    }

    private void applyFilter(String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        this.filteredItems = new ArrayList<>();

        for (Item item : this.allItems) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase(Locale.ROOT);
            String name = item.getDescriptionId().toLowerCase(Locale.ROOT);
            if (needle.isEmpty() || id.contains(needle) || name.contains(needle)) {
                this.filteredItems.add(item);
            }
        }

        this.topRowAbsolute = 0;
        this.loadedStartIndex = 0;
        this.loadedEndIndex = Math.min(LOAD_BATCH_SIZE, this.filteredItems.size());

        if (this.initiallySelected != null && this.filteredItems.contains(this.initiallySelected)) {
            this.selectedItem = this.initiallySelected;
        } else if (!this.filteredItems.isEmpty()) {
            this.selectedItem = this.filteredItems.getFirst();
        } else {
            this.selectedItem = null;
        }

        updateLoadedWindowForViewport();
    }

    private void updateLoadedWindowForViewport() {
        int total = this.filteredItems.size();
        if (total <= 0) {
            this.loadedStartIndex = 0;
            this.loadedEndIndex = 0;
            return;
        }

        if (this.loadedEndIndex <= this.loadedStartIndex) {
            this.loadedStartIndex = 0;
            this.loadedEndIndex = Math.min(LOAD_BATCH_SIZE, total);
        }

        int cols = getGridColumns();
        int viewportStart = this.topRowAbsolute * cols;
        int viewportEndExclusive = Math.min(total, (this.topRowAbsolute + getVisibleRows()) * cols);

        while (viewportEndExclusive > this.loadedEndIndex && this.loadedEndIndex < total) {
            this.loadedEndIndex = Math.min(total, this.loadedEndIndex + LOAD_BATCH_SIZE);
        }

        while (viewportStart < this.loadedStartIndex && this.loadedStartIndex > 0) {
            this.loadedStartIndex = Math.max(0, this.loadedStartIndex - LOAD_BATCH_SIZE);
        }

        if (this.loadedEndIndex - this.loadedStartIndex > MAX_LOADED_RESULTS) {
            int preferredStart = Math.max(0, this.loadedEndIndex - MAX_LOADED_RESULTS);
            if (viewportStart < preferredStart) {
                preferredStart = viewportStart;
            }
            this.loadedStartIndex = preferredStart;
            this.loadedEndIndex = Math.min(total, this.loadedStartIndex + MAX_LOADED_RESULTS);

            if (viewportEndExclusive > this.loadedEndIndex) {
                this.loadedEndIndex = viewportEndExclusive;
                this.loadedStartIndex = Math.max(0, this.loadedEndIndex - MAX_LOADED_RESULTS);
            }
        }
    }

    private int getGridColumns() {
        return Math.max(1, getGridWidth() / GRID_CELL_SIZE);
    }

    private int getVisibleRows() {
        return Math.max(1, getGridHeight() / GRID_CELL_SIZE);
    }

    private int getMaxTopRow() {
        int totalRows = (int) Math.ceil(this.filteredItems.size() / (double) getGridColumns());
        return Math.max(0, totalRows - getVisibleRows());
    }

    private boolean isInsideGrid(double mouseX, double mouseY) {
        int x = getGridX();
        int y = getGridY();
        return mouseX >= x && mouseX < x + getGridWidth() && mouseY >= y && mouseY < y + getGridHeight();
    }

    private int getPanelX() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int getPanelY() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private int getGridX() {
        return getPanelX() + 15;
    }

    private int getGridY() {
        return getPanelY() + 54;
    }

    private int getGridWidth() {
        return PANEL_WIDTH - 30;
    }

    private int getGridHeight() {
        return PANEL_HEIGHT - 100;
    }
}

