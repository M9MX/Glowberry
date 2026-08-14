package org.m9mx.cactus.glowberry.util.waila;

import java.util.List;

/**
 * A single piece of Waila information. Implementations append {@link WailaLine}s
 * to the list for the current target, or do nothing when the target isn't
 * relevant. One provider is registered per {@link WailaFeature}.
 */
@FunctionalInterface
public interface WailaInfoProvider {
    void addInfo(WailaInfoContext context, List<WailaLine> lines);
}
