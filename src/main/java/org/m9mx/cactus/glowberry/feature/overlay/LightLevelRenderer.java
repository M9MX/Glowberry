package org.m9mx.cactus.glowberry.feature.overlay;

/**
 * Overlay rendering is handled by overlaylib's SimpleTextureOverlayRenderer.
 * This class is kept for reference but actual rendering is delegated to:
 * - LightLevelNumberRenderer: Extends SimpleTextureOverlayRenderer with the numbers texture
 * - LightLevelOverlayHandler: Manages the Overlay instance and CachedOverlayManager
 * 
 * The system automatically renders light level numbers on blocks during world rendering.
 */
public class LightLevelRenderer {
    // Rendering is handled by overlaylib infrastructure
}
