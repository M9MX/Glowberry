package org.m9mx.cactus.glowberry.util.config;

import com.dwarslooper.cactus.client.systems.config.ConfigHandler;
import com.dwarslooper.cactus.client.systems.config.FileConfiguration;
import com.dwarslooper.cactus.client.systems.config.TreeSerializerFilter;
import com.google.gson.JsonObject;

/**
 * Glowberry's own config file. It exists so the addon can register a
 * {@link FileConfiguration} through the addon API - Cactus only makes the
 * {@link ConfigHandler} service available to addon registrations of this type
 * (and it does so before the configs are loaded). The actual work - adding the
 * Auto Save setting to Cactus' settings and registering the Cheats content
 * pack - happens as a side effect in the registration factory.
 */
public class GlowberryConfig extends FileConfiguration<GlowberryConfig> {

    public GlowberryConfig(ConfigHandler handler) {
        super("glowberry", handler);
    }

    @Override
    public JsonObject toJson(TreeSerializerFilter filter) {
        return new JsonObject();
    }

    @Override
    public GlowberryConfig fromJson(JsonObject json) {
        return this;
    }
}
