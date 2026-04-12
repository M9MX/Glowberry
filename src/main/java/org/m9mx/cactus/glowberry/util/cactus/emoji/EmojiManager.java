package org.m9mx.cactus.glowberry.util.cactus.emoji;

import java.util.HashSet;
import java.util.Set;

import static org.m9mx.cactus.glowberry.util.cactus.emoji.EmojiBase.BASE_EMOJIS;

public class EmojiManager {
    public static final Set<EmojiCode> CUSTOM_EMOJIS = Set.of(
            new EmojiCode("bucket", "\ud83e\udea3"),
            new EmojiCode("shears", "✂"),
            new EmojiCode("flag", "⚑"),
            new EmojiCode("bell", "\ud83d\udd14"),
            new EmojiCode("hunger", "\ud83c\udf56"),
            new EmojiCode("happy", "☺"),
            new EmojiCode("happy_filled", "☻"),
            new EmojiCode("sad", "☹"),
            new EmojiCode("pickaxe", "⛏"),
            new EmojiCode("axe", "\ud83e\ude93"),
            new EmojiCode("fishing", "\ud83c\udfa3"),
            new EmojiCode("umbrella", "☂"),
            new EmojiCode("trident", "\ud83d\udd31"),
            new EmojiCode("sword", "\ud83d\udde1"),
            new EmojiCode("shield", "\ud83d\udee1"),
            new EmojiCode("swords", "⚔"),
            new EmojiCode("bow", "\ud83c\udff9"),
            new EmojiCode("potion", "\ud83e\uddea"),
            new EmojiCode("splash", "⚗"),
            new EmojiCode("copyright", "©"),
            new EmojiCode("registered", "®"),
            new EmojiCode("protected", "℗"),
            new EmojiCode("trademark", "™"),
            new EmojiCode("globe", "\ud83c\udf0d"),
            new EmojiCode("globe2", "\ud83c\udf0e"),
            new EmojiCode("globe3", "\ud83c\udf0f"),
            new EmojiCode("anchor", "⚓"),
            new EmojiCode("wave", "\ud83c\udf0a"),
            new EmojiCode("male", "♂"),
            new EmojiCode("female", "♀"),
            new EmojiCode("intersex", "⚥"),
            new EmojiCode("left", "⏴"),
            new EmojiCode("right", "⏵"),
            new EmojiCode("up", "⏶"),
            new EmojiCode("down", "⏷"),
            new EmojiCode("hand_left", "☜"),
            new EmojiCode("hand_right", "☞"),
            new EmojiCode("swap", "⇄"),
            new EmojiCode("ying_yang", "☯"),
            new EmojiCode("peace", "☮"),
            new EmojiCode("sun", "☀"),
            new EmojiCode("cloud", "☁"),
            new EmojiCode("comet", "☄"),
            new EmojiCode("moon", "☽"),
            new EmojiCode("snowman", "⛄"),
            new EmojiCode("snowman_snow", "☃"),
            new EmojiCode("storm", "⛈"),
            new EmojiCode("snowflake", "❄"),
            new EmojiCode("eject", "⏏"),
            new EmojiCode("fast_forward", "⏩"),
            new EmojiCode("fast_backward", "⏪"),
            new EmojiCode("to_end", "⏭"),
            new EmojiCode("to_start", "⏮"),
            new EmojiCode("play_pause", "⏯"),
            new EmojiCode("pause", "⏸"),
            new EmojiCode("power_on", "⏻"),
            new EmojiCode("power_off", "⏼"),
            new EmojiCode("hourglass_full", "⏳")
    );

    public static final Set<EmojiCode> EMOJIS = new HashSet<>();

    static {
        EMOJIS.addAll(BASE_EMOJIS);
        EMOJIS.addAll(CUSTOM_EMOJIS);
    }
}