package org.m9mx.cactus.glowberry.util.text;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses [icon:item_id] tags from plain strings so render code can draw item icons inline.
 */
public final class InlineItemIconTextHelper {
    public static final String ICON_TAG_PREFIX = "[icon:";
    public static final String ITEM_CHAT_TAG_PREFIX = ":item:";
    private static final Pattern ICON_TAG_PATTERN = Pattern.compile(
            "\\[icon:([a-z0-9_./-]+(?::[a-z0-9_./-]+)?)]|:item:([a-z0-9_./-]+(?::[a-z0-9_./-]+)?):"
    );

    private InlineItemIconTextHelper() {
    }

    public static List<Segment> parse(String input) {
        List<Segment> segments = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return segments;
        }

        Matcher matcher = ICON_TAG_PATTERN.matcher(input);
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                segments.add(Segment.text(input.substring(cursor, matcher.start())));
            }

            String idRaw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            Item item = resolveItem(idRaw);
            if (item == Items.AIR) {
                segments.add(Segment.text(matcher.group()));
            } else {
                segments.add(Segment.icon(new ItemStack(item)));
            }

            cursor = matcher.end();
        }

        if (cursor < input.length()) {
            segments.add(Segment.text(input.substring(cursor)));
        }

        return segments;
    }

    public static boolean hasIconTag(String input) {
        return input != null && (input.contains(ICON_TAG_PREFIX) || input.contains(ITEM_CHAT_TAG_PREFIX));
    }

    private static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Items.AIR;
        }

        String normalized = itemId.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).toString().equals(normalized)) {
                return item;
            }
        }

        return Items.AIR;
    }

    public record Segment(String text, ItemStack iconStack, boolean icon) {
        public static Segment text(String text) {
            return new Segment(text, ItemStack.EMPTY, false);
        }

        public static Segment icon(ItemStack iconStack) {
            return new Segment("", iconStack, true);
        }
    }
}

