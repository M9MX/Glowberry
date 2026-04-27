package org.m9mx.cactus.glowberry.feature.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.m9mx.cactus.glowberry.mixin.InventoryAccessor;

public class ShuffleModule extends Module {
    public static volatile ShuffleModule INSTANCE;

    public ShuffleModule(Category category) {
        super("shuffle", category, new Module.Options().set(Flag.SERVER_UNSAFE, true));
        if (INSTANCE == null) {
            synchronized (ShuffleModule.class) {
                if (INSTANCE == null) {
                    INSTANCE = this;
                }
            }
        }
    }

    @Override
    public void onEnable() {
        // Shuffle runs passively from placement hooks.
    }

    @Override
    public void onDisable() {
        // No persistent state to clear.
    }

    public void onBlockPlaced() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }

        Inventory inventory = mc.player.getInventory();
        int currentSlot = ((InventoryAccessor) inventory).getSelected();
        Item currentItem = inventory.getItem(currentSlot).getItem();

        List<Integer> differentBlockSlots = new ArrayList<>();
        List<Integer> otherBlockSlots = new ArrayList<>();

        for (int slot = 0; slot < 9; slot++) {
            if (slot == currentSlot) {
                continue;
            }

            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
                continue;
            }

            otherBlockSlots.add(slot);
            if (stack.getItem() != currentItem) {
                differentBlockSlots.add(slot);
            }
        }

        List<Integer> targetPool = !differentBlockSlots.isEmpty() ? differentBlockSlots : otherBlockSlots;
        if (targetPool.isEmpty()) {
            return;
        }

        int nextSlot = targetPool.get(ThreadLocalRandom.current().nextInt(targetPool.size()));
        ((InventoryAccessor) inventory).setSelected(nextSlot);
    }
}

