package org.m9mx.cactus.glowberry.util.waila;

import com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup;
import com.dwarslooper.cactus.client.systems.config.settings.impl.EnumSetSetting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for the grouped Waila providers.
 */
public final class WailaRegistry {
    private static final Map<WailaFeature, WailaInfoProvider> PROVIDERS = new EnumMap<>(WailaFeature.class);

    private WailaRegistry() {}

    public static void registerProviders() {
        register(WailaFeature.HEALTH, WailaProviders::health);
        register(WailaFeature.EXTRA_INFO, WailaProviders::extraInfo);
        register(WailaFeature.POTION_EFFECTS, WailaProviders::potionEffects);
        register(WailaFeature.EQUIPMENT, WailaProviders::equipment);
        register(WailaFeature.MOD_NAME, WailaProviders::modName);

        register(WailaFeature.BREAK_PROGRESS, (ctx, lines) -> {});
        register(WailaFeature.REQUIRED_TOOL, (ctx, lines) -> {});
        register(WailaFeature.COORDINATES, WailaProviders::coordinates);
        register(WailaFeature.BLOCK_EXTRA_INFO, WailaProviders::blockExtraInfo);
        register(WailaFeature.BLOCK_PROPERTIES, WailaProviders::blockProperties);
        register(WailaFeature.STORAGE_INFO, WailaProviders::storageInfo);
    }

    public static void register(WailaFeature feature, WailaInfoProvider provider) {
        PROVIDERS.put(feature, provider);
    }

    public static WailaInfoProvider provider(WailaFeature feature) {
        return PROVIDERS.get(feature);
    }

    public static EnumSetSetting<WailaFeature> buildSetting(SettingGroup group) {
       List<WailaFeature> defaults = new ArrayList<>();
       for (WailaFeature feature : WailaFeature.values()) {
            if (feature.defaultEnabled && PROVIDERS.containsKey(feature)) {
                defaults.add(feature);
            }
        }
        EnumSetSetting<WailaFeature> setting = new EnumSetSetting<>("show", WailaFeature.class, defaults.toArray(new WailaFeature[0]));
        group.add(setting);
        return setting;
    }

    public static List<WailaLine> buildLines(WailaInfoContext ctx) {
       List<WailaLine> lines = new ArrayList<>();

        if (ctx.inEditor) {
            WailaProviders.addNameLine(ctx, lines);
            WailaProviders.addEditorPreview(ctx, lines);
            return lines;
        }

        if (ctx.hit == null || ctx.hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            return lines;
        }

        WailaProviders.addNameLine(ctx, lines);

        if (ctx.enabled(WailaFeature.COORDINATES)) {
            WailaInfoProvider provider = PROVIDERS.get(WailaFeature.COORDINATES);
            if (provider != null) {
                try {
                    provider.addInfo(ctx, lines);
                } catch (Throwable ignored) {
                }
            }
        }

        for (WailaFeature feature : WailaFeature.values()) {
            if (feature == WailaFeature.COORDINATES) continue;
            if (!ctx.enabled(feature)) continue;
            WailaInfoProvider provider = PROVIDERS.get(feature);
            if (provider == null) continue;
            try {
                provider.addInfo(ctx, lines);
            } catch (Throwable ignored) {
                // A single feature must never break the whole element - catching
                // Throwable also guards against linkage errors from API mismatches
                // (a NoSuchMethodError would otherwise kill the entire HUD render)
            }
        }
        return lines;
    }
}
