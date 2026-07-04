package org.m9mx.cactus.glowberry.util.update;

import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UpdateChecker {
    private static final Logger LOGGER = LogManager.getLogger("Glowberry Update");
    private static final String GITHUB_API = "https://api.github.com/repos/M9MX/Glowberry/releases/latest";
    private static final String MODRINTH_URL = "https://modrinth.com/mod/glowberry";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build();

    private static final Map<String, String> FINAL_VERSIONS = new HashMap<>();

    private static volatile boolean updateAvailable = false;
    private static volatile String latestVersion = "";
    private static volatile boolean checkDone = false;

    static {
        try (var in = UpdateChecker.class.getResourceAsStream("/final_versions.properties")) {
            if (in != null) {
                var props = new Properties();
                props.load(in);
                for (var entry : props.entrySet()) {
                    FINAL_VERSIONS.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        } catch (Exception ignored) {}
    }

    public static void check(String currentVersion) {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(GITHUB_API))
				.header("Accept", "application/vnd.github+json")
				.header("User-Agent", "Glowberry-Addon")
				.GET()
				.build();

		CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.orTimeout(8, TimeUnit.SECONDS)
				.thenApply(HttpResponse::body)
				.thenAccept(json -> {
					try {
						String tag = JsonParser.parseString(json)
								.getAsJsonObject()
								.get("tag_name")
								.getAsString();

						latestVersion = tag.startsWith("v") ? tag.substring(1) : tag;
						updateAvailable = isNewer(latestVersion, currentVersion);
						checkDone = true;

						if (updateAvailable) {
							LOGGER.info("Update available: {} (current: {})", latestVersion, currentVersion);
						}
					} catch (Exception e) {
						LOGGER.debug("Failed to parse update check response", e);
						checkDone = true;
					}
				})
				.exceptionally(ex -> {
					LOGGER.debug("Update check failed", ex);
					checkDone = true;
					return null;
				});
    }

    public static boolean shouldSkipCheck(String currentModVersion) {
        String mcVersion = getMinecraftVersion();
        if (mcVersion.isEmpty()) return false;

        // Only use the major.minor part (e.g. "26.1" from "26.1.2") to match the key
        String[] parts = mcVersion.split("\\.");
        String key = parts.length >= 2 ? "final_mod_version-" + parts[0] + "." + parts[1] : "final_mod_version-" + mcVersion;

        String finalVersion = FINAL_VERSIONS.get(key);
        if (finalVersion == null) return false;       // No entry → don't skip
        if ("-1".equals(finalVersion)) return false;  // -1 → no cutoff, always check

        // If current mod version >= final version, skip the update check
        return !isNewer(finalVersion, currentModVersion);
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
            return false;
        }
    }

    public static boolean isUpdateAvailable() { return updateAvailable; }
    public static String getLatestVersion() { return latestVersion; }
    public static boolean isCheckDone() { return checkDone; }
    public static String getModrinthUrl() { return MODRINTH_URL; }
}
