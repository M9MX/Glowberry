package org.m9mx.cactus.glowberry.util.update;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class UpdateChecker {
	private static final Logger LOGGER = LogManager.getLogger("Glowberry Update");
	// Single source of truth: the repo's gradle.properties holds both the newest
	// mod version (mod_version) and the final Glowberry per Minecraft version
	// (final_mod_version-{mcVersion}, -1 = no cutoff, always check).
	private static final String VERSION_SOURCE = "https://raw.githubusercontent.com/M9MX/Glowberry/version-26.x/gradle.properties";
	private static final String MODRINTH_URL = "https://modrinth.com/mod/glowberry";

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(java.time.Duration.ofSeconds(5))
			.build();

	private static volatile boolean updateAvailable = false;
	private static volatile String latestVersion = "";
	private static volatile boolean checkDone = false;

	public static void check(String currentVersion) {
		String mcVersion = getMinecraftVersion();
		LOGGER.info("Update check started (current mod: {}, minecraft: {})", currentVersion, mcVersion);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(VERSION_SOURCE))
				.header("User-Agent", "Glowberry-Addon")
				.GET()
				.build();

		CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.orTimeout(8, TimeUnit.SECONDS)
				.thenApply(HttpResponse::body)
				.thenAccept(content -> {
					try {
						Properties props = new Properties();
						props.load(new StringReader(content));

						String newest = props.getProperty("mod_version");
						String finalVersion = finalVersionFor(props, mcVersion);

						if (newest == null || newest.isBlank()) {
							LOGGER.warn("Update check: no mod_version found in {}", VERSION_SOURCE);
							checkDone = true;
							return;
						}

						String target;
						if (finalVersion != null && !"-1".equals(finalVersion)) {
							// This Minecraft version has a final Glowberry release:
							// advertise that one, not the newest overall (which may be
							// for a different Minecraft version).
							target = finalVersion;
						} else {
							// No cutoff for this Minecraft version: advertise the newest overall.
							target = newest;
						}

						latestVersion = target.startsWith("v") ? target.substring(1) : target;
						updateAvailable = isNewer(latestVersion, currentVersion);
						checkDone = true;

						LOGGER.info("Update check result: newest={}, final={}, target={}, updateAvailable={}",
								newest, finalVersion, latestVersion, updateAvailable);
						if (updateAvailable) {
							LOGGER.info("Update available: {} (current: {})", latestVersion, currentVersion);
						}
					} catch (Exception e) {
						LOGGER.warn("Update check: failed to parse response from {}", VERSION_SOURCE, e);
						checkDone = true;
					}
				})
				.exceptionally(ex -> {
					// Offline or unreachable: don't bother the user, just note it in the logs.
					LOGGER.info("Update check failed (offline or unreachable?): {}", ex.getMessage());
					checkDone = true;
					return null;
				});
	}

	private static String finalVersionFor(Properties props, String mcVersion) {
		if (mcVersion.isEmpty()) return null;
		// Only use the major.minor part (e.g. "26.1" from "26.1.2") to match the key
		String[] parts = mcVersion.split("\\.");
		String key = parts.length >= 2
				? "final_mod_version-" + parts[0] + "." + parts[1]
				: "final_mod_version-" + mcVersion;
		String value = props.getProperty(key);
		return value == null ? null : value.trim();
	}

	private static String getMinecraftVersion() {
		try {
			return FabricLoader.getInstance()
					.getModContainer("minecraft")
					.orElseThrow()
					.getMetadata()
					.getVersion()
					.getFriendlyString();
		} catch (Exception e) {
			return "";
		}
	}

	private static boolean isNewer(String latest, String current) {
		try {
			String[] latestParts = latest.split("\\.");
			String[] currentParts = current.split("\\.");

			for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
				int l = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
				int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
				if (l > c) return true;
				if (l < c) return false;
			}
			return false;
		} catch (NumberFormatException e) {
			LOGGER.debug("Could not compare versions '{}' vs '{}'", latest, current);
			return false;
		}
	}

	public static boolean isUpdateAvailable() { return updateAvailable; }
	public static String getLatestVersion() { return latestVersion; }
	public static boolean isCheckDone() { return checkDone; }
	public static String getModrinthUrl() { return MODRINTH_URL; }
}
