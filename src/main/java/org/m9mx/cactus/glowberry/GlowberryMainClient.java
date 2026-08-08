package org.m9mx.cactus.glowberry;

import com.dwarslooper.cactus.client.feature.macro.MacroManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents; // Added this
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import com.dwarslooper.cactus.client.gui.hud.HudManager;
import com.dwarslooper.cactus.client.gui.hud.element.HudElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.m9mx.cactus.glowberry.util.cactus.macro.GlowberryMacroManager; // Import your manager
import org.m9mx.cactus.glowberry.util.appleskin.client.DebugInfoHudEntry;
import org.m9mx.cactus.glowberry.util.appleskin.network.ClientSyncHandler;
import org.m9mx.cactus.glowberry.util.trajectorypreview.PtpClient;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import org.m9mx.cactus.glowberry.feature.hud.PickUpLogHud;

public class GlowberryMainClient implements ClientModInitializer {
	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitializeClient() {
		ClientSyncHandler.init();
		PtpClient.initializeNetworking();
		PtpClient.initializeRendering();
		GlowberryMacroManager.load();
		DebugScreenEntries.register(DebugInfoHudEntry.ENTRY_ID, new DebugInfoHudEntry());

		ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
			// Iterate through Cactus macros to find a string-trigger match
			return MacroManager.get().getMacros().stream()
					.filter(m -> {
						String bindStr = m.getKeyBinding().saveString();
						// Check if it's a Glowberry string macro and matches the message
						return bindStr.startsWith("glowberry:") &&
								bindStr.substring(10).equalsIgnoreCase(message);
					})
					.findFirst()
					.map(macro -> {
						macro.run(); // Triggers MixinCactusMacro
						return false; // Cancel the message from being sent to server
					})
					.orElse(true); // If no macro matches, let the message through
		});
		// Register a shutdown hook to save memory to disk on close
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			GlowberryMacroManager.saveToFile();
		}));

		// Register PickUpLogHud tick hook
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				for (HudElement<?> element : HudManager.getInstance().getElements()) {
					if (element instanceof PickUpLogHud) {
						((PickUpLogHud) element).onTick(client.player);
					}
				}
			}
		});
	}
}