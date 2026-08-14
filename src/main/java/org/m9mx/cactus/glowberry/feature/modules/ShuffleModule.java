package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.event.EventHandler;
import com.dwarslooper.cactus.client.event.impl.ClientTickEvent;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.KeybindSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.key.KeyBind;

import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.lwjgl.glfw.GLFW;
import org.m9mx.cactus.glowberry.mixin.Modules.Shuffle.ShufflePlacementMixin;
import org.m9mx.cactus.glowberry.mixin.util.InventoryAccessor;
import org.m9mx.cactus.glowberry.util.ModuleMessageUtil;

/**
 * Client-side Shuffle module that toggles with a keybind (R by default).
 * When enabled and you place a block, it picks a random block stack from your
 * hotbar - weighted by how many blocks that stack holds - and switches to it.
 * The slot you are currently holding is part of the pool too, so the selection
 * may land on it again and the hotbar simply doesn't change.
 *
 * Based on the Shuffle mod by Dion Tryban (Trikzon).
 */
public class ShuffleModule extends Module {
    public static volatile ShuffleModule INSTANCE;

    private final SettingGroup generalGroup;
    public final Setting<KeyBind> toggleKeybind;
    public final Setting<Boolean> useWeightedRandom;
    public final Setting<Boolean> playSoundEffects;

    private boolean shuffleEnabled = false;
    private boolean keyWasDown = false;
    private int slotToSwitchTo = -1;

    public ShuffleModule(Category category) {
        super("shuffle", category, new Module.Options().set(Flag.SERVER_UNSAFE, false));
        INSTANCE = this;

        this.generalGroup = this.settings.buildGroup("general");
        this.toggleKeybind = this.generalGroup.add(new KeybindSetting("toggleKeybind", KeyBind.of(GLFW.GLFW_KEY_R)));
        this.useWeightedRandom = this.generalGroup.add(new BooleanSetting("useWeightedRandom", false));
        this.playSoundEffects = this.generalGroup.add(new BooleanSetting("playSoundEffects", true));
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {
        shuffleEnabled = false;
    }

    @EventHandler
    public void onTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Deferred slot switching — set from onBlockPlaced to avoid
        // modifying the inventory in the middle of block placement logic
        if (slotToSwitchTo >= 0 && slotToSwitchTo <= 8) {
            ((InventoryAccessor) mc.player.getInventory()).setSelected(slotToSwitchTo);
            slotToSwitchTo = -1;
        }

        // Toggle key handling
        boolean keyDown = isToggleKeyPressed();
        if (keyDown && !keyWasDown) {
            keyWasDown = true;
            shuffleEnabled = !shuffleEnabled;

            if (shuffleEnabled) {
                ModuleMessageUtil.show(Component.translatable("modules.shuffle.enabled"), 0xFF55FF55);
                if (playSoundEffects.get()) {
                    mc.player.playSound(SoundEvents.TRIPWIRE_CLICK_OFF, 0.5f, 1.0f);
                }
            } else {
                ModuleMessageUtil.show(Component.translatable("modules.shuffle.disabled"), 0xFFFF5555);
                if (playSoundEffects.get()) {
                    mc.player.playSound(SoundEvents.TRIPWIRE_CLICK_ON, 0.5f, 1.0f);
                }
            }
        } else if (!keyDown && keyWasDown) {
            keyWasDown = false;
        }
    }

    /**
     * Called from {@link ShufflePlacementMixin}
     * when a block placement is detected and the module is active.
     */
    public void onBlockPlaced() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!shuffleEnabled || mc.player.isSpectator()) return;

        NonNullList<ItemStack> items = mc.player.getInventory().getNonEquipmentItems();

        slotToSwitchTo = useWeightedRandom.get()
                ? pickSlot(items, mc.level.random, true)
                : pickSlot(items, mc.level.random, false);
    }

    /**
     * Randomly pick a hotbar slot that currently holds a placeable block. Every
     * eligible slot - including the one you are holding right now - is added to
     * the pool, so picking the current slot means the hotbar simply doesn't
     * change. When {@code weighted} is enabled each slot is weighted by its
     * stack size, so a stack of 64 blocks is far more likely to be chosen than
     * a stack of 1, while the smaller stack still gets picked from time to time.
     *
     * @param items the full non-equipment item list (hotbar is indices 0-8)
     * @param random a random source
     * @param weighted whether to weight the selection by stack size
     * @return the slot index to switch to, or -1 if no eligible block slot exists
     */
    private static int pickSlot(NonNullList<ItemStack> items, RandomSource random, boolean weighted) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return -1;

        var validSlotsBuilder = new WeightedList.Builder<Integer>();

        for (int slotIdx = 0; slotIdx < 9; slotIdx++) {
            ItemStack stack = items.get(slotIdx);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof BlockItem)) continue;
            if (Block.byItem(stack.getItem()) == Blocks.AIR) continue;

            // Weight = stack size when weighted is on (a just-used stack is one
            // smaller, so its chance of being re-picked drops slightly); otherwise
            // every eligible slot is equally likely.
            validSlotsBuilder.add(slotIdx, weighted ? Math.max(1, stack.getCount()) : 1);
        }

        var validSlots = validSlotsBuilder.build();
        return validSlots.getRandom(random).orElse(-1);
    }

    private boolean isToggleKeyPressed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen != null) return false;
        try {
            Object keybind = this.toggleKeybind.get();
            if (keybind instanceof KeyBind) {
                return ((KeyBind) keybind).isPressed();
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }
}
