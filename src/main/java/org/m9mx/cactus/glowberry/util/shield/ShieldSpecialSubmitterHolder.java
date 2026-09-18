package org.m9mx.cactus.glowberry.util.shield;
/**
 * Credits: https://github.com/Walksy/ShieldStatus (26.2 branch)
 *
 * Holds the ShieldSpecialSubmitter captured from vanilla's ShieldSpecialRenderer.
 */
public final class ShieldSpecialSubmitterHolder {
	private static volatile ShieldSpecialSubmitter submitter;

	private ShieldSpecialSubmitterHolder() {
	}

	public static void set(ShieldSpecialSubmitter s) {
		submitter = s;
	}

	public static ShieldSpecialSubmitter get() {
		return submitter;
	}
}
