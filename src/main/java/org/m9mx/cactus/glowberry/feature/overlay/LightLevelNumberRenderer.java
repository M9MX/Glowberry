package org.m9mx.cactus.glowberry.feature.overlay;

import net.lugo.overlaylib.renderers.SimpleTextureOverlayRenderer;
import net.minecraft.resources.Identifier;

public class LightLevelNumberRenderer extends SimpleTextureOverlayRenderer {
    private static final Identifier NUMBERS_TEXTURE = Identifier.fromNamespaceAndPath("glowberry", "textures/numbers.png");

    public LightLevelNumberRenderer() {
        super(NUMBERS_TEXTURE, true);
    }
}
