package org.m9mx.cactus.glowberry.feature.commands;

import com.dwarslooper.cactus.client.feature.command.Command;
import com.dwarslooper.cactus.client.util.game.ChatUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Client-side {@code #privateshare} command that whispers coordinates
 * to a target player via vanilla {@code /w} (whisper).
 *
 * Usage:
 *   - {@code #privateshare <player> location}   whisper your position to a player
 */
public class PrivateShareCommand extends Command {

    public PrivateShareCommand() {
        super("privateshare");
    }

    @Override
    public void build(LiteralArgumentBuilder builder) {
        // #privateshare <player> location
        builder.then(argument("player", StringArgumentType.word())
                .then(literal("location").executes(context -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.level == null) return SINGLE_SUCCESS;

                    String target = StringArgumentType.getString(context, "player");

                    int x = mc.player.getBlockX();
                    int y = mc.player.getBlockY();
                    int z = mc.player.getBlockZ();
                    String dim = dimDisplayName(mc.level.dimension());

                    String whisper = "/w " + target + " [PrivateShare] I am at X: " + x + ", Y: " + y + ", Z: " + z + " in the " + dim + ".";
                    mc.player.connection.sendCommand(whisper.substring(1));
                    return SINGLE_SUCCESS;
                })));

        // bare #privateshare -> usage
        builder.executes(context -> {
            ChatUtils.infoPrefix("PrivateShare", "§6§lPrivateShare §7- usage:");
            ChatUtils.info("§aprivateshare <player> location§7 - whisper your coordinates to a player");
            return SINGLE_SUCCESS;
        });
    }

    private static String dimDisplayName(ResourceKey<Level> dim) {
        if (dim.equals(Level.OVERWORLD)) return "Overworld";
        if (dim.equals(Level.NETHER))   return "Nether";
        if (dim.equals(Level.END))      return "The End";

        // This perfectly satisfies the Identifier class requirement without an AW
        return dim.registry().getPath();
    }
}
