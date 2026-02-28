package org.m9mx.cactus.glowberry.util.appleskin.client;
/**
 * Credits: https://github.com/squeek502/AppleSkin/tree/1.21.11-fabric
 */
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.m9mx.cactus.glowberry.util.appleskin.helpers.ExhaustionHelper;
import org.m9mx.cactus.glowberry.util.appleskin.helpers.FoodHelper;
import org.m9mx.cactus.glowberry.feature.modules.AppleSkinModule;

import java.text.DecimalFormat;

public class DebugInfoHudEntry implements DebugScreenEntry
{
	public static final Identifier ENTRY_ID = Identifier.fromNamespaceAndPath("appleskin", "food_stats");

	private static final DecimalFormat saturationDF = new DecimalFormat("#.##");
	private static final DecimalFormat exhaustionValDF = new DecimalFormat("0.00");
	private static final DecimalFormat exhaustionMaxDF = new DecimalFormat("#.##");

	@Override
	public void display(DebugScreenDisplayer displayer, @Nullable Level world, @Nullable LevelChunk clientChunk, @Nullable LevelChunk chunk)
	{
		// Only show debug info if module is active
		if (AppleSkinModule.INSTANCE == null || !AppleSkinModule.INSTANCE.active())
			return;

		if (world != null) {
			Minecraft mc = Minecraft.getInstance();
			if (mc == null || mc.player == null)
				return;

			Player player = mc.player;
			if (player == null) {
				return;
		}

			float curExhaustion = ExhaustionHelper.getExhaustion(player);
			float maxExhaustion = FoodHelper.MAX_EXHAUSTION;
			
			displayer.addLine("hunger: " + player.getFoodData().getFoodLevel() + 
			              ", sat: " + saturationDF.format(player.getFoodData().getSaturationLevel()) + 
			              ", exh: " + exhaustionValDF.format(curExhaustion) + "/" + 
			              exhaustionMaxDF.format(maxExhaustion));
		}
	}
}