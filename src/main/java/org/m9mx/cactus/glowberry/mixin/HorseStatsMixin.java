package org.m9mx.cactus.glowberry.mixin;


import net.minecraft.world.entity.animal.equine.AbstractHorse;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Mixin(AbstractHorse.class)
public class HorseStatsMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void onHorseInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player != player) { 
            return;
        }

        AbstractHorse horse = (AbstractHorse) (Object) this;
        
        if (HorseStatsModule.INSTANCE != null && HorseStatsModule.INSTANCE.active()) {
            if (hand == InteractionHand.MAIN_HAND) {
                ItemStack itemInHand = player.getItemInHand(hand);
                
                if (itemInHand.isEmpty() || (!itemInHand.is(Items.SADDLE) && !itemInHand.is(Items.CHEST))) {
                    
                    // Capture stats immediately at the moment of interaction
                    double jumpStrength = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
                    double movementSpeed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
                    double maxHealth = horse.getAttributeValue(Attributes.MAX_HEALTH);
                    
                    double jumpHeight = -1.291 * Math.pow(jumpStrength, 2) + 4.707 * jumpStrength - 0.016;
                    
                    String statsMessage = String.format(
                        "§6Horse Stats §7| §bSpeed: §f%.2f b/s §7| §aJump: §f%.1f blocks §7| §cHealth: §f%.0f",
                        movementSpeed * 42.15,
                        jumpHeight,
                        maxHealth
                    );

                    int displayDurationTicks = HorseStatsModule.INSTANCE.displayDuration.get() * 20;

                    // --- 10 TICKET DELAY LOGIC ---
                    // 10 ticks = 500ms (at 20tps)
                    CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS, mc).execute(() -> {
                        // Ensure player is still in-game after the delay
                        if (mc.player != null && mc.level != null) {
                            ActionBarUtil.sendActionBarMessageWithDuration(statsMessage, displayDurationTicks);
                        }
                    });
                }
            }
        }
    }
}