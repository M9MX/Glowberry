package org.m9mx.cactus.glowberry.util.cactus.emoji;

public record EmojiCode(String name, String emoji) {
    public EmojiCode(String name, String emoji) {
        this.name = name;
        this.emoji = emoji;
    }

    public String name() {
        return this.name;
    }

    public String emoji() {
        return this.emoji;
    }
}

