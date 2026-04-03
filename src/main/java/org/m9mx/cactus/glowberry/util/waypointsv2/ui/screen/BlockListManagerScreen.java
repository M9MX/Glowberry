package org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;
import org.m9mx.cactus.glowberry.util.ActionBarUtil;
import com.dwarslooper.cactus.client.util.game.ChatUtils;
import org.m9mx.cactus.glowberry.feature.modules.WaypointsV2Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Screen for managing a list of block coordinates.
 * Allows adding blocks by looking at them and pressing a keybind,
 * and deleting blocks from the list without confirmation.
 */
public class BlockListManagerScreen extends Screen {
    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int PANEL_WIDTH = 500;
    private static final int PANEL_HEIGHT = 400;
    private static final int LIST_TOP_OFFSET = 38;
    private static final int LIST_BOTTOM_OFFSET = 80;
    private static final int ROW_HEIGHT = 36;
    private static final int ROW_GAP = 5;
    private static final int ROW_STRIDE = ROW_HEIGHT + ROW_GAP;
    private static final int DELETE_WIDTH = 66;

    private final Screen previousScreen;
    private final List<BlockCoordinate> blockCoordinates;
    private final Consumer<List<BlockCoordinate>> onCloseSave;
    private int scrollPixels = 0;

    public static class BlockCoordinate {
        public int x;
        public int y;
        public int z;

        public BlockCoordinate(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockCoordinate that = (BlockCoordinate) o;
            return x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }

        @Override
        public String toString() {
            return String.format("X: %d, Y: %d, Z: %d", x, y, z);
        }
    }

    public BlockListManagerScreen(Screen previousScreen, List<BlockCoordinate> blockCoordinates) {
        this(previousScreen, blockCoordinates, null);
    }

    public BlockListManagerScreen(Screen previousScreen, List<BlockCoordinate> blockCoordinates, Consumer<List<BlockCoordinate>> onCloseSave) {
        super(Component.literal("Manage Blocks"));
        this.previousScreen = previousScreen;
        this.blockCoordinates = new ArrayList<>(blockCoordinates);
        this.onCloseSave = onCloseSave;
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Add button
        this.addRenderableWidget(
                Button.builder(Component.literal("Add Block"), btn -> startAddingMode())
                        .pos(panelX + 10, panelY + PANEL_HEIGHT - 45)
                        .width((PANEL_WIDTH - 30) / 2)
                        .build()
        );

        // Close button
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
                        .pos(panelX + 10 + (PANEL_WIDTH - 30) / 2 + 10, panelY + PANEL_HEIGHT - 45)
                        .width((PANEL_WIDTH - 30) / 2)
                        .build()
        );
    }

    private void startAddingMode() {
        WaypointsV2Module module = WaypointsV2Module.INSTANCE;
        if (module != null) {
            module.setBlockAddingMode(true, this);
        }

        String keyName = getAddBlockKeyName();
        String message = "§6Look at a block and press §e" + keyName + "§6 to add it. Press §eESC§6 to exit.";
        ActionBarUtil.sendActionBarMessageWithDuration(message, 200);

        // Close all screens so player can target blocks in-world.
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private String getAddBlockKeyName() {
        WaypointsV2Module module = WaypointsV2Module.INSTANCE;
        if (module != null) {
            try {
                Object keybind = module.AddBlockKeybind.get();
                if (keybind != null) {
                    return toShortKeyName(keybind.toString());
                }
            } catch (Exception ignored) {
                // Keep fallback below.
            }
        }
        return "N";
    }

    private String toShortKeyName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "N";
        }

        String value = raw.trim();
        String lower = value.toLowerCase(Locale.ROOT);

        int keyboardIndex = lower.indexOf("key.keyboard.");
        if (keyboardIndex >= 0) {
            String suffix = value.substring(keyboardIndex + "key.keyboard.".length());
            int end = 0;
            while (end < suffix.length()) {
                char ch = suffix.charAt(end);
                if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '.') {
                    end++;
                } else {
                    break;
                }
            }
            if (end > 0) {
                suffix = suffix.substring(0, end);
            }

            if (!suffix.isBlank()) {
                if (suffix.length() == 1) {
                    return suffix.toUpperCase(Locale.ROOT);
                }
                int dot = suffix.lastIndexOf('.');
                if (dot >= 0 && dot + 1 < suffix.length()) {
                    String tail = suffix.substring(dot + 1);
                    if (tail.length() == 1) {
                        return tail.toUpperCase(Locale.ROOT);
                    }
                    return tail.toUpperCase(Locale.ROOT);
                }
                return suffix.toUpperCase(Locale.ROOT);
            }
        }

        if (value.length() == 1) {
            return value.toUpperCase(Locale.ROOT);
        }

        return value;
    }

    private void pushChangesToParent() {
        if (this.onCloseSave != null) {
            this.onCloseSave.accept(getBlockCoordinates());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // Background
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);

        // Panel background
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, BACKGROUND_COLOR);

        // Panel border
        guiGraphics.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + 1, BORDER_COLOR);
        guiGraphics.fill(panelX - 1, panelY, panelX + 1, panelY + PANEL_HEIGHT, BORDER_COLOR);
        guiGraphics.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT, BORDER_COLOR);
        guiGraphics.fill(panelX - 1, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, BORDER_COLOR);

        // Title
        guiGraphics.drawCenteredString(this.font, "Manage Blocks", panelX + PANEL_WIDTH / 2, panelY + 15, 0xFFFFFF);

        // Status message
        guiGraphics.drawString(this.font, "Blocks: " + this.blockCoordinates.size(), panelX + 15, panelY + PANEL_HEIGHT - 70, 0xFFFFFF);

        int listX = getListX();
        int listY = getListY();
        int listWidth = getListWidth();
        int listHeight = getListHeight();
        guiGraphics.fill(listX, listY, listX + listWidth, listY + listHeight, 0x55202020);
        guiGraphics.renderOutline(listX, listY, listWidth, listHeight, 0xFF555555);

        int y = listY - this.scrollPixels;
        for (int i = 0; i < this.blockCoordinates.size(); i++) {
            BlockCoordinate block = this.blockCoordinates.get(i);
            if (y + ROW_HEIGHT < listY) {
                y += ROW_STRIDE;
                continue;
            }
            if (y > listY + listHeight) {
                break;
            }

            renderBlockRow(guiGraphics, block, i, y, listX, listWidth);
            y += ROW_STRIDE;
        }

        // Render all widgets
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderBlockRow(GuiGraphics guiGraphics, BlockCoordinate coordinate, int index, int y, int x, int width) {
        guiGraphics.fill(x + 2, y, x + width - 2, y + ROW_HEIGHT, 0xAA232323);
        guiGraphics.renderOutline(x + 2, y, width - 4, ROW_HEIGHT, 0xFF3B3B3B);

        BlockDisplay display = resolveBlockDisplay(coordinate);
        guiGraphics.fill(x + 8, y + 8, x + 28, y + 28, 0xAA1A1A1A);
        guiGraphics.renderOutline(x + 8, y + 8, 20, 20, 0xFF5A5A5A);
        guiGraphics.renderItem(new ItemStack(display.iconItem), x + 10, y + 10);

        guiGraphics.drawString(this.font, display.blockName, x + 34, y + 7, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, coordinate.toString(), x + 34, y + 19, 0xFFB5B5B5, false);

        int deleteX = x + width - DELETE_WIDTH - 8;
        int deleteY = y + 8;
        guiGraphics.fill(deleteX, deleteY, deleteX + DELETE_WIDTH, deleteY + 20, 0xFF6A2F2F);
        guiGraphics.renderOutline(deleteX, deleteY, DELETE_WIDTH, 20, 0xFFD17A7A);
        guiGraphics.drawCenteredString(this.font, "Delete", deleteX + (DELETE_WIDTH / 2), deleteY + 6, 0xFFFFFFFF);
    }

    private BlockDisplay resolveBlockDisplay(BlockCoordinate coordinate) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return new BlockDisplay("Unknown Block", Items.BARRIER);
        }

        BlockPos pos = new BlockPos(coordinate.x, coordinate.y, coordinate.z);
        if (!this.minecraft.level.hasChunkAt(pos)) {
            return new BlockDisplay("Unloaded Block", Items.BARRIER);
        }

        BlockState state = this.minecraft.level.getBlockState(pos);
        Block block = state.getBlock();
        Item icon = block.asItem();
        if (icon == null || icon == Items.AIR) {
            icon = Items.BARRIER;
        }

        String name = block.getName().getString();
        if (name == null || name.isBlank()) {
            name = "Unknown Block";
        }
        return new BlockDisplay(name, icon);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInsideList(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int delta = (int) Math.round(-scrollY * 20.0D);
        if (delta != 0) {
            this.scrollPixels = clamp(this.scrollPixels + delta, 0, getMaxScrollPixels());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInsideList(event.x(), event.y())) {
            if (handleRowClick(event.x(), event.y())) {
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    private boolean handleRowClick(double mouseX, double mouseY) {
        int listX = getListX();
        int listY = getListY();
        int listWidth = getListWidth();
        int y = listY - this.scrollPixels;

        for (int i = 0; i < this.blockCoordinates.size(); i++) {
            if (mouseY >= y && mouseY <= y + ROW_HEIGHT) {
                int deleteX = listX + listWidth - DELETE_WIDTH - 8;
                int deleteY = y + 8;
                if (mouseX >= deleteX && mouseX <= deleteX + DELETE_WIDTH && mouseY >= deleteY && mouseY <= deleteY + 20) {
                    removeBlockAt(i);
                    return true;
                }
            }
            y += ROW_STRIDE;
        }

        return false;
    }

    private int getListX() {
        return (this.width - PANEL_WIDTH) / 2 + 10;
    }

    private int getListY() {
        return (this.height - PANEL_HEIGHT) / 2 + LIST_TOP_OFFSET;
    }

    private int getListWidth() {
        return PANEL_WIDTH - 20;
    }

    private int getListHeight() {
        return PANEL_HEIGHT - LIST_TOP_OFFSET - LIST_BOTTOM_OFFSET;
    }

    private boolean isInsideList(double mouseX, double mouseY) {
        int x = getListX();
        int y = getListY();
        return mouseX >= x && mouseX <= x + getListWidth() && mouseY >= y && mouseY <= y + getListHeight();
    }

    private int getMaxScrollPixels() {
        int totalHeight = Math.max(0, (this.blockCoordinates.size() * ROW_STRIDE) - ROW_GAP);
        return Math.max(0, totalHeight - getListHeight());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void onClose() {
        pushChangesToParent();

        // Disable block adding mode
        WaypointsV2Module module = WaypointsV2Module.INSTANCE;
        if (module != null) {
            module.setBlockAddingMode(false, null);
        }

        // Close all screens and return to previous
        if (this.previousScreen != null) {
            this.minecraft.setScreen(this.previousScreen);
        } else {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    public void addBlock(int x, int y, int z) {
        BlockCoordinate newBlock = new BlockCoordinate(x, y, z);
        // Check for duplicates
        if (!this.blockCoordinates.contains(newBlock)) {
            this.blockCoordinates.add(newBlock);
            this.scrollPixels = clamp(this.scrollPixels, 0, getMaxScrollPixels());
            pushChangesToParent();
            String message = "§aBlock added at " + newBlock;
            ChatUtils.info(message);
        } else {
            String message = "§cBlock already exists at this location";
            ChatUtils.info(message);
        }
    }

    public void removeBlock(BlockCoordinate block) {
        this.blockCoordinates.remove(block);
        this.scrollPixels = clamp(this.scrollPixels, 0, getMaxScrollPixels());
        pushChangesToParent();
    }

    private void removeBlockAt(int index) {
        if (index < 0 || index >= this.blockCoordinates.size()) {
            return;
        }
        this.blockCoordinates.remove(index);
        this.scrollPixels = clamp(this.scrollPixels, 0, getMaxScrollPixels());
        pushChangesToParent();
    }

    public List<BlockCoordinate> getBlockCoordinates() {
        return new ArrayList<>(this.blockCoordinates);
    }

    private static final class BlockDisplay {
        private final String blockName;
        private final Item iconItem;

        private BlockDisplay(String blockName, Item iconItem) {
            this.blockName = blockName;
            this.iconItem = iconItem;
        }
    }
}
