package org.m9mx.cactus.glowberry.util.calc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory, session-only storage for the calculator's last answer ({@code ans})
 * and its calculation history. State resets when the game is restarted.
 */
public final class CalculatorSession {

    /** A single history entry: the raw expression and its computed result. */
    public static final class Entry {
        private final String expression;
        private final double result;

        public Entry(String expression, double result) {
            this.expression = expression;
            this.result = result;
        }

        public String expression() {
            return expression;
        }

        public double result() {
            return result;
        }
    }

    private static final int MAX_HISTORY = 100;

    private static double ans = 0.0;
    private static boolean hasAns = false;
    private static final List<Entry> HISTORY = new ArrayList<>();

    private CalculatorSession() {
    }

    /** The last successful result, or 0 if none yet. */
    public static double getAns() {
        return ans;
    }

    /** Whether at least one calculation has been performed this session. */
    public static boolean hasAns() {
        return hasAns;
    }

    /** Records a successful calculation, updating {@code ans} and the history. */
    public static void record(String expression, double result) {
        ans = result;
        hasAns = true;
        HISTORY.add(new Entry(expression, result));
        while (HISTORY.size() > MAX_HISTORY) {
            HISTORY.remove(0);
        }
    }

    /** An unmodifiable view of the session history, oldest first. */
    public static List<Entry> getHistory() {
        return Collections.unmodifiableList(HISTORY);
    }

    /** Clears the history and resets {@code ans}. */
    public static void clear() {
        HISTORY.clear();
        ans = 0.0;
        hasAns = false;
    }
}
