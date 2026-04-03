package org.m9mx.cactus.glowberry.util.waypointsv2.render;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.m9mx.cactus.glowberry.feature.modules.WaypointsV2Module;
import org.m9mx.cactus.glowberry.util.waypointsv2.storage.WaypointsV2FileManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WaypointsV2Renderer {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final List<ProjectedData> PROJECTED_DATA = new ArrayList<>();

    private static final int ICON_BOX_SIZE = 18;
    private static final int ICON_SIZE = 16;
    private static final int ICON_TO_NAME_Y_OFFSET = 12;
    private static final int NAME_TO_DISTANCE_Y_OFFSET = 10;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int ICON_BACKGROUND_COLOR = 0x90000000;

    public static void init() {
        WorldRenderEvents.AFTER_ENTITIES.register(WaypointsV2Renderer::captureProjectedWaypoints);
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> renderHud(graphics));
    }

    private static void captureProjectedWaypoints(WorldRenderContext context) {
        PROJECTED_DATA.clear();

        WaypointsV2Module module = WaypointsV2Module.getInstance();
        if (module == null || !module.active() || MC.player == null || MC.level == null) {
            return;
        }

        Matrix4f modelViewMatrix = extractModelViewMatrix(context);
        Matrix4f projectionMatrix = extractProjectionMatrix(context);
        Matrix4f viewProjection = projectionMatrix.mul(modelViewMatrix);

        Vec3 cameraPos = MC.gameRenderer.getMainCamera().position();

        for (WaypointsV2FileManager.WaypointRecord waypoint : module.getWaypoints()) {
            if (!shouldRenderWaypoint(module, waypoint, cameraPos)) {
                continue;
            }

            Vector4f pos = new Vector4f(waypoint.x + 0.5f, waypoint.y + 0.5f, waypoint.z + 0.5f, 1.0f);
            pos.mul(viewProjection);

            if (pos.w() <= 0.0f) {
                continue;
            }

            float ndcX = pos.x() / pos.w();
            float ndcY = pos.y() / pos.w();
            if (Math.abs(ndcX) > 1.0f || Math.abs(ndcY) > 1.0f) {
                continue;
            }

            int screenX = (int) ((ndcX * 0.5f + 0.5f) * MC.getWindow().getGuiScaledWidth());
            int screenY = (int) ((1.0f - (ndcY * 0.5f + 0.5f)) * MC.getWindow().getGuiScaledHeight());
            int distanceBlocks = (int) Math.round(Math.sqrt(cameraPos.distanceToSqr(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5)));

            PROJECTED_DATA.add(new ProjectedData(
                    screenX,
                    screenY,
                    sanitizeName(waypoint.name),
                    formatDistance(distanceBlocks),
                    normalizeDisplayMode(waypoint.displayMode),
                    resolveItemStack(waypoint.iconItemId)
            ));
        }
    }

    private static boolean shouldRenderWaypoint(WaypointsV2Module module, WaypointsV2FileManager.WaypointRecord waypoint, Vec3 cameraPos) {
        if (waypoint == null || !waypoint.enabled || !"LOCATION".equalsIgnoreCase(waypoint.type)) {
            return false;
        }

        if (MC.level == null || MC.player == null) {
            return false;
        }

        if (module.maxRenderDistance.get() > 0) {
            double maxDistanceSq = Math.pow(module.maxRenderDistance.get(), 2);
            double distanceSq = cameraPos.distanceToSqr(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5);
            if (distanceSq > maxDistanceSq) {
                return false;
            }
        }

        if (WaypointsV2Module.getInstance().showThroughWalls.get()) {
            return true;
        }

        Vec3 target = new Vec3(waypoint.x + 0.5, waypoint.y + 0.5, waypoint.z + 0.5);
        HitResult hitResult = MC.level.clip(new ClipContext(cameraPos, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, MC.player));
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return true;
        }

        BlockPos hitPos = BlockPos.containing(hitResult.getLocation());
        BlockPos waypointPos = BlockPos.containing(target);
        return hitPos.equals(waypointPos);
    }

    private static void renderHud(GuiGraphics graphics) {
        if (PROJECTED_DATA.isEmpty()) {
            return;
        }

        for (ProjectedData data : PROJECTED_DATA) {
            drawWaypointSticker(graphics, data);
        }
    }

    private static void drawWaypointSticker(GuiGraphics graphics, ProjectedData data) {
        int centerX = data.screenX;
        int y = data.screenY;
        int nameY = y + ICON_TO_NAME_Y_OFFSET;
        int distanceY = nameY + NAME_TO_DISTANCE_Y_OFFSET;

        if ("TEXT".equals(data.displayMode)) {
            drawCenteredText(graphics, data.name, centerX, y);
            drawCenteredText(graphics, data.distanceText, centerX, y + NAME_TO_DISTANCE_Y_OFFSET);
            return;
        }

        int iconLeft = centerX - ICON_BOX_SIZE / 2;
        int iconTop = y - ICON_BOX_SIZE / 2;
        graphics.fill(iconLeft, iconTop, iconLeft + ICON_BOX_SIZE, iconTop + ICON_BOX_SIZE, ICON_BACKGROUND_COLOR);
        graphics.renderFakeItem(data.iconStack, centerX - (ICON_SIZE / 2), y - (ICON_SIZE / 2));

        if ("TEXT_ICON".equals(data.displayMode)) {
            drawCenteredText(graphics, data.name, centerX, nameY);
        }
        drawCenteredText(graphics, data.distanceText, centerX, distanceY);
    }

    private static void drawCenteredText(GuiGraphics graphics, String text, int centerX, int y) {
        String safe = text == null ? "" : text;
        int fontWidth = MC.font.width(safe);
        int textX = centerX - (fontWidth / 2);
        graphics.drawString(MC.font, safe, textX, y, TEXT_COLOR, true);
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isBlank()) {
            return "(Unnamed)";
        }
        return name;
    }

    private static String formatDistance(int distanceBlocks) {
        return "[" + Math.max(0, distanceBlocks) + " b]";
    }

    private static String normalizeDisplayMode(String displayMode) {
        String mode = displayMode == null ? "TEXT_ICON" : displayMode.toUpperCase(Locale.ROOT);
        if ("ICON".equals(mode) || "TEXT".equals(mode) || "TEXT_ICON".equals(mode)) {
            return mode;
        }
        return "TEXT_ICON";
    }

    private static ItemStack resolveItemStack(String iconId) {
        try {
            Identifier id = parseIdentifier(iconId);
            Item item = BuiltInRegistries.ITEM.get(id)
                    .map(net.minecraft.core.Holder.Reference::value)
                    .orElse(Items.COMPASS);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception ignored) {
            // Fall back to a valid icon when stored id is invalid.
        }
        return new ItemStack(Items.COMPASS);
    }

    private static Identifier parseIdentifier(String idText) {
        String raw = (idText == null || idText.isBlank()) ? "minecraft:compass" : idText.trim();
        String normalized = raw.contains(":") ? raw : "minecraft:" + raw;

        Identifier fromOf = tryIdentifierOf(normalized);
        if (fromOf != null) {
            return fromOf;
        }

        Identifier parsed = Identifier.tryParse(normalized);
        if (parsed != null) {
            return parsed;
        }

        int separator = normalized.indexOf(':');
        if (separator > 0 && separator < normalized.length() - 1) {
            return Identifier.fromNamespaceAndPath(normalized.substring(0, separator), normalized.substring(separator + 1));
        }
        return Identifier.fromNamespaceAndPath("minecraft", raw);
    }

    private static Identifier tryIdentifierOf(String idText) {
        try {
            Object value = Identifier.class.getMethod("of", String.class).invoke(null, idText);
            if (value instanceof Identifier identifier) {
                return identifier;
            }
        } catch (Exception ignored) {
            // Method may not exist in all mappings; fallback to tryParse below.
        }
        return null;
    }

    private static Matrix4f extractProjectionMatrix(WorldRenderContext context) {
        try {
            Object projection = context.getClass().getMethod("projectionMatrix").invoke(context);
            if (projection instanceof Matrix4f matrix4f) {
                return new Matrix4f(matrix4f);
            }
        } catch (Exception ignored) {
            // Continue to fallback options.
        }

        try {
            Object position = context.getClass().getMethod("positionMatrix").invoke(context);
            if (position instanceof Matrix4f matrix4f) {
                return new Matrix4f(matrix4f);
            }
        } catch (Exception ignored) {
            // Final fallback below.
        }

        try {
            Class<?> renderSystem = Class.forName("com.mojang.blaze3d.systems.RenderSystem");
            Object projection = renderSystem.getMethod("getProjectionMatrix").invoke(null);
            if (projection instanceof Matrix4f matrix4f) {
                return new Matrix4f(matrix4f);
            }
        } catch (Exception ignored) {
            // Final fallback below.
        }

        return new Matrix4f().identity();
    }

    private static Matrix4f extractModelViewMatrix(WorldRenderContext context) {
        try {
            Object matrixStack = context.getClass().getMethod("matrixStack").invoke(context);
            Object lastPose = matrixStack.getClass().getMethod("last").invoke(matrixStack);
            Object pose = lastPose.getClass().getMethod("pose").invoke(lastPose);
            if (pose instanceof Matrix4f matrix4f) {
                return new Matrix4f(matrix4f);
            }
        } catch (Exception ignored) {
            // Fall back to context.matrices() API.
        }

        return new Matrix4f(context.matrices().last().pose());
    }

    private record ProjectedData(
            int screenX,
            int screenY,
            String name,
            String distanceText,
            String displayMode,
            ItemStack iconStack
    ) {
    }
}