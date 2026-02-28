package org.m9mx.cactus.glowberry.util.trajectorypreview;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.m9mx.cactus.glowberry.util.trajectorypreview.main.HandshakeNetworking.HANDSHAKE_C2SPayload;
import org.m9mx.cactus.glowberry.util.trajectorypreview.main.HandshakeNetworking.HANDSHAKE_S2CPayload;

public class Ptp {
	public static final String MOD_ID = "trajectoryPreview";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public static void initialize() {
		// Register payload types
		PayloadTypeRegistry.playC2S().register(HANDSHAKE_C2SPayload.ID, HANDSHAKE_C2SPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(HANDSHAKE_S2CPayload.ID, HANDSHAKE_S2CPayload.CODEC);

		// Register server-side handshake receiver
		ServerPlayNetworking.registerGlobalReceiver(HANDSHAKE_C2SPayload.ID,
			(payload, context) -> {
				// Send back a reply packet
				ServerPlayNetworking.send(context.player(), new HANDSHAKE_S2CPayload("Is installed on server"));
				LOGGER.info("[TrajectoryPreview] Sending handshake to player...");
			});

		LOGGER.info("[TrajectoryPreview] Server-side networking initialized!");
	}
}