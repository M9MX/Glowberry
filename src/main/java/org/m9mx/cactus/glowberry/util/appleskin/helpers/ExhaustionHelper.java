package org.m9mx.cactus.glowberry.util.appleskin.helpers;
/**
 * Credits: https://github.com/squeek502/AppleSkin/tree/1.21.11-fabric
 */
import net.minecraft.world.entity.player.Player;

public class ExhaustionHelper
{
	public interface ExhaustionManipulator
	{
		float getExhaustion();

		void setExhaustion(float exhaustion);
	}

	public static float getExhaustion(Player player)
	{
		return ((ExhaustionManipulator) player.getFoodData()).getExhaustion();
	}

	public static void setExhaustion(Player player, float exhaustion)
	{
		((ExhaustionManipulator) player.getFoodData()).setExhaustion(exhaustion);
	}
}