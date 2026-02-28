package org.m9mx.cactus.glowberry.mixin.appleskin;
/**
 * Credits: https://github.com/squeek502/AppleSkin/tree/1.21.11-fabric
 */
import net.minecraft.network.Connection;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.util.appleskin.network.SyncHandler;
import org.m9mx.cactus.glowberry.feature.modules.AppleSkinModule;

@Mixin(PlayerList.class)
public class PlayerManagerMixin
{
	@Inject(at = @At("TAIL"), method = "placeNewPlayer")
	private void onPlayerConnect(Connection conn, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo info)
	{
		// Only sync AppleSkin data if module is active
		if (AppleSkinModule.INSTANCE != null && AppleSkinModule.INSTANCE.active()) {
			SyncHandler.onPlayerLoggedIn(player);
		}
	}
}