package org.m9mx.cactus.glowberry.util.shield;
/**
 * Credits: https://github.com/Walksy/ShieldStatus
 */
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GrayscaleTextureCache {
	private static final Map<Identifier, Identifier> cache = new HashMap<>();

	public Identifier get(Identifier original) {
		return cache.computeIfAbsent(original, this::convert);
	}

	private Identifier convert(Identifier original) {
		Minecraft client = Minecraft.getInstance();
		TextureManager textureManager = client.getTextureManager();

		// Try to load the original texture
		Optional<Resource> optRes = client.getResourceManager().getResource(original);
		if (optRes.isEmpty()) {
			return original;
		}

		try (InputStream in = optRes.get().open()) {
			// Load the image
			com.mojang.blaze3d.platform.NativeImage originalImage = com.mojang.blaze3d.platform.NativeImage.read(in);

			// Create grayscale version
			com.mojang.blaze3d.platform.NativeImage grayscale = new com.mojang.blaze3d.platform.NativeImage(
				originalImage.getWidth(),
				originalImage.getHeight(),
				false
			);

			for (int y = 0; y < grayscale.getHeight(); y++) {
				for (int x = 0; x < grayscale.getWidth(); x++) {
					int rgba = originalImage.getPixel(x, y);

					int a = (rgba >> 24) & 0xFF;
					int r = (rgba >> 16) & 0xFF;
					int g = (rgba >> 8) & 0xFF;
					int b = rgba & 0xFF;

					// Standard grayscale conversion
					int gray = (int) (r * 0.299f + g * 0.587f + b * 0.114f);
					grayscale.setPixel(x, y, (a << 24) | (gray << 16) | (gray << 8) | gray);
				}
			}

			// Register the grayscale texture
			Identifier newId = Identifier.withDefaultNamespace("glowberry/grayscale/" + original.getPath());
			DynamicTexture dynamicTexture = new DynamicTexture(() -> "glowberry:grayscale/" + original.getPath(), grayscale);
			textureManager.register(newId, dynamicTexture);

			return newId;
		} catch (IOException e) {
			e.printStackTrace();
			return original;
		}
	}
}
