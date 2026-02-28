package org.m9mx.cactus.glowberry.mixin.appleskin;
/**
 * Credits: https://github.com/squeek502/AppleSkin/tree/1.21.11-fabric
 */
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.m9mx.cactus.glowberry.util.appleskin.network.SyncHandler;
import org.m9mx.cactus.glowberry.feature.modules.AppleSkinModule;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Entity
{
	public ServerPlayerEntityMixin(EntityType<?> entityType, Level level)
	{
		super(entityType, level);
	}

	@Inject(at = @At("HEAD"), method = "tick")
	void onUpdate(CallbackInfo info)
	{
		// Only sync AppleSkin data if module is active
		if (AppleSkinModule.INSTANCE != null && AppleSkinModule.INSTANCE.active()) {
			ServerPlayer player = (ServerPlayer) (Object) this;
			SyncHandler.onPlayerUpdate(player);
		}
	}
}