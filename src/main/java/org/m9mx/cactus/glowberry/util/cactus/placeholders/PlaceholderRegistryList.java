package org.m9mx.cactus.glowberry.util.cactus.placeholders;

import com.dwarslooper.cactus.client.util.CactusConstants;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderRegistryList {
    public static final List<PlaceholderInfo> PLACEHOLDERS = new ArrayList<>();

    static {
        category("Cactus")
                .add("cactus.version", "1.0.0", "Installed Cactus version")
                .add("cactus.dev", "false", "Indicates whether this is a dev build");

        category("Player")
                .add("player.name", "Player123", "Your username")
                .add("player.health", "20", "Current HP")
                .add("player.max_health", "20", "Maximum HP")
                .add("player.hunger", "20", "Hunger level")
                .add("player.saturation", "5.0", "Saturation level")
                .add("player.xp.level", "30", "Current XP level")
                .add("player.xp.progress", "0.5", "XP progress (0.0 - 1.0)")
                .add("player.x", "1250", "X coordinate")
                .add("player.y", "64", "Y coordinate")
                .add("player.z", "-300", "Z coordinate")
                .add("player.x.opposite", "156", "X coordinate in opposite dimension")
                .add("player.y.opposite", "64", "Y coordinate in opposite dimension")
                .add("player.z.opposite", "-37", "Z coordinate in opposite dimension")
                .add("player.chunk.x", "78", "Current chunk X position")
                .add("player.chunk.z", "-18", "Current chunk Z position")
                .add("player.chunk.x.offset", "5", "Block offset inside chunk (X)")
                .add("player.chunk.z.offset", "10", "Block offset inside chunk (Z)")
                .add("player.rotation.yaw", "180.0", "Horizontal view rotation")
                .add("player.rotation.pitch", "0.0", "Vertical view rotation")
                .add("player.velocity.x", "0.0", "Movement vector X")
                .add("player.velocity.y", "0.0", "Movement vector Y")
                .add("player.velocity.z", "0.0", "Movement vector Z");

        category("World")
                .add("world.biome", "Plains", "Current biome (formatted)")
                .add("world.difficulty", "Hard", "World difficulty")
                .add("world.weather", "Clear", "Weather state (Clear, Rain, Thunderstorm)")
                .add("world.weather.rain", "false", "Is it raining (true/false)")
                .add("world.weather.thunder", "false", "Is it thundering (true/false)")
                .add("world.time", "Day", "Game time as state (Day, Noon, Night, Midnight)")
                .add("world.day.ticks", "1000", "Elapsed ticks of the current day")
                .add("world.time.sec", "60", "World age in seconds")
                .add("world.time.min", "1", "World age in minutes")
                .add("world.time.hour", "0", "World age in hours")
                .add("world.time.day", "0", "World age in days")
                .add("world.time.week", "0", "World age in weeks")
                .add("world.time.formatted", "1w 2d", "Human-readable time (e.g. 1w 2d)");

        category("Server")
                .add("server.name", "Survival", "Display name of the server/world")
                .add("server.address", "cactusmod.xyz", "Server IP or world name")
                .add("server.ping", "45", "Your latency to the server")
                .add("server.tps", "20.0", "Server performance (ticks per second)")
                .add("server.brand", "Fabric/Paper", "Full server brand")
                .add("server.brand.short", "Fabric", "Short server brand")
                .add("server.packets.in", "150", "Average incoming packets")
                .add("server.packets.out", "100", "Average outgoing packets");

        category("Game Performance")
                .add("client.fps", "144", "Current frames per second")
                .add("client.time", "12:30:45", "Current system time (HH:mm:ss)")
                .add("client.version", "1.20.1", "Client Minecraft version")
                .add("client.protocol", "763", "Network protocol version")
                .add("client.brand", "vanilla", "Client version type/brand")
                .add("client.is_singleplayer", "false", "Whether you are in singleplayer")
                .add("client.render.distance", "16", "Render distance setting")
                .add("client.gui.scale", "2", "GUI scale")
                .add("client.vsync", "false", "VSync status")
                .add("client.fullscreen", "false", "Fullscreen enabled")
                .add("client.window.width", "1920", "Window width in pixels")
                .add("client.window.height", "1080", "Window height in pixels")
                .add("input.cps.left", "7", "Left-clicks per second")
                .add("input.cps.right", "12", "Right-clicks per second");

        category("Hardware & JVM")
                .add("system.os.name", "Windows 11", "Operating system name")
                .add("system.os.arch", "x64", "OS architecture")
                .add("system.os.version", "10.0", "OS version")
                .add("system.user.name", "Admin", "OS username")
                .add("system.cpu.cores", "16", "Available CPU cores")
                .add("system.memory.total.mb", "4096", "Total reserved RAM")
                .add("system.memory.free.mb", "1024", "Free RAM in the JVM heap")
                .add("system.memory.used.mb", "3072", "Currently used RAM")
                .add("system.memory.max.mb", "8192", "Maximum RAM for the JVM")
                .add("java.version", "17.0.5", "Installed Java version")
                .add("jvm.uptime.ms", "3600000", "JVM uptime (ms)")
                .add("jvm.threads.count", "45", "Number of active threads")
                .add("jvm.gc.count", "12", "Number of garbage collections")
                .add("jvm.gc.time.ms", "250", "Time spent on GC (ms)");

        category("Glowberry")
                .add("glowberry.timeofday.24h", "14:30", "Minecraft Time (24h)")
                .add("glowberry.timeofday.12h", "02:30 PM", "Minecraft Time (12h)");
    }

    private static CategoryBuilder category(String name) {
        return new CategoryBuilder(name);
    }

    private static class CategoryBuilder {
        private final String categoryName;
        CategoryBuilder(String name) { this.categoryName = name; }

        public CategoryBuilder add(String key, String ex, String desc) {
            PLACEHOLDERS.add(new PlaceholderInfo(key, ex, desc, categoryName));
            return this;
        }
    }
}