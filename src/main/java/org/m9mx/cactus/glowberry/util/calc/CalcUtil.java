package org.m9mx.cactus.glowberry.util.calc;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stateless helper utilities for the calculator command:
 * number formatting, coordinate conversion, and item storage breakdown.
 */
public final class CalcUtil {

    public static final int STACK = 64;
    public static final int SHULKER = 1728;

    private CalcUtil() {
    }

    /**
     * Formats a coordinate value with exactly 1 decimal place (e.g. {@code 47.4}, {@code 800.0}).
     */
    public static String formatCoord(double value) {
        return String.format("%.1f", value);
    }

    /**
     * Formats a double for display: whole numbers show without a decimal point,
     * otherwise up to 6 significant fractional digits with trailing zeros trimmed.
     */
    public static String formatNumber(double value) {
        if (Double.isNaN(value)) {
            return "NaN";
        }
        if (Double.isInfinite(value)) {
            return value > 0 ? "Infinity" : "-Infinity";
        }
        if (value == Math.rint(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        BigDecimal bd = BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toPlainString();
    }

    /** Result of a dimension coordinate conversion. */
    public static final class CoordResult {
        public final double x;
        public final double z;

        public CoordResult(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }

    /** Converts Overworld coordinates to their Nether equivalent (divide by 8). */
    public static CoordResult overworldToNether(double x, double z) {
        return new CoordResult(x / 8.0, z / 8.0);
    }

    /** Converts Nether coordinates to their Overworld equivalent (multiply by 8). */
    public static CoordResult netherToOverworld(double x, double z) {
        return new CoordResult(x * 8.0, z * 8.0);
    }

    /** Result of breaking a raw item count into shulkers, stacks, and remaining items. */
    public static final class StorageBreakdown {
        public final long total;
        public final long shulkers;
        public final long stacks;
        public final long items;

        public StorageBreakdown(long total, long shulkers, long stacks, long items) {
            this.total = total;
            this.shulkers = shulkers;
            this.stacks = stacks;
            this.items = items;
        }
    }

    /**
     * Breaks a raw item count into the exact number of full shulker boxes (1728),
     * full stacks (64), and remaining individual items.
     */
    public static StorageBreakdown breakdown(long count) {
        long total = count;
        long shulkers = count / SHULKER;
        long afterShulkers = count % SHULKER;
        long stacks = afterShulkers / STACK;
        long items = afterShulkers % STACK;
        return new StorageBreakdown(total, shulkers, stacks, items);
    }
}
