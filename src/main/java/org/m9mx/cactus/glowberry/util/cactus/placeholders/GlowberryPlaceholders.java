package org.m9mx.cactus.glowberry.util.cactus.placeholders;

import com.dwarslooper.cactus.client.addon.v2.RegistryBus;
import com.dwarslooper.cactus.client.gui.hud.PresetConfig;
import com.dwarslooper.cactus.client.gui.hud.element.impl.MultiLineTextElement;
import com.dwarslooper.cactus.client.systems.params.PlaceholderHandler;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GlowberryPlaceholders {
    public static void register(RegistryBus bus) {
        // Register time of day placeholders (24h and 12h)
        bus.register(PlaceholderHandler.PlaceholderRegistration.class, ctx ->
            new PlaceholderHandler.PlaceholderRegistration(
                "glowberry.timeofday.24h",
                new PlaceholderHandler.StaticPlaceholderValue(
                    () -> formatTimeOfDay24h(getCurrentMcTimeTicks()),
                    "00:00",
                    () -> true
                )
            )
        );
        bus.register(PlaceholderHandler.PlaceholderRegistration.class, ctx ->
            new PlaceholderHandler.PlaceholderRegistration(
                "glowberry.timeofday.12h",
                new PlaceholderHandler.StaticPlaceholderValue(
                    () -> formatTimeOfDay12h(getCurrentMcTimeTicks()),
                    "12:00 AM",
                    () -> true
                )
            )
        );

        // Register presets for both time formats
        bus.register(PresetConfig.class, ctx ->
            new PresetConfig<>(
                MultiLineTextElement.class,
                "glowberry_time_24h",
                (e) -> e.lines.set(List.of(
                    "Time: {glowberry.timeofday.24h}"
                ))
            )
        );
        bus.register(PresetConfig.class, ctx ->
            new PresetConfig<>(
                MultiLineTextElement.class,
                "glowberry_time_12h",
                (e) -> e.lines.set(List.of(
                    "Time: {glowberry.timeofday.12h}"
                ))
            )
        );
    }

    // Helper to get current MC time in ticks (returns 0 if not available)
    private static long getCurrentMcTimeTicks() {
        try {
            // CactusConstants.mc.level.getDayTime() is the usual way
            return com.dwarslooper.cactus.client.util.CactusConstants.mc.level.getDayTime();
        } catch (Exception e) {
            return 0;
        }
    }

    // Convert MC ticks to LocalTime (MC day = 24000 ticks, 0 ticks = 6:00 AM)
    private static LocalTime mcTicksToLocalTime(long ticks) {
        long dayTicks = ticks % 24000L;
        long totalSeconds = dayTicks * 86400L / 24000L;
        long shiftedSeconds = (totalSeconds + 21600L) % 86400L; // +6h shift
        return LocalTime.ofSecondOfDay(shiftedSeconds);
    }

    private static String formatTimeOfDay24h(long ticks) {
        LocalTime time = mcTicksToLocalTime(ticks);
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private static String formatTimeOfDay12h(long ticks) {
        LocalTime time = mcTicksToLocalTime(ticks);
        return time.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }
}