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
import org.m9mx.cactus.glowberry.feature.modules.CustomEmojiModule;
import org.m9mx.cactus.glowberry.util.compat.IncompatibilityRegistry;
import com.dwarslooper.cactus.client.systems.emoji.EmojiManager;
import com.dwarslooper.cactus.client.systems.emoji.EmojiCode;
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
		LOGGER.info("Hello, Cactus!");

		registryBus.register(Category.class, (list, ctx) -> list.add(GLOWBERRY_CATEGORY));

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
		registryBus.register(Command.class, ctx -> new ExampleCommand());
		registerModule(registryBus, "customEmojis", () -> new CustomEmojiModule(GLOWBERRY_CATEGORY));

		try {
			for (org.m9mx.cactus.glowberry.feature.EmojiCode myEmoji : org.m9mx.cactus.glowberry.feature.EmojiManager.EMOJIS) {
				EmojiManager.getEmojis().add(new EmojiCode(myEmoji.name(), myEmoji.emoji()));
			}
			LOGGER.info("Successfully registered {} Glowberry emojis into Cactus!", org.m9mx.cactus.glowberry.feature.EmojiManager.EMOJIS.size());
		} catch (Exception e) {
			LOGGER.error("Glowberry failed to inject emojis into Cactus");
		}

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
	public void onLoadComplete() {}

	@Override
	public void onShutdown() {}
}