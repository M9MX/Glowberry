package org.m9mx.cactus.glowberry.util.waila;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One rendered row in the Waila element. Rows come in four visual kinds:
 * <ul>
 *   <li><b>text</b> - optionally with a leading item icon and/or a trailing item
 *       icon (e.g. the required-tool icon next to the block name),</li>
 *   <li><b>sprite</b> - a leading sprite drawn from the GUI atlas (e.g. potion
 *       effect icons via {@code Hud.getMobEffectSprite}),</li>
 *   <li><b>bar</b> - a progress bar with an optional leading icon,</li>
 *   <li><b>hearts</b> - a row of vanilla heart sprites (health / horse HP).</li>
 * </ul>
 */
public class WailaLine {
    public final boolean isBar;
    public final boolean isHearts;
    public final boolean isIconRow;
    public final boolean isBlockHeader;
    /** Overlay drawn on top of {@link #icon2}: nothing, a red X or a green check. */
    public enum Badge { NONE, ERROR, OK }

    public final ItemStack icon;      // leading 16x16 item icon (text + bar rows)
    public final ItemStack icon2;     // trailing 16x16 item icon (text rows only)
    public Badge icon2Badge = Badge.NONE; // e.g. tool can't / can harvest the block
    public final List<ItemStack> icons; // horizontal icon strip rows
    public final Identifier sprite;   // leading GUI sprite (potion effect icons)
    public final Component label;     // left-aligned label (may be null)
    public final Component value;     // value text / right-side suffix
    public final int color;           // text color, or bar fill color
    public final float progress;      // bar: fill 0..1 | hearts: current HP
    public final float max;           // hearts: max HP

    /** Convenience constructor kept for simple icon rows (name/equipment lines). */
    public WailaLine(boolean isBar, ItemStack icon, ItemStack icon2, Component label, Component value, int color, float progress) {
        this(isBar, false, false, false, icon, icon2, null, List.of(), label, value, color, progress, 0);
    }

    public WailaLine(boolean isBar, boolean isHearts, ItemStack icon, ItemStack icon2, Identifier sprite,
                     Component label, Component value, int color, float progress, float max) {
        this(isBar, isHearts, false, false, icon, icon2, sprite, List.of(), label, value, color, progress, max);
    }

    public static WailaLine blockHeader(ItemStack icon, ItemStack icon2, Component value, int color) {
        return new WailaLine(false, false, false, true, icon, icon2, null, List.of(), null, value, color, 0, 0);
    }

    private WailaLine(boolean isBar, boolean isHearts, boolean isIconRow, boolean isBlockHeader, ItemStack icon, ItemStack icon2, Identifier sprite,
                      List<ItemStack> icons, Component label, Component value, int color, float progress, float max) {
        this.isBar = isBar;
        this.isHearts = isHearts;
        this.isIconRow = isIconRow;
        this.isBlockHeader = isBlockHeader;
        this.icon = icon;
        this.icon2 = icon2;
        this.sprite = sprite;
        this.icons = icons;
        this.label = label;
        this.value = value;
        this.color = color;
        this.progress = progress;
        this.max = max;
    }

    public static WailaLine text(Component label, Component value, int color) {
        return new WailaLine(false, false, false, false, ItemStack.EMPTY, ItemStack.EMPTY, null, List.of(), label, value, color, 0, 0);
    }

    public static WailaLine iconText(ItemStack icon, Component label, Component value, int color) {
        return new WailaLine(false, false, false, false, icon, ItemStack.EMPTY, null, List.of(), label, value, color, 0, 0);
    }

    public static WailaLine spriteText(Identifier sprite, Component label, Component value, int color) {
        return new WailaLine(false, false, false, false, ItemStack.EMPTY, ItemStack.EMPTY, sprite, List.of(), label, value, color, 0, 0);
    }

    public static WailaLine icons(List<ItemStack> icons, Component label, Component value, int color) {
        return new WailaLine(false, false, true, false, ItemStack.EMPTY, ItemStack.EMPTY, null, List.copyOf(icons), label, value, color, 0, 0);
    }

    public static WailaLine bar(Component label, Component value, int color, float progress) {
        return new WailaLine(true, false, false, false, ItemStack.EMPTY, ItemStack.EMPTY, null, List.of(), label, value, color, progress, 0);
    }

    public static WailaLine barIcon(ItemStack icon, Component label, Component value, int color, float progress) {
        return new WailaLine(true, false, false, false, icon, ItemStack.EMPTY, null, List.of(), label, value, color, progress, 0);
    }

    public static WailaLine hearts(Component value, float current, float max) {
        return new WailaLine(false, true, false, false, ItemStack.EMPTY, ItemStack.EMPTY, null, List.of(), null, value, 0xFFFFFFFF, current, max);
    }
}
