package org.m9mx.cactus.glowberry.mixin;

import net.minecraft.world.entity.animal.equine.AbstractHorse; // Updated package for 1.21.11
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.Minecraft;
import org.m9mx.cactus.glowberry.util.ActionBarUtil;
import org.m9mx.cactus.glowberry.feature.modules.HorseStatsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractHorse.class)
public class HorseStatsMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void onHorseInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        // Ensure we are only running this logic on the client-side player thread
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player != player) { 
            return;
        }

        AbstractHorse horse = (AbstractHorse) (Object) this;
        
        // Check if the module is active
        if (HorseStatsModule.INSTANCE != null && HorseStatsModule.INSTANCE.active()) {
            if (hand == InteractionHand.MAIN_HAND) { // Check main hand to avoid double triggers
                ItemStack itemInHand = player.getItemInHand(hand);
                
                // Only show stats if not trying to use specific items (Saddle, Chest, or Horse Armor)
                if (itemInHand.isEmpty() || (!itemInHand.is(Items.SADDLE) && !itemInHand.is(Items.CHEST))) {
                    
                    // 1.21.11 Attribute access
                    double jumpStrength = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
                    double movementSpeed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    double maxHealth = horse.getAttributeValue(Attributes.MAX_HEALTH);
                    
                    // Math for 1.21.11: Jump height in blocks
                    // Formula: -0.1817584952 * x^3 + 3.689713992 * x^2 + 2.128599134 * x - 0.343930367
                    // But the standard simplified approximation is:
                    double jumpHeight = -1.291 * Math.pow(jumpStrength, 2) + 4.707 * jumpStrength - 0.016;
                    
                    // Formatting the string
                    // Speed: internal value * 42.15 = Blocks Per Second
                    String statsMessage = String.format(
                        "§6Horse Stats §7| §bSpeed: §f%.2f b/s §7| §aJump: §f%.1f blocks §7| §cHealth: §f%.0f",
                        movementSpeed * 42.15, 
                        jumpHeight,
                        maxHealth
                    );
                    
                    // Send via your fixed ActionBarUtil
                    ActionBarUtil.sendActionBarMessageWithDuration(statsMessage, 60); // 3 seconds
                }
            }
        }
    }
}