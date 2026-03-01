package org.m9mx.cactus.glowberry;

import org.m9mx.cactus.glowberry.feature.commands.ExampleCommand;
import org.m9mx.cactus.glowberry.feature.modules.AppleSkinModule;
import org.m9mx.cactus.glowberry.feature.modules.AutoClickerModule;
import org.m9mx.cactus.glowberry.feature.modules.AutoFishModule;
import org.m9mx.cactus.glowberry.feature.modules.AutoToolModule;
import org.m9mx.cactus.glowberry.feature.modules.FastBreakModule;
import org.m9mx.cactus.glowberry.feature.modules.FastPlaceModule;
import org.m9mx.cactus.glowberry.feature.modules.HorseStatsModule;
import org.m9mx.cactus.glowberry.feature.modules.LightLevelModule;
import org.m9mx.cactus.glowberry.feature.modules.NoHurtcamModule;
import org.m9mx.cactus.glowberry.feature.modules.ScribbleModule;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;
import org.m9mx.cactus.glowberry.feature.modules.TabListModule;
import org.m9mx.cactus.glowberry.feature.modules.TotemCounterModule;
import org.m9mx.cactus.glowberry.feature.modules.TrajectoryPreviewModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dwarslooper.cactus.client.addon.v2.ICactusAddon;
import com.dwarslooper.cactus.client.addon.v2.RegistryBus;
import com.dwarslooper.cactus.client.feature.command.Command;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;

import net.minecraft.world.item.Items;

public class GlowberryCactus implements ICactusAddon {

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
		registryBus.register(Module.class, ctx -> new AutoClickerModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new AutoFishModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new ShieldStatusModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new TabListModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new TotemCounterModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new AppleSkinModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new TrajectoryPreviewModule(GLOWBERRY_CATEGORY));
		registryBus.register(Module.class, ctx -> new ScribbleModule(GLOWBERRY_CATEGORY));
		registryBus.register(Command.class, ctx -> new ExampleCommand());

		
		LOGGER.info("Glowberry Addon successfully loaded!");
	}

	@Override
	public void onLoadComplete() {
		// This is called when Cactus is fully done initializing
	}

	@Override
	public void onShutdown() {
		// This is called when the client is shutting down
	}
}