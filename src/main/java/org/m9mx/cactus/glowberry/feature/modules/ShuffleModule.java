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
import org.m9mx.cactus.glowberry.util.ActionBarUtil;

import java.util.function.BiFunction;

/**
 * Client-side Shuffle module that toggles with a keybind (R by default).
 * When enabled and you place a block, it automatically switches to a random
 * different block in your hotbar.
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
                ActionBarUtil.sendActionBarMessage(Component.translatable("modules.shuffle.enabled").getString());
                if (playSoundEffects.get()) {
                    mc.player.playSound(SoundEvents.TRIPWIRE_CLICK_OFF, 0.5f, 1.0f);
                }
            } else {
                ActionBarUtil.sendActionBarMessage(Component.translatable("modules.shuffle.disabled").getString());
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

        if (useWeightedRandom.get()) {
            slotToSwitchTo = switchSlotWeighted(items, mc.level.random);
        } else {
            slotToSwitchTo = switchSlotRandom(items, mc.level.random);
        }
    }

    /**
     * Randomly choose a hotbar slot with equal weight per slot.
     */
    private static int switchSlotRandom(NonNullList<ItemStack> items, RandomSource random) {
        return switchSlotLogic(items, random, (slotIdx, stack) -> 1);
    }

    /**
     * Randomly choose a hotbar slot using the item count as weight.
     */
    private static int switchSlotWeighted(NonNullList<ItemStack> items, RandomSource random) {
        return switchSlotLogic(items, random, (slotIdx, stack) -> stack.getCount());
    }

    /**
     * Choose a hotbar slot using a per-slot weight function.
     * Skips the currently selected slot, empty slots, and non-block items.
     *
     * @param items the full non-equipment item list (hotbar is indices 0-8)
     * @param random a random source
     * @param calculateWeight function that returns the weight for a given slot
     * @return the slot index to switch to, or -1 if no valid slot found
     */
    private static int switchSlotLogic(
            NonNullList<ItemStack> items,
            RandomSource random,
            BiFunction<Integer, ItemStack, Integer> calculateWeight
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return -1;

        int currentSlot = ((InventoryAccessor) mc.player.getInventory()).getSelected();
        var validSlotsBuilder = new WeightedList.Builder<Integer>();

        for (int slotIdx = 0; slotIdx < 9; slotIdx++) {
            if (slotIdx == currentSlot) continue;

            ItemStack stack = items.get(slotIdx);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof BlockItem)) continue;
            if (Block.byItem(stack.getItem()) == Blocks.AIR) continue;

            validSlotsBuilder.add(slotIdx, calculateWeight.apply(slotIdx, stack));
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
