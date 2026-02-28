package org.m9mx.cactus.glowberry.util.appleskin.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.m9mx.cactus.glowberry.util.appleskin.helpers.ExhaustionHelper;
import org.m9mx.cactus.glowberry.feature.modules.AppleSkinModule;

public class ClientSyncHandler
{
	public static boolean naturalRegeneration = true;
	private static boolean initialized = false;

	@Environment(EnvType.CLIENT)
	public static void init()
	{
		if (initialized) return;
		initialized = true;

		// Register payload types on client side
		PayloadTypeRegistry.playS2C().register(ExhaustionSyncPayload.TYPE, ExhaustionSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SaturationSyncPayload.TYPE, SaturationSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(NaturalRegenerationSyncPayload.TYPE, NaturalRegenerationSyncPayload.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(ExhaustionSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				// Only apply AppleSkin sync if module is active
				if (AppleSkinModule.INSTANCE != null && AppleSkinModule.INSTANCE.active())
					ExhaustionHelper.setExhaustion(context.client().player, payload.exhaustion());
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(SaturationSyncPayload.TYPE, (payload, context) -> {
			context.client().execute(() -> {
				// Only apply AppleSkin sync if module is active
				if (AppleSkinModule.INSTANCE != null && AppleSkinModule.INSTANCE.active())
					context.client().player.getFoodData().setSaturation(payload.saturation());
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(NaturalRegenerationSyncPayload.TYPE, (payload, context) -> {
			// Only apply AppleSkin sync if module is active
			if (AppleSkinModule.INSTANCE != null && AppleSkinModule.INSTANCE.active())
				naturalRegeneration = payload.enabled();
		});
	}
}