package org.m9mx.cactus.glowberry.feature.commands;

import com.dwarslooper.cactus.client.feature.command.Command;
import com.dwarslooper.cactus.client.util.game.ChatUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

public class ExampleCommand extends Command {

	public ExampleCommand() {
		super("example");
	}

	public void build(LiteralArgumentBuilder builder) {
		builder.then(argument("name", StringArgumentType.greedyString()).executes(context -> {
			ChatUtils.infoPrefix("Example Command", "Hello, " + StringArgumentType.getString(context, "name"));
			return SINGLE_SUCCESS;
		}));
	}

}