package org.m9mx.cactus.glowberry.util.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Centralized compatibility map used by both module registration and mixin loading.
 */
public final class IncompatibilityRegistry {

    private static final List<Rule> RULES = List.of(
            mod("accurateblockplacement")
                    .mixin("BreakMixin", "GameRendererMixin")
                    .module("fastPlace", "fastBreak")
                    .setting("fastPlace", "enabled")
                    .setting("fastBreak", "enabled")
                    .build(),
            mod("scribble")
                    .mixin(
                            "scribble.BookSignScreenMixin",
                            "scribble.ClientPacketListenerMixin",
                            "scribble.KeyboardHandlerMixin",
                            "scribble.LocalPlayerMixin",
                            "scribble.MenuScreensMixin"
                    )
                    .module("scribble")
                    .build(),
            mod("better-ping-display")
                    .modAlias("betterpingdisplay", "pingview")
                    .mixin("PlayerTabOverlayMixin")
                    .setting("tabList", "showPing")
                    .build(),
            mod("nmc") // No Mining Cooldown
                    .mixin("BreakMixin")
                    .module("fastBreak")
                    .build()
    );

    private IncompatibilityRegistry() {
    }

    private static RuleBuilder mod(String modId) {
        return new RuleBuilder(modId);
    }

    public static boolean isMixinBlocked(String mixinClassName) {
        if (mixinClassName == null) {
            return false;
        }

        for (Rule rule : RULES) {
            if (!rule.isTriggered()) {
                continue;
            }

            for (String blockedMixin : rule.blockedMixins()) {
                if (mixinClassName.equals(blockedMixin) || mixinClassName.endsWith("." + blockedMixin)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isModuleBlocked(String moduleId) {
        if (moduleId == null) {
            return false;
        }

        String normalizedModuleId = moduleId.toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            if (rule.isTriggered() && rule.blockedModules().contains(normalizedModuleId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSettingBlocked(String moduleId, String settingId) {
        if (moduleId == null || settingId == null) {
            return false;
        }

        String normalizedKey = (moduleId + "." + settingId).toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            if (rule.isTriggered() && rule.blockedSettings().contains(normalizedKey)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> blockingModsForModule(String moduleId) {
        if (moduleId == null) {
            return Collections.emptySet();
        }

        Set<String> blockingMods = new LinkedHashSet<>();
        String normalizedModuleId = moduleId.toLowerCase(Locale.ROOT);
        for (Rule rule : RULES) {
            if (!rule.blockedModules().contains(normalizedModuleId)) {
                continue;
            }

            for (String modId : rule.triggerModIds()) {
                if (isModLoaded(modId)) {
                    blockingMods.add(modId);
                }
            }
        }
        return blockingMods;
    }

    private static boolean isModLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class RuleBuilder {
        private final Set<String> triggerModIds = new LinkedHashSet<>();
        private final Set<String> blockedMixins = new LinkedHashSet<>();
        private final Set<String> blockedModules = new LinkedHashSet<>();
        private final Set<String> blockedSettings = new LinkedHashSet<>();

        private RuleBuilder(String modId) {
            this.triggerModIds.add(modId);
        }

        private RuleBuilder modAlias(String... modIds) {
            Collections.addAll(this.triggerModIds, modIds);
            return this;
        }

        private RuleBuilder mixin(String... mixinClassNames) {
            Collections.addAll(this.blockedMixins, mixinClassNames);
            return this;
        }

        private RuleBuilder module(String... moduleIds) {
            Collections.addAll(this.blockedModules, moduleIds);
            return this;
        }

        private RuleBuilder setting(String moduleId, String settingId) {
            this.blockedSettings.add(moduleId + "." + settingId);
            return this;
        }

        private Rule build() {
            return Rule.of(triggerModIds, blockedMixins, blockedModules, blockedSettings);
        }
    }

    private record Rule(
            Set<String> triggerModIds,
            Set<String> blockedMixins,
            Set<String> blockedModules,
            Set<String> blockedSettings
    ) {
        private static Rule of(
                Set<String> triggerModIds,
                Set<String> blockedMixins,
                Set<String> blockedModules,
                Set<String> blockedSettings
        ) {
            return new Rule(
                    normalize(triggerModIds),
                    blockedMixins,
                    normalize(blockedModules),
                    normalize(blockedSettings)
            );
        }

        private boolean isTriggered() {
            for (String modId : triggerModIds) {
                if (isModLoaded(modId)) {
                    return true;
                }
            }
            return false;
        }

        private static Set<String> normalize(Set<String> values) {
            Set<String> normalized = new LinkedHashSet<>();
            for (String value : values) {
                normalized.add(value.toLowerCase(Locale.ROOT));
            }
            return Collections.unmodifiableSet(normalized);
        }
    }
}

