package org.m9mx.cactus.glowberry.feature.overlay;

import net.lugo.overlaylib.Overlay;
import net.lugo.overlaylib.managers.CachedOverlayManager;
import net.lugo.overlaylib.util.OverlayRendererBlockData;
import net.lugo.overlaylib.util.TextureSection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import org.m9mx.cactus.glowberry.feature.modules.LightLevelModule;
import com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting;
import java.awt.Color;

import java.util.Optional;
import java.util.Set;

public class LightLevelOverlayHandler {
    private static final Minecraft MC = Minecraft.getInstance();
    private static boolean isActive = false;
    private static Overlay overlay;

    private static final Set<net.minecraft.world.level.block.Block> forbiddenBlocks = Set.of(
            Blocks.BEDROCK,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK
    );

    private static final TextureSection.TextureSectionData lightLevelSpecificTextureSectionData = 
            new TextureSection.TextureSectionData(16, 1);

    private static int currentScanRange = 8;
    private static int currentThreshold = 1;
    private static ColorSetting.ColorValue currentUnsafeColor = new ColorSetting.ColorValue(new Color(255, 68, 68), true);
    private static ColorSetting.ColorValue currentSafeColor = new ColorSetting.ColorValue(new Color(68, 255, 68), true);

    // State caching to prevent unnecessary updates
    private static boolean showSafeAreasState = false;
    private static int lastScanRange = -1;
    private static int lastThreshold = -1;
    private static ColorSetting.ColorValue lastUnsafeColor = null;
    private static ColorSetting.ColorValue lastSafeColor = null;

    private static final CachedOverlayManager overlayManager = new CachedOverlayManager((blockPos -> {
        if (!shouldRenderOverlay(blockPos)) return OverlayRendererBlockData.NO_RENDER;

        if (MC.level == null || MC.player == null) return OverlayRendererBlockData.NO_RENDER;

        // Check if within block-based range around player
        BlockPos playerPos = MC.player.blockPosition();
        double distance = Math.sqrt(Math.pow(blockPos.getX() - playerPos.getX(), 2) + 
                                  Math.pow(blockPos.getZ() - playerPos.getZ(), 2));
        if (distance > currentScanRange) {
            return OverlayRendererBlockData.NO_RENDER;
        }

        int lightLevel = MC.level.getBrightness(LightLayer.BLOCK, blockPos.above());

        // Use cached showSafeAreas state to prevent flickering
        if (!showSafeAreasState && lightLevel > 0) {
            return OverlayRendererBlockData.NO_RENDER;
        }

        float[] colors = getCurrentColorFloats(lightLevel);

        // Always show numbers
        return new OverlayRendererBlockData(blockPos, colors[0], colors[1], colors[2], 
                Optional.of(new TextureSection(lightLevelSpecificTextureSectionData, lightLevel, 0)));
    }));
    
    private static BlockPos lastPlayerPos = null;
    private static int updateCounter = 0;

    public static void init() {
        if (overlay == null) {
            // Use a larger fixed chunk radius to cover our block-based range
            // 32 blocks = 2 chunks, so use 4 chunks to be safe
            overlay = new Overlay(new LightLevelNumberRenderer(), 4, overlayManager);
            overlay.register();
        }
        setActive(true);
    }

    public static void setActive(boolean active) {
        isActive = active;
        if (overlay != null) {
            overlay.setActive(active);
        }
    }

    public static boolean shouldRenderOverlay(BlockPos pos) {
        if (MC.level == null || MC.player == null) return false;

        // Check if position is forbidden
        if (forbiddenBlocks.contains(MC.level.getBlockState(pos).getBlock())) {
            return false;
        }

        // Check if position is solid and suitable for mob spawning
        BlockPos above = pos.above();
        boolean isTopSolid = MC.level.loadedAndEntityCanStandOn(pos, MC.player);
        boolean aboveTopSolid = MC.level.loadedAndEntityCanStandOnFace(above, MC.player, Direction.DOWN);

        if (!isTopSolid || aboveTopSolid) {
            return false;
        }

        return true;
    }

    private static float[] getColorFloats(int lightLevel) {
        int color = LightLevelModule.INSTANCE.getColorForLightLevel(lightLevel);
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        return new float[]{r, g, b};
    }

    private static float[] getCurrentColorFloats(int lightLevel) {
        // Use current threshold and colors for real-time updates
        ColorSetting.ColorValue colorValue = lightLevel < currentThreshold ? currentUnsafeColor : currentSafeColor;
        
        // Check if color uses RGB (rainbow mode)
        if (colorValue.usesRgb()) {
            return getRainbowColor(lightLevel);
        }
        
        int colorInt = colorValue.color();
        float r = ((colorInt >> 16) & 0xFF) / 255.0f;
        float g = ((colorInt >> 8) & 0xFF) / 255.0f;
        float b = (colorInt & 0xFF) / 255.0f;
        return new float[]{r, g, b};
    }
    
    private static void checkAndUpdateColors() {
        // Check if colors changed and update if needed
        ColorSetting.ColorValue newUnsafeColor = LightLevelModule.INSTANCE.unsafeColor.get();
        ColorSetting.ColorValue newSafeColor = LightLevelModule.INSTANCE.safeColor.get();
        
        boolean colorsChanged = false;
        
        if (!currentUnsafeColor.equals(newUnsafeColor)) {
            currentUnsafeColor = newUnsafeColor;
            colorsChanged = true;
        }
        
        if (!currentSafeColor.equals(newSafeColor)) {
            currentSafeColor = newSafeColor;
            colorsChanged = true;
        }
        
        // Force update if colors changed
        if (colorsChanged) {
            overlayManager.clearAll();
        }
    }
    
    private static float[] getRainbowColor(int lightLevel) {
        // Create rainbow effect based on light level and time
        long time = System.currentTimeMillis() / 30; // Faster color change
        float hue = (time + lightLevel * 25) % 360; // Different hue per light level
        float saturation = 0.8f;
        float brightness = 0.9f;
        
        // Use Java's built-in HSB to RGB conversion
        java.awt.Color rainbowColor = java.awt.Color.getHSBColor(hue / 360f, saturation, brightness);
        return new float[]{
            rainbowColor.getRed() / 255.0f,
            rainbowColor.getGreen() / 255.0f,
            rainbowColor.getBlue() / 255.0f
        };
    }

    public static void updateSettings(int scanRange, int threshold, 
                                    ColorSetting.ColorValue unsafeColor, ColorSetting.ColorValue safeColor) {
        boolean needsUpdate = false;
        
        // Check showSafeAreas state
        boolean newShowSafeAreas = LightLevelModule.INSTANCE.shouldShowSafeAreas();
        if (showSafeAreasState != newShowSafeAreas) {
            showSafeAreasState = newShowSafeAreas;
            needsUpdate = true;
        }
        
        // Check scan range
        if (lastScanRange != scanRange) {
            currentScanRange = scanRange;
            lastScanRange = scanRange;
            needsUpdate = true;
        }
        
        // Check threshold
        if (lastThreshold != threshold) {
            currentThreshold = threshold;
            lastThreshold = threshold;
            needsUpdate = true;
        }
        
        // Check unsafe color
        if (lastUnsafeColor == null || !lastUnsafeColor.equals(unsafeColor)) {
            currentUnsafeColor = unsafeColor;
            lastUnsafeColor = unsafeColor;
            needsUpdate = true;
        }
        
        // Check safe color
        if (lastSafeColor == null || !lastSafeColor.equals(safeColor)) {
            currentSafeColor = safeColor;
            lastSafeColor = safeColor;
            needsUpdate = true;
        }
        
        // Only refresh overlays if settings actually changed
        if (needsUpdate) {
            overlayManager.clearAll();
        }
        
        // Also update colors every tick for smooth rainbow transitions
        checkAndUpdateColors();
        
        // Track player position and force updates on movement for new areas
        if (MC.player != null) {
            BlockPos currentPos = MC.player.blockPosition();
            if (lastPlayerPos == null || !lastPlayerPos.equals(currentPos)) {
                lastPlayerPos = currentPos;
                updateCounter++;
                
                // Only update when necessary to prevent flickering
                // Update every 20 ticks when still, every 5 ticks when moving
                if (updateCounter % 20 == 0 || (updateCounter % 5 == 0 && !lastPlayerPos.equals(currentPos))) {
                    overlayManager.clearAll();
                }
            }
        }
    }

    public static void updateChunkScanRadius(int radius) {
        if (overlay != null) {
            overlay.setChunkScanRadius(radius);
        }
    }

    public static void clear(BlockPos pos) {
        overlayManager.clearFromBlockPos(pos);
    }

    public static void clear(SectionPos section) {
        overlayManager.clear(section);
    }

    public static void clearAll() {
        overlayManager.clearAll();
    }

    public static boolean isActive() {
        return isActive;
    }
}
