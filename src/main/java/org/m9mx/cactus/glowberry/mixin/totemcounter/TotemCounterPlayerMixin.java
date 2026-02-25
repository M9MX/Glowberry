/*
 * Adapted from TotemCounter mod by uku3lig
 * Original mod: https://github.com/uku3lig/totemcounter
 * DISABLED: Shows in tab list and nameplates instead (not in chat)
 */
package org.m9mx.cactus.glowberry.mixin.totemcounter;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.class)
public class TotemCounterPlayerMixin {
    // Pop counter shows in:
    // - Tab list (PlayerTabOverlayMixin)
    // - Nameplates (TextDisplayRenderer mixin)
    // NOT in chat messages
}
