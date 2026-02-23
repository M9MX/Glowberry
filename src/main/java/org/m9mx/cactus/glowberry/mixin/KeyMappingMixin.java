package org.m9mx.cactus.glowberry.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.m9mx.cactus.glowberry.accessor.IKeyBindingAccessor;
import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements IKeyBindingAccessor
{
	@Shadow
	private int clickCount;
	
	@Override
	public int glowberry_GetTimesPressed()
	{
		return clickCount;
	}

	@Override
	public void glowberry_SetTimesPressed(int count)
	{
		this.clickCount = count;
	}
}
