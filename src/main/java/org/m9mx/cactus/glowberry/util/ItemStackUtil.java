package org.m9mx.cactus.glowberry.util;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Helpers for constructing {@link ItemStack}s safely.
 *
 * <p>In 26.2 item components are bound to registry holders lazily. During HUD
 * editor placeholder rendering (and other early client screens) the holder's
 * components may not be bound yet, and {@code new ItemStack(...)} throws
 * {@code NullPointerException} ("Components not bound yet"). Always go through
 * {@link #safeStack(ItemLike)} so an unbound holder yields {@link ItemStack#EMPTY}
 * instead of crashing the game.
 */
public final class ItemStackUtil {
    private ItemStackUtil() {
    }

    /** Returns a stack for the given item-like, or {@link ItemStack#EMPTY} if its holder is not ready. */
    public static ItemStack safeStack(ItemLike itemLike) {
        if (itemLike == null) {
            return ItemStack.EMPTY;
        }
        try {
            Item item = itemLike.asItem();
            if (item == null) {
                return ItemStack.EMPTY;
            }
            Holder.Reference<Item> holder = item.builtInRegistryHolder();
            if (holder == null || !holder.areComponentsBound()) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
