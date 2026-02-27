package org.m9mx.cactus.glowberry.util.appleskin.helpers;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;

public record ConsumableFood(FoodProperties food, Consumable consumable) {
	public float getSaturationIncrement() {
		return food.saturation() * 2.0F;
	}
}