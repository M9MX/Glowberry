package org.m9mx.cactus.glowberry.mixin;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.m9mx.cactus.glowberry.feature.overlay.LightLevelOverlayHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Minecraft.class)
public class ClientTickOptimizerMixin {
    
    @Unique
    private static final int UPDATE_INTERVAL_TICKS = 10; // Update every 10 ticks (0.5 seconds at 20 TPS)
    @Unique
    private static int tickCounter = 0;
    
    // Initialize the event handler in the static block
    static {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            tickCounter++;
            if (tickCounter >= UPDATE_INTERVAL_TICKS) {
                tickCounter = 0;
                // Only update if the light level feature is active
                if (LightLevelOverlayHandler.isActive() && client.player != null && client.level != null) {
                    LightLevelOverlayHandler.updateBlocksInRadiusOptimized();
                }
            }
        });
    }
}