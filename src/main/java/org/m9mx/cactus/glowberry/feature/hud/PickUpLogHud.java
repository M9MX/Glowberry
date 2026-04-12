package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;

import java.awt.*;
import java.util.*;
import java.util.List;

@SuppressWarnings("unused")
public class PickUpLogHud extends DynamicHudElement<PickUpLogHud> {
    public enum Origin {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    public enum Alignment { LEFT, CENTER, RIGHT }

    private Origin origin;
    public void setOrigin(Origin origin) { this.origin = origin; }
    public Origin getOrigin() { return origin; }

    private static class LogEntry {
        final ItemStack representative;
        int netCount;
        long lastUpdate;

        LogEntry(ItemStack representative, int netCount, long lastUpdate) {
            this.representative = representative.copyWithCount(1);
            this.netCount   = netCount;
            this.lastUpdate = lastUpdate;
        }
    }

    private final List<LogEntry>  logEntries          = new ArrayList<>();
    private final List<ItemStack> lastInventoryStacks = new ArrayList<>();
    private final List<Integer>   lastInventoryCounts = new ArrayList<>();

    private static final long ENTRY_LIFETIME_MS = 3000;

    private final Setting<Boolean>   alwaysShow;
    private final Setting<Boolean>   shortenThousands;
    private final Setting<Boolean>   allowItemFormatting;
    private final Setting<Boolean>   allowItemModel;
    private final Setting<Alignment> alignment;
    private final Setting<Integer>   scale;

    private boolean initializedInventory = false;

    private static final int OFFSCREEN = -99999;
    private int savedX      = Integer.MIN_VALUE;
    private int savedY      = Integer.MIN_VALUE;
    private boolean isHidden = false;

    private int lastBgWidth  = -1;
    private int lastBgHeight = -1;

    private static final int ICON_SIZE      = 16;
    private static final int PAD_X          = 5;
    private static final int PAD_Y          = 4;
    private static final int ENTRY_HEIGHT   = 20;
    private static final int ICON_VPAD      = (ENTRY_HEIGHT - ICON_SIZE) / 2;
    private static final int ICON_TEXT_GAP  = 3;
    private static final int COUNT_NAME_GAP = 4;

    public PickUpLogHud() {
        super("pickup_log", Direction.Horizontal.size);
        this.style.set(com.dwarslooper.cactus.client.gui.hud.element.HudElement.Style.Default);
        this.textColor.set(new ColorSetting.ColorValue(Color.WHITE, false));
        this.origin = Origin.BOTTOM_LEFT;

        var sgGeneral = this.settings.buildGroup("general");
        this.alwaysShow          = sgGeneral.add(new BooleanSetting("alwaysShow", false));
        this.shortenThousands    = sgGeneral.add(new BooleanSetting("shortenThousands", false));
        this.allowItemFormatting = sgGeneral.add(new BooleanSetting("allowItemFormatting", true));
        this.allowItemModel      = sgGeneral.add(new BooleanSetting("allowItemModel", true));
        this.alignment           = sgGeneral.add(new EnumSetting<>("alignment", Alignment.LEFT));
        this.scale               = sgGeneral.add(new IntegerSetting("scale", 100).min(25).max(400));
    }

    @Override
    public boolean canResize() { return false; }

    @Override
    public PickUpLogHud duplicate() { return new PickUpLogHud(); }

    private int findSnapshot(ItemStack needle) {
        for (int i = 0; i < lastInventoryStacks.size(); i++) {
            if (ItemStack.isSameItemSameComponents(lastInventoryStacks.get(i), needle)) return i;
        }
        return -1;
    }

    private LogEntry findLogEntry(ItemStack needle) {
        for (LogEntry e : logEntries) {
            if (ItemStack.isSameItemSameComponents(e.representative, needle)) return e;
        }
        return null;
    }

    public void onTick(Player player) {
        if (player == null) return;
        Inventory inv = player.getInventory();

        List<ItemStack> currentStacks = new ArrayList<>();
        List<Integer>   currentCounts = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            boolean found = false;
            for (int j = 0; j < currentStacks.size(); j++) {
                if (ItemStack.isSameItemSameComponents(currentStacks.get(j), stack)) {
                    currentCounts.set(j, currentCounts.get(j) + stack.getCount());
                    found = true;
                    break;
                }
            }
            if (!found) {
                currentStacks.add(stack.copyWithCount(1));
                currentCounts.add(stack.getCount());
            }
        }

        if (!initializedInventory) {
            lastInventoryStacks.clear();
            lastInventoryCounts.clear();
            lastInventoryStacks.addAll(currentStacks);
            lastInventoryCounts.addAll(currentCounts);
            initializedInventory = true;
            return;
        }

        long now = System.currentTimeMillis();

        for (int i = 0; i < currentStacks.size(); i++) {
            ItemStack stack = currentStacks.get(i);
            int prevIdx     = findSnapshot(stack);
            int prevCount   = prevIdx >= 0 ? lastInventoryCounts.get(prevIdx) : 0;
            int diff        = currentCounts.get(i) - prevCount;
            if (diff != 0) applyDiff(stack, diff, now);
        }

        for (int i = 0; i < lastInventoryStacks.size(); i++) {
            ItemStack stack = lastInventoryStacks.get(i);
            boolean stillPresent = false;
            for (ItemStack cs : currentStacks) {
                if (ItemStack.isSameItemSameComponents(cs, stack)) { stillPresent = true; break; }
            }
            if (!stillPresent) applyDiff(stack, -lastInventoryCounts.get(i), now);
        }

        logEntries.removeIf(e -> (now - e.lastUpdate) > ENTRY_LIFETIME_MS || e.netCount == 0);

        lastInventoryStacks.clear();
        lastInventoryCounts.clear();
        lastInventoryStacks.addAll(currentStacks);
        lastInventoryCounts.addAll(currentCounts);
    }

    private void applyDiff(ItemStack stack, int diff, long now) {
        LogEntry existing = findLogEntry(stack);
        if (existing != null) {
            existing.netCount += diff;
            existing.lastUpdate = now;
        } else {
            logEntries.add(new LogEntry(stack, diff, now));
        }
    }

    private String formatCount(int count, boolean shorten) {
        String prefix = count > 0 ? "+" : "";
        if (shorten && Math.abs(count) >= 1000) {
            String kStr = String.format("%.1f", count / 1000.0);
            if (kStr.endsWith(".0")) kStr = kStr.substring(0, kStr.length() - 2);
            return prefix + kStr + "k";
        }
        return prefix + count;
    }

    private static int getRarityColor(ItemStack stack, int defaultColor) {
        return switch (stack.getRarity()) {
            case UNCOMMON -> 0xFFFFFF55;
            case RARE     -> 0xFF55FFFF;
            case EPIC     -> 0xFFFF55FF;
            default       -> defaultColor;
        };
    }

    private Component resolveDisplayName(ItemStack stack) {
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom != null) return custom;
        return stack.getHoverName();
    }

    private ItemStack resolveDisplayStack(ItemStack stack, boolean useModel) {
        if (useModel) return stack;
        ItemStack stripped = stack.copy();
        stripped.remove(DataComponents.ITEM_MODEL);
        return stripped;
    }

    private void hideOffscreen() {
        if (!isHidden) {
            savedX = this.getRelativePosition().x();
            savedY = this.getRelativePosition().y();
            this.move(OFFSCREEN, OFFSCREEN);
            isHidden = true;
        }
    }

    private void restorePosition() {
        if (isHidden && savedX != Integer.MIN_VALUE) {
            this.move(savedX, savedY);
            isHidden = false;
        }
    }

    private void anchoredResize(int newWidth, int newHeight) {
        if (newWidth == lastBgWidth && newHeight == lastBgHeight) return;

        int oldWidth  = lastBgWidth  == -1 ? newWidth  : lastBgWidth;
        int oldHeight = lastBgHeight == -1 ? newHeight : lastBgHeight;

        lastBgWidth  = newWidth;
        lastBgHeight = newHeight;

        super.resize(newWidth, newHeight);

        int px = this.getRelativePosition().x();
        int py = this.getRelativePosition().y();

        int dx = newWidth  - oldWidth;
        int dy = newHeight - oldHeight;

        Alignment align = alignment.get();

        if (align == Alignment.CENTER && dx != 0) {
            px -= dx / 2;
        } else if (align == Alignment.RIGHT && dx != 0) {
            px -= dx;
        }

        if ((origin == Origin.BOTTOM_LEFT || origin == Origin.BOTTOM_RIGHT) && dy != 0 && oldHeight > 0) {
            py -= dy;
        }

        if (dx != 0 || dy != 0) this.move(px, py);
    }

    @Override
    public void resize(int width, int height) {
        anchoredResize(width, height);
    }

    @Override
    public void renderContent(GuiGraphics context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        Minecraft mc       = Minecraft.getInstance();
        boolean shorten    = shortenThousands.get();
        boolean formatting = allowItemFormatting.get();
        boolean useModel   = allowItemModel.get();
        boolean always     = alwaysShow.get();
        Alignment align    = alignment.get();
        float scaleFactor  = scale.get() / 100f;

        List<LogEntry> renderEntries = new ArrayList<>(logEntries);
        boolean hasEntries = !renderEntries.isEmpty();

        if (!hasEntries && !always && !inEditor) {
            hideOffscreen();
            return;
        }

        restorePosition();

        if (inEditor && !hasEntries) {
            int phCountW  = mc.font.width("+10x");
            int phNameW   = mc.font.width("Example Item");
            int unscaledW = PAD_X * 2 + ICON_SIZE + ICON_TEXT_GAP + phCountW + COUNT_NAME_GAP + phNameW;
            int unscaledH = ENTRY_HEIGHT + PAD_Y * 2;
            anchoredResize(Math.round(unscaledW * scaleFactor), Math.round(unscaledH * scaleFactor));

            var pose = context.pose();
            pose.pushMatrix();
            pose.translate(x, y);
            pose.scale(scaleFactor, scaleFactor);

            int countX = PAD_X + ICON_SIZE + ICON_TEXT_GAP;
            int nameX  = countX + phCountW + COUNT_NAME_GAP;
            int textY  = PAD_Y + ICON_VPAD + (ICON_SIZE - mc.font.lineHeight) / 2;
            context.drawString(mc.font, "+10x", countX, textY, 0xFF55FF55);
            context.drawString(mc.font, "Example Item", nameX, textY, 0xFFFFFFFF);

            pose.popMatrix();
            return;
        }

        int maxCountWidth = 0;
        int maxNameWidth  = 0;
        for (LogEntry entry : renderEntries) {
            String countStr = formatCount(entry.netCount, shorten) + "x";
            int cw = mc.font.width(countStr);
            if (cw > maxCountWidth) maxCountWidth = cw;

            int nw = formatting
                    ? mc.font.width(resolveDisplayName(entry.representative))
                    : mc.font.width(entry.representative.getHoverName().getString());
            if (nw > maxNameWidth) maxNameWidth = nw;
        }

        int unscaledRowW = ICON_SIZE + ICON_TEXT_GAP + maxCountWidth + COUNT_NAME_GAP + maxNameWidth;
        int unscaledW    = unscaledRowW + PAD_X * 2;
        int unscaledH    = renderEntries.size() * ENTRY_HEIGHT + PAD_Y * 2;

        anchoredResize(Math.round(unscaledW * scaleFactor), Math.round(unscaledH * scaleFactor));

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scaleFactor, scaleFactor);

        int n = renderEntries.size();
        for (int i = 0; i < n; i++) {
            LogEntry entry = renderEntries.get(
                    (origin == Origin.TOP_LEFT || origin == Origin.TOP_RIGHT) ? i : (n - 1 - i)
            );

            ItemStack displayStack = resolveDisplayStack(entry.representative, useModel);
            String countStr        = formatCount(entry.netCount, shorten) + "x";
            int cw                 = mc.font.width(countStr);

            int rowTop = (origin == Origin.TOP_LEFT || origin == Origin.TOP_RIGHT)
                    ? PAD_Y + ENTRY_HEIGHT * i
                    : unscaledH - PAD_Y - ENTRY_HEIGHT * (i + 1);

            int rowOffset;
            if (align == Alignment.RIGHT) {
                int rowW = ICON_SIZE + ICON_TEXT_GAP + cw + COUNT_NAME_GAP + mc.font.width(formatting
                        ? resolveDisplayName(entry.representative).getString()
                        : entry.representative.getHoverName().getString());
                rowOffset = unscaledRowW - rowW;
            } else if (align == Alignment.CENTER) {
                int rowW = ICON_SIZE + ICON_TEXT_GAP + cw + COUNT_NAME_GAP + mc.font.width(formatting
                        ? resolveDisplayName(entry.representative).getString()
                        : entry.representative.getHoverName().getString());
                rowOffset = (unscaledRowW - rowW) / 2;
            } else {
                rowOffset = 0;
            }

            int itemIconX = PAD_X + rowOffset;
            int iconY     = rowTop + ICON_VPAD;
            int countX    = itemIconX + ICON_SIZE + ICON_TEXT_GAP;
            int nameX     = countX + maxCountWidth + COUNT_NAME_GAP;
            int textY     = iconY + (ICON_SIZE - mc.font.lineHeight) / 2;

            context.renderFakeItem(displayStack, itemIconX, iconY);

            int countColor = entry.netCount > 0 ? 0xFF55FF55 : 0xFFFF5555;
            context.drawString(mc.font, countStr, countX, textY, countColor);

            if (formatting) {
                context.drawString(mc.font, resolveDisplayName(entry.representative), nameX, textY, getRarityColor(entry.representative, this.textColor.get().color()));
            } else {
                context.drawString(mc.font, entry.representative.getHoverName().getString(), nameX, textY, getRarityColor(entry.representative, this.textColor.get().color()));
            }
        }

        pose.popMatrix();
    }

    enum Direction {
        Vertical(new Vector2i(1, 1)),
        Horizontal(new Vector2i(1, 1));
        final Vector2i size;
        Direction(Vector2i size) { this.size = size; }
    }

    @Override
    public String getName() {
        return "Pickup Log";
    }
}