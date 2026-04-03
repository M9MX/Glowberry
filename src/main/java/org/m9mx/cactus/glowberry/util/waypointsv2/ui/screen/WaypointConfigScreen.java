package org.m9mx.cactus.glowberry.util.waypointsv2.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.m9mx.cactus.glowberry.util.waypointsv2.storage.WaypointsV2FileManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for adding a new waypoint.
 * Provides UI for configuring waypoint name, type, display options, and dimensions.
 */
public class WaypointConfigScreen extends Screen {
    public enum Mode {
        CREATE,
        EDIT
    }

    private static final int BACKGROUND_COLOR = 0x66000000; // More transparent black
    private static final int BORDER_COLOR = 0xFFFFFFFF; // White border
    private static final int DIVIDER_COLOR = 0xFF888888; // Gray divider
    private static final int BASE_BOX_WIDTH = 450;
    private static final int BASE_BOX_HEIGHT = 420;

    private final Screen previousScreen;
    private final Mode mode;
    private String editingWaypointId;

    // Box dimensions
    private int boxX;
    private int boxY;
    private int boxWidth = BASE_BOX_WIDTH;
    private int boxHeight = BASE_BOX_HEIGHT;
    private int minBoxY;
    private int maxBoxY;

    // ===== UI Components =====
    private EditBox waypointNameBox;
    private Button displayModeButton;
    private Button iconSelectorButton;
    private Button iconPreviewButton;
    private Button colorButton;
    private Button colorPreviewButton;
    private Button waypointTypeButton;
    private EditBox coordXBox = null;
    private EditBox coordYBox = null;
    private EditBox coordZBox = null;
    private Button blockListButton;
    private Button scopeToggle;
    private Button overworldToggle;
    private Button netherToggle;
    private Button endToggle;
    private CategoryObjectSelectionList categorySelector;
    private Button addCategoryButton;
    private Button removeCategoryButton;
    private Button cancelButton;
    private Button createButton;
    private int categoryBoxX;
    private int categoryBoxY;
    private int categoryBoxWidth;
    private int categoryBoxHeight;

    // Runtime notice text (used for block-list add-mode warning)
    private String infoMessage = "";
    private int infoMessageTicks = 0;
    private List<String> cachedCategories = List.of("None");

    // ===== State =====
    private DisplayMode selectedDisplayMode = DisplayMode.TEXT_ICON;
    private WaypointType selectedWaypointType = WaypointType.LOCATION;
    private Item selectedIconItem = Items.COMPASS;
    private int selectedColor = 0xFFFFFF; // White
    private boolean localWaypoint = true;
    private boolean[] dimensions = { true, false, false }; // Overworld, Nether, End (only Overworld on by default)
    private String selectedCategory = "None";
    private String cachedWaypointName = "";
    private String cachedCoordX = "";
    private String cachedCoordY = "";
    private String cachedCoordZ = "";
    private List<BlockListManagerScreen.BlockCoordinate> blockCoordinates = new ArrayList<>();
    private boolean suppressAutoSave = true;

    public enum DisplayMode {
        TEXT_ICON("Text + Icon"),
        TEXT("Text"),
        ICON("Icon");

        public final String displayName;

        DisplayMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum WaypointType {
        LOCATION("Location"),
        BLOCKS("Blocks");

        public final String displayName;

        WaypointType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public WaypointConfigScreen(Screen previousScreen) {
        this(previousScreen, Mode.CREATE, null);
    }

    public static WaypointConfigScreen forCreate(Screen previousScreen) {
        return new WaypointConfigScreen(previousScreen, Mode.CREATE, null);
    }

    public static WaypointConfigScreen forEdit(Screen previousScreen, String waypointId) {
        return new WaypointConfigScreen(previousScreen, Mode.EDIT, waypointId);
    }

    private WaypointConfigScreen(Screen previousScreen, Mode mode, String editingWaypointId) {
        super(Component.empty());
        this.previousScreen = previousScreen;
        this.mode = mode;
        this.editingWaypointId = editingWaypointId;
    }

    @Override
    protected void init() {
        this.suppressAutoSave = true;
        this.cachedCategories = WaypointsV2FileManager.loadCategories();
        loadWaypointForEditMode();
        applyCreateModeDimensionDefaults();
        int[] suggestedCoords = getPlayerCoordsOrNull();

        // Keep the box at a fixed design size regardless of GUI scale.
        int horizontalMargin = 12;
        int verticalMargin = 12;
        this.boxWidth = BASE_BOX_WIDTH;
        this.boxHeight = BASE_BOX_HEIGHT;

        // Center when it fits; otherwise clamp to keep top/left visible.
        if (this.width >= this.boxWidth + horizontalMargin * 2) {
            this.boxX = (this.width - this.boxWidth) / 2;
        } else {
            this.boxX = Math.max(0, this.width - this.boxWidth);
        }

        if (this.height >= this.boxHeight + verticalMargin * 2) {
            this.boxY = (this.height - this.boxHeight) / 2;
            this.minBoxY = this.boxY;
            this.maxBoxY = this.boxY;
        } else {
            // Allow scrolling between full top visibility and full bottom visibility.
            this.minBoxY = this.height - this.boxHeight;
            this.maxBoxY = 0;
            this.boxY = this.maxBoxY;
        }

        int currentY = this.boxY + 34;
        int contentX = this.boxX + 15;
        int contentWidth = this.boxWidth - 30;

        // ===== WAYPOINT NAME =====
        this.waypointNameBox = this.addRenderableWidget(new EditBox(this.font, contentX, currentY, 
            contentWidth, 20, Component.literal("Waypoint Name")));
        this.waypointNameBox.setMaxLength(50);
        this.waypointNameBox.setFilter(text -> text.matches("[a-zA-Z0-9_ -]*"));
        this.waypointNameBox.setHint(Component.literal("Enter waypoint name..."));
        this.waypointNameBox.setValue(this.cachedWaypointName);
        this.waypointNameBox.setResponder(value -> autoSaveIfEditMode());

        currentY += 35;

        // ===== LEFT & RIGHT SECTIONS =====
        int leftX = contentX;
        int rightX = contentX + (contentWidth / 2) + 5;
        int sectionWidth = (contentWidth / 2) - 10;

        currentY += 5;
        int leftY = currentY;
        int rightY = currentY;

        // --- LEFT SECTION: DISPLAY MODE ---
        this.displayModeButton = this.addRenderableWidget(
            Button.builder(Component.literal("◀ " + selectedDisplayMode.displayName + " ▶"), btn -> {
                int ordinal = (selectedDisplayMode.ordinal() + 1) % DisplayMode.values().length;
                this.selectedDisplayMode = DisplayMode.values()[ordinal];
                btn.setMessage(Component.literal("◀ " + selectedDisplayMode.displayName + " ▶"));
                updateIconSelectorVisibility();
                autoSaveIfEditMode();
            })
            .pos(leftX, leftY)
            .width(sectionWidth)
            .build()
        );
        this.displayModeButton.setTooltip(Tooltip.create(Component.literal("Switch between Text+Icon, Text-only, and Icon-only.")));

        leftY += 34;

        // Icon Selector
        int iconPreviewGap = 5;
        int iconPreviewWidth = 20;
        int iconButtonWidth = sectionWidth - (iconPreviewGap + iconPreviewWidth);

        this.iconSelectorButton = this.addRenderableWidget(
            Button.builder(Component.literal(getIconButtonText()), btn -> openIconChooser())
            .pos(leftX, leftY)
            .width(iconButtonWidth)
            .build()
        );
        this.iconSelectorButton.setTooltip(Tooltip.create(Component.literal("Pick the icon shown for this waypoint.")));
        this.iconSelectorButton.visible = selectedDisplayMode != DisplayMode.TEXT;

        this.iconPreviewButton = this.addRenderableWidget(
            Button.builder(Component.empty(), btn -> openIconChooser())
            .pos(leftX + iconButtonWidth + iconPreviewGap, leftY)
            .width(iconPreviewWidth)
            .build()
        );
        this.iconPreviewButton.setTooltip(Tooltip.create(Component.literal("Selected icon preview.")));
        this.iconPreviewButton.visible = selectedDisplayMode != DisplayMode.TEXT;

        leftY += 34;

        // Color Selector
        int colorPreviewGap = 5;
        int colorPreviewWidth = 20;
        int colorButtonWidth = sectionWidth - (colorPreviewGap + colorPreviewWidth);

        this.colorButton = this.addRenderableWidget(
            Button.builder(Component.literal(getColorButtonText()), btn -> openColorPicker())
            .pos(leftX, leftY)
            .width(colorButtonWidth)
            .build()
        );
        this.colorButton.setTooltip(Tooltip.create(Component.literal("Set waypoint color.")));

        this.colorPreviewButton = this.addRenderableWidget(
            Button.builder(Component.empty(), btn -> openColorPicker())
            .pos(leftX + colorButtonWidth + colorPreviewGap, leftY)
            .width(colorPreviewWidth)
            .build()
        );
        this.colorPreviewButton.setTooltip(Tooltip.create(Component.literal("Selected color preview.")));
        leftY += 28;

        // --- RIGHT SECTION: WAYPOINT TYPE & COORDINATES ---
        this.waypointTypeButton = this.addRenderableWidget(
            Button.builder(Component.literal("◀ " + selectedWaypointType.displayName + " ▶"), btn -> {
                this.selectedWaypointType = selectedWaypointType == WaypointType.LOCATION ? WaypointType.BLOCKS : WaypointType.LOCATION;
                btn.setMessage(Component.literal("◀ " + selectedWaypointType.displayName + " ▶"));
                updateWaypointTypeVisibility();
                autoSaveIfEditMode();
            })
            .pos(rightX, rightY)
            .width(sectionWidth)
            .build()
        );
        this.waypointTypeButton.setTooltip(Tooltip.create(Component.literal("Location uses XYZ. Blocks uses a block filter list.")));

        rightY += 34;
        int blockListRowY = rightY;

        // Coordinates on one line or Block List
        if (selectedWaypointType == WaypointType.LOCATION) {
            int coordWidth = (sectionWidth - 45) / 3;
            // Keep coordinates on the same row as the left-side Icon button.
            int coordRowY = blockListRowY;

            // X coordinate
            this.coordXBox = this.addRenderableWidget(
                new EditBox(this.font, rightX + 12, coordRowY, coordWidth, 18, Component.literal("X"))
            );
            this.coordXBox.setFilter(text -> text.matches("-?[0-9]*"));
            this.coordXBox.setMaxLength(6);
            this.coordXBox.setValue(this.cachedCoordX);
            this.coordXBox.setResponder(value -> autoSaveIfEditMode());
            if (suggestedCoords != null) {
                this.coordXBox.setHint(Component.literal(String.valueOf(suggestedCoords[0])));
            }

            // Y coordinate
            this.coordYBox = this.addRenderableWidget(
                new EditBox(this.font, rightX + 12 + coordWidth + 15, coordRowY, coordWidth, 18, Component.literal("Y"))
            );
            this.coordYBox.setFilter(text -> text.matches("-?[0-9]*"));
            this.coordYBox.setMaxLength(6);
            this.coordYBox.setValue(this.cachedCoordY);
            this.coordYBox.setResponder(value -> autoSaveIfEditMode());
            if (suggestedCoords != null) {
                this.coordYBox.setHint(Component.literal(String.valueOf(suggestedCoords[1])));
            }

            // Z coordinate
            this.coordZBox = this.addRenderableWidget(
                new EditBox(this.font, rightX + 12 + (coordWidth * 2) + 30, coordRowY, coordWidth, 18, Component.literal("Z"))
            );
            this.coordZBox.setFilter(text -> text.matches("-?[0-9]*"));
            this.coordZBox.setMaxLength(6);
            this.coordZBox.setValue(this.cachedCoordZ);
            this.coordZBox.setResponder(value -> autoSaveIfEditMode());
            if (suggestedCoords != null) {
                this.coordZBox.setHint(Component.literal(String.valueOf(suggestedCoords[2])));
            }

            rightY += 29;
        }
        
        // Block List button - always created but hidden initially
        this.blockListButton = this.addRenderableWidget(
            Button.builder(Component.literal("Block List"), btn -> {
                if (isEditMode()) {
                    openBlockListEditor();
                    return;
                }

                this.infoMessage = "Cannot open Block List while creating. Save and edit the waypoint.";
                this.infoMessageTicks = 80;
            })
            // Keep Block List button aligned with the left-side Choose Icon button row.
            .pos(rightX, blockListRowY)
            .width(sectionWidth)
            .build()
        );
        this.blockListButton.setTooltip(Tooltip.create(Component.literal(
            isEditMode()
                ? "Open Block List editor for this waypoint."
                : "Available in edit mode after saving this waypoint."
        )));
        this.blockListButton.visible = selectedWaypointType == WaypointType.BLOCKS;
        rightY += 28;

        // Move to next section (below both columns)
        currentY = Math.max(leftY, rightY) + 15;

        // ===== SCOPE (LOCAL/GLOBAL) =====
        this.scopeToggle = this.addRenderableWidget(
            Button.builder(Component.literal(getScopeText()), btn -> {
                this.localWaypoint = !this.localWaypoint;
                btn.setMessage(Component.literal(getScopeText()));
                autoSaveIfEditMode();
            })
            .pos(contentX, currentY)
            .width(contentWidth)
            .build()
        );
        this.scopeToggle.setTooltip(Tooltip.create(Component.literal("Local: tied to current world/server. Global: shared everywhere.")));

        currentY += 50;

        // ===== DIMENSIONS =====
        this.overworldToggle = this.addRenderableWidget(
            Button.builder(Component.literal(getDimensionText(0)), btn -> {
                dimensions[0] = !dimensions[0];
                btn.setMessage(Component.literal(getDimensionText(0)));
                autoSaveIfEditMode();
            })
            .pos(contentX, currentY)
            .width(contentWidth)
            .build()
        );
        this.overworldToggle.setTooltip(Tooltip.create(Component.literal("Toggle Overworld visibility.")));

        currentY += 25;

        this.netherToggle = this.addRenderableWidget(
            Button.builder(Component.literal(getDimensionText(1)), btn -> {
                dimensions[1] = !dimensions[1];
                btn.setMessage(Component.literal(getDimensionText(1)));
                autoSaveIfEditMode();
            })
            .pos(contentX, currentY)
            .width(contentWidth)
            .build()
        );
        this.netherToggle.setTooltip(Tooltip.create(Component.literal("Toggle Nether visibility.")));

        currentY += 25;

        this.endToggle = this.addRenderableWidget(
            Button.builder(Component.literal(getDimensionText(2)), btn -> {
                dimensions[2] = !dimensions[2];
                btn.setMessage(Component.literal(getDimensionText(2)));
                autoSaveIfEditMode();
            })
            .pos(contentX, currentY)
            .width(contentWidth)
            .build()
        );
        this.endToggle.setTooltip(Tooltip.create(Component.literal("Toggle End visibility.")));

        // Add a little more separation after End so category label doesn't overlap.
        currentY += 40;

        // ===== CATEGORY SELECTOR (custom boxed list with scrolling) =====
        this.categoryBoxX = contentX;
        this.categoryBoxY = currentY;
        this.categoryBoxWidth = contentWidth;
        this.categoryBoxHeight = 64;

        this.categorySelector = this.addRenderableWidget(
                new CategoryObjectSelectionList(this.minecraft, this.categoryBoxWidth, this.categoryBoxHeight, this.categoryBoxY, 18, this.categoryBoxX)
        );
        this.categorySelector.updateSizeAndPosition(this.categoryBoxWidth, this.categoryBoxHeight, this.categoryBoxX, this.categoryBoxY);
        this.categorySelector.setCategories(this.cachedCategories, this.selectedCategory);

        currentY += 70;

        // Add/Remove Category buttons (fixed width)
        int categoryButtonGap = 5;
        int addCategoryWidth = 395;
        int removeCategoryWidth = 20;

        this.addCategoryButton = this.addRenderableWidget(
            Button.builder(Component.literal("Add New Category"), btn -> {
                cacheInputValues();
                this.minecraft.setScreen(
                    new AddStringScreen(
                            this,
                            Component.literal("Category Name"),
                            Component.literal("Category name..."),
                            50,
                            this::onCategoryCreated
                    )
                );
            })
            .pos(contentX, currentY)
            .width(addCategoryWidth)
            .build()
        );
        this.addCategoryButton.setTooltip(Tooltip.create(Component.literal("Create a new category.")));

        this.removeCategoryButton = this.addRenderableWidget(
            Button.builder(Component.literal("♻"), btn -> openRemoveCategoryScreen())
            .pos(contentX + addCategoryWidth + categoryButtonGap, currentY)
            .width(removeCategoryWidth)
            .build()
        );
        this.removeCategoryButton.setTooltip(Tooltip.create(Component.literal("Remove an existing category.")));

        currentY += 30;

        // ===== ACTION BUTTONS =====
        int buttonWidth = (contentWidth - 10) / 2;

        if (isEditMode()) {
            this.cancelButton = this.addRenderableWidget(
                Button.builder(Component.literal("Delete"), btn -> openDeleteConfirmScreen())
                    .pos(contentX, currentY)
                    .width(buttonWidth)
                    .build()
            );

            this.createButton = this.addRenderableWidget(
                Button.builder(Component.literal("Save"), btn -> this.onClose())
                    .pos(contentX + buttonWidth + 10, currentY)
                    .width(buttonWidth)
                    .build()
            );
        } else {
            this.cancelButton = this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                    .pos(contentX, currentY)
                    .width(buttonWidth)
                    .build()
            );

            this.createButton = this.addRenderableWidget(
                Button.builder(Component.literal("Create"), btn -> createWaypoint())
                    .pos(contentX + buttonWidth + 10, currentY)
                    .width(buttonWidth)
                    .build()
            );
        }

        // Ensure initial visibility is consistent.
        updateIconSelectorVisibility();
        updateWaypointTypeVisibility();
        this.suppressAutoSave = false;
    }

    private String getDimensionText(int index) {
        String[] names = { "Overworld", "Nether", "End" };
        return names[index] + ": " + (dimensions[index] ? "On" : "Off");
    }

    private String getScopeText() {
        return "Waypoint Scope: " + (this.localWaypoint ? "Local" : "Global");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw semi-transparent background behind box
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        // Draw box
        graphics.fill(this.boxX, this.boxY, this.boxX + this.boxWidth, this.boxY + this.boxHeight, BACKGROUND_COLOR);

        // Draw a large, centered title at the top.
        int titleX = this.boxX + this.boxWidth / 2;
        int titleY = this.boxY + 8;
        Component title = Component.literal(isEditMode() ? "Edit Waypoint" : "Create Waypoint");
        float titleScale = 1.45F;
        graphics.pose().pushMatrix();
        graphics.pose().scale(titleScale, titleScale);
        graphics.drawCenteredString(this.font, title, (int) (titleX / titleScale), (int) (titleY / titleScale), 0xFFFFFF);
        graphics.pose().popMatrix();

        // Divider under the title.
        graphics.fill(this.boxX + 10, this.boxY + 28, this.boxX + this.boxWidth - 10, this.boxY + 29, DIVIDER_COLOR);

        // Draw text labels (anchor to real widget positions to avoid overlap)
        int contentX = this.boxX + 15;

        if (this.categorySelector != null) {
            String category = this.categorySelector.getSelectedCategoryOrDefault("None");
            if (!category.equals(this.selectedCategory)) {
                this.selectedCategory = category;
                autoSaveIfEditMode();
            }
        }

        // Name label
        graphics.drawString(this.font, "Name:", this.waypointNameBox.getX(), this.waypointNameBox.getY() - this.font.lineHeight - 6, 0xFFFFFFFF, true);

        // Left section labels
        graphics.drawString(this.font, "Display Icon Mode:", this.displayModeButton.getX(), this.displayModeButton.getY() - this.font.lineHeight - 6, 0xFFFFFFFF, true);
        if (this.iconSelectorButton != null && this.iconSelectorButton.visible) {
            graphics.drawString(this.font, "Icon:", this.iconSelectorButton.getX(), this.iconSelectorButton.getY() - this.font.lineHeight, 0xFFFFFFFF, true);
        }
        graphics.drawString(this.font, "Color:", this.colorButton.getX(), this.colorButton.getY() - this.font.lineHeight, 0xFFFFFFFF, true);

        // Right section labels
        graphics.drawString(this.font, "Waypoint Type:", this.waypointTypeButton.getX(), this.waypointTypeButton.getY() - this.font.lineHeight - 6, 0xFFFFFFFF, true);
        if (selectedWaypointType == WaypointType.LOCATION && this.coordXBox != null && this.coordYBox != null && this.coordZBox != null) {
            // Align Coordinates text with the left-side Icon text row.
            graphics.drawString(this.font, "Coordinates:", this.coordXBox.getX() - 12, this.coordXBox.getY() - this.font.lineHeight, 0xFFFFFFFF, true);
            // Keep X/Y/Z labels on the same row as the edit boxes.
            int coordLabelY = this.coordXBox.getY() + (this.coordXBox.getHeight() - this.font.lineHeight) / 2;
            graphics.drawString(this.font, "X:", this.coordXBox.getX() - 10, coordLabelY, 0xFFFFFFFF, true);
            graphics.drawString(this.font, "Y:", this.coordYBox.getX() - 10, coordLabelY, 0xFFFFFFFF, true);
            graphics.drawString(this.font, "Z:", this.coordZBox.getX() - 10, coordLabelY, 0xFFFFFFFF, true);
        } else if (this.blockListButton != null && this.blockListButton.visible) {
            graphics.drawString(this.font, "Block List:", this.blockListButton.getX(), this.blockListButton.getY() - this.font.lineHeight, 0xFFFFFFFF, true);
        }

        // Scope + dimensions + category labels anchored to their widgets.
        if (this.scopeToggle != null) {
            graphics.drawString(this.font, "Scope:", this.scopeToggle.getX(), this.scopeToggle.getY() - this.font.lineHeight - 2, 0xFFFFFFFF, true);
        }
        graphics.drawString(this.font, "Dimensions:", this.overworldToggle.getX(), this.overworldToggle.getY() - this.font.lineHeight - 2, 0xFFFFFFFF, true);
        if (this.categorySelector != null) {
            graphics.drawString(this.font, "Category (Selected: " + this.selectedCategory + "):", this.categorySelector.getX(), this.categorySelector.getY() - this.font.lineHeight - 2, 0xFFFFFFFF, true);
        }

        // Block-list warning / info message
        if (this.infoMessageTicks > 0 && !this.infoMessage.isEmpty()) {
            int mx = this.boxX + this.boxWidth / 2;
            int my = this.boxY + this.boxHeight - 18;
            graphics.drawCenteredString(this.font, this.infoMessage, mx, my, 0xFFFF66);
            this.infoMessageTicks--;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        renderIconPreviewSwatch(graphics);
        renderColorPreviewSwatch(graphics);
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
        // Prioritize category list scrolling when the cursor is over it.
        if (this.categorySelector != null && this.categorySelector.isMouseOver(mouseX, mouseY)) {
            this.categorySelector.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            return true;
        }

        // If the panel is taller than the screen, wheel scroll moves the whole panel up/down.
        if (this.maxBoxY > this.minBoxY) {
            int delta = (int) Math.round(scrollY * 20.0D);
            if (delta != 0) {
                shiftPanelY(clamp(this.boxY + delta, this.minBoxY, this.maxBoxY) - this.boxY);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return super.mouseClicked(event, doubled);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(previousScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void updateIconSelectorVisibility() {
        if (this.iconSelectorButton != null) {
            this.iconSelectorButton.visible = selectedDisplayMode != DisplayMode.TEXT;
        }
        if (this.iconPreviewButton != null) {
            this.iconPreviewButton.visible = selectedDisplayMode != DisplayMode.TEXT;
        }
    }

    private void openIconChooser() {
        cacheInputValues();
        this.minecraft.setScreen(new IconChooserScreen(this, this.selectedIconItem, this::onIconSelected));
    }

    private void onIconSelected(Item item) {
        if (item == null) {
            return;
        }

        this.selectedIconItem = item;
        if (this.iconSelectorButton != null) {
            this.iconSelectorButton.setMessage(Component.literal(getIconButtonText()));
        }
        autoSaveIfEditMode();
    }

    private void renderIconPreviewSwatch(GuiGraphics graphics) {
        if (this.iconPreviewButton == null || this.selectedIconItem == null || !this.iconPreviewButton.visible) {
            return;
        }

        int x = this.iconPreviewButton.getX();
        int y = this.iconPreviewButton.getY();
        int w = this.iconPreviewButton.getWidth();
        int h = this.iconPreviewButton.getHeight();
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF222222);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF111111);
        graphics.renderItem(new ItemStack(this.selectedIconItem), x + 2, y + 2);
    }

    private String getIconButtonText() {
        if (this.selectedIconItem == null) {
            return "Choose Icon";
        }

        String name = this.selectedIconItem.getDefaultInstance().getHoverName().getString();
        if (name.length() > 14) {
            name = name.substring(0, 14) + "...";
        }
        return "Icon: " + name;
    }

    private void openColorPicker() {
        cacheInputValues();
        this.minecraft.setScreen(new ColorPickerScreen(this, this.selectedColor, this::onColorSelected));
    }

    private void onColorSelected(int color) {
        this.selectedColor = color & 0xFFFFFF;
        if (this.colorButton != null) {
            this.colorButton.setMessage(Component.literal(getColorButtonText()));
        }
        autoSaveIfEditMode();
    }

    private void renderColorPreviewSwatch(GuiGraphics graphics) {
        if (this.colorPreviewButton == null) {
            return;
        }

        int x = this.colorPreviewButton.getX();
        int y = this.colorPreviewButton.getY();
        int w = this.colorPreviewButton.getWidth();
        int h = this.colorPreviewButton.getHeight();
        graphics.fill(x, y, x + w, y + h, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF222222);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF000000 | (this.selectedColor & 0xFFFFFF));
    }

    private String getColorButtonText() {
        return "Select Color #" + String.format("%06X", this.selectedColor & 0xFFFFFF);
    }

    private void updateWaypointTypeVisibility() {
        // Hide all coordinate boxes
        if (this.coordXBox != null) this.coordXBox.visible = selectedWaypointType == WaypointType.LOCATION;
        if (this.coordYBox != null) this.coordYBox.visible = selectedWaypointType == WaypointType.LOCATION;
        if (this.coordZBox != null) this.coordZBox.visible = selectedWaypointType == WaypointType.LOCATION;
        
        // Show block list button only when type is BLOCKS
        if (this.blockListButton != null) this.blockListButton.visible = selectedWaypointType == WaypointType.BLOCKS;
    }

    private void onCategoryCreated(String rawCategory) {
        String sanitized = WaypointsV2FileManager.sanitizeName(rawCategory);
        if (sanitized.isEmpty()) {
            this.infoMessage = "Category name is invalid.";
            this.infoMessageTicks = 80;
            return;
        }

        boolean added = WaypointsV2FileManager.addCategory(sanitized);
        this.cachedCategories = WaypointsV2FileManager.loadCategories();
        if (this.categorySelector != null) {
            this.categorySelector.setCategories(this.cachedCategories, sanitized);
            this.selectedCategory = this.categorySelector.getSelectedCategoryOrDefault("None");
        }

        if (!added) {
            this.infoMessage = "Category already exists.";
            this.infoMessageTicks = 80;
        }
        autoSaveIfEditMode();
    }

    private void openRemoveCategoryScreen() {
        cacheInputValues();
        this.cachedCategories = WaypointsV2FileManager.loadCategories();
        this.minecraft.setScreen(
                new RemoveStringScreen(
                        this,
                        Component.literal("Select Category to Remove"),
                        this.cachedCategories,
                        this::onCategoryRemoved
                )
        );
    }

    private void onCategoryRemoved(String rawCategory) {
        String sanitized = WaypointsV2FileManager.sanitizeName(rawCategory);
        if (sanitized.isEmpty() || sanitized.equalsIgnoreCase("None")) {
            this.infoMessage = "Cannot remove category None.";
            this.infoMessageTicks = 80;
            return;
        }

        boolean removed = WaypointsV2FileManager.removeCategory(sanitized);
        this.cachedCategories = WaypointsV2FileManager.loadCategories();
        if (this.categorySelector != null) {
            this.categorySelector.setCategories(this.cachedCategories, this.selectedCategory);
            this.selectedCategory = this.categorySelector.getSelectedCategoryOrDefault("None");
        }

        if (!removed) {
            this.infoMessage = "Category not found.";
            this.infoMessageTicks = 80;
        }
        autoSaveIfEditMode();
    }

    private void createWaypoint() {
        cacheInputValues();
        WaypointsV2FileManager.WaypointRecord waypoint = buildWaypointFromUi(true);
        if (waypoint == null) {
            return;
        }

        if (!WaypointsV2FileManager.addWaypoint(waypoint)) {
            this.infoMessage = "Could not save waypoint.";
            this.infoMessageTicks = 80;
            return;
        }

        this.onClose();
    }

    private void loadWaypointForEditMode() {
        if (!isEditMode() || this.editingWaypointId == null || this.editingWaypointId.isBlank()) {
            return;
        }

        WaypointsV2FileManager.WaypointRecord waypoint = WaypointsV2FileManager.getWaypointById(this.editingWaypointId);
        if (waypoint == null) {
            this.infoMessage = "Waypoint not found.";
            this.infoMessageTicks = 100;
            return;
        }

        this.editingWaypointId = waypoint.id;
        this.cachedWaypointName = waypoint.name;
        this.cachedCoordX = String.valueOf(waypoint.x);
        this.cachedCoordY = String.valueOf(waypoint.y);
        this.cachedCoordZ = String.valueOf(waypoint.z);
        this.selectedCategory = waypoint.category;
        this.selectedColor = waypoint.color;
        this.dimensions = new boolean[]{ waypoint.overworld, waypoint.nether, waypoint.end };
        this.localWaypoint = waypoint.local;
        this.selectedDisplayMode = parseDisplayMode(waypoint.displayMode);
        this.selectedWaypointType = parseWaypointType(waypoint.type);
        this.selectedIconItem = resolveItemById(waypoint.iconItemId);
        this.blockCoordinates = fromStorageCoordinates(waypoint.blockCoordinates);
    }

    private void openBlockListEditor() {
        List<BlockListManagerScreen.BlockCoordinate> copy = copyBlockCoordinates(this.blockCoordinates);
        this.minecraft.setScreen(new BlockListManagerScreen(this, copy, this::onBlockListUpdated));
    }

    private void onBlockListUpdated(List<BlockListManagerScreen.BlockCoordinate> updatedCoordinates) {
        this.blockCoordinates = copyBlockCoordinates(updatedCoordinates);
        autoSaveIfEditMode();
    }

    private List<BlockListManagerScreen.BlockCoordinate> copyBlockCoordinates(List<BlockListManagerScreen.BlockCoordinate> source) {
        List<BlockListManagerScreen.BlockCoordinate> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }

        for (BlockListManagerScreen.BlockCoordinate coordinate : source) {
            if (coordinate == null) {
                continue;
            }
            copy.add(new BlockListManagerScreen.BlockCoordinate(coordinate.x, coordinate.y, coordinate.z));
        }
        return copy;
    }

    private List<BlockListManagerScreen.BlockCoordinate> fromStorageCoordinates(List<WaypointsV2FileManager.BlockCoordinateRecord> source) {
        List<BlockListManagerScreen.BlockCoordinate> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }

        for (WaypointsV2FileManager.BlockCoordinateRecord coordinate : source) {
            if (coordinate == null) {
                continue;
            }
            copy.add(new BlockListManagerScreen.BlockCoordinate(coordinate.x, coordinate.y, coordinate.z));
        }
        return copy;
    }

    private boolean isEditMode() {
        return this.mode == Mode.EDIT;
    }

    private void autoSaveIfEditMode() {
        if (!isEditMode() || this.suppressAutoSave) {
            return;
        }

        WaypointsV2FileManager.WaypointRecord waypoint = buildWaypointFromUi(false);
        if (waypoint == null) {
            return;
        }

        WaypointsV2FileManager.updateWaypoint(waypoint);
    }

    private void deleteEditedWaypoint() {
        if (!isEditMode() || this.editingWaypointId == null || this.editingWaypointId.isBlank()) {
            this.onClose();
            return;
        }

        if (!WaypointsV2FileManager.deleteWaypointById(this.editingWaypointId)) {
            this.infoMessage = "Could not delete waypoint.";
            this.infoMessageTicks = 80;
            return;
        }

        this.onClose();
    }

    private void openDeleteConfirmScreen() {
        this.minecraft.setScreen(
                new ConfirmScreen(
                        this,
                        Component.literal("Delete this waypoint?"),
                        this::deleteEditedWaypoint
                )
        );
    }

    private WaypointsV2FileManager.WaypointRecord buildWaypointFromUi(boolean requireName) {
        String name = this.waypointNameBox != null ? this.waypointNameBox.getValue().trim() : this.cachedWaypointName;
        if (requireName && name.isEmpty()) {
            this.infoMessage = "Name is required.";
            this.infoMessageTicks = 80;
            return null;
        }

        int[] playerCoords = getPlayerCoordsOrNull();
        int x = playerCoords != null ? playerCoords[0] : 0;
        int y = playerCoords != null ? playerCoords[1] : 64;
        int z = playerCoords != null ? playerCoords[2] : 0;

        if (this.selectedWaypointType == WaypointType.LOCATION) {
            if (playerCoords == null) {
                if (requireName) {
                    this.infoMessage = "Could not read player coordinates.";
                    this.infoMessageTicks = 80;
                }
                return null;
            }

            try {
                x = parseCoordinateOrFallback(this.coordXBox, playerCoords[0]);
                y = parseCoordinateOrFallback(this.coordYBox, playerCoords[1]);
                z = parseCoordinateOrFallback(this.coordZBox, playerCoords[2]);
            } catch (NumberFormatException e) {
                if (requireName) {
                    this.infoMessage = "Coordinates must be numbers.";
                    this.infoMessageTicks = 80;
                }
                return null;
            }
        }

        WaypointsV2FileManager.WaypointRecord waypoint = new WaypointsV2FileManager.WaypointRecord();
        if (isEditMode()) {
            waypoint.id = this.editingWaypointId;
        }
        waypoint.name = WaypointsV2FileManager.sanitizeName(name);
        waypoint.type = this.selectedWaypointType.name();
        waypoint.x = x;
        waypoint.y = y;
        waypoint.z = z;
        waypoint.category = this.selectedCategory;
        waypoint.displayMode = this.selectedDisplayMode.name();
        waypoint.iconItemId = BuiltInRegistries.ITEM.getKey(this.selectedIconItem).toString();
        waypoint.color = this.selectedColor;
        waypoint.local = this.localWaypoint;
        waypoint.localContext = this.localWaypoint ? getCurrentLocalContext() : "";
        waypoint.originDimension = getCurrentDimensionKey();
        applyLinkedDimensionCoordinates(waypoint, x, y, z);
        waypoint.overworld = this.dimensions[0];
        waypoint.nether = this.dimensions[1];
        waypoint.end = this.dimensions[2];
        waypoint.blockCoordinates = new ArrayList<>();
        if (this.selectedWaypointType == WaypointType.BLOCKS) {
            for (BlockListManagerScreen.BlockCoordinate coordinate : this.blockCoordinates) {
                if (coordinate == null) {
                    continue;
                }
                waypoint.blockCoordinates.add(new WaypointsV2FileManager.BlockCoordinateRecord(coordinate.x, coordinate.y, coordinate.z));
            }
        }
        return waypoint;
    }

    private DisplayMode parseDisplayMode(String value) {
        if (value == null || value.isBlank()) {
            return DisplayMode.TEXT_ICON;
        }
        try {
            return DisplayMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return DisplayMode.TEXT_ICON;
        }
    }

    private WaypointType parseWaypointType(String value) {
        if (value == null || value.isBlank()) {
            return WaypointType.LOCATION;
        }
        try {
            return WaypointType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return WaypointType.LOCATION;
        }
    }

    private Item resolveItemById(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Items.COMPASS;
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).toString().equalsIgnoreCase(itemId)) {
                return item;
            }
        }
        return Items.COMPASS;
    }

    private void cacheInputValues() {
        this.cachedWaypointName = this.waypointNameBox != null ? this.waypointNameBox.getValue() : this.cachedWaypointName;
        this.cachedCoordX = this.coordXBox != null ? this.coordXBox.getValue() : this.cachedCoordX;
        this.cachedCoordY = this.coordYBox != null ? this.coordYBox.getValue() : this.cachedCoordY;
        this.cachedCoordZ = this.coordZBox != null ? this.coordZBox.getValue() : this.cachedCoordZ;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int parseCoordinateOrFallback(EditBox box, int fallback) {
        if (box == null) {
            return fallback;
        }

        String text = box.getValue().trim();
        if (text.isEmpty()) {
            return fallback;
        }
        return Integer.parseInt(text);
    }

    private int[] getPlayerCoordsOrNull() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return null;
        }

        return new int[] {
                this.minecraft.player.getBlockX(),
                this.minecraft.player.getBlockY(),
                this.minecraft.player.getBlockZ()
        };
    }

    private void applyCreateModeDimensionDefaults() {
        if (isEditMode() || this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        this.dimensions[0] = false;
        this.dimensions[1] = false;
        this.dimensions[2] = false;

        if (this.minecraft.level.dimension() == Level.OVERWORLD) {
            this.dimensions[0] = true;
        } else if (this.minecraft.level.dimension() == Level.NETHER) {
            this.dimensions[1] = true;
        } else if (this.minecraft.level.dimension() == Level.END) {
            this.dimensions[2] = true;
        } else {
            this.dimensions[0] = true;
        }
    }

    private String getCurrentDimensionKey() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return "overworld";
        }
        if (this.minecraft.level.dimension() == Level.NETHER) {
            return "nether";
        }
        if (this.minecraft.level.dimension() == Level.END) {
            return "end";
        }
        return "overworld";
    }

    private void applyLinkedDimensionCoordinates(WaypointsV2FileManager.WaypointRecord waypoint, int x, int y, int z) {
        waypoint.linkedOverworldX = x;
        waypoint.linkedOverworldY = y;
        waypoint.linkedOverworldZ = z;
        waypoint.linkedNetherX = x;
        waypoint.linkedNetherY = y;
        waypoint.linkedNetherZ = z;

        if ("overworld".equalsIgnoreCase(waypoint.originDimension)) {
            waypoint.linkedOverworldX = x;
            waypoint.linkedOverworldY = y;
            waypoint.linkedOverworldZ = z;
            waypoint.linkedNetherX = Math.floorDiv(x, 8);
            waypoint.linkedNetherY = y;
            waypoint.linkedNetherZ = Math.floorDiv(z, 8);
            return;
        }

        if ("nether".equalsIgnoreCase(waypoint.originDimension)) {
            waypoint.linkedNetherX = x;
            waypoint.linkedNetherY = y;
            waypoint.linkedNetherZ = z;
            waypoint.linkedOverworldX = x * 8;
            waypoint.linkedOverworldY = y;
            waypoint.linkedOverworldZ = z * 8;
        }
    }

    private String getCurrentLocalContext() {
        if (this.minecraft == null) {
            return "";
        }

        // Multiplayer: save exact server IP/address when available.
        try {
            Object serverData = this.minecraft.getClass().getMethod("getCurrentServer").invoke(this.minecraft);
            if (serverData != null) {
                try {
                    Object ip = serverData.getClass().getField("ip").get(serverData);
                    if (ip != null) {
                        String value = ip.toString().trim();
                        if (!value.isEmpty()) {
                            return "server:" + value;
                        }
                    }
                } catch (Exception ignored) {
                    // Fall through to world context.
                }
            }
        } catch (Exception ignored) {
            // Fall through to world context.
        }

        // Singleplayer/world fallback.
        try {
            if (this.minecraft.hasSingleplayerServer()) {
                Object integratedServer = this.minecraft.getClass().getMethod("getSingleplayerServer").invoke(this.minecraft);
                if (integratedServer != null) {
                    Object worldData = integratedServer.getClass().getMethod("getWorldData").invoke(integratedServer);
                    if (worldData != null) {
                        Object levelName = worldData.getClass().getMethod("getLevelName").invoke(worldData);
                        if (levelName != null) {
                            String value = levelName.toString().trim();
                            if (!value.isEmpty()) {
                                return "world:" + value;
                            }
                        }
                    }
                }
                return "world:singleplayer";
            }
        } catch (Exception ignored) {
            // Fall through to dimension fallback.
        }

        if (this.minecraft.level != null && this.minecraft.level.dimension() != null) {
            return "world:" + this.minecraft.level.dimension().toString();
        }

        return "";
    }

    private void shiftWidgetY(AbstractWidget widget, int deltaY) {
        if (widget != null) {
            widget.setY(widget.getY() + deltaY);
        }
    }

    private void shiftPanelY(int deltaY) {
        if (deltaY == 0) {
            return;
        }

        this.boxY += deltaY;
        shiftWidgetY(this.waypointNameBox, deltaY);
        shiftWidgetY(this.displayModeButton, deltaY);
        shiftWidgetY(this.iconSelectorButton, deltaY);
        shiftWidgetY(this.iconPreviewButton, deltaY);
        shiftWidgetY(this.colorButton, deltaY);
        shiftWidgetY(this.colorPreviewButton, deltaY);
        shiftWidgetY(this.waypointTypeButton, deltaY);
        shiftWidgetY(this.coordXBox, deltaY);
        shiftWidgetY(this.coordYBox, deltaY);
        shiftWidgetY(this.coordZBox, deltaY);
        shiftWidgetY(this.blockListButton, deltaY);
        shiftWidgetY(this.scopeToggle, deltaY);
        shiftWidgetY(this.overworldToggle, deltaY);
        shiftWidgetY(this.netherToggle, deltaY);
        shiftWidgetY(this.endToggle, deltaY);
        shiftWidgetY(this.addCategoryButton, deltaY);
        shiftWidgetY(this.removeCategoryButton, deltaY);
        shiftWidgetY(this.cancelButton, deltaY);
        shiftWidgetY(this.createButton, deltaY);

        this.categoryBoxY += deltaY;
        if (this.categorySelector != null) {
            this.categorySelector.updateSizeAndPosition(this.categoryBoxWidth, this.categoryBoxHeight, this.categoryBoxX, this.categoryBoxY);
        }
    }

    private static class CategoryObjectSelectionList extends ObjectSelectionList<CategoryObjectSelectionList.CategoryEntry> {
        CategoryObjectSelectionList(net.minecraft.client.Minecraft minecraft, int width, int height, int y, int itemHeight, int left) {
            super(minecraft, width, height, y, itemHeight);
            this.setCategories(List.of("None"), "None");
        }

        void setCategories(List<String> categories, String selectedCategory) {
            this.clearEntries();

            CategoryEntry selectedEntry = null;
            if (categories != null) {
                for (String category : categories) {
                    CategoryEntry entry = new CategoryEntry(category);
                    this.addEntry(entry);
                    if (selectedCategory != null && category.equalsIgnoreCase(selectedCategory)) {
                        selectedEntry = entry;
                    }
                }
            }

            if (this.children().isEmpty()) {
                CategoryEntry none = new CategoryEntry("None");
                this.addEntry(none);
                selectedEntry = none;
            }

            if (selectedEntry == null) {
                selectedEntry = this.children().getFirst();
            }
            this.setSelected(selectedEntry);
        }

        String getSelectedCategoryOrDefault(String fallback) {
            CategoryEntry selected = this.getSelected();
            return selected != null ? selected.text : fallback;
        }

        @Override
        public int getRowLeft() {
            return this.getX() + 4;
        }

        @Override
        public int getRowWidth() {
            // Leave a little room for an internal scrollbar lane.
            return this.getWidth() - 14;
        }

        @Override
        protected int scrollBarX() {
            // Keep scrollbar inside the category list box instead of sticking out.
            return this.getX() + this.getWidth() - 6;
        }

        class CategoryEntry extends Entry<CategoryEntry> {
            private final String text;

            CategoryEntry(String text) {
                this.text = text;
            }

            @Override
            public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
                // NOTE: AbstractSelectionList passes mouseX/mouseY here, not row coordinates.
                // Use entry content bounds to render text in the row itself.
                int textColor = hovered ? 0xFFFFFFFF : 0xFFDDDDDD;
                graphics.drawString(
                        net.minecraft.client.Minecraft.getInstance().font,
                        this.text,
                        this.getContentX() + 2,
                        this.getContentY() + 2,
                        textColor,
                        false
                );
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
                WaypointConfigScreen.CategoryObjectSelectionList.this.setSelected(this);
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.text);
            }
        }
    }
}

