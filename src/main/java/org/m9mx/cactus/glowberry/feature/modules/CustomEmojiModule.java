package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.systems.emoji.EmojiManager;
import com.dwarslooper.cactus.client.systems.emoji.EmojiCode;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import org.m9mx.cactus.glowberry.GlowberryCactus;
import net.minecraft.client.Minecraft;
import java.awt.Desktop;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

public class CustomEmojiModule extends Module {
    private final File emojiFile;

    public final Setting<Boolean> openFileOnEnable;
    public final Setting<Boolean> reloadOnEnable;

    public CustomEmojiModule(Category category) {
        super("customEmojis", category, new Module.Options());

        this.emojiFile = new File(Minecraft.getInstance().gameDirectory, "cactus/glowberry_emojis.txt");

        SettingGroup sgGeneral = this.settings.buildGroup("settings");

        this.openFileOnEnable = sgGeneral.add(new BooleanSetting("Open Emoji Config on Enable", true));
        this.reloadOnEnable = sgGeneral.add(new BooleanSetting("Reload Config on Enable", true));
    }

    @Override
    public void onEnable() {
        if (openFileOnEnable.get()) {
            openEmojiFile();
        }

        if (reloadOnEnable.get()) {
            loadCustomEmojis();
        }
    }

    private void openEmojiFile() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(emojiFile);
            }
        } catch (Exception e) {
            GlowberryCactus.LOGGER.error("Could not open emoji file: {}", e.getMessage());
        }
    }

    public void loadCustomEmojis() {
        if (!emojiFile.exists()) {
            createDefaultFile();
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(emojiFile), StandardCharsets.UTF_8)) {
            Properties prop = new Properties();
            prop.load(reader);

            prop.forEach((key, value) -> {
                String name = key.toString().trim();
                String emoji = value.toString().trim();
                EmojiManager.getEmojis().add(new EmojiCode(name, emoji));
            });

            GlowberryCactus.LOGGER.info("Loaded {} custom emojis from glowberry_emojis.txt", prop.size());
        } catch (IOException ex) {
            GlowberryCactus.LOGGER.error("Failed to load custom emojis from file!");
        }
    }

    private void createDefaultFile() {
        try {
            File parent = emojiFile.getParentFile();
            if (parent != null && !parent.exists()) {
                if (parent.mkdirs()) {
                    GlowberryCactus.LOGGER.info("Created directory: {}", parent.getAbsolutePath());
                }
            }

            String defaultContent = """
                # Glowberry Custom Emojis
                # Usage: code=emoji
                L=★
                smile=☺
                """;

            Files.writeString(emojiFile.toPath(), defaultContent, StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }
}

// by error dev
// this hurt my soul making this