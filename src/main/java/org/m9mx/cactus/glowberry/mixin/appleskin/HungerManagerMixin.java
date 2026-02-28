package org.m9mx.cactus.glowberry.mixin.appleskin;
/**
 * Credits: https://github.com/squeek502/AppleSkin/tree/1.21.11-fabric
 */
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.m9mx.cactus.glowberry.util.appleskin.helpers.ExhaustionHelper;

@Mixin(FoodData.class)
public class HungerManagerMixin implements ExhaustionHelper.ExhaustionManipulator
{
	@Shadow
	private float exhaustionLevel;

	@Override
	public void setExhaustion(float value)
	{
		this.exhaustionLevel = value;
	}

	@Override
	public float getExhaustion()
	{
		return this.exhaustionLevel;
	}
}