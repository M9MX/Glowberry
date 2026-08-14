package org.m9mx.cactus.glowberry;

import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import org.m9mx.cactus.glowberry.feature.commands.CalculatorCommand;
import org.m9mx.cactus.glowberry.feature.commands.ShareCommand;
import org.m9mx.cactus.glowberry.feature.commands.PrivateShareCommand;
import org.m9mx.cactus.glowberry.feature.modules.AntiAfkModule;
import org.m9mx.cactus.glowberry.feature.modules.AutoClickerModule;
import org.m9mx.cactus.glowberry.feature.modules.AutoFishModule;
import org.m9mx.cactus.glowberry.feature.modules.AutoToolModule;
import org.m9mx.cactus.glowberry.feature.modules.FastBreakModule;
import org.m9mx.cactus.glowberry.feature.modules.FastPlaceModule;
import org.m9mx.cactus.glowberry.feature.modules.LightLevelModule;
import org.m9mx.cactus.glowberry.feature.modules.ScribbleModule;
import org.m9mx.cactus.glowberry.feature.modules.ShieldStatusModule;
import org.m9mx.cactus.glowberry.feature.modules.TabListModule;
import org.m9mx.cactus.glowberry.feature.modules.TotemCounterModule;
import org.m9mx.cactus.glowberry.feature.modules.TimerModule;
import org.m9mx.cactus.glowberry.feature.modules.StopwatchModule;
import org.m9mx.cactus.glowberry.feature.modules.ToggleSprintModule;	import org.m9mx.cactus.glowberry.feature.hud.ArmorHudElement;
	import org.m9mx.cactus.glowberry.feature.hud.HorseStatsHudElement;
import org.m9mx.cactus.glowberry.feature.hud.PickUpLogHud;
import org.m9mx.cactus.glowberry.feature.hud.TimerStopwatchHudElement;
import org.m9mx.cactus.glowberry.feature.hud.ToggleSprintHudElement;
import org.m9mx.cactus.glowberry.feature.hud.WailaHudElement;
import org.m9mx.cactus.glowberry.feature.hud.ModulesListHudElement;
import org.m9mx.cactus.glowberry.feature.hud.ModuleMessageHudElement;
import org.m9mx.cactus.glowberry.feature.modules.*;
import org.m9mx.cactus.glowberry.util.cactus.emoji.EmojiCode;
import org.m9mx.cactus.glowberry.util.cactus.emoji.EmojiManager;
import org.m9mx.cactus.glowberry.util.cactus.placeholders.GlowberryPlaceholders;
import org.m9mx.cactus.glowberry.util.compat.IncompatibilityRegistry;
import com.dwarslooper.cactus.client.addon.v2.ICactusAddon;
import com.dwarslooper.cactus.client.addon.v2.RegistryBus;
import com.dwarslooper.cactus.client.feature.command.Command;
import com.dwarslooper.cactus.client.systems.config.CactusSettings;
import com.dwarslooper.cactus.client.systems.config.ConfigHandler;
import com.dwarslooper.cactus.client.systems.config.FileConfiguration;
import com.dwarslooper.cactus.client.systems.config.impl.CactusConfig;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.feature.content.ContentPack;
import com.dwarslooper.cactus.client.feature.content.ContentPackManager;
import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.feature.module.ModuleManager;
import org.m9mx.cactus.glowberry.util.AutoSaveHandler;
import org.m9mx.cactus.glowberry.util.config.GlowberryConfig;
import net.minecraft.world.item.Items;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

public class GlowberryCactus implements ICactusAddon {
	// Utility to get the name of a Setting via reflection
	private static String getSettingName(Object setting) {
		try {
			var field = setting.getClass().getSuperclass().getDeclaredField("name");
			field.setAccessible(true);
			return (String) field.get(setting);
		} catch (Exception e) {
			return null;
		}
	}

	// Single static cached instance to ensure registration matches completely
	private static Category cachedCategory;

	public static Category getCategory() {
		if (cachedCategory == null) {
			cachedCategory = new Category("glowberry", net.minecraft.world.item.Items.GLOW_BERRIES);
		}
		return cachedCategory;
	}

	public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("Glowberry (Cactus Addon)");

	@Override
	public void onInitialize(RegistryBus registryBus) {
		// This is called when the addon is initialized. It provides a RegistryBus
		// which will be used to register new features and content

		LOGGER.info("Hello, Cactus!");

		GlowberryPlaceholders.register(registryBus);

		registryBus.register(Category.class, (list, ctx) -> list.add(getCategory()));

		registryBus.register(HudElement.class, ctx -> new PickUpLogHud());
		registryBus.register(HudElement.class, ctx -> new TimerStopwatchHudElement());
		registryBus.register(HudElement.class, ctx -> new ToggleSprintHudElement());
		registryBus.register(HudElement.class, ctx -> new HorseStatsHudElement());
		registryBus.register(HudElement.class, ctx -> new WailaHudElement());
		registryBus.register(HudElement.class, ctx -> new ArmorHudElement());
		registryBus.register(HudElement.class, ctx -> new ModulesListHudElement());
		registryBus.register(HudElement.class, ctx -> new ModuleMessageHudElement());

		// Register our modules inside the custom category
		registerModule(registryBus, "lightLevel", () -> new LightLevelModule(getCategory()));
		registerModule(registryBus, "fastPlace", () -> new FastPlaceModule(getCategory()));
		registerModule(registryBus, "fastBreak", () -> new FastBreakModule(getCategory()));
		registerModule(registryBus, "autoTool", () -> new AutoToolModule(getCategory()));
		registerModule(registryBus, "autoClicker", () -> new AutoClickerModule(getCategory()));
		registerModule(registryBus, "autoFish", () -> new AutoFishModule(getCategory()));
		registerModule(registryBus, "shuffle", () -> new ShuffleModule(getCategory()));
		registerModule(registryBus, "shieldStatus", () -> new ShieldStatusModule(getCategory()));
		registerModule(registryBus, "tabList", () -> new TabListModule(getCategory()));
		registerModule(registryBus, "totemCounter", () -> new TotemCounterModule(getCategory()));
		registerModule(registryBus, "scribble", () -> new ScribbleModule(getCategory()));
		registerModule(registryBus, "timer", () -> new TimerModule(getCategory()));
		registerModule(registryBus, "stopwatch", () -> new StopwatchModule(getCategory()));
		registerModule(registryBus, "toggleSprint", () -> new ToggleSprintModule(getCategory()));
		registerModule(registryBus, "antiAfk", () -> new AntiAfkModule(getCategory()));
		registerGlowberryConfig(registryBus);
		registryBus.register(Command.class, ctx -> new CalculatorCommand("calc"));
		registryBus.register(Command.class, ctx -> new CalculatorCommand("calculator"));
		registryBus.register(Command.class, ctx -> new ShareCommand());
		registryBus.register(Command.class, ctx -> new PrivateShareCommand());

		// Always clear previously injected custom emojis before injecting
		com.dwarslooper.cactus.client.systems.emoji.EmojiManager.getEmojis().removeIf(
				emoji -> emoji instanceof com.dwarslooper.cactus.client.systems.emoji.EmojiCode &&
						!(org.m9mx.cactus.glowberry.util.cactus.emoji.EmojiManager.EMOJIS.contains(emoji))
		);
		File emojiFile = new File("cactus/glowberry/glowberry_emojis.txt");
		if (!emojiFile.exists()) {
			createDefaultEmojiFile(emojiFile);
		}
		injectEmojisFromFile(emojiFile);
		// Always inject built-in emojis
		for (EmojiCode myEmoji : EmojiManager.EMOJIS) {
			com.dwarslooper.cactus.client.systems.emoji.EmojiManager.getEmojis().add(
					new com.dwarslooper.cactus.client.systems.emoji.EmojiCode(myEmoji.name(), myEmoji.emoji())
			);
		}
	}

	private void createDefaultEmojiFile(File emojiFile) {
		try {
			File parent = emojiFile.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			String defaultContent = "# Glowberry Custom Emojis\n# Usage: code=emoji\nL=★\nsmile=☺\n";
			Files.writeString(emojiFile.toPath(), defaultContent, StandardCharsets.UTF_8);
		} catch (IOException ignored) {}
	}

	private void injectEmojisFromFile(File emojiFile) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(emojiFile), StandardCharsets.UTF_8))) {
			Properties prop = new Properties();
			prop.load(reader);
			for (var entry : prop.entrySet()) {
				String name = entry.getKey().toString().trim();
				String emoji = entry.getValue().toString().trim();
				com.dwarslooper.cactus.client.systems.emoji.EmojiManager.getEmojis().add(new com.dwarslooper.cactus.client.systems.emoji.EmojiCode(name, emoji));
			}
		} catch (IOException ignored) {}
	}

	private void registerModule(RegistryBus registryBus, String moduleId, Supplier<Module> factory) {
		if (IncompatibilityRegistry.isModuleBlocked(moduleId)) {
			Set<String> blockingMods = IncompatibilityRegistry.blockingModsForModule(moduleId);
			LOGGER.info("Skipping module '{}' due to incompatibility with loaded mod(s): {}", moduleId, blockingMods);
			return;
		}

		registryBus.register(Module.class, ctx -> factory.get());
	}

	// Kept so onLoadComplete can sync the modules without an id lookup.
	private static ContentPack cheatsPack;

	// Registers Glowberry's FileConfiguration through the official addon API. This
	// is the one registration type whose factory receives the ConfigHandler service
	// (Cactus provides it right before completing the FileConfiguration registry,
	// still before the configs are loaded), so it is the right place to:
	//
	//  1. Add the "Auto Save on Update" setting to Cactus' own settings container
	//     (shows in the Cactus Settings screen, serialized with Cactus' config,
	//     and - because it is added before the load - its saved value restores).
	//  2. Register the Cheats content pack directly with the raw id (like Cactus'
	//     own packs), so it exists before the content pack config is loaded and its
	//     saved enabled state restores. Registering it via the ContentPack registry
	//     would namespace the id to "glowberry-addon:glowberry_cheats", which no
	//     longer matches the saved config.
	private void registerGlowberryConfig(RegistryBus registryBus) {
		registryBus.register(FileConfiguration.class, ctx -> {
			ConfigHandler handler = ctx.require(ConfigHandler.class);

			CactusConfig cactusConfig = handler.getConfig(CactusConfig.class);
			if (cactusConfig != null) {
				CactusSettings cactusSettings = cactusConfig.getSubConfig(CactusSettings.class);
				if (cactusSettings != null) {
					Setting<Boolean> setting = cactusSettings.settings.getDefault()
							.add(new BooleanSetting("autoSaveOnUpdate", true));
					AutoSaveHandler.setAutoSaveSetting(setting);

					// How the text rainbow animation plays on HUD elements with
					// "Text Chroma" or an RGB text color enabled: smooth (whole text
					// shifts together) or diagonal (a colored line sweeps across,
					// character by character).
					Setting<org.m9mx.cactus.glowberry.util.RainbowMode> rainbowMode = cactusSettings.settings.getDefault()
							.add(new EnumSetting<>("rainbowMode", org.m9mx.cactus.glowberry.util.RainbowMode.DIAGONAL));
					org.m9mx.cactus.glowberry.util.RainbowModeHandler.setSetting(rainbowMode);
				}
			}

			ContentPackManager contentPackManager = handler.getConfig(ContentPackManager.class);
			if (contentPackManager != null) {
				cheatsPack = new ContentPack(
						"glowberry_cheats",
						ContentPack.ActivationPolicy.DEFAULT_DISABLED,
						Items.COMMAND_BLOCK,
						pack -> syncCheatModules(pack.isEnabled())
				);
				contentPackManager.registerPack(cheatsPack);
			}

			return new GlowberryConfig(handler);
		});
	}

	private static final Class<?>[] CHEAT_MODULE_CLASSES = {
		AutoClickerModule.class,
		AutoFishModule.class,
		AntiAfkModule.class,
	};

	@Override
	public void onLoadComplete() {
		// The pack was registered via the registry bus during initialization, so by
		// now Cactus has already restored its saved state from the config. We just
		// need to apply that state to the cheat modules.
		ContentPackManager contentPackManager = ContentPackManager.get();
		if (contentPackManager == null || cheatsPack == null) {
			LOGGER.warn("ContentPackManager/Cheats content pack not available, skipping Cheats content pack sync");
			return;
		}
		// Safety net: the changed listener already synced the modules when the saved
		// state was restored during config load, but sync again in case it didn't run.
		syncCheatModules(cheatsPack.isEnabled());
		LOGGER.info("Cheats content pack is {}", cheatsPack.isEnabled() ? "enabled" : "disabled");
	}

	private void syncCheatModules(boolean enabled) {
		ModuleManager moduleManager = ModuleManager.get();
		if (moduleManager == null) return;

		Map<Class<? extends Module>, Module> modules = moduleManager.getModules();
		Category category = getCategory();

		if (enabled) {
			// Re-add modules if missing (pack was toggled on)
			addCheatModule(modules, AutoClickerModule.class, () -> new AutoClickerModule(category));
			addCheatModule(modules, AutoFishModule.class, () -> new AutoFishModule(category));
			addCheatModule(modules, AntiAfkModule.class, () -> new AntiAfkModule(category));
		} else {
			// Remove modules (pack was toggled off)
			for (Class<?> clazz : CHEAT_MODULE_CLASSES) {
				Module removed = modules.remove(clazz);
				if (removed != null) {
					if (removed.active()) {
						removed.toggle();
					}
					LOGGER.info("Hidden module '{}' because Cheats content pack is disabled", removed.getDisplayName());
				}
			}
		}
	}

	private void addCheatModule(Map<Class<? extends Module>, Module> modules, Class<? extends Module> clazz, Supplier<Module> factory) {
		if (!modules.containsKey(clazz)) {
			Module module = factory.get();
			modules.put(clazz, module);
			LOGGER.info("Shown module '{}' because Cheats content pack is enabled", module.getDisplayName());
		}
	}

	@Override
	public void onShutdown() {
		// This is called when the client is shutting down
	}
}