package org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.m9mx.cactus.glowberry.util.waypointsv2.storage.WaypointsV2FileManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Scrollable grouped list of waypoints by category.
 */
public class WaypointsListScreen extends Screen {
    private static final int PANEL_WIDTH = 640;
    private static final int PANEL_HEIGHT = 420;
    private static final int PANEL_COLOR = 0xCC1A1A1A;
    private static final int PANEL_BORDER = 0xFFFFFFFF;

    private static final int CATEGORY_ROW_HEIGHT = 20;
    private static final int WAYPOINT_ROW_HEIGHT = 42;

    private final Screen previousScreen;

    private int panelX;
    private int panelY;

    private int scrollPixels = 0;

    private final Map<String, Boolean> collapsedCategories = new LinkedHashMap<>();
    private final List<RowEntry> rows = new ArrayList<>();

    public WaypointsListScreen(Screen previousScreen) {
        super(Component.literal("Waypoints"));
        this.previousScreen = previousScreen;
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_WIDTH) / 2;
        this.panelY = (this.height - PANEL_HEIGHT) / 2;

        int contentX = getContentX();
        int buttonsY = getContentY() + getContentHeight() + 8;
        int buttonWidth = (getContentWidth() - 10) / 2;

        this.addRenderableWidget(
                Button.builder(Component.literal("Create Waypoint"), btn -> this.minecraft.setScreen(WaypointConfigScreen.forCreate(this)))
                        .pos(contentX, buttonsY)
                        .width(buttonWidth)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Close"), btn -> this.onClose())
                        .pos(contentX + buttonWidth + 10, buttonsY)
                        .width(buttonWidth)
                        .build()
        );

        reloadRows();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x80000000);
        graphics.fill(this.panelX, this.panelY, this.panelX + PANEL_WIDTH, this.panelY + PANEL_HEIGHT, PANEL_COLOR);

        graphics.drawCenteredString(this.font, "Waypoints", this.width / 2, this.panelY + 10, 0xFFFFFFFF);

        int contentX = getContentX();
        int contentY = getContentY();
        int contentWidth = getContentWidth();
        int contentHeight = getContentHeight();

        graphics.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, 0x99212121);

        renderRows(graphics, contentX, contentY, contentWidth, contentHeight);

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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isInsideContent(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int delta = (int) Math.round(-scrollY * 16.0D);
        if (delta != 0) {
            this.scrollPixels = clamp(this.scrollPixels + delta, 0, getMaxScrollPixels());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInsideContent(event.x(), event.y())) {
            if (handleListClick(event.x(), event.y())) {
                return true;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.previousScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void reloadRows() {
        List<WaypointsV2FileManager.WaypointRecord> waypoints = WaypointsV2FileManager.loadWaypoints();

        Map<String, List<WaypointsV2FileManager.WaypointRecord>> grouped = new LinkedHashMap<>();
        for (WaypointsV2FileManager.WaypointRecord waypoint : waypoints) {
            String category = waypoint.category == null || waypoint.category.isBlank() ? "None" : waypoint.category;
            grouped.computeIfAbsent(category, key -> new ArrayList<>()).add(waypoint);
        }

        List<String> categories = new ArrayList<>(grouped.keySet());
        categories.sort(String.CASE_INSENSITIVE_ORDER);

        this.rows.clear();
        for (String category : categories) {
            this.collapsedCategories.putIfAbsent(category, false);
            this.rows.add(RowEntry.category(category));

            if (!this.collapsedCategories.getOrDefault(category, false)) {
                List<WaypointsV2FileManager.WaypointRecord> categoryWaypoints = grouped.get(category);
                categoryWaypoints.sort(Comparator.comparing(w -> safeLower(w.name)));
                for (WaypointsV2FileManager.WaypointRecord waypoint : categoryWaypoints) {
                    this.rows.add(RowEntry.waypoint(waypoint));
                }
            }
        }

        this.scrollPixels = clamp(this.scrollPixels, 0, getMaxScrollPixels());
    }

    private void renderRows(GuiGraphics graphics, int contentX, int contentY, int contentWidth, int contentHeight) {
        int y = contentY - this.scrollPixels;

        for (RowEntry row : this.rows) {
            int rowHeight = row.height();
            if (y + rowHeight < contentY) {
                y += rowHeight;
                continue;
            }
            if (y > contentY + contentHeight) {
                break;
            }

            if (row.isCategory()) {
                renderCategoryRow(graphics, row, contentX, y, contentWidth);
            } else {
                renderWaypointRow(graphics, row.waypoint, contentX, y, contentWidth);
            }

            y += rowHeight;
        }
    }

    private void renderCategoryRow(GuiGraphics graphics, RowEntry row, int x, int y, int width) {
        boolean collapsed = this.collapsedCategories.getOrDefault(row.category, false);
        graphics.fill(x + 1, y, x + width - 1, y + CATEGORY_ROW_HEIGHT, 0xAA2C2C2C);
        graphics.drawString(this.font, (collapsed ? "> " : "v ") + row.category + ":", x + 6, y + 6, 0xFFFFFFFF, false);
    }

    private void renderWaypointRow(GuiGraphics graphics, WaypointsV2FileManager.WaypointRecord waypoint, int x, int y, int width) {
        graphics.fill(x + 1, y, x + width - 1, y + WAYPOINT_ROW_HEIGHT, 0xAA1F1F1F);

        String scopeLabel = waypoint.local ? "L" : "G";
        int scopeColor = waypoint.local ? 0xFF6FD3FF : 0xFFFFD36F;
        graphics.fill(x + 4, y + 6, x + 16, y + 18, 0xFF2A2A2A);
        graphics.drawCenteredString(this.font, scopeLabel, x + 10, y + 8, scopeColor);

        int iconX = x + 20;
        int iconY = y + 3;
        graphics.renderItem(new ItemStack(resolveItem(waypoint.iconItemId)), iconX, iconY);

        int textX = iconX + 20;
        String name = waypoint.name == null || waypoint.name.isBlank() ? "(Unnamed)" : waypoint.name;
        
        // Apply waypoint color to the name
        int colorARGB = 0xFF000000 | (waypoint.color & 0xFFFFFF);
        graphics.drawString(this.font, name, textX, y + 4, colorARGB, false);

        String contextText;
        if (waypoint.local) {
            String rawContext = waypoint.localContext == null || waypoint.localContext.isBlank() ? "(unspecified)" : waypoint.localContext;
            contextText = "Local: " + rawContext;
        } else {
            contextText = "Global";
        }
        int contextMaxWidth = Math.max(80, width - 300);
        graphics.drawString(this.font, trimToWidth(contextText, contextMaxWidth), textX, y + 14, 0xFFCCCCCC, false);

        if ("LOCATION".equalsIgnoreCase(waypoint.type)) {
            graphics.drawString(this.font, "Location XYZ: " + waypoint.x + ", " + waypoint.y + ", " + waypoint.z, textX, y + 24, 0xFFAAAAAA, false);
        } else {
            int blockCount = waypoint.blockCoordinates != null ? waypoint.blockCoordinates.size() : 0;
            graphics.drawString(this.font, "Blocks: " + blockCount, textX, y + 24, 0xFFAAAAAA, false);
        }

        // Display full dimension names instead of abbreviations
        String dimensions = formatDimensions(waypoint.overworld, waypoint.nether, waypoint.end);
        int dimensionsX = x + width - 180;
        graphics.drawString(this.font, dimensions, dimensionsX, y + 6, 0xFFDDDDDD, false);

        int deleteX = x + width - 6 - 20;
        int stateX = deleteX - 2 - 30;
        int editX = stateX - 2 - 30;
        int buttonY = y + 5;

        graphics.fill(editX, buttonY, editX + 30, buttonY + 20, 0xFF2F3F6A);
        graphics.drawCenteredString(this.font, "Edit", editX + 15, buttonY + 6, 0xFFFFFFFF);

        boolean enabled = waypoint.enabled;
        graphics.fill(stateX, buttonY, stateX + 30, buttonY + 20, enabled ? 0xFF2F6A3A : 0xFF5A5A5A);
        graphics.drawCenteredString(this.font, enabled ? "On" : "Off", stateX + 15, buttonY + 6, 0xFFFFFFFF);

        graphics.fill(deleteX, buttonY, deleteX + 20, buttonY + 20, 0xFF6A2F2F);
        graphics.drawCenteredString(this.font, "X", deleteX + 10, buttonY + 6, 0xFFFFFFFF);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int allowed = Math.max(0, maxWidth - this.font.width(ellipsis));
        String value = text;
        while (!value.isEmpty() && this.font.width(value) > allowed) {
            value = value.substring(0, value.length() - 1);
        }
        return value + ellipsis;
    }

    private boolean handleListClick(double mouseX, double mouseY) {
        int contentX = getContentX();
        int contentY = getContentY();
        int contentWidth = getContentWidth();

        int y = contentY - this.scrollPixels;
        for (RowEntry row : this.rows) {
            int height = row.height();
            if (mouseY >= y && mouseY < y + height) {
                if (row.isCategory()) {
                    boolean current = this.collapsedCategories.getOrDefault(row.category, false);
                    this.collapsedCategories.put(row.category, !current);
                    reloadRows();
                    return true;
                }

                int deleteX = contentX + contentWidth - 6 - 20;
                int stateX = deleteX - 2 - 30;
                int editX = stateX - 2 - 30;
                int buttonY = y + 5;

                if (mouseX >= editX && mouseX <= editX + 30 && mouseY >= buttonY && mouseY <= buttonY + 20) {
                    this.minecraft.setScreen(WaypointConfigScreen.forEdit(this, row.waypoint.id));
                    return true;
                }

                if (mouseX >= stateX && mouseX <= stateX + 30 && mouseY >= buttonY && mouseY <= buttonY + 20) {
                    toggleWaypointEnabled(row.waypoint.id);
                    return true;
                }

                if (mouseX >= deleteX && mouseX <= deleteX + 20 && mouseY >= buttonY && mouseY <= buttonY + 20) {
                    openDeleteConfirm(row.waypoint.id, row.waypoint.name);
                    return true;
                }

                return false;
            }

            y += height;
        }

        return false;
    }

    private void openDeleteConfirm(String waypointId, String waypointName) {
        String displayName = waypointName == null || waypointName.isBlank() ? "this waypoint" : waypointName;
        this.minecraft.setScreen(new ConfirmScreen(this, Component.literal("Delete " + displayName + "?"), () -> {
            WaypointsV2FileManager.deleteWaypointById(waypointId);
            reloadRows();
        }));
    }

    private void toggleWaypointEnabled(String waypointId) {
        WaypointsV2FileManager.WaypointRecord waypoint = WaypointsV2FileManager.getWaypointById(waypointId);
        if (waypoint == null) {
            return;
        }
        waypoint.enabled = !waypoint.enabled;
        WaypointsV2FileManager.updateWaypoint(waypoint);
        reloadRows();
    }

    private Item resolveItem(String itemId) {
        if (itemId != null && !itemId.isBlank()) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (BuiltInRegistries.ITEM.getKey(item).toString().equalsIgnoreCase(itemId)) {
                    return item;
                }
            }
        }
        return Items.COMPASS;
    }

    private boolean isInsideContent(double mouseX, double mouseY) {
        int x = getContentX();
        int y = getContentY();
        return mouseX >= x && mouseX <= x + getContentWidth() && mouseY >= y && mouseY <= y + getContentHeight();
    }

    private int getMaxScrollPixels() {
        int totalHeight = 0;
        for (RowEntry row : this.rows) {
            totalHeight += row.height();
        }
        return Math.max(0, totalHeight - getContentHeight());
    }

    private int getContentX() {
        return this.panelX + 12;
    }

    private int getContentY() {
        return this.panelY + 30;
    }

    private int getContentWidth() {
        return PANEL_WIDTH - 24;
    }

    private int getContentHeight() {
        return PANEL_HEIGHT - 76;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String onOff(boolean enabled) {
        return enabled ? "On" : "Off";
    }

    private static String formatDimensions(boolean overworld, boolean nether, boolean end) {
        List<String> dims = new ArrayList<>();
        if (overworld) dims.add("Overworld");
        if (nether) dims.add("Nether");
        if (end) dims.add("End");
        return dims.isEmpty() ? "None" : String.join(", ", dims);
    }

    private static final class RowEntry {
        private final String category;
        private final WaypointsV2FileManager.WaypointRecord waypoint;

        private RowEntry(String category, WaypointsV2FileManager.WaypointRecord waypoint) {
            this.category = category;
            this.waypoint = waypoint;
        }

        static RowEntry category(String category) {
            return new RowEntry(category, null);
        }

        static RowEntry waypoint(WaypointsV2FileManager.WaypointRecord waypoint) {
            return new RowEntry(null, waypoint);
        }

        boolean isCategory() {
            return this.category != null;
        }

        int height() {
            return isCategory() ? CATEGORY_ROW_HEIGHT : WAYPOINT_ROW_HEIGHT;
        }
    }
}

