package org.m9mx.cactus.glowberry.feature.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.m9mx.cactus.glowberry.feature.modules.LightLevelModule;

import java.util.Map;

/**
 * Renders light level overlays on blocks without overlaylib dependency.
 * Uses Fabric's WorldRenderEvents to render textured quads on block tops.
 */
public class LightLevelRenderer {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Identifier NUMBERS_TEXTURE = Identifier.fromNamespaceAndPath("glowberry", "textures/numbers.png");
    private static boolean initialized = false;
    
    // Cache commonly used values to avoid repeated property access
    private static final float OVERLAY_HEIGHT_OFFSET_BLOCK = 1.02f;
    private static final float OVERLAY_HEIGHT_OFFSET_NUMBER = 1.04f;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Register to render after entities
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (LightLevelOverlayHandler.isActive() && MC.player != null && MC.level != null) {
                renderLightLevelOverlays(context);
            }
        });
    }

    private static void renderLightLevelOverlays(WorldRenderContext context) {
        Map<BlockPos, Integer> blocksToRender = LightLevelOverlayHandler.getBlocksToRender();
        if (blocksToRender.isEmpty()) return;

        PoseStack poseStack = context.matrices();
        Vec3 camPos = MC.gameRenderer.getMainCamera().position();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // Render each block with appropriate overlay type
        LightLevelModule.OverlayType overlayType = LightLevelModule.INSTANCE.getOverlayType();
        
        if (overlayType == LightLevelModule.OverlayType.BLOCK || overlayType == LightLevelModule.OverlayType.BOTH) {
            // Render block overlays (colored hitboxes)
            VertexConsumer blockOverlayConsumer = context.consumers().getBuffer(RenderTypes.debugFilledBox());
            
            for (Map.Entry<BlockPos, Integer> entry : blocksToRender.entrySet()) {
                BlockPos pos = entry.getKey();
                int lightLevel = entry.getValue();
                int color = LightLevelModule.INSTANCE.getColorForLightLevel(lightLevel);
                
                renderBlockOverlay(poseStack, blockOverlayConsumer, pos, color);
            }
        }
        
        if (overlayType == LightLevelModule.OverlayType.NUMBER || overlayType == LightLevelModule.OverlayType.BOTH) {
            // Render number overlays (textured numbers)
            VertexConsumer numberOverlayConsumer = context.consumers().getBuffer(RenderTypes.entityTranslucent(NUMBERS_TEXTURE));
            
            for (Map.Entry<BlockPos, Integer> entry : blocksToRender.entrySet()) {
                BlockPos pos = entry.getKey();
                int lightLevel = entry.getValue();
                int color = LightLevelModule.INSTANCE.getColorForLightLevel(lightLevel);
                
                renderNumberOverlay(poseStack, numberOverlayConsumer, pos, lightLevel, color);
            }
        }

        poseStack.popPose();
    }

    private static void renderBlockOverlay(PoseStack poseStack, VertexConsumer vertexConsumer, BlockPos pos, int color) {
        // Precompute color values to avoid repeated bit shifting
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float alpha = 0.3f; // More transparent for block overlay

        poseStack.pushPose();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

        Matrix4f positionMatrix = poseStack.last().pose();
        
        // Draw solid colored quad on top of block (following trajectory preview pattern)
        // Top face of the block (Y+) - moved higher to avoid being inside the block
        // Using constants for consistent positioning
        float minX = 0.0f, minY = OVERLAY_HEIGHT_OFFSET_BLOCK, minZ = 0.0f;
        float maxX = 1.0f, maxZ = 1.0f;
        
        // Add vertices for the top face following counter-clockwise order to ensure visibility from above
        vertexConsumer.addVertex(positionMatrix, minX, minY, minZ).setColor(r, g, b, alpha);
        vertexConsumer.addVertex(positionMatrix, minX, minY, maxZ).setColor(r, g, b, alpha);
        vertexConsumer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(r, g, b, alpha);
        vertexConsumer.addVertex(positionMatrix, maxX, minY, minZ).setColor(r, g, b, alpha);

        poseStack.popPose();
    }

    private static void renderNumberOverlay(PoseStack poseStack, VertexConsumer vertexConsumer, BlockPos pos, int lightLevel, int color) {
        // Precompute UV coordinates based on light level (0-15)
        // Each number takes 1/16 of the texture width
        float uSize = 1.0f / 16.0f;
        float uStart = lightLevel * uSize;
        float uEnd = (lightLevel + 1) * uSize;
        float vStart = 0.0f;
        float vEnd = 1.0f;

        // Precompute color values to avoid repeated bit shifting
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float alpha = 1.0f; // Increased to make numbers more visible

        poseStack.pushPose();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

        Matrix4f positionMatrix = poseStack.last().pose();
        PoseStack.Pose pose = poseStack.last();
        
        // Apply consistent height offset for number overlay
        positionMatrix.translate(0f, OVERLAY_HEIGHT_OFFSET_NUMBER, 0f);

        // Draw textured quad on top of block facing up
        // Normal pointing up (0, 1, 0)
        vertexConsumer.addVertex(positionMatrix, 0, 0, 0)
                .setColor(r, g, b, alpha)
                .setUv(uStart, vStart)
                .setUv2(0xF000F0, 0xF000F0)  // Add light map UV coordinates
                .setOverlay(OverlayTexture.NO_OVERLAY)  // Add overlay coordinates
                .setNormal(pose, 0f, 1f, 0f);
        
        vertexConsumer.addVertex(positionMatrix, 1, 0, 0)
                .setColor(r, g, b, alpha)
                .setUv(uEnd, vStart)
                .setUv2(0xF000F0, 0xF000F0)  // Add light map UV coordinates
                .setOverlay(OverlayTexture.NO_OVERLAY)  // Add overlay coordinates
                .setNormal(pose, 0f, 1f, 0f);
        
        vertexConsumer.addVertex(positionMatrix, 1, 0, 1)
                .setColor(r, g, b, alpha)
                .setUv(uEnd, vEnd)
                .setUv2(0xF000F0, 0xF000F0)  // Add light map UV coordinates
                .setOverlay(OverlayTexture.NO_OVERLAY)  // Add overlay coordinates
                .setNormal(pose, 0f, 1f, 0f);
        
        vertexConsumer.addVertex(positionMatrix, 0, 0, 1)
                .setColor(r, g, b, alpha)
                .setUv(uStart, vEnd)
                .setUv2(0xF000F0, 0xF000F0)  // Add light map UV coordinates
                .setOverlay(OverlayTexture.NO_OVERLAY)  // Add overlay coordinates
                .setNormal(pose, 0f, 1f, 0f);

        poseStack.popPose();
    }
}