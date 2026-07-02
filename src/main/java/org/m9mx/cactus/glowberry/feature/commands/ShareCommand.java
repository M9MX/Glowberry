package org.m9mx.cactus.glowberry.feature.commands;

import com.dwarslooper.cactus.client.feature.command.Command;
import com.dwarslooper.cactus.client.util.game.ChatUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Client-side {@code #share} command that broadcasts your position
 * to the entire server through a vanilla chat message.
 *
 * Usage:
 *   - {@code #share location}         broadcast your current coordinates
 */
public class ShareCommand extends Command {

    public ShareCommand() {
        super("share");
    }

    @Override
    public void build(LiteralArgumentBuilder builder) {
        // #share location
        builder.then(literal("location").executes(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return SINGLE_SUCCESS;

            int x = mc.player.getBlockX();
            int y = mc.player.getBlockY();
            int z = mc.player.getBlockZ();
            String dim = dimDisplayName(mc.level.dimension());

            // No § codes — sendChat sends a raw packet to the server which rejects them
            String message = "[Share] Player is at X: " + x + ", Y: " + y + ", Z: " + z + " in the " + dim + ".";
            mc.player.connection.sendChat(message);
            return SINGLE_SUCCESS;
        }));

        // bare #share -> usage
        builder.executes(context -> {
            ChatUtils.infoPrefix("Share", "§6§lShare §7- usage:");
            ChatUtils.info("§ashare location§7 - broadcast your current coordinates");
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
