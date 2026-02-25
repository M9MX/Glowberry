/*
 * Adapted from TotemCounter mod by uku3lig
 * Original mod: https://github.com/uku3lig/totemcounter
 */
package org.m9mx.cactus.glowberry.mixin.totemcounter;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.m9mx.cactus.glowberry.feature.modules.TotemCounterModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerTabOverlay.class)
public class TotemCounterPlayerTabOverlayMixin {
    @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
    public Component addPopCounter(Component original, @Local(argsOnly = true) PlayerInfo entry) {
        if (TotemCounterModule.INSTANCE == null || !TotemCounterModule.INSTANCE.active()) return original;
        if (!TotemCounterModule.INSTANCE.counterEnabled.get()) return original;

        Level world = Minecraft.getInstance().level;
        if (world != null) {
            Player entity = world.getPlayerByUUID(entry.getProfile().id());
            if (entity != null) {
                if (!entity.isAlive()) TotemCounterModule.getPops().remove(entity.getUUID());
                return showPopsInText(entity, original);
            }
        }

        return original;
    }

    private Component showPopsInText(Player entity, Component text) {
        if (TotemCounterModule.INSTANCE == null || !TotemCounterModule.INSTANCE.active()) return text;
        if (!TotemCounterModule.INSTANCE.counterEnabled.get()) return text;

        if (TotemCounterModule.getPops().containsKey(entity.getUUID())) {
            int pops = TotemCounterModule.getPops().get(entity.getUUID());

            MutableComponent label = text.copy().append(" ");
            MutableComponent counter = Component.literal("-" + pops);
            if (TotemCounterModule.INSTANCE.separator.get()) {
                label.append(Component.literal("| ").withStyle(s -> s.withColor(0xFF808080)));
            }
            if (TotemCounterModule.INSTANCE.counterColors.get()) {
                counter.setStyle(Style.EMPTY.withColor(TotemCounterModule.getPopColor(pops)));
            }
            label.append(counter);
            text = label;
        }

        return text;
    }
}
