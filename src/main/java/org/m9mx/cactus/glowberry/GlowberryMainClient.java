package org.m9mx.cactus.glowberry;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.m9mx.cactus.glowberry.util.appleskin.client.DebugInfoHudEntry;
import org.m9mx.cactus.glowberry.util.appleskin.network.ClientSyncHandler;
import org.m9mx.cactus.glowberry.util.trajectorypreview.PtpClient;

public class GlowberryMainClient implements ClientModInitializer
{
	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitializeClient()
	{
		ClientSyncHandler.init();
		// Don't initialize AppleSkin handlers directly - they should be controlled by the module
		// HUDOverlayHandler.init(); 
		// TooltipOverlayHandler.init();
		
		// Initialize trajectory preview networking and rendering
		PtpClient.initializeNetworking();
		PtpClient.initializeRendering();
		
		// Only register debug entry - it's not dependent on module activation
		DebugScreenEntries.register(DebugInfoHudEntry.ENTRY_ID, new DebugInfoHudEntry());
	}
}