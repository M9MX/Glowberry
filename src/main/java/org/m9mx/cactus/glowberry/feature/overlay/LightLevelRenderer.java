package org.m9mx.cactus.glowberry.feature.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
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
    
    // Small offsets above the block's top surface to avoid z-fighting without
    // visibly hovering above the surface. The number must stay ABOVE the block quad
    // so it isn't washed out by the translucent block overlay in BOTH mode.
    private static final float OVERLAY_OFFSET_BLOCK = 0.02f;
    private static final float OVERLAY_OFFSET_NUMBER = 0.04f;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Register to render after entities
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
            if (LightLevelOverlayHandler.isActive() && MC.player != null && MC.level != null) {
                renderLightLevelOverlays(context);
            }
        });
    }

    private static void renderLightLevelOverlays(LevelRenderContext context) {
        Map<BlockPos, LightLevelOverlayHandler.BlockOverlayInfo> blocksToRender = LightLevelOverlayHandler.getBlocksToRender();
        if (blocksToRender.isEmpty()) return;

        PoseStack poseStack = context.poseStack();
        Vec3 camPos = MC.gameRenderer.mainCamera().position();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        // 26.2 removed LevelRenderContext.bufferSource() in favor of the staged
        // SubmitNodeCollector renderer; custom geometry goes through submitCustomGeometry.
        SubmitNodeCollector collector = context.submitNodeCollector();

        // Render each block with appropriate overlay type
        LightLevelModule.OverlayType overlayType = LightLevelModule.INSTANCE.getOverlayType();
        
        if (overlayType == LightLevelModule.OverlayType.BLOCK || overlayType == LightLevelModule.OverlayType.BOTH) {
            // Render block overlays (colored hitboxes)
            collector.submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, blockOverlayConsumer) -> {
                for (Map.Entry<BlockPos, LightLevelOverlayHandler.BlockOverlayInfo> entry : blocksToRender.entrySet()) {
                    BlockPos pos = entry.getKey();
                    LightLevelOverlayHandler.BlockOverlayInfo info = entry.getValue();
                    int color = LightLevelModule.INSTANCE.getColorForLightLevel(info.lightLevel());
                    renderBlockOverlay(pose, blockOverlayConsumer, pos, info.topY(), color);
                }
            });
        }
        
        if (overlayType == LightLevelModule.OverlayType.NUMBER || overlayType == LightLevelModule.OverlayType.BOTH) {
            // Render number overlays (textured numbers)
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(NUMBERS_TEXTURE), (pose, numberOverlayConsumer) -> {
                for (Map.Entry<BlockPos, LightLevelOverlayHandler.BlockOverlayInfo> entry : blocksToRender.entrySet()) {
                    BlockPos pos = entry.getKey();
                    LightLevelOverlayHandler.BlockOverlayInfo info = entry.getValue();
                    int lightLevel = info.lightLevel();
                    int color = LightLevelModule.INSTANCE.getColorForLightLevel(lightLevel);
                    renderNumberOverlay(pose, numberOverlayConsumer, pos, info.topY(), lightLevel, color);
                }
            });
        }

        poseStack.popPose();
    }

    private static void renderBlockOverlay(PoseStack.Pose pose, VertexConsumer vertexConsumer, BlockPos pos, float topY, int color) {
        // Precompute color values to avoid repeated bit shifting
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float alpha = 0.3f; // More transparent for block overlay

        // 26.2: the pose captured at submit time already includes the camera
        // translation, so copy it and apply the block offset on top.
        Matrix4f positionMatrix = new Matrix4f(pose.pose());
        positionMatrix.translate(pos.getX(), pos.getY(), pos.getZ());

        // Draw solid colored quad on top of the block's actual surface (topY), so it
        // stays glued to the surface instead of hovering at a fixed height above the
        // block base (which made it float over snow layers / slabs).
        float minX = 0.0f, minY = topY + OVERLAY_OFFSET_BLOCK, minZ = 0.0f;
        float maxX = 1.0f, maxZ = 1.0f;
        
        // Add vertices for the top face following counter-clockwise order to ensure visibility from above
        vertexConsumer.addVertex(positionMatrix, minX, minY, minZ).setColor(r, g, b, alpha);
        vertexConsumer.addVertex(positionMatrix, minX, minY, maxZ).setColor(r, g, b, alpha);
        vertexConsumer.addVertex(positionMatrix, maxX, minY, maxZ).setColor(r, g, b, alpha);
        vertexConsumer.addVertex(positionMatrix, maxX, minY, minZ).setColor(r, g, b, alpha);
    }

    private static void renderNumberOverlay(PoseStack.Pose pose, VertexConsumer vertexConsumer, BlockPos pos, float topY, int lightLevel, int color) {
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

        // 26.2: the pose captured at submit time already includes the camera
        // translation; copy it and apply block offset + surface height on top.
        // The number is glued to the block's actual top surface (topY) so it sits on
        // top of snow layers / slabs instead of floating above the block base.
        Matrix4f positionMatrix = new Matrix4f(pose.pose());
        positionMatrix.translate(pos.getX(), pos.getY() + topY + OVERLAY_OFFSET_NUMBER, pos.getZ());

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
    }
}