package org.m9mx.cactus.glowberry.util;

import com.dwarslooper.cactus.client.systems.config.CactusSettings;
import com.dwarslooper.cactus.client.util.generic.ColorUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.m9mx.cactus.glowberry.feature.modules.DamageIndicator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tracks health drops of rendered living entities and renders floating
 * damage numbers ("5HP" or "2.5♥") with the vanilla font. Popups billboard
 * to the camera, float upward and fade out. They spawn at the entity's head,
 * pulled slightly towards the camera.
 *
 * The popup color is sampled every frame, so the color setting's built-in RGB
 * (rainbow) mode animates live. New hits on the same entity restart the popup
 * with the accumulated damage.
 */
public final class DamagePopupTracker {

    private static final class Popup {
        final double x;
        final double y;
        final double z;
        final String text;
        final int entityId;
        final boolean rainbowColor;
        final long bornAt;

        Popup(double x, double y, double z, String text, int entityId, boolean rainbowColor, long bornAt) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.text = text;
            this.entityId = entityId;
            this.rainbowColor = rainbowColor;
            this.bornAt = bornAt;
        }
    }

    private static final List<Popup> POPUPS = new ArrayList<>();
    private static final Map<Integer, Float> LAST_HEALTH = new HashMap<>();
    private static final Map<Integer, Long> LAST_SEEN = new HashMap<>();
    private static boolean registered = false;

    private DamagePopupTracker() {
    }

    public static void ensureRegistered() {
        if (registered) return;
        registered = true;
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(DamagePopupTracker::render);
    }

    /** Called from the entity render-state extraction for every rendered living entity. */
    public static void track(LivingEntity entity) {
        DamageIndicator module = DamageIndicator.INSTANCE;
        if (module == null || !module.active()) return;

        int id = entity.getId();
        float health = entity.getHealth();
        Float prev = LAST_HEALTH.put(id, health);
        LAST_SEEN.put(id, System.currentTimeMillis());
        if (prev == null || health >= prev) return;

        if (!module.showOnPlayers.get() && entity instanceof Player) return;

        long now = System.currentTimeMillis();

        float delta = prev - health;
        String text = formatDamage(module, delta);

        // Spawn position: on the entity's body, pulled slightly towards the
        // camera so the number never sits inside the body. The height fraction
        // is user-configurable (100 = top of hitbox, 0 = feet).
        Minecraft mc = Minecraft.getInstance();
        Vec3 entityPos = entity.position();
        Vec3 camPos = mc.gameRenderer.mainCamera().position();
        Vec3 toCam = new Vec3(camPos.x - entityPos.x, 0, camPos.z - entityPos.z);
        double horiz = toCam.horizontalDistance();
        if (horiz < 0.001) {
            toCam = new Vec3(0, 0, 1);
        } else {
            toCam = toCam.scale(1.0 / horiz);
        }
        double pull = entity.getBbWidth() * 0.55 + 0.25;
        double x = entityPos.x + toCam.x * pull;
        double z = entityPos.z + toCam.z * pull;
        double heightFrac = Math.max(0, module.spawnHeightPercent.get()) / 100.0;
        double y = entityPos.y + entity.getBbHeight() * heightFrac;

        long ttl = module.duration.get() * 50L;
        boolean rainbow = module.popupColor.get().usesRgb();

        // Every hit gets its own popup - no dropping. Consecutive popups on the
        // same entity stack upward so overlapping hits stay readable; once the
        // stack gets excessive (autoclicker + long duration) the oldest popup
        // for that entity is recycled instead of piling higher.
        long alive = POPUPS.stream().filter(p -> p.entityId == id).count();
        if (alive >= 8) {
            POPUPS.removeIf(p -> p.entityId == id);
            alive = 0;
        }
        double stack = alive * 0.22;
        POPUPS.add(new Popup(x, y + stack, z, text, id, rainbow, now));

        // Prune stale tracking entries so the maps cannot grow unbounded.
        if (LAST_HEALTH.size() > 512) {
            long cutoff = System.currentTimeMillis() - 30_000L;
            LAST_SEEN.values().removeIf(t -> t < cutoff);
            LAST_HEALTH.keySet().removeIf(k -> !LAST_SEEN.containsKey(k));
        }
    }

    private static String formatDamage(DamageIndicator module, float delta) {
        if (module.format.get() == DamageIndicator.DamageFormat.Hearts) {
            return compact(delta / 2.0f) + "♥";
        }
        return compact(delta) + "HP";
    }

    private static String compact(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /**
     * Samples the configured popup color fresh on every frame. With the color
     * picker's RGB (rainbow) mode enabled this returns the animated fading
     * color, so the popups cycle while they are on screen.
     */
    private static int sampleColor(DamageIndicator module, boolean rainbow) {
        if (rainbow) {
            int fadingSpeed = (Integer) CactusSettings.get().fadingSpeed.get();
            return ColorUtils.getFadingRgb(fadingSpeed);
        }
        return module.popupColor.get().value().getRGB();
    }

    private static void render(LevelRenderContext context) {
        if (POPUPS.isEmpty()) return;

        DamageIndicator module = DamageIndicator.INSTANCE;
        if (module == null || !module.active()) {
            POPUPS.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long ttl = module.duration.get() * 50L;

        Iterator<Popup> it = POPUPS.iterator();
        while (it.hasNext()) {
            if (now - it.next().bornAt > ttl) it.remove();
        }
        if (POPUPS.isEmpty()) return;

        boolean shadow = module.textShadow.get();
        boolean backplate = module.backplate.get();
        float plateAlpha = module.backplateOpacityPercent.get() / 100.0f;

        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = mc.gameRenderer.mainCamera().position();
        Quaternionf cameraRot = mc.gameRenderer.mainCamera().rotation();
        Font font = mc.font;

        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        SubmitNodeCollector collector = context.submitNodeCollector();

        for (Popup popup : POPUPS) {
            float age = (now - popup.bornAt) / 1000.0f;
            float rise = age * 0.8f;
            float fade = 1.0f - Math.max(0.0f, (age * 1000.0f - (ttl - 400)) / 400.0f);
            float alpha01 = Math.max(0.0f, Math.min(1.0f, fade));
            if (alpha01 <= 0.02f) continue;

            // Font pixels -> world units (0.02 matches the heart row scale).
            float p = 0.025f * (module.scale.get() / 100.0f);
            int rgb = sampleColor(module, popup.rainbowColor) & 0x00FFFFFF;
            int color = ((int) (alpha01 * 255.0f) << 24) | rgb;
            int backColor = ((int) (alpha01 * plateAlpha * 255.0f) << 24); // black plate

            poseStack.pushPose();
            poseStack.translate(popup.x, popup.y + rise, popup.z);
            poseStack.mulPose(cameraRot);
            // Vanilla name tag convention (see SubmitNodeCollection#submitNameTag):
            // after billboarding to the camera, font pixels map to world units with
            // scale(p, -p, p) - +x reads left-to-right and glyph tops extend toward
            // screen-up. Any other sign combination rotates the text.
            poseStack.scale(p, -p, p);
            float textX = -font.width(popup.text) / 2.0f;
            // Center the ~9px text line on the popup point: glyphs hang BELOW the
            // submit anchor (top of glyphs = anchor - ~0.03px), so shift the anchor
            // up by roughly half the line height (4.5px) in the font frame.
            float textY = -4.5f;

            if (backplate) {
                // Translucent dark plate behind the number so it reads on red
                // mobs and against the red hurt flash. Rendered just behind the
                // glyphs in the same text-background layer the nametag uses.
                drawBackplate(poseStack, collector, textX, textY, font.width(popup.text), backColor);
            }

            collector.submitText(
                    poseStack,
                    textX,
                    textY,
                    Component.literal(popup.text).getVisualOrderText(),
                    shadow,
                    Font.DisplayMode.SEE_THROUGH,
                    0xF000F0,
                    color,
                    0,
                    0
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /**
     * Submits a filled quad behind the text line, matching the vanilla nametag
     * background geometry: the text line is 9px tall with a 1px padding on
     * every side, and the quad sits slightly behind the glyph plane (negative
     * font z points away from the camera) so it cannot z-fight with the
     * glyphs. Vertex order mirrors vanilla BakedGlyph effect quads (left
     * bottom -> right bottom -> right top -> left top); the font background
     * pass draws before the text pass, so the plate lands under the glyphs.
     */
    private static void drawBackplate(PoseStack poseStack, SubmitNodeCollector collector, float textX, float textY, float textWidth, int backColor) {
        float pad = 1.0f;
        float x0 = textX - pad;
        float x1 = textX + textWidth + pad;
        float yTop = textY - pad;
        float yBottom = textY + 9.0f + pad;
        float z = -0.03f;

        RenderType renderType = RenderTypes.textBackgroundSeeThrough();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            buffer.addVertex(pose, x0, yBottom, z).setColor(backColor);
            buffer.addVertex(pose, x1, yBottom, z).setColor(backColor);
            buffer.addVertex(pose, x1, yTop, z).setColor(backColor);
            buffer.addVertex(pose, x0, yTop, z).setColor(backColor);
        });
    }
}
