package org.m9mx.cactus.glowberry;

import net.fabricmc.api.ModInitializer;
import org.m9mx.cactus.glowberry.util.appleskin.network.SyncHandler;
import org.m9mx.cactus.glowberry.util.trajectorypreview.Ptp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlowberryMain implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("Glowberry (Main)");

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Glowberry mod...");
		
		// Initialize server-side sync handler
		SyncHandler.init();
		
		// Initialize trajectory preview server-side networking
		Ptp.initialize();
		
		LOGGER.info("Glowberry mod initialization complete!");
	}
}
