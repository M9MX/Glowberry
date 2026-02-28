package org.m9mx.cactus.glowberry.feature.overlay;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import org.m9mx.cactus.glowberry.feature.modules.LightLevelModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LightLevelOverlayHandler {
    private static final Minecraft MC = Minecraft.getInstance();
    private static boolean isActive = false;
    private static int blockRadius = 128;

    private static final Set<net.minecraft.world.level.block.Block> forbiddenBlocks = Set.of(
            Blocks.BEDROCK,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK
    );

    // Store blocks to render: BlockPos -> light level
    private static final Map<BlockPos, Integer> blocksToRender = new HashMap<>();

    public static void init() {
        // We removed the frequent tick update - now handled by mixin with reduced frequency
        // Initialize renderer
        LightLevelRenderer.init();
    }

    private static void updateBlocksInRadius() {
        blocksToRender.clear();
        
        if (MC.player == null || MC.level == null) return;

        BlockPos playerPos = MC.player.blockPosition();
        int radiusSq = blockRadius * blockRadius;

        // Scan all blocks within radius using spherical distance
        for (int x = playerPos.getX() - blockRadius; x <= playerPos.getX() + blockRadius; x++) {
            for (int z = playerPos.getZ() - blockRadius; z <= playerPos.getZ() + blockRadius; z++) {
                // Quick 2D distance check to skip obvious cubes
                int dx2 = (x - playerPos.getX());
                int dz2 = (z - playerPos.getZ());
                if (dx2 * dx2 + dz2 * dz2 > radiusSq) continue;

                for (int y = playerPos.getY() - blockRadius; y <= playerPos.getY() + blockRadius; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    // Full 3D distance check
                    int dx = x - playerPos.getX();
                    int dy = y - playerPos.getY();
                    int dz = z - playerPos.getZ();
                    if (dx * dx + dy * dy + dz * dz > radiusSq) continue;

                    if (shouldRenderBlock(pos)) {
                        int lightLevel = MC.level.getBrightness(LightLayer.BLOCK, pos.above());
                        blocksToRender.put(pos, lightLevel);
                    }
                }
            }
        }
    }

    private static boolean shouldRenderBlock(BlockPos pos) {
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

        int lightLevel = MC.level.getBrightness(LightLayer.BLOCK, above);
        int threshold = LightLevelModule.INSTANCE.getThreshold();

        // Only render if light level is below threshold (unsafe areas) or if safe areas should be shown
        boolean isUnsafeArea = lightLevel < threshold;
        boolean isSafeArea = lightLevel >= threshold;
        
        // If it's a safe area, only render if showSafeAreas is enabled
        if (isSafeArea && !LightLevelModule.INSTANCE.shouldShowSafeAreas()) {
            return false;
        }

        // At this point, we're either rendering an unsafe area (always) or a safe area (when enabled)
        return true;
    }

    public static void setActive(boolean active) {
        isActive = active;
        if (!active) {
            blocksToRender.clear();
        }
    }

    public static void updateChunkScanRadius(int radius) {
        blockRadius = radius;
    }

    public static void clear(BlockPos pos) {
        blocksToRender.remove(pos);
    }

    public static void clearAll() {
        blocksToRender.clear();
    }

    public static boolean isActive() {
        return isActive;
    }

    // Getter for rendering
    public static Map<BlockPos, Integer> getBlocksToRender() {
        return blocksToRender;
    }

    public static int getColorForLightLevel(int lightLevel) {
        return LightLevelModule.INSTANCE.getColorForLightLevel(lightLevel);
    }
    
    /**
     * Optimized version that reduces the frequency of expensive block scanning
     */
    public static void updateBlocksInRadiusOptimized() {
        blocksToRender.clear();
        
        if (MC.player == null || MC.level == null) return;

        BlockPos playerPos = MC.player.blockPosition();
        int radius = LightLevelModule.INSTANCE.getChunkScanRange(); // Use the configured range instead of hardcoded value
        int radiusSq = radius * radius;

        // Scan all blocks within radius using spherical distance
        for (int x = playerPos.getX() - radius; x <= playerPos.getX() + radius; x++) {
            for (int z = playerPos.getZ() - radius; z <= playerPos.getZ() + radius; z++) {
                // Quick 2D distance check to skip obvious cubes
                int dx2 = (x - playerPos.getX());
                int dz2 = (z - playerPos.getZ());
                if (dx2 * dx2 + dz2 * dz2 > radiusSq) continue;

                for (int y = playerPos.getY() - radius; y <= playerPos.getY() + radius; y++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    // Full 3D distance check
                    int dx = x - playerPos.getX();
                    int dy = y - playerPos.getY();
                    int dz = z - playerPos.getZ();
                    if (dx * dx + dy * dy + dz * dz > radiusSq) continue;

                    if (shouldRenderBlock(pos)) {
                        int lightLevel = MC.level.getBrightness(LightLayer.BLOCK, pos.above());
                        // Optimize by only adding to render if it's significantly different from threshold
                        blocksToRender.put(pos, lightLevel);
                    }
                }
            }
        }
    }
}
