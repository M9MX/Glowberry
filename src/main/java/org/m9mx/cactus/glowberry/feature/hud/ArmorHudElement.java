package org.m9mx.cactus.glowberry.feature.hud;

import com.dwarslooper.cactus.client.gui.hud.element.DynamicHudElement;
import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2i;
import org.m9mx.cactus.glowberry.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ArmorHudElement extends DynamicHudElement<ArmorHudElement> {
    public enum Orientation { VERTICAL, HORIZONTAL }
    public enum Alignment { LEFT, CENTER, RIGHT }
    public enum DurabilityFormat { PERCENT, NUMBER }

    private final Setting<Orientation>      orientation;
    private final Setting<Boolean>          showDurability;
    private final Setting<Boolean>          showHeldItem;
    private final Setting<DurabilityFormat> durabilityFormat;
    private final Setting<Boolean>          colorDurability;
    private final Setting<Alignment>        alignment;
    private final Setting<Integer>          scale;

    private static final int PAD_X       = 5;
    private static final int PAD_Y       = 4;
    private static final int ICON_SIZE   = 21;           // slot size for 1.3x icons (16 * 1.3)
    private static final float ICON_SCALE = 1.3125f;     // renders the 16px item model at 21px
    private static final int LINE_HEIGHT = 21;           // matches the icon size
    private static final int TEXT_OFFSET = (LINE_HEIGHT - 9) / 2; // vertically centers the 9px text
    private static final int GAP         = 3;
    private static final int COL_GAP     = 5;
    private static final int OFFSCREEN   = -99999;

    private static final int COL_NAME   = 0xFFFFFFFF;
    private static final int COL_GREEN  = 0xFF55FF55;
    private static final int COL_ORANGE = 0xFFFFA500;
    private static final int COL_RED    = 0xFFFF5555;

    // Empty slot placeholder (drawn only in the HUD editor when items aren't bound yet)
    private static final int COL_SLOT_BG    = 0x66141414;
    private static final int COL_SLOT_BORDER = 0xAA3A3A3A;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private int savedX   = Integer.MIN_VALUE;
    private int savedY   = Integer.MIN_VALUE;
    private boolean isHidden = false;
    private int lastWidth = -1;

    /** One displayed item: an armor piece or the held item. */
    private static final class Row {
        final ItemStack icon;      // ItemStack.EMPTY = no icon (editor placeholder shows an empty slot)
        final boolean damageable;
        final int remaining;
        final int max;

        Row(ItemStack icon, boolean damageable, int remaining, int max) {
            this.icon = icon;
            this.damageable = damageable;
            this.remaining = remaining;
            this.max = max;
        }

        /**
         * Builds a row from a real item stack. Returns {@code null} for empty stacks or
         * when the item holder is not usable yet (e.g. HUD editor rendering before item
         * components are bound) - the caller should skip null rows.
         */
        static Row fromStack(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return null;
            try {
                boolean damageable = stack.isDamageableItem();
                int max = stack.getMaxDamage();
                int remaining = stack.getMaxDamage() - stack.getDamageValue();
                return new Row(stack, damageable, remaining, max);
            } catch (Exception e) {
                return null;
            }
        }

        int percent() {
            return max <= 0 ? 100 : Math.round(remaining * 100f / max);
        }

        /** The durability text shown after the icon, or null if not damageable. */
        String durabilityText(DurabilityFormat format) {
            if (!damageable) return null;
            return format == DurabilityFormat.NUMBER
                    ? remaining + "/" + max
                    : percent() + "%";
        }
    }

    public ArmorHudElement() {
        super("armor_hud", new Vector2i(1, 1));
        this.style.set(HudElement.Style.Default);
        // Cactus ships its own ArmorElement which defaults to the top-left corner.
        // Spawn just below it so both armor HUDs can be used together without stacking.
        this.move(0, 84);
        var sgGeneral = this.settings.buildGroup("general");
        this.orientation      = sgGeneral.add(new EnumSetting<>("orientation", Orientation.VERTICAL));
        this.showDurability   = sgGeneral.add(new BooleanSetting("showDurability", true));
        this.showHeldItem     = sgGeneral.add(new BooleanSetting("showHeldItem", false));
        this.durabilityFormat = sgGeneral.add(new EnumSetting<>("durabilityFormat", DurabilityFormat.PERCENT));
        this.colorDurability  = sgGeneral.add(new BooleanSetting("colorDurability", true));
        this.alignment        = sgGeneral.add(new EnumSetting<>("alignment", Alignment.LEFT));
        this.scale            = sgGeneral.add(new IntegerSetting("scale", 100).min(25).max(400));
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
        int oldWidth = lastWidth == -1 ? newWidth : lastWidth;
        lastWidth = newWidth;
        this.resize(newWidth, newHeight);
        if (lastWidth != -1 && newWidth != oldWidth) {
            int dx = newWidth - oldWidth;
            Alignment align = alignment.get();
            if (align == Alignment.CENTER) {
                this.move(this.getRelativePosition().x() - dx / 2, this.getRelativePosition().y());
            } else if (align == Alignment.RIGHT) {
                this.move(this.getRelativePosition().x() - dx, this.getRelativePosition().y());
            }
        }
    }

    private static int durabilityColor(int remaining, int max) {
        float p = max <= 0 ? 1f : remaining / (float) max;
        if (p > 0.6f) return COL_GREEN;   // high durability
        if (p > 0.3f) return COL_ORANGE;  // medium durability
        return COL_RED;                   // low durability
    }

    private static ItemStack previewStack(Item item, int damage) {
        // safeStack guards against unbound item holders (e.g. HUD editor placeholder rendering)
        ItemStack stack = ItemStackUtil.safeStack(item);
        if (stack.isEmpty()) return stack;
        if (stack.isDamageableItem()) stack.setDamageValue(damage);
        return stack;
    }

    private static int alignStart(Alignment align, int contentW, int lineW) {
        return switch (align) {
            case CENTER -> (contentW - lineW) / 2;
            case RIGHT  -> contentW - lineW;
            default     -> 0;
        };
    }

    /** Renders an empty inventory slot - used as the icon placeholder in the editor before items bind. */
    private static void renderEmptySlot(GuiGraphicsExtractor context, int x, int y) {
        context.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, COL_SLOT_BG);
        context.fill(x, y, x + ICON_SIZE, y + 1, COL_SLOT_BORDER);
        context.fill(x, y + ICON_SIZE - 1, x + ICON_SIZE, y + ICON_SIZE, COL_SLOT_BORDER);
        context.fill(x, y, x + 1, y + ICON_SIZE, COL_SLOT_BORDER);
        context.fill(x + ICON_SIZE - 1, y, x + ICON_SIZE, y + ICON_SIZE, COL_SLOT_BORDER);
    }

    /** Renders a real item icon at 2x size (32x32) via a scaled pose around the model render. */
    private static void renderIcon(Matrix3x2fStack pose, GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(ICON_SCALE, ICON_SCALE);
        context.fakeItem(stack, 0, 0);
        pose.popMatrix();
    }

    private List<ItemStack> wornArmor(Player player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) stacks.add(stack);
        }
        return stacks;
    }

    /**
     * Builds the rows to display, honoring the held-item toggle. The held item is
     * always last. When {@code feetFirst} is true the armor part is reversed so the
     * feet land at the bottom (vertical layout); otherwise the classic head-to-feet
     * order is kept (horizontal layout).
     */
    private List<Row> buildRows(List<ItemStack> armor, ItemStack heldStack, boolean held, boolean feetFirst) {
        List<Row> rows = new ArrayList<>();
        if (feetFirst) {
            for (int i = armor.size() - 1; i >= 0; i--) {
                Row row = Row.fromStack(armor.get(i));
                if (row != null) rows.add(row);
            }
        } else {
            for (ItemStack stack : armor) {
                Row row = Row.fromStack(stack);
                if (row != null) rows.add(row);
            }
        }
        if (held && !heldStack.isEmpty()) {
            Row row = Row.fromStack(heldStack);
            if (row != null) rows.add(row);
        }
        return rows;
    }

    /** Durability color of a row, or the default text color when coloring is off. */
    private int durabilityColor(Row row, boolean color) {
        return color && row.damageable ? durabilityColor(row.remaining, row.max) : COL_NAME;
    }

    @Override
    public void renderContent(GuiGraphicsExtractor context, int x, int y, int width, int height, int screenWidth, int screenHeight, float delta, boolean inEditor) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        boolean horizontal = orientation.get() == Orientation.HORIZONTAL;
        boolean dur = showDurability.get();
        boolean held = showHeldItem.get();
        DurabilityFormat format = durabilityFormat.get();
        boolean color = colorDurability.get();
        Alignment align = alignment.get();
        float scaleF = scale.get() / 100f;

        List<Row> rows;
        if (inEditor) {
            // Sample values so the HUD editor preview shows the element: all 4 armor
            // pieces + the held item when its toggle is on.
            ItemStack helmet = previewStack(Items.DIAMOND_HELMET, 60);       // ~83%
            ItemStack chest  = previewStack(Items.IRON_CHESTPLATE, 120);     // 50% (orange)
            ItemStack legs   = previewStack(Items.IRON_LEGGINGS, 135);       // ~40%
            ItemStack boots  = previewStack(Items.LEATHER_BOOTS, 45);        // ~31% (red)

            if (!helmet.isEmpty() && !chest.isEmpty() && !legs.isEmpty() && !boots.isEmpty()) {
                // Items are usable (components bound) - show the real preview with icons
                ItemStack heldStack = held ? previewStack(Items.DIAMOND_SWORD, 1000) : ItemStack.EMPTY;
                rows = buildRows(List.of(helmet, chest, legs, boots), heldStack, held, !horizontal);
            } else {
                // HUD editor renders before item components are bound - icon-only preview
                // (empty slot placeholders + durability, never item names).
                rows = new ArrayList<>();
                if (horizontal) {
                    rows.add(new Row(ItemStack.EMPTY, true, 303, 363));  // helmet
                    rows.add(new Row(ItemStack.EMPTY, true, 120, 240));  // chestplate
                    rows.add(new Row(ItemStack.EMPTY, true, 90, 225));   // leggings
                    rows.add(new Row(ItemStack.EMPTY, true, 20, 65));    // boots
                } else {
                    rows.add(new Row(ItemStack.EMPTY, true, 20, 65));    // boots (bottom)
                    rows.add(new Row(ItemStack.EMPTY, true, 90, 225));   // leggings
                    rows.add(new Row(ItemStack.EMPTY, true, 120, 240));  // chestplate
                    rows.add(new Row(ItemStack.EMPTY, true, 303, 363));  // helmet
                }
                if (held) rows.add(new Row(ItemStack.EMPTY, true, 561, 1561)); // held last
            }
        } else {
            Player player = mc.player;
            if (player == null) {
                hideOffscreen();
                return;
            }
            rows = buildRows(wornArmor(player), player.getMainHandItem(), held, !horizontal);
        }

        if (rows.isEmpty()) {
            hideOffscreen();
            return;
        }
        restorePosition();

        // ---------------- HORIZONTAL (columns side by side) ----------------
        if (horizontal) {
            // Each column: icon (or empty slot placeholder) on top, durability text under it
            int maxColW = ICON_SIZE;
            for (Row row : rows) {
                String text = dur ? row.durabilityText(format) : null;
                if (text != null) maxColW = Math.max(maxColW, font.width(text));
            }
            int colW = maxColW + COL_GAP;

            int colContentH = ICON_SIZE;
            if (dur) colContentH += 2 + font.lineHeight; // top + durability text

            int unscaledW = PAD_X * 2 + rows.size() * colW - COL_GAP;
            int unscaledH = PAD_Y * 2 + colContentH;
            anchoredResize(Math.round(unscaledW * scaleF), Math.round(unscaledH * scaleF));

            var pose = context.pose();
            pose.pushMatrix();
            pose.translate(x, y);
            pose.scale(scaleF, scaleF);

            int colX = PAD_X;
            for (Row row : rows) {
                int centerX = colX + (colW - ICON_SIZE) / 2;
                if (row.icon.isEmpty()) {
                    renderEmptySlot(context, centerX, PAD_Y);
                } else {
                    renderIcon(pose, context, row.icon, centerX, PAD_Y);
                }

                String text = dur ? row.durabilityText(format) : null;
                if (text != null) {
                    int textX = colX + (colW - font.width(text)) / 2;
                    context.text(font, text, textX, PAD_Y + ICON_SIZE + 2, durabilityColor(row, color));
                }
                colX += colW;
            }

            pose.popMatrix();
            return;
        }

        // ---------------- VERTICAL (bottom-anchored list) ----------------
        // The element always reserves slotCount rows. Equipped pieces are packed from
        // the BOTTOM of the element upward, so even a single equipped piece (e.g. just
        // a helmet) sits at the bottom, and new pieces stack above it without moving.
        int slotCount = 4 + (held ? 1 : 0);

        int maxRowW = 0;
        for (Row row : rows) {
            String text = dur ? row.durabilityText(format) : null;
            int rowW = ICON_SIZE;
            if (text != null) rowW += GAP + font.width(text);
            maxRowW = Math.max(maxRowW, rowW);
        }

        int unscaledW = PAD_X * 2 + maxRowW;
        int unscaledH = PAD_Y * 2 + slotCount * LINE_HEIGHT;
        anchoredResize(Math.round(unscaledW * scaleF), Math.round(unscaledH * scaleF));

        var pose = context.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scaleF, scaleF);

        // rows[0] is the bottom-most piece (feet); each following row sits above it
        int slotY = unscaledH - PAD_Y - LINE_HEIGHT;
        for (Row row : rows) {
            String text = dur ? row.durabilityText(format) : null;
            int rowW = ICON_SIZE;
            if (text != null) rowW += GAP + font.width(text);
            int startX = PAD_X + alignStart(align, maxRowW, rowW);

            int curX = startX;
            int textY = slotY + TEXT_OFFSET;

            if (row.icon.isEmpty()) {
                renderEmptySlot(context, curX, slotY);
            } else {
                renderIcon(pose, context, row.icon, curX, slotY);
            }
            curX += ICON_SIZE;

            if (text != null) {
                context.text(font, text, curX + GAP, textY, durabilityColor(row, color));
            }

            slotY -= LINE_HEIGHT;
        }

        pose.popMatrix();
    }

    @Override
    public ArmorHudElement duplicate() {
        return new ArmorHudElement();
    }

    @Override
    public String getName() {
        return "Armor HUD+";
    }
}
