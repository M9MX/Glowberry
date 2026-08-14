package org.m9mx.cactus.glowberry.util;

/**
 * How the Cactus text rainbow ("Text Chroma" on a HUD element) animates:
 *
 * <ul>
 *   <li>{@link #SMOOTH} - the whole text slowly shifts color together (Cactus'
 *       original behavior).</li>
 *   <li>{@link #DIAGONAL} - each character has its own color; a diagonal line of
 *       color sweeps across the text, so the rainbow visibly moves character by
 *       character instead of everything changing at once.</li>
 * </ul>
 */
public enum RainbowMode {
    SMOOTH,
    DIAGONAL
}
