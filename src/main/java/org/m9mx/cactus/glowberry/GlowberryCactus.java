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
import org.m9mx.cactus.glowberry.util.compat.IncompatibilityRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dwarslooper.cactus.client.addon.v2.ICactusAddon;
import com.dwarslooper.cactus.client.addon.v2.RegistryBus;
import com.dwarslooper.cactus.client.feature.command.Command;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;

import net.minecraft.world.item.Items;

import java.util.Set;
import java.util.function.Supplier;

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
		registerModule(registryBus, "lightLevel", () -> new LightLevelModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "fastPlace", () -> new FastPlaceModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "fastBreak", () -> new FastBreakModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "noHurtcam", () -> new NoHurtcamModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "autoTool", () -> new AutoToolModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "horseStats", () -> new HorseStatsModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "autoClicker", () -> new AutoClickerModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "autoFish", () -> new AutoFishModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "shieldStatus", () -> new ShieldStatusModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "tabList", () -> new TabListModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "totemCounter", () -> new TotemCounterModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "appleSkin", () -> new AppleSkinModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "trajectoryPreview", () -> new TrajectoryPreviewModule(GLOWBERRY_CATEGORY));
		registerModule(registryBus, "scribble", () -> new ScribbleModule(GLOWBERRY_CATEGORY));
		// registerModule(registryBus, "waypointsV2", () -> new WaypointsV2Module(GLOWBERRY_CATEGORY));
		registryBus.register(Command.class, ctx -> new ExampleCommand());

		
		LOGGER.info("Glowberry Addon successfully loaded!");
	}

	private void registerModule(RegistryBus registryBus, String moduleId, Supplier<Module> factory) {
		if (IncompatibilityRegistry.isModuleBlocked(moduleId)) {
			Set<String> blockingMods = IncompatibilityRegistry.blockingModsForModule(moduleId);
			LOGGER.info("Skipping module '{}' due to incompatibility with loaded mod(s): {}", moduleId, blockingMods);
			return;
		}

		registryBus.register(Module.class, ctx -> factory.get());
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