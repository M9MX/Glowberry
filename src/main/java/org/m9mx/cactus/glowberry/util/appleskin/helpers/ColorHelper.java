package org.m9mx.cactus.glowberry.util.appleskin.helpers;
/**
 * Credits: https://github.com/squeek502/AppleSkin/tree/1.21.11-fabric
 */
import net.minecraft.util.Mth;

public class ColorHelper
{
	public static int argbFromRGBA(float r, float g, float b, float a)
	{
		return (Mth.floor(a * 255.0F) << 24) |
			(Mth.floor(r * 255.0F) << 16) |
			(Mth.floor(g * 255.0F) << 8) |
			Mth.floor(b * 255.0F);
	}
}