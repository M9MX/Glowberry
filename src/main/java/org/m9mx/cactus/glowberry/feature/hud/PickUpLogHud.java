package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;

import java.util.*;

public class PickUpLogHud extends DynamicHudElement<PickUpLogHud> {
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

    public PickUpLogHud() {
        super("pickup_log", Direction.Horizontal.size);
        // No options, always right-aligned, transparent style
        this.style.set(com.dwarslooper.cactus.client.gui.hud.element.HudElement.Style.Transparent);
    }

    @Override
    public boolean canResize() {
        return false;
    }

    @Override
    public PickUpLogHud duplicate() {
        return new PickUpLogHud();
    }

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

    @Override
    public void renderContent(GuiGraphics context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        // Only render if there are log entries or in editor mode
        if (!inEditor && logEntries.isEmpty()) {
            return;
        }
        int entryHeight = 22;
        List<LogEntry> renderEntries;
        if (inEditor) {
            // Show example entries in editor mode
            renderEntries = new ArrayList<>();
            renderEntries.add(new LogEntry(net.minecraft.world.item.Items.DIAMOND, 5, System.currentTimeMillis()));
            renderEntries.add(new LogEntry(net.minecraft.world.item.Items.STICK, -2, System.currentTimeMillis()));
        } else {
            renderEntries = logEntries;
        }
        int bgPadX = 4; // reduced padding for tighter background
        int bgPadY = 3;
        int iconSize = 16; // slightly smaller icon
        int drawY = y + height - 24;
        for (int i = 0; i < renderEntries.size(); i++) {
            LogEntry entry = renderEntries.get(i);
            ItemStack stack = new ItemStack(entry.item);
            String countStr = (entry.netCount > 0 ? "+" : "") + entry.netCount + "x ";
            String nameStr = stack.getHoverName().getString();
            int countWidth = Minecraft.getInstance().font.width(countStr);
            int nameWidth = Minecraft.getInstance().font.width(nameStr);
            int textWidth = countWidth + nameWidth;
            int bgWidth = textWidth + iconSize + bgPadX * 2 + 6;
            int bgHeight = iconSize + bgPadY * 2 - 4;
            int bgX = x + width - bgWidth - 3;
            int bgY = drawY - i * entryHeight - 2; // move background 2px up
            // Draw background with rounded corners using Cactus HUD style
            context.blitSprite(
                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                com.dwarslooper.cactus.client.gui.hud.element.HudElement.BACKGROUND,
                bgX, bgY, bgWidth, bgHeight,
                net.minecraft.util.ARGB.color(224, 0, 0, 0)
            );
            // Draw item icon 2px higher
            context.renderFakeItem(stack, bgX + bgPadX, bgY + bgPadY - 2);
            // Get rarity color (fallback to white)
            int rarityColor = 0xFFFFFFFF;
            try {
                java.lang.reflect.Method getRarity = stack.getClass().getMethod("getRarity");
                Object rarity = getRarity.invoke(stack);
                if (rarity != null) {
                    java.lang.reflect.Field colorField = rarity.getClass().getDeclaredField("color");
                    colorField.setAccessible(true);
                    rarityColor = colorField.getInt(rarity);
                }
            } catch (Exception ignored) {}
            // Count color: green for +, red for -
            int countColor = entry.netCount > 0 ? 0xFF55FF55 : 0xFFFF5555;
            int textY = bgY + bgPadY + 2; // move text 2px up
            int textX = bgX + bgPadX + iconSize + 3;
            // Draw count
            context.drawString(Minecraft.getInstance().font, countStr, textX, textY, countColor, false);
            // Draw name right after count
            context.drawString(Minecraft.getInstance().font, nameStr, textX + countWidth, textY, rarityColor, false);
        }
    }


    enum Direction {
        Vertical(new Vector2i(22, 76)),
        Horizontal(new Vector2i(200, 76));

        final Vector2i size;

        Direction(Vector2i size) {
            this.size = size;
        }
    }
}
