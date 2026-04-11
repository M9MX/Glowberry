package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PickUpLogHud extends DynamicHudElement<PickUpLogHud> {
    // Add origin enum for growth direction
    public enum Origin {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
    private Origin origin = Origin.TOP_LEFT;
    public void setOrigin(Origin origin) { this.origin = origin; }
    public Origin getOrigin() { return origin; }
    // Track previous position for movement detection
    private int prevX = -1, prevY = -1;
    // Represents a transient pickup/drop event
    private static class LogEntry {
        final Item item;
        int netCount;
        long lastUpdate; // ms

        LogEntry(Item item, int netCount, long lastUpdate) {
            this.item = item;
            this.netCount = netCount;
            this.lastUpdate = lastUpdate;
        }
    }

    // List of active log entries
    private final List<LogEntry> logEntries = new ArrayList<>();
    // Track previous inventory state
    private final Map<Item, Integer> lastInventory = new HashMap<>();

    private static final long ENTRY_LIFETIME_MS = 3000;

    // For deduplicating log output
    private String lastLogMessage = null;
    private void logIfChanged(String msg) {
        if (!msg.equals(lastLogMessage)) {
            System.out.println(msg);
            lastLogMessage = msg;
        }
    }

    public PickUpLogHud() {
        super("pickup_log", Direction.Horizontal.size);
        this.style.set(com.dwarslooper.cactus.client.gui.hud.element.HudElement.Style.Default);
        this.textColor.set(new ColorSetting.ColorValue(Color.WHITE, false)); // Default to white
        // No anchor set, will use manual move()
        // Default origin is BOTTOM_LEFT; HUD grows upwards from bottom left
        this.origin = Origin.BOTTOM_LEFT;
    }

    @Override
    public boolean canResize() {
        return false;
    }

    @Override
    public PickUpLogHud duplicate() {
        return new PickUpLogHud();
    }

    private boolean initializedInventory = false;

    public void onTick(Player player) {
        if (player == null) return;
        Inventory inv = player.getInventory();
        Map<Item, Integer> current = new HashMap<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                current.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        if (!initializedInventory) {
            lastInventory.clear();
            lastInventory.putAll(current);
            initializedInventory = true;
            return;
        }
        long now = System.currentTimeMillis();
        // Track changes
        // Check for pickups/drops
        for (Map.Entry<Item, Integer> entry : current.entrySet()) {
            Item item = entry.getKey();
            int prev = lastInventory.getOrDefault(item, 0);
            int diff = entry.getValue() - prev;
            if (diff != 0) {
                // Update or create log entry
                LogEntry log = null;
                for (LogEntry e : logEntries) {
                    if (e.item == item) {
                        log = e;
                        break;
                    }
                }
                if (log == null) {
                    log = new LogEntry(item, diff, now);
                    logEntries.add(log);
                } else {
                    log.netCount += diff;
                    log.lastUpdate = now;
                }
            }
        }
        // Handle items that were removed entirely
        for (Map.Entry<Item, Integer> entry : lastInventory.entrySet()) {
            Item item = entry.getKey();
            if (!current.containsKey(item)) {
                int diff = -entry.getValue();
                LogEntry log = null;
                for (LogEntry e : logEntries) {
                    if (e.item == item) {
                        log = e;
                        break;
                    }
                }
                if (log == null) {
                    log = new LogEntry(item, diff, now);
                    logEntries.add(log);
                } else {
                    log.netCount += diff;
                    log.lastUpdate = now;
                }
            }
        }
        // Remove expired entries
        logEntries.removeIf(e -> (now - e.lastUpdate) > ENTRY_LIFETIME_MS || e.netCount == 0);
        lastInventory.clear();
        lastInventory.putAll(current);
    }

    // Track previous HUD height for upward growth
    private int prevHudHeight = -1;

    @Override
    public void resize(int width, int height) {
        int oldWidth = this.getSize().x();
        int oldHeight = this.getSize().y();
        // Don't resize/move if new size is 0 (prevents sliding off screen)
        if (width == 0 || height == 0) {
            return;
        }
        super.resize(width, height);
        // Adjust position so the selected origin stays fixed
        switch (origin) {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                // Move left by width difference to keep right edge fixed
                this.move(this.getRelativePosition().x() - (width - oldWidth),
                          origin == Origin.TOP_RIGHT ? this.getRelativePosition().y() : this.getRelativePosition().y() - (height - oldHeight));
                break;
            case BOTTOM_LEFT:
                this.move(this.getRelativePosition().x(), this.getRelativePosition().y() - (height - oldHeight));
                break;
            case TOP_LEFT:
            default:
                // No adjustment needed
                break;
        }
        // Clamp position to screen bounds if possible (assume screen size is available)
        // If you have access to screenWidth/screenHeight, clamp here
        // Example (pseudo):
        // int px = Math.max(0, Math.min(this.getRelativePosition().x(), screenWidth - width));
        // int py = Math.max(0, Math.min(this.getRelativePosition().y(), screenHeight - height));
        // this.move(px, py);
    }

    private static final String MAX_NAME = "Waxed Weathered Cut Copper Stairs";
    private static final String MAX_COUNT = "+100x "; // Reserve space for up to 100x
    private static final int ICON_SIZE = 16;
    private static final int BG_PAD_X = 4;
    private static final int BG_PAD_Y = 3;
    private static final int ENTRY_HEIGHT = 22;
    private int fixedBgWidth = -1;

    private static int getRarityColor(ItemStack stack, int defaultColor) {
        switch (stack.getRarity()) {
            case UNCOMMON:
                return 0xFFFFFF55; // Yellow (#FFFF55)
            case RARE:
                return 0xFF55FFFF; // Aqua/Cyan (#55FFFF)
            case EPIC:
                return 0xFFFF55FF; // Magenta (#FF55FF)
            case COMMON:
            default:
                return defaultColor; // Use HUD's set color for common
        }
    }

    @Override
    public void renderContent(GuiGraphics context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        // Compute fixed width if not already done
        if (fixedBgWidth == -1) {
            int countWidth = Minecraft.getInstance().font.width(MAX_COUNT); // Use widest possible count
            int nameWidth = Minecraft.getInstance().font.width(MAX_NAME);
            fixedBgWidth = countWidth + nameWidth + ICON_SIZE + BG_PAD_X * 2 + 6;
        }
        boolean hasEntries = !logEntries.isEmpty();
        int bgWidth = fixedBgWidth;
        int bgHeight;
        List<LogEntry> renderEntries;
        if (!hasEntries) {
            // If empty, set size as if there was one item
            bgHeight = (ENTRY_HEIGHT - 3) + BG_PAD_Y * 2;
            renderEntries = Collections.emptyList();
        } else {
            renderEntries = logEntries;
            bgHeight = renderEntries.size() * (ENTRY_HEIGHT - 3) + BG_PAD_Y * 2;
        }
        this.resize(bgWidth, bgHeight);
        // In editor, ensure minimum size so HUD is always visible
        int editorW = bgWidth, editorH = bgHeight;
        if (inEditor && !hasEntries) {
            editorH = Math.max(height, 24);
            this.resize(editorW, editorH);
            // For right-side origins, anchor to right edge
            if (origin == Origin.TOP_RIGHT || origin == Origin.BOTTOM_RIGHT) {
                x = x + width - editorW;
            }
        }
        if (!hasEntries && !inEditor) {
            // No entries, not in editor: just reserve space, don't render anything
            return;
        }
        int n = renderEntries.size();
        // Use origin to determine where to start drawing
        for (int i = 0; i < n; i++) {
            LogEntry entry = renderEntries.get(origin == Origin.TOP_LEFT || origin == Origin.TOP_RIGHT ? i : (n - 1 - i));
            ItemStack stack = new ItemStack(entry.item);
            String countStr = (entry.netCount > 0 ? "+" : "") + entry.netCount + "x ";
            String nameStr = stack.getHoverName().getString();
            int countWidth = Minecraft.getInstance().font.width(MAX_COUNT); // Use widest possible count
            int nameWidth = Minecraft.getInstance().font.width(MAX_NAME);   // Use fixed width
            int totalTextWidth = countWidth + nameWidth;
            int drawY;
            if (origin == Origin.TOP_LEFT || origin == Origin.TOP_RIGHT) {
                drawY = y + BG_PAD_Y + (ENTRY_HEIGHT - 3) * i;
            } else {
                drawY = y + height - BG_PAD_Y - (ENTRY_HEIGHT - 3) * (i + 1);
            }
            int rightEdge = x + width - BG_PAD_X - 2;
            int textY = drawY + 4;
            int textX = rightEdge - totalTextWidth;
            int iconX = textX - ICON_SIZE - 3;
            context.renderFakeItem(stack, iconX, drawY);
            int countColor = entry.netCount > 0 ? 0xFF55FF55 : 0xFFFF5555;
            int textColor = getRarityColor(stack, this.textColor.get().color());
            context.drawString(Minecraft.getInstance().font, countStr, textX, textY + 2, countColor, true);
            context.drawString(Minecraft.getInstance().font, nameStr, textX + countWidth, textY + 2, textColor, true);
        }
    }


    enum Direction {
        Vertical(new Vector2i(1, 1)),
        Horizontal(new Vector2i(1, 1));
        final Vector2i size;

        Direction(Vector2i size) {
            this.size = size;
        }
    }

    // Placeholder for background check
    private boolean isCactusBackgroundVisible() {
        // TODO: Implement actual check for Cactus HUD background visibility
        return false;
    }
}
