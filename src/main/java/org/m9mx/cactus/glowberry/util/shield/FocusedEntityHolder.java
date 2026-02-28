package org.m9mx.cactus.glowberry.util.shield;
/**
 * Credits: https://github.com/Walksy/ShieldStatus
 */
import net.minecraft.world.entity.player.Player;

public class FocusedEntityHolder {
	public static Player focusedEntity;

	public static void setFocused(Player player) {
		focusedEntity = player;
	}

	public static Player getFocused() {
		return focusedEntity;
	}

	public static void clear() {
		focusedEntity = null;
	}
}
