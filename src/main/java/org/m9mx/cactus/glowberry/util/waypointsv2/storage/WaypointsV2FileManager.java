package org.m9mx.cactus.glowberry.util.waypointsv2.storage;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * File manager for WaypointsV2 data.
 *
 * For now it only handles waypoint categories in:
 * config/glowberry/waypointsv2/categories.yml
 */
public final class WaypointsV2FileManager {
    private static final List<String> DEFAULT_CATEGORIES = List.of("None");
    private static final String WAYPOINTS_FILE_NAME = "waypoints.yml";
    private static final String CATEGORIES_FILE_NAME = "categories.yml";
    private static final String DEFAULT_ICON_ITEM_ID = "minecraft:compass";

    private WaypointsV2FileManager() {
    }

    public static List<String> loadCategories() {
        Path categoriesPath = getCategoriesPath();

        if (!Files.exists(categoriesPath)) {
            saveCategories(DEFAULT_CATEGORIES);
            return new ArrayList<>(DEFAULT_CATEGORIES);
        }

        List<String> loaded = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(categoriesPath, StandardCharsets.UTF_8);
            boolean inCategoriesSection = false;

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.equalsIgnoreCase("categories:")) {
                    inCategoriesSection = true;
                    continue;
                }

                if (line.startsWith("- ") && (inCategoriesSection || !containsYamlRoot(lines))) {
                    String value = sanitizeName(line.substring(2).trim());
                    if (!value.isEmpty()) {
                        loaded.add(value);
                    }
                }
            }
        } catch (IOException ignored) {
            // Falls back to defaults below.
        }

        List<String> normalized = normalizeCategories(loaded);
        if (normalized.isEmpty()) {
            normalized = new ArrayList<>(DEFAULT_CATEGORIES);
            saveCategories(normalized);
        }
        return normalized;
    }

    public static void saveCategories(List<String> categories) {
        List<String> normalized = normalizeCategories(categories);
        if (normalized.isEmpty()) {
            normalized = new ArrayList<>(DEFAULT_CATEGORIES);
        }

        Path categoriesPath = getCategoriesPath();

        try {
            Files.createDirectories(categoriesPath.getParent());

            List<String> lines = new ArrayList<>();
            lines.add("# Glowberry WaypointsV2 categories");
            lines.add("categories:");
            for (String category : normalized) {
                lines.add("  - " + category);
            }

            Files.write(categoriesPath, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Best effort write.
        }
    }

    public static boolean addCategory(String categoryName) {
        String sanitized = sanitizeName(categoryName);
        if (sanitized.isEmpty()) {
            return false;
        }

        List<String> categories = loadCategories();
        for (String existing : categories) {
            if (existing.equalsIgnoreCase(sanitized)) {
                return false;
            }
        }

        categories.add(sanitized);
        saveCategories(categories);
        return true;
    }

    public static boolean removeCategory(String categoryName) {
        String sanitized = sanitizeName(categoryName);
        if (sanitized.isEmpty() || sanitized.equalsIgnoreCase("None")) {
            return false;
        }

        List<String> categories = loadCategories();
        boolean removed = categories.removeIf(existing -> existing.equalsIgnoreCase(sanitized));
        if (!removed) {
            return false;
        }

        saveCategories(categories);
        return true;
    }

    public static List<WaypointRecord> loadWaypoints() {
        Path waypointsPath = getWaypointsPath();
        if (!Files.exists(waypointsPath)) {
            saveWaypoints(List.of());
            return new ArrayList<>();
        }

        List<WaypointRecord> loaded = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(waypointsPath, StandardCharsets.UTF_8);
            WaypointRecord current = null;

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || line.equals("waypoints:")) {
                    continue;
                }

                if (line.startsWith("- ")) {
                    if (current != null) {
                        loaded.add(current);
                    }
                    current = new WaypointRecord();

                    String inlineField = line.substring(2).trim();
                    if (!inlineField.isEmpty()) {
                        applyWaypointField(current, inlineField);
                    }
                    continue;
                }

                if (current != null) {
                    applyWaypointField(current, line);
                }
            }

            if (current != null) {
                loaded.add(current);
            }
        } catch (IOException ignored) {
            // Falls back to empty list below.
        }

        List<WaypointRecord> normalized = normalizeWaypoints(loaded);
        if (!areSameIdsAndSize(loaded, normalized)) {
            saveWaypoints(normalized);
        }
        return normalized;
    }

    public static void saveWaypoints(List<WaypointRecord> waypoints) {
        List<WaypointRecord> normalized = normalizeWaypoints(waypoints);
        Path waypointsPath = getWaypointsPath();

        try {
            Files.createDirectories(waypointsPath.getParent());

            List<String> lines = new ArrayList<>();
            lines.add("# Glowberry WaypointsV2 waypoints");
            lines.add("waypoints:");

            for (WaypointRecord waypoint : normalized) {
                lines.add("  - id: " + sanitizeValue(waypoint.id));
                lines.add("    name: " + sanitizeValue(waypoint.name));
                lines.add("    type: " + sanitizeValue(waypoint.type));
                lines.add("    enabled: " + waypoint.enabled);
                lines.add("    x: " + waypoint.x);
                lines.add("    y: " + waypoint.y);
                lines.add("    z: " + waypoint.z);
                lines.add("    category: " + sanitizeValue(waypoint.category));
                lines.add("    displayMode: " + sanitizeValue(waypoint.displayMode));
                lines.add("    iconItemId: " + sanitizeValue(waypoint.iconItemId));
                lines.add("    color: " + waypoint.color);
                lines.add("    local: " + waypoint.local);
                lines.add("    localContext: " + sanitizeValue(waypoint.localContext));
                lines.add("    originDimension: " + sanitizeValue(waypoint.originDimension));
                lines.add("    linkedOverworldX: " + waypoint.linkedOverworldX);
                lines.add("    linkedOverworldY: " + waypoint.linkedOverworldY);
                lines.add("    linkedOverworldZ: " + waypoint.linkedOverworldZ);
                lines.add("    linkedNetherX: " + waypoint.linkedNetherX);
                lines.add("    linkedNetherY: " + waypoint.linkedNetherY);
                lines.add("    linkedNetherZ: " + waypoint.linkedNetherZ);
                lines.add("    overworld: " + waypoint.overworld);
                lines.add("    nether: " + waypoint.nether);
                lines.add("    end: " + waypoint.end);
                lines.add("    blockCoordinates: " + serializeBlockCoordinates(waypoint.blockCoordinates));
            }

            Files.write(waypointsPath, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Best effort write.
        }
    }

    public static boolean addWaypoint(WaypointRecord waypoint) {
        if (waypoint == null) {
            return false;
        }

        WaypointRecord sanitized = sanitizeWaypoint(waypoint);
        if (sanitized.name.isEmpty()) {
            return false;
        }

        List<WaypointRecord> waypoints = loadWaypoints();
        waypoints.add(sanitized);
        saveWaypoints(waypoints);
        return true;
    }

    public static WaypointRecord getWaypointById(String waypointId) {
        if (waypointId == null || waypointId.isBlank()) {
            return null;
        }

        List<WaypointRecord> waypoints = loadWaypoints();
        for (WaypointRecord waypoint : waypoints) {
            if (waypoint.id != null && waypoint.id.equalsIgnoreCase(waypointId.trim())) {
                return sanitizeWaypoint(waypoint);
            }
        }
        return null;
    }

    public static boolean updateWaypoint(WaypointRecord waypoint) {
        if (waypoint == null || waypoint.id == null || waypoint.id.isBlank()) {
            return false;
        }

        List<WaypointRecord> waypoints = loadWaypoints();
        boolean updated = false;

        for (int i = 0; i < waypoints.size(); i++) {
            WaypointRecord existing = waypoints.get(i);
            if (existing.id != null && existing.id.equalsIgnoreCase(waypoint.id.trim())) {
                WaypointRecord sanitized = sanitizeWaypoint(waypoint);
                sanitized.id = existing.id;
                waypoints.set(i, sanitized);
                updated = true;
                break;
            }
        }

        if (!updated) {
            return false;
        }

        saveWaypoints(waypoints);
        return true;
    }

    public static boolean deleteWaypointById(String waypointId) {
        if (waypointId == null || waypointId.isBlank()) {
            return false;
        }

        List<WaypointRecord> waypoints = loadWaypoints();
        boolean removed = waypoints.removeIf(existing -> existing.id != null && existing.id.equalsIgnoreCase(waypointId.trim()));
        if (!removed) {
            return false;
        }

        saveWaypoints(waypoints);
        return true;
    }

    public static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }

        String sanitized = name.trim().replaceAll("[^a-zA-Z0-9_ -]", "");
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        return sanitized;
    }

    private static Path getCategoriesPath() {
        return getWaypointsConfigDir().resolve(CATEGORIES_FILE_NAME);
    }

    private static Path getWaypointsPath() {
        return getWaypointsConfigDir().resolve(WAYPOINTS_FILE_NAME);
    }

    private static Path getWaypointsConfigDir() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("glowberry")
                .resolve("waypointsv2");
    }

    private static List<String> normalizeCategories(List<String> categories) {
        Set<String> unique = new LinkedHashSet<>();
        if (categories != null) {
            for (String category : categories) {
                String sanitized = sanitizeName(category);
                if (!sanitized.isEmpty()) {
                    unique.add(sanitized);
                }
            }
        }

        if (!containsIgnoreCase(unique, "None")) {
            unique.add("None");
        }

        List<String> result = new ArrayList<>();
        if (containsIgnoreCase(unique, "None")) {
            result.add("None");
        }
        for (String value : unique) {
            if (!value.equalsIgnoreCase("None")) {
                result.add(value);
            }
        }
        return result;
    }

    private static boolean containsYamlRoot(List<String> lines) {
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.endsWith(":")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(Set<String> values, String needle) {
        for (String value : values) {
            if (value.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    private static void applyWaypointField(WaypointRecord waypoint, String keyValueLine) {
        int separator = keyValueLine.indexOf(':');
        if (separator <= 0) {
            return;
        }

        String key = keyValueLine.substring(0, separator).trim();
        String value = keyValueLine.substring(separator + 1).trim();

        switch (key) {
            case "id" -> waypoint.id = sanitizeValue(value);
            case "name" -> waypoint.name = sanitizeName(value);
            case "type" -> waypoint.type = sanitizeValue(value).isEmpty() ? "LOCATION" : sanitizeValue(value);
            case "enabled" -> waypoint.enabled = Boolean.parseBoolean(value);
            case "x" -> waypoint.x = parseIntSafe(value, waypoint.x);
            case "y" -> waypoint.y = parseIntSafe(value, waypoint.y);
            case "z" -> waypoint.z = parseIntSafe(value, waypoint.z);
            case "category" -> waypoint.category = sanitizeValue(value).isEmpty() ? "None" : sanitizeValue(value);
            case "displayMode" -> waypoint.displayMode = sanitizeValue(value).isEmpty() ? "TEXT_ICON" : sanitizeValue(value);
            case "iconItemId" -> waypoint.iconItemId = sanitizeValue(value).isEmpty() ? DEFAULT_ICON_ITEM_ID : sanitizeValue(value);
            case "color" -> waypoint.color = parseIntSafe(value, waypoint.color);
            case "local" -> waypoint.local = Boolean.parseBoolean(value);
            case "localContext" -> waypoint.localContext = sanitizeValue(value);
            case "originDimension" -> waypoint.originDimension = sanitizeValue(value);
            case "linkedOverworldX" -> waypoint.linkedOverworldX = parseIntSafe(value, waypoint.linkedOverworldX);
            case "linkedOverworldY" -> waypoint.linkedOverworldY = parseIntSafe(value, waypoint.linkedOverworldY);
            case "linkedOverworldZ" -> waypoint.linkedOverworldZ = parseIntSafe(value, waypoint.linkedOverworldZ);
            case "linkedNetherX" -> waypoint.linkedNetherX = parseIntSafe(value, waypoint.linkedNetherX);
            case "linkedNetherY" -> waypoint.linkedNetherY = parseIntSafe(value, waypoint.linkedNetherY);
            case "linkedNetherZ" -> waypoint.linkedNetherZ = parseIntSafe(value, waypoint.linkedNetherZ);
            case "overworld" -> waypoint.overworld = Boolean.parseBoolean(value);
            case "nether" -> waypoint.nether = Boolean.parseBoolean(value);
            case "end" -> waypoint.end = Boolean.parseBoolean(value);
            case "blockCoordinates" -> waypoint.blockCoordinates = parseBlockCoordinates(value);
            default -> {
            }
        }
    }

    private static String serializeBlockCoordinates(List<BlockCoordinateRecord> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (BlockCoordinateRecord coordinate : coordinates) {
            if (coordinate == null) {
                continue;
            }
            parts.add(coordinate.x + "," + coordinate.y + "," + coordinate.z);
        }
        return String.join("|", parts);
    }

    private static List<BlockCoordinateRecord> parseBlockCoordinates(String value) {
        List<BlockCoordinateRecord> result = new ArrayList<>();
        String sanitized = sanitizeValue(value);
        if (sanitized.isEmpty()) {
            return result;
        }

        String[] entries = sanitized.split("\\|");
        for (String entry : entries) {
            String[] xyz = entry.trim().split(",");
            if (xyz.length != 3) {
                continue;
            }

            try {
                int x = Integer.parseInt(xyz[0].trim());
                int y = Integer.parseInt(xyz[1].trim());
                int z = Integer.parseInt(xyz[2].trim());
                result.add(new BlockCoordinateRecord(x, y, z));
            } catch (NumberFormatException ignored) {
                // Skip malformed entries and keep the rest.
            }
        }

        return result;
    }

    private static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String sanitizeValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }

    private static List<WaypointRecord> normalizeWaypoints(List<WaypointRecord> waypoints) {
        List<WaypointRecord> normalized = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();

        if (waypoints == null) {
            return normalized;
        }

        for (WaypointRecord source : waypoints) {
            WaypointRecord copy = sanitizeWaypoint(source);

            String id = sanitizeValue(copy.id);
            if (id.isEmpty() || usedIds.contains(id.toLowerCase())) {
                id = generateUniqueWaypointId(usedIds);
            }

            copy.id = id;
            usedIds.add(id.toLowerCase());
            normalized.add(copy);
        }

        return normalized;
    }

    private static WaypointRecord sanitizeWaypoint(WaypointRecord source) {
        WaypointRecord result = new WaypointRecord();
        if (source == null) {
            result.id = "";
            return result;
        }

        result.id = sanitizeValue(source.id);
        result.name = sanitizeName(source.name);
        result.type = sanitizeValue(source.type).isEmpty() ? "LOCATION" : sanitizeValue(source.type);
        result.enabled = source.enabled;
        result.x = source.x;
        result.y = source.y;
        result.z = source.z;
        result.category = sanitizeValue(source.category).isEmpty() ? "None" : sanitizeValue(source.category);
        result.displayMode = sanitizeValue(source.displayMode).isEmpty() ? "TEXT_ICON" : sanitizeValue(source.displayMode);
        result.iconItemId = sanitizeValue(source.iconItemId).isEmpty() ? DEFAULT_ICON_ITEM_ID : sanitizeValue(source.iconItemId);
        result.color = source.color;
        result.local = source.local;
        result.localContext = sanitizeValue(source.localContext);
        result.originDimension = sanitizeValue(source.originDimension).isEmpty() ? "overworld" : sanitizeValue(source.originDimension);
        result.linkedOverworldX = source.linkedOverworldX;
        result.linkedOverworldY = source.linkedOverworldY;
        result.linkedOverworldZ = source.linkedOverworldZ;
        result.linkedNetherX = source.linkedNetherX;
        result.linkedNetherY = source.linkedNetherY;
        result.linkedNetherZ = source.linkedNetherZ;
        result.overworld = source.overworld;
        result.nether = source.nether;
        result.end = source.end;
        result.blockCoordinates = copyBlockCoordinates(source.blockCoordinates);
        return result;
    }

    private static List<BlockCoordinateRecord> copyBlockCoordinates(List<BlockCoordinateRecord> source) {
        List<BlockCoordinateRecord> copy = new ArrayList<>();
        if (source == null) {
            return copy;
        }

        for (BlockCoordinateRecord coordinate : source) {
            if (coordinate == null) {
                continue;
            }
            copy.add(new BlockCoordinateRecord(coordinate.x, coordinate.y, coordinate.z));
        }
        return copy;
    }

    private static String generateUniqueWaypointId(Set<String> usedIds) {
        String candidate;
        do {
            candidate = "wp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (usedIds.contains(candidate.toLowerCase()));
        return candidate;
    }

    private static boolean areSameIdsAndSize(List<WaypointRecord> first, List<WaypointRecord> second) {
        if (first == null) {
            return second == null || second.isEmpty();
        }
        if (second == null || first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            String a = sanitizeValue(first.get(i).id);
            String b = sanitizeValue(second.get(i).id);
            if (!a.equalsIgnoreCase(b)) {
                return false;
            }
        }
        return true;
    }

    public static class WaypointRecord {
        public String id = "";
        public String name = "";
        public String type = "LOCATION";
        public boolean enabled = true;
        public int x = 0;
        public int y = 64;
        public int z = 0;
        public String category = "None";
        public String displayMode = "TEXT_ICON";
        public String iconItemId = DEFAULT_ICON_ITEM_ID;
        public int color = 0xFFFFFF;
        public boolean local = true;
        public String localContext = "";
        public String originDimension = "overworld";
        public int linkedOverworldX = 0;
        public int linkedOverworldY = 64;
        public int linkedOverworldZ = 0;
        public int linkedNetherX = 0;
        public int linkedNetherY = 64;
        public int linkedNetherZ = 0;
        public boolean overworld = true;
        public boolean nether = false;
        public boolean end = false;
        public List<BlockCoordinateRecord> blockCoordinates = new ArrayList<>();
    }

    public static class BlockCoordinateRecord {
        public int x;
        public int y;
        public int z;

        public BlockCoordinateRecord(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}

