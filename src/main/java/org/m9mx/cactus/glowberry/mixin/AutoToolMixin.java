package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.m9mx.cactus.glowberry.feature.modules.AutoToolModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Credits: https://github.com/zelythia/AutoTools
 */
@Mixin(MultiPlayerGameMode.class)
public class AutoToolMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void onStartDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (AutoToolModule.INSTANCE != null && AutoToolModule.INSTANCE.active()) {
            if (minecraft.level != null && minecraft.player != null) {
                BlockState blockState = minecraft.level.getBlockState(pos);
                getCorrectBlockTool(blockState, minecraft);
                // Just select the correct tool, don't cancel the original call
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttack(Player player, Entity target, CallbackInfo ci) {
        if (AutoToolModule.INSTANCE != null && AutoToolModule.INSTANCE.active() && 
            AutoToolModule.INSTANCE.changeForEntities.get()) {
            getCorrectWeapon(minecraft);
            // Ensure we select a weapon again right after the attack starts
            // This ensures our weapon selection isn't overridden by base system
            if (minecraft.player != null) {
                getCorrectWeapon(minecraft);
            }
        }
        // Don't cancel the attack call - allow it to proceed normally
    }

    // Make sure our weapon selection takes priority after the attack
    @Inject(method = "attack", at = @At("RETURN"))
    private void onAttackReturn(Player player, Entity target, CallbackInfo ci) {
        if (AutoToolModule.INSTANCE != null && AutoToolModule.INSTANCE.active() && 
            AutoToolModule.INSTANCE.changeForEntities.get()) {
            // Run our weapon selection again to ensure it's maintained
            getCorrectWeapon(minecraft);
        }
    }

    private void getCorrectBlockTool(BlockState blockState, Minecraft client) {
        if (client.player == null) return;
        
        Inventory inventory = client.player.getInventory();
        
        int bestSlot = -1;
        float bestSpeed = -1;
        
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            Item item = stack.getItem();
            
            if (item == Items.AIR) continue;
            
            // Check if item is appropriate for this block and if the tool category is enabled in settings
            if (isToolCategoryEnabled(item) && 
                (item.isCorrectToolForDrops(stack, blockState) || !blockState.requiresCorrectToolForDrops())) {
                float destroySpeed = stack.getDestroySpeed(blockState);
                
                if (destroySpeed > bestSpeed) {
                    bestSpeed = destroySpeed;
                    bestSlot = i;
                }
            }
        }
        
        if (bestSlot != -1 && bestSlot != ((InventoryAccessor) inventory).getSelected()) {
            ((InventoryAccessor) inventory).setSelected(bestSlot);
        }
    }
    
    private void getCorrectWeapon(Minecraft client) {
        if (client.player == null) return;
        
        Inventory inventory = client.player.getInventory();
        
        int bestSlot = -1;
        float bestDamage = -1;
        
        // Only consider swords (not axes) for entity combat based on module settings
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            Item item = stack.getItem();
            
            if (item == Items.AIR) continue;
            
            // Check if it's a sword by checking the actual item type using is
            boolean isSword = stack.is(Items.WOODEN_SWORD) ||
                              stack.is(Items.STONE_SWORD) ||
                              stack.is(Items.IRON_SWORD) ||
                              stack.is(Items.GOLDEN_SWORD) ||
                              stack.is(Items.DIAMOND_SWORD) ||
                              stack.is(Items.NETHERITE_SWORD);
                            
            // Explicitly check that it's not a pickaxe
            boolean isPickaxe = stack.is(Items.WOODEN_PICKAXE) ||
                                stack.is(Items.STONE_PICKAXE) ||
                                stack.is(Items.IRON_PICKAXE) ||
                                stack.is(Items.GOLDEN_PICKAXE) ||
                                stack.is(Items.DIAMOND_PICKAXE) ||
                                stack.is(Items.NETHERITE_PICKAXE);

            // Only consider swords if swords setting is enabled and it's not a pickaxe
            if (isSword && !isPickaxe && AutoToolModule.INSTANCE.swords.get()) {
                float damage = getApproximateDamage(stack, item);
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSlot = i;
                }
            } 
        }
        
        // Only switch if we found a suitable weapon (sword) that's enabled in settings
        if (bestSlot != -1 && bestSlot != ((InventoryAccessor) inventory).getSelected()) {
            ((InventoryAccessor) inventory).setSelected(bestSlot);
        }
    }

    private boolean isToolCategoryEnabled(Item item) {
        // Check the actual item using is method
        ItemStack stack = new ItemStack(item);
        
        boolean isPickaxe = stack.is(Items.WOODEN_PICKAXE) ||
                            stack.is(Items.STONE_PICKAXE) ||
                            stack.is(Items.IRON_PICKAXE) ||
                            stack.is(Items.GOLDEN_PICKAXE) ||
                            stack.is(Items.DIAMOND_PICKAXE) ||
                            stack.is(Items.NETHERITE_PICKAXE);
                            
        boolean isAxe = stack.is(Items.WOODEN_AXE) ||
                        stack.is(Items.STONE_AXE) ||
                        stack.is(Items.IRON_AXE) ||
                        stack.is(Items.GOLDEN_AXE) ||
                        stack.is(Items.DIAMOND_AXE) ||
                        stack.is(Items.NETHERITE_AXE);
                        
        boolean isShovel = stack.is(Items.WOODEN_SHOVEL) ||
                           stack.is(Items.STONE_SHOVEL) ||
                           stack.is(Items.IRON_SHOVEL) ||
                           stack.is(Items.GOLDEN_SHOVEL) ||
                           stack.is(Items.DIAMOND_SHOVEL) ||
                           stack.is(Items.NETHERITE_SHOVEL);
                           
        boolean isHoe = stack.is(Items.WOODEN_HOE) ||
                        stack.is(Items.STONE_HOE) ||
                        stack.is(Items.IRON_HOE) ||
                        stack.is(Items.GOLDEN_HOE) ||
                        stack.is(Items.DIAMOND_HOE) ||
                        stack.is(Items.NETHERITE_HOE);
                        
        boolean isSword = stack.is(Items.WOODEN_SWORD) ||
                          stack.is(Items.STONE_SWORD) ||
                          stack.is(Items.IRON_SWORD) ||
                          stack.is(Items.GOLDEN_SWORD) ||
                          stack.is(Items.DIAMOND_SWORD) ||
                          stack.is(Items.NETHERITE_SWORD);

        // Only enable tool categories if they're enabled in the module settings
        if (isPickaxe && AutoToolModule.INSTANCE.pickaxes.get()) {
            return true;
        } else if (isAxe && !isPickaxe && AutoToolModule.INSTANCE.axes.get()) {
            return true; // Only treat as axe if it's not also a pickaxe
        } else if (isShovel && AutoToolModule.INSTANCE.shovels.get()) {
            return true;
        } else if (isHoe && AutoToolModule.INSTANCE.hoes.get()) {
            return true;
        } else if (isSword && AutoToolModule.INSTANCE.swords.get()) {
            return true;
        }
        
        return false;
    }
    
    private float getApproximateDamage(ItemStack stack, Item item) {
        // Base damage based on material using is checks
        if (stack.is(Items.NETHERITE_SWORD)) {
            return 8.0f;
        } else if (stack.is(Items.DIAMOND_SWORD)) {
            return 7.0f;
        } else if (stack.is(Items.IRON_SWORD)) {
            return 6.0f;
        } else if (stack.is(Items.STONE_SWORD)) {
            return 5.0f;
        } else if (stack.is(Items.WOODEN_SWORD) || stack.is(Items.GOLDEN_SWORD)) {
            return 4.0f;
        } else if (stack.is(Items.NETHERITE_AXE)) {
            return 10.0f;
        } else if (stack.is(Items.DIAMOND_AXE)) {
            return 9.0f;
        } else if (stack.is(Items.IRON_AXE)) {
            return 8.0f;
        } else if (stack.is(Items.STONE_AXE)) {
            return 7.0f;
        } else if (stack.is(Items.WOODEN_AXE) || stack.is(Items.GOLDEN_AXE)) {
            return 6.0f;
        }
        
        return 1.0f; // Default fallback
    }
}