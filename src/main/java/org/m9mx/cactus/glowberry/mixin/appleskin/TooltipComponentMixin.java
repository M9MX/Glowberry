package org.m9mx.cactus.glowberry.mixin.appleskin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.m9mx.cactus.glowberry.util.appleskin.client.TooltipOverlayHandler;

@Mixin(ClientTooltipComponent.class)
public interface TooltipComponentMixin
{
	// This allows AppleSkin to add its tooltip as a FormattedCharSequence, which gets converted
	// into our custom ClientTooltipComponent implementation during ClientTooltipComponent::of
	@Inject(
		at = @At("HEAD"),
		method = "create(Lnet/minecraft/util/FormattedCharSequence;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
		cancellable = true
	)
	private static void AppleSkin_create(FormattedCharSequence text, CallbackInfoReturnable<ClientTooltipComponent> info)
	{
		if (text instanceof TooltipOverlayHandler.FoodOverlayTextComponent)
        {
            info.setReturnValue(((TooltipOverlayHandler.FoodOverlayTextComponent) text).foodOverlay);
		}
	}

	// Also allow for TooltipComponent conversion, needed for compatibility since we do
	// FormattedCharSequence -> TooltipComponent -> ClientTooltipComponent
	@Inject(
		at = @At("HEAD"),
		method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
		cancellable = true
	)
	private static void AppleSkin_createComponent(TooltipComponent data, CallbackInfoReturnable<ClientTooltipComponent> info)
	{
		if (data instanceof TooltipOverlayHandler.FoodOverlay)
		{
			info.setReturnValue((ClientTooltipComponent) data);
		}
	}
}