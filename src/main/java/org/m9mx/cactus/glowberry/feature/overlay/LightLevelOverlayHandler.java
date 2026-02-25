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

    private static final CachedOverlayManager overlayManager = new CachedOverlayManager((blockPos -> {
        if (!shouldRenderOverlay(blockPos)) return OverlayRendererBlockData.NO_RENDER;

        if (MC.level == null || MC.player == null) return OverlayRendererBlockData.NO_RENDER;

        int lightLevel = MC.level.getBrightness(LightLayer.BLOCK, blockPos.above());
        int threshold = LightLevelModule.INSTANCE.getThreshold();

        // Hide safe areas if showSafeAreas is false
        if (lightLevel >= threshold && !LightLevelModule.INSTANCE.shouldShowSafeAreas()) {
            return OverlayRendererBlockData.NO_RENDER;
        }

        float[] colors = getColorFloats(lightLevel);

        // Return block data with texture section for number overlay
        return new OverlayRendererBlockData(blockPos, colors[0], colors[1], colors[2], 
                Optional.of(new TextureSection(lightLevelSpecificTextureSectionData, lightLevel, 0)));
    }));

    public static void init() {
        if (overlay == null) {
            overlay = new Overlay(new LightLevelNumberRenderer(), 
                    LightLevelModule.INSTANCE.getChunkScanRange(), overlayManager);
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
