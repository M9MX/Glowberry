package org.m9mx.cactus.glowberry;

import org.m9mx.cactus.glowberry.feature.commands.ExampleCommand;
import org.m9mx.cactus.glowberry.feature.modules.FastBreakModule;
import org.m9mx.cactus.glowberry.feature.modules.FastPlaceModule;
import org.m9mx.cactus.glowberry.feature.modules.LightLevelModule;
import org.m9mx.cactus.glowberry.feature.modules.NoHurtcamModule;
import org.m9mx.cactus.glowberry.feature.modules.AutoToolModule;
import org.m9mx.cactus.glowberry.feature.modules.HorseStatsModule;
import org.m9mx.cactus.glowberry.feature.modules.AutoClickerModule; // Import the new module
import com.dwarslooper.cactus.client.addon.v2.ICactusAddon;
import com.dwarslooper.cactus.client.addon.v2.RegistryBus;
import com.dwarslooper.cactus.client.feature.command.Command;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.gui.hud.element.impl.HudElement;

import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.Items;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlowberryMain implements ICactusAddon {

	public static final Logger LOGGER = LoggerFactory.getLogger("Glowberry (Cactus Addon)");

	private static final Category GLOWBERRY_CATEGORY = new Category("glowberry", Items.GLOW_BERRIES.getDefaultInstance());

	@Override
	public void onInitialize(RegistryBus registryBus) {
		// This is called when the addon is initialized. It provides a RegistryBus
		// which will be used to register new features and content

		LOGGER.info("Hello, Cactus!");

		// Register our custom category first
		registryBus.register(Category.class, (list, ctx) -> list.add(GLOWBERRY_CATEGORY));
		
		// Register our modules inside the custom category
		registryBus.register(Module.class, ctx -> new LightLevelModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new FastPlaceModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new FastBreakModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new NoHurtcamModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new AutoToolModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new HorseStatsModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new AutoClickerModule(GLOWBERRY_CATEGORY)); // Register the new AutoClicker module
		registryBus.register(Command.class, ctx -> new ExampleCommand());

		
		LOGGER.info("Glowberry Addon successfully loaded!");
	}

	@Override
	public void onLoadComplete() {
		// This is called when Cactus is fully done initializing
		// This does not mean the game has completely loaded yet
	}

	@Override
	public void onShutdown() {
		// This is called when the client is shutting down
	}
}