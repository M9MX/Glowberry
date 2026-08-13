package org.m9mx.cactus.glowberry;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlowberryMain implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("Glowberry (Main)");

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Glowberry mod...");

		LOGGER.info("Glowberry mod initialization complete!");
	}
}