/*
 * Adapted from Moss Addon by Datenflieger
 * Original mod: https://github.com/Datenflieger/MossAddon
 */
package org.m9mx.cactus.glowberry.mixin.Modules.moss;

import java.util.Locale;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

import org.m9mx.cactus.glowberry.feature.modules.DamageIndicator;
import org.m9mx.cactus.glowberry.feature.modules.HealthIndicators;
import org.m9mx.cactus.glowberry.util.HealthRenderStateExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class DamageIndicatorLivingEntityRendererMixin {

    private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.withDefaultNamespace("hud/heart/half");
    private static final Identifier HEART_ABS_FULL = Identifier.withDefaultNamespace("hud/heart/absorbing_full");
    private static final Identifier HEART_ABS_HALF = Identifier.withDefaultNamespace("hud/heart/absorbing_half");

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    private void glowberry$renderHealthLabel(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState, CallbackInfo ci) {
        HealthIndicators module = HealthIndicators.INSTANCE;
        if (module == null || !module.active()) return;
        if (!(state instanceof HealthRenderStateExtension ext)) return;

        float health = ext.glowberry$getHealth();
        float maxHealth = ext.glowberry$getMaxHealth();
        float absorption = module.includeAbsorption.get() ? ext.glowberry$getAbsorption() : 0f;
        if (maxHealth <= 0.0f) return;

        // Anchor above the hitbox. state.nameTagAttachment is only populated for
        // entities that actually show a vanilla name tag (null for regular mobs),
        // so derive the position from the bounding box instead - vanilla fills
        // boundingBoxWidth/Height/eyeHeight for every rendered entity.
        //
        // submitNameTag lifts the anchor by +0.5 internally (that is why vanilla
        // nametags float half a block above the head), so the text path passes the
        // plain hitbox top while the hearts path (custom geometry, no submitNameTag)
        // adds the +0.5 itself.
        float hitboxTop = state.boundingBoxHeight;

        String text;
        switch (module.displayType.get()) {
            case Hearts -> {
                renderTexturedHearts(poseStack, submitNodeCollector, new Vec3(0.0, hitboxTop + 0.5, 0.0), state, cameraState, health, absorption, maxHealth);
                return;
            }
            case Amount -> text = formatAmount(health, maxHealth, absorption);
            case Percent -> text = formatPercent(health, maxHealth);
            default -> {
                return;
            }
        }
        renderTextLabel(poseStack, submitNodeCollector, new Vec3(0.0, hitboxTop, 0.0), state, cameraState, text);
    }

    // ==================================================================================
    // Hurt-flash color override (Damage Indicator module)
    //
    // While an entity flashes red from damage, LivingEntityRenderer.getOverlayCoords
    // selects the RED row of the overlay texture and the model tint is
    // ARGB.multiply(invisibleFlag, getModelTint(state)) = -1. The two wraps below
    // cooperate: the first one samples the plain WHITE overlay row instead and
    // remembers that this submit wants a recolor; the second replaces the model
    // tint with the user's color. The color is sampled fresh every frame, so the
    // color picker's RGB (rainbow) mode animates live on the flashing entity.
    // ==================================================================================

    @Unique
    private boolean glowberry$recolorNextTint;

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getOverlayCoords(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)I")
    )
    private int glowberry$overrideHurtFlashOverlay(LivingEntityRenderState state, float gweight, Operation<Integer> original) {
        DamageIndicator module = DamageIndicator.INSTANCE;
        if (module == null || !module.active() || !module.overrideHurtFlash.get()) {
            return original.call(state, gweight);
        }
        // Only recolor entities that actually show the vanilla red hurt flash.
        if (!state.hasRedOverlay || state instanceof AvatarRenderState) {
            return original.call(state, gweight);
        }
        // Select the WHITE overlay row (v(false) = row 10; v(true) is the
        // RED row, which multiplied with a blue tint into purple) and let
        // the tint wrap below carry the color.
        this.glowberry$recolorNextTint = true;
        return OverlayTexture.pack(OverlayTexture.u(gweight), OverlayTexture.v(false));
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;multiply(II)I")
    )
    private int glowberry$overrideHurtFlashTint(int a, int b, Operation<Integer> original) {
        if (this.glowberry$recolorNextTint) {
            this.glowberry$recolorNextTint = false;
            DamageIndicator module = DamageIndicator.INSTANCE;
            if (module != null && module.active() && module.overrideHurtFlash.get()) {
                int tint = module.hurtFlashColor.get().color() | 0xFF000000;
                // Keep vanilla's invisibility dimming if it was applied.
                return a == -1 ? tint : ARGB.multiply(a, tint);
            }
        }
        return original.call(a, b);
    }

    @Unique
    private void renderTextLabel(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, Vec3 labelPos, LivingEntityRenderState state, CameraRenderState cameraState, String text) {
        Component healthText = Component.literal(text).withStyle(ChatFormatting.RED);

        // submitNameTag lifts the anchor +0.5 (nametag height). When the entity
        // also has a real nametag, stack our label one line above it, exactly the
        // way vanilla stacks the below-name score text over the nametag.
        float lineOffset = state.nameTag != null ? 9F * 1.15F * 0.025F : 0.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, lineOffset, 0.0F);
        submitNodeCollector.submitNameTag(poseStack, labelPos, 0, healthText, !state.isDiscrete, state.lightCoords, cameraState);
        poseStack.popPose();
    }

    @Unique
    private void renderTexturedHearts(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            Vec3 labelPos,
            LivingEntityRenderState state,
            CameraRenderState cameraState,
            float health,
            float absorption,
            float maxHealth
    ) {
        HealthIndicators module = HealthIndicators.INSTANCE;
        final int heartsPerRow = 10;
        final float pixelSize = 0.02f;
        final float heartSize = 9.0f * pixelSize;
        final float spacing = 8.0f * pixelSize;
        final float rowSpacing = 10.0f * pixelSize;
        final float yOffset = ((Integer) module.heartsYOffsetPx.get()) * pixelSize;

        int normalHearts = Math.max(1, (int) Math.ceil(maxHealth / 2.0f));
        int absorptionHearts = (int) Math.ceil(absorption / 2.0f);
        int heartsToDisplay = normalHearts + absorptionHearts;

        int redFull = Math.min(normalHearts, (int) (health / 2.0f));
        boolean redHalf = (health % 2.0f) >= 1.0f && redFull < normalHearts;
        int yellowFull = (int) (absorption / 2.0f);
        boolean yellowHalf = (absorption % 2.0f) >= 1.0f;

        // Default: rows of 10 stack upward without a cap.
        //
        // Compact: only one row is drawn - the row that holds the end of the
        // current health (so half hearts at the boundary stay visible) - with a
        // "5x" prefix rendered with the vanilla font on the same line as the
        // hearts. N counts down row by row as the entity loses health.
        boolean compact = module.compactRows.get();
        int firstHeart = 0;
        int heartsInRow = heartsToDisplay;
        int healthRows = 1;

        if (compact) {
            float currentHearts = (health + absorption) / 2.0f;
            healthRows = Math.max(1, (int) Math.ceil(currentHearts / heartsPerRow - 1.0E-4f));
            firstHeart = (healthRows - 1) * heartsPerRow;
            heartsInRow = Math.max(1, Math.min(heartsPerRow, heartsToDisplay - firstHeart));
        }

        Minecraft mc = Minecraft.getInstance();
        var atlas = mc.getAtlasManager();

        // Hearts use the vanilla HUD sprite IDs (hud/heart/container, /full,
        // /half, /absorbing_*), so any resource pack that overrides the vanilla
        // heart icons automatically applies here too.
        TextureAtlasSprite containerSprite = atlas.get(new SpriteId(Sheets.GUI_SHEET, HEART_CONTAINER));
        TextureAtlasSprite fullSprite = atlas.get(new SpriteId(Sheets.GUI_SHEET, HEART_FULL));
        TextureAtlasSprite halfSprite = atlas.get(new SpriteId(Sheets.GUI_SHEET, HEART_HALF));
        TextureAtlasSprite absFullSprite = atlas.get(new SpriteId(Sheets.GUI_SHEET, HEART_ABS_FULL));
        TextureAtlasSprite absHalfSprite = atlas.get(new SpriteId(Sheets.GUI_SHEET, HEART_ABS_HALF));

        // Anchored at the nametag attachment point so the bottom row always sits
        // just above the head regardless of entity size; rows stack upward.
        poseStack.pushPose();
        poseStack.translate(labelPos.x, labelPos.y + yOffset, labelPos.z);
        poseStack.mulPose(cameraState.orientation);
        poseStack.scale(1.0f, -1.0f, 1.0f);
        RenderType renderType = RenderTypes.entityCutout(Sheets.GUI_SHEET, false);
        int light = state.lightCoords;

        Font font = mc.font;

        // In compact mode the "5x" prefix is rendered with the vanilla font in
        // its own submit, but with a pose matching the heart row exactly, so
        // prefix + hearts form one continuous centered line.
        final boolean showPrefix = compact && healthRows > 1;
        final String prefixText = showPrefix ? healthRows + "x" : null;
        final int prefixColor = module.compactTextColor.get().color();
        // Prefix text height: font renders at ~9px per line, same as a heart.
        final float prefixWidth = prefixText != null ? font.width(prefixText) * pixelSize : 0.0f;
        final float prefixGap = prefixText != null ? spacing * 0.35f : 0.0f;
        final float labelWidth = prefixWidth + prefixGap;

        final float rowWidth = (heartsInRow - 1) * spacing + heartSize + labelWidth;

        final int fFirstHeart = firstHeart;
        final int fHeartsInRow = heartsInRow;
        final int fHeartsToDisplay = heartsToDisplay;
        final int fNormalHearts = normalHearts;
        final int fRedFull = redFull;
        final boolean fRedHalf = redHalf;
        final int fYellowFull = yellowFull;
        final boolean fYellowHalf = yellowHalf;
        final int fHealthRows = healthRows;
        final int fLight = light;

        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (int i = 0; i < fHeartsInRow; i++) {
                int heartIndex = fFirstHeart + i;
                float x;
                float y;
                if (showPrefix) {
                    x = -rowWidth / 2.0f + labelWidth + i * spacing;
                    y = 0.0f;
                } else {
                    int row = heartIndex / heartsPerRow;
                    int col = heartIndex % heartsPerRow;
                    int heartsInThisRow = Math.min(heartsPerRow, fHeartsToDisplay - row * heartsPerRow);
                    float rowW = (heartsInThisRow - 1) * spacing + heartSize;
                    x = -rowW / 2.0f + col * spacing;
                    // Rows stack upward: local +Y points down on screen after the
                    // camera flip, so higher rows use negative offsets.
                    y = -row * rowSpacing;
                }

                drawHeartQuad(buffer, pose, x, y, heartSize, containerSprite, fLight);
                if (heartIndex < fRedFull) {
                    drawHeartQuad(buffer, pose, x, y, heartSize, fullSprite, fLight);
                } else if (fRedHalf && heartIndex == fRedFull) {
                    drawHeartQuad(buffer, pose, x, y, heartSize, halfSprite, fLight);
                }
                if (heartIndex >= fNormalHearts) {
                    int yellowIndex = heartIndex - fNormalHearts;
                    if (yellowIndex < fYellowFull) {
                        drawHeartQuad(buffer, pose, x, y, heartSize, absFullSprite, fLight);
                    } else if (fYellowHalf && yellowIndex == fYellowFull) {
                        drawHeartQuad(buffer, pose, x, y, heartSize, absHalfSprite, fLight);
                    }
                }
            }
        });

        // Vanilla-font "Nx" prefix, submitted with the same pose as the heart row
        // so it lands exactly on that line, right before the first heart.
        if (prefixText != null) {
            poseStack.pushPose();
            // The heart frame is already camera-facing AND in GUI convention
            // (+x reads right, +y points screen-down) thanks to the earlier
            // scale(1, -1, 1), exactly like vanilla's name tag frame - so the
            // font pixels just need a positive uniform scale, no flips.
            poseStack.scale(pixelSize, pixelSize, pixelSize);
            // Left edge of the prefix: width/2 left of the row center.
            float textX = -(rowWidth / pixelSize) / 2.0f;
            // submitText anchors the TOP of the text line (glyphs hang below the
            // anchor, same as the heart quads which span y = 0..heartSize), so
            // y = 0 top-aligns the text with the heart row.
            float textY = 0.0f;
            submitNodeCollector.submitText(
                    poseStack,
                    textX,
                    textY,
                    Component.literal(prefixText).getVisualOrderText(),
                    false,
                    Font.DisplayMode.SEE_THROUGH,
                    fLight,
                    prefixColor,
                    0,
                    0
            );
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @Unique
    private static String formatAmount(float health, float maxHealth, float absorption) {
        float total = health + absorption;
        return compactFloat((float) Math.ceil(total) / 2.0f) + "/" + compactFloat((float) Math.ceil(maxHealth) / 2.0f);
    }

    @Unique
    private static String formatPercent(float health, float maxHealth) {
        if (maxHealth <= 0.0f) return "0%";
        return String.format(Locale.ROOT, "%.0f%%", (health / maxHealth) * 100.0f);
    }

    @Unique
    private static String compactFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Unique
    private static void drawHeartQuad(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float size, TextureAtlasSprite sprite, int light) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();

        float x1 = x + size;
        float y1 = y + size;

        buffer.addVertex(pose, x, y1, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(u0, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 0.0f, 1.0f);
        buffer.addVertex(pose, x1, y1, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 0.0f, 1.0f);
        buffer.addVertex(pose, x1, y, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 0.0f, 1.0f);
        buffer.addVertex(pose, x, y, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(u0, v0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 0.0f, 1.0f);
    }
}
