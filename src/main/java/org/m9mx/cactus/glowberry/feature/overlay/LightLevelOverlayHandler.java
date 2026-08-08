package org.m9mx.cactus.glowberry.feature.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    /**
     * Info needed to render one overlay: the light level to display and the height of the
     * top-most point of the block's collision surface (relative to the block base, e.g.
     * 1.0 for a full block, 0.125 * layers for snow, 0.5 for a bottom slab). Rendering is
     * glued to that surface so overlays never float above or sink into partial blocks.
     */
    public record BlockOverlayInfo(int lightLevel, float topY) {}

    // Store blocks to render: BlockPos -> light level + top surface height
    private static final Map<BlockPos, BlockOverlayInfo> blocksToRender = new HashMap<>();

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
                        blocksToRender.put(pos, new BlockOverlayInfo(lightLevel, getTopSurfaceHeight(pos)));
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

        // The block itself must have a collision surface an entity could stand on:
        // full blocks, snow layers, slabs, farmland, fences, pressure plates, ...
        if (!hasWalkableSurface(pos)) {
            return false;
        }

        // If the block above also has a walkable surface, it is the real surface of the
        // column; only the top-most standable block should get an overlay.
        if (hasWalkableSurface(pos.above())) {
            return false;
        }

        int lightLevel = MC.level.getBrightness(LightLayer.BLOCK, pos.above());
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

    /**
     * Whether an entity could stand on the top of this block: the block has a non-empty
     * collision shape. Works for full blocks as well as partial blocks (snow layers, slabs).
     */
    private static boolean hasWalkableSurface(BlockPos pos) {
        if (MC.level == null) return false;
        BlockState state = MC.level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(MC.level, pos);
        return !shape.isEmpty();
    }

    /**
     * Height of the top-most point of the block's collision shape, relative to the block
     * base. 1.0 for full blocks, 0.125 * layers for snow layers, 0.5 for bottom slabs,
     * 0.9375 for farmland, etc. Falls back to 1.0 for blocks without a shape.
     */
    private static float getTopSurfaceHeight(BlockPos pos) {
        if (MC.level == null) return 1.0f;
        VoxelShape shape = MC.level.getBlockState(pos).getCollisionShape(MC.level, pos);
        if (shape.isEmpty()) return 1.0f;
        return (float) shape.max(Direction.Axis.Y);
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
    public static Map<BlockPos, BlockOverlayInfo> getBlocksToRender() {
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
                        blocksToRender.put(pos, new BlockOverlayInfo(lightLevel, getTopSurfaceHeight(pos)));
                    }
                }
            }
        }
    }
}
