package org.m9mx.cactus.glowberry.feature.modules;

import com.dwarslooper.cactus.client.feature.module.Category;
import com.dwarslooper.cactus.client.feature.module.Module;
import com.dwarslooper.cactus.client.feature.module.Module.Options;
import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting;
import com.dwarslooper.cactus.client.systems.config.settings.impl.Setting;
import com.dwarslooper.cactus.client.systems.emoji.EmojiCode;
import com.dwarslooper.cactus.client.systems.emoji.EmojiManager;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import 	net.minecraft.client.Minecraft;
import org.m9mx.cactus.glowberry.GlowberryCactus;

public class CustomEmojiModule extends Module {
    private final File emojiFile;
    public final Setting<Boolean> openFileOnEnable;
    public final Setting<Boolean> reloadOnEnable;

    public CustomEmojiModule(Category category) {
        super("customEmojis", category, new Options());
        this.emojiFile = new File(Minecraft.getInstance().gameDirectory, "cactus/glowberry_emojis.txt");
        SettingGroup sgGeneral = this.settings.buildGroup("settings");
        this.openFileOnEnable = sgGeneral.add(new BooleanSetting("Open Emoji Config on Enable", true));
        this.reloadOnEnable = sgGeneral.add(new BooleanSetting("Reload Config on Enable", true));
    }

    public void onEnable() {
        if ((Boolean)this.openFileOnEnable.get()) {
            this.openEmojiFile();
        }

        if ((Boolean)this.reloadOnEnable.get()) {
            this.loadCustomEmojis();
        }

    }

    private void openEmojiFile() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(this.emojiFile);
            }
        } catch (Exception var2) {
            GlowberryCactus.LOGGER.error("Could not open emoji file: {}", var2.getMessage());
        }

    }

    public void loadCustomEmojis() {
        if (!this.emojiFile.exists()) {
            this.createDefaultFile();
        } else {
            try {
                InputStreamReader reader = new InputStreamReader(new FileInputStream(this.emojiFile), StandardCharsets.UTF_8);

                try {
                    Properties prop = new Properties();
                    prop.load(reader);
                    prop.forEach((key, value) -> {
                        String name = key.toString().trim();
                        String emoji = value.toString().trim();
                        EmojiManager.getEmojis().add(new EmojiCode(name, emoji));
                    });
                    GlowberryCactus.LOGGER.info("Loaded {} custom emojis from glowberry_emojis.txt", prop.size());
                } catch (Throwable var5) {
                    try {
                        reader.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }

                    throw var5;
                }

                reader.close();
            } catch (IOException var6) {
                GlowberryCactus.LOGGER.error("Failed to load custom emojis from file!");
            }

        }
    }

    private void createDefaultFile() {
        try {
            File parent = this.emojiFile.getParentFile();
            if (parent != null && !parent.exists() && parent.mkdirs()) {
                GlowberryCactus.LOGGER.info("Created directory: {}", parent.getAbsolutePath());
            }

            String defaultContent = "# Glowberry Custom Emojis\n# Usage: code=emoji\nL=★\nsmile=☺\n";
            Files.writeString(this.emojiFile.toPath(), defaultContent, StandardCharsets.UTF_8);
        } catch (IOException var3) {
        }

    }
}
