package org.m9mx.cactus.glowberry.feature.overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class BlockRadiusOverlay {
    private int blockRadius = 128;
    private static final Minecraft MC = Minecraft.getInstance();

    public void setBlockRadius(int radius) {
        this.blockRadius = radius;
    }

    public int getBlockRadius() {
        return blockRadius;
    }

    public boolean isBlockInRadius(BlockPos blockPos) {
        if (MC.player == null) return false;
        
        BlockPos playerPos = MC.player.blockPosition();
        double dx = blockPos.getX() - playerPos.getX();
        double dy = blockPos.getY() - playerPos.getY();
        double dz = blockPos.getZ() - playerPos.getZ();
        
        double distSquared = dx * dx + dy * dy + dz * dz;
        double radiusSquared = blockRadius * blockRadius;
        
        return distSquared <= radiusSquared;
    }
}
