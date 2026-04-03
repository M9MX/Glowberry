package org.m9mx.cactus.glowberry.util.cactus.macro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GlowberryMacroManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File STORAGE_FILE = new File("cactus/glowberry/macros.json");

    // THE MEMORY: This stays active as long as the game is open
    public static final List<GlowberryMacro> MACROS = new ArrayList<>();

    public static void load() {
        if (!STORAGE_FILE.exists()) return;
        try (FileReader reader = new FileReader(STORAGE_FILE)) {
            List<GlowberryMacro> loaded = GSON.fromJson(reader, new TypeToken<List<GlowberryMacro>>(){}.getType());
            if (loaded != null) {
                MACROS.clear();
                MACROS.addAll(loaded);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void saveToFile() {
        // Don't create the file if there's nothing to save
        if (MACROS.isEmpty() && !STORAGE_FILE.exists()) return;

        if (!STORAGE_FILE.getParentFile().exists()) STORAGE_FILE.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(STORAGE_FILE)) {
            GSON.toJson(MACROS, writer);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static class GlowberryMacro {
        public String name;
        public String trigger;
        public boolean isStringMode;
        public List<String> commands;

        public GlowberryMacro(String name, String trigger, boolean isStringMode, List<String> commands) {
            this.name = name;
            this.trigger = trigger;
            this.isStringMode = isStringMode;
            this.commands = new ArrayList<>(commands); // Always use a new list to avoid reference issues
        }
    }
}