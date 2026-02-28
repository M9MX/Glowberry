package org.m9mx.cactus.glowberry.util.appleskin.helpers;
/**
 * Credits: https://github.com/squeek502/AppleSkin/tree/1.21.11-fabric
 */
import net.minecraft.resources.Identifier;

public class TextureHelper
{
	public static final Identifier MOD_ICONS = Identifier.fromNamespaceAndPath("glowberry", "textures/icons.png");
	public static final Identifier HUNGER_OUTLINE_SPRITE = Identifier.fromNamespaceAndPath("glowberry", "textures/gui/sprites/tooltip_hunger_outline");

	// Hunger
	public static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "hud/food_empty_hunger");
	public static final Identifier FOOD_HALF_HUNGER_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "hud/food_half_hunger");
	public static final Identifier FOOD_FULL_HUNGER_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "hud/food_full_hunger");
	public static final Identifier FOOD_EMPTY_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "hud/food_empty");
	public static final Identifier FOOD_HALF_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "hud/food_half");
	public static final Identifier FOOD_FULL_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "hud/food_full");

	public enum FoodType
	{
		EMPTY,
		HALF,
		FULL,
	}

	public static Identifier getFoodTexture(boolean isRotten, FoodType type)
	{
		return switch (type)
		{
			case EMPTY -> isRotten ? FOOD_EMPTY_HUNGER_TEXTURE : FOOD_EMPTY_TEXTURE;
			case HALF -> isRotten ? FOOD_HALF_HUNGER_TEXTURE : FOOD_HALF_TEXTURE;
			case FULL -> isRotten ? FOOD_FULL_HUNGER_TEXTURE : FOOD_FULL_TEXTURE;
		};
	}

	// Hearts
	public static final Identifier HEART_CONTAINER = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/container");
	public static final Identifier HEART_HARDCORE_CONTAINER = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/container_hardcore");
	public static final Identifier HEART_FULL = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full");
	public static final Identifier HEART_HARDCORE_FULL = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/hardcore_full");
	public static final Identifier HEART_HALF = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/half");
	public static final Identifier HEART_HARDCORE_HALF = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/hardcore_half");

	public enum HeartType
	{
		CONTAINER,
		FULL,
		HALF,
	}

	public static Identifier getHeartTexture(boolean hardcore, HeartType type)
	{
		return switch (type)
		{
			case CONTAINER -> hardcore ? HEART_HARDCORE_CONTAINER : HEART_CONTAINER;
			case FULL -> hardcore ? HEART_HARDCORE_FULL : HEART_FULL;
			case HALF -> hardcore ? HEART_HARDCORE_HALF : HEART_HALF;
		};
	}
}