package org.m9mx.cactus.glowberry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.m9mx.cactus.glowberry.util.text.InlineItemIconTextHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Injects inline [icon:item_id] support into text rendering.
 */
@Mixin(Font.class)
public class FontInlineIconMixin {
    @Unique
    private static final ThreadLocal<Boolean> GLOWBERRY$INLINE_ICON_REENTRY = ThreadLocal.withInitial(() -> false);
    @Unique
    private static final float GLOWBERRY$ICON_TARGET_SIZE = 9.0f;
    @Unique
    private static final float GLOWBERRY$ICON_ADVANCE = 10.0f;
    @Unique
    private static final float GLOWBERRY$ICON_Y_OFFSET = -1.0f;
    @Unique
    private static Method GLOWBERRY$RENDER_STATIC_METHOD;
    @Unique
    private static boolean GLOWBERRY$RENDER_STATIC_LOOKED_UP;


    @Inject(
            method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void glowberry$drawInlineIcons(
            String text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            MultiBufferSource buffers,
            Font.DisplayMode displayMode,
            int backgroundColor,
            int light,
            CallbackInfo ci
    ) {
        glowberry$renderInline(text, x, y, color, shadow, matrix, buffers, displayMode, backgroundColor, light, ci);
    }

    @Inject(
            method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void glowberry$drawInlineIconsComponent(
            Component text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            MultiBufferSource buffers,
            Font.DisplayMode displayMode,
            int backgroundColor,
            int light,
            CallbackInfo ci
    ) {
        glowberry$renderInline(text == null ? "" : text.getString(), x, y, color, shadow, matrix, buffers, displayMode, backgroundColor, light, ci);
    }

    @Inject(
            method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void glowberry$drawInlineIconsFormatted(
            FormattedCharSequence text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            MultiBufferSource buffers,
            Font.DisplayMode displayMode,
            int backgroundColor,
            int light,
            CallbackInfo ci
    ) {
        glowberry$renderInline(glowberry$toPlainString(text), x, y, color, shadow, matrix, buffers, displayMode, backgroundColor, light, ci);
    }

    @Unique
    private void glowberry$renderInline(
            String text,
            float x,
            float y,
            int color,
            boolean shadow,
            Matrix4f matrix,
            MultiBufferSource buffers,
            Font.DisplayMode displayMode,
            int backgroundColor,
            int light,
            CallbackInfo ci
    ) {
        if (GLOWBERRY$INLINE_ICON_REENTRY.get() || !InlineItemIconTextHelper.hasIconTag(text)) {
            return;
        }

        var segments = InlineItemIconTextHelper.parse(text);
        if (segments.isEmpty()) {
            return;
        }

        Font self = (Font) (Object) this;
        Minecraft mc = Minecraft.getInstance();
        float cursorX = x;
        boolean renderedAnyIcon = false;

        GLOWBERRY$INLINE_ICON_REENTRY.set(true);
        try {
            for (InlineItemIconTextHelper.Segment segment : segments) {
                if (segment.icon()) {
                    Level level = mc.level;
                    if (level != null) {
                        PoseStack iconPose = new PoseStack();
                        iconPose.last().pose().set(matrix);
                        iconPose.translate(cursorX, y + GLOWBERRY$ICON_Y_OFFSET, 0.0f);
                        float scale = GLOWBERRY$ICON_TARGET_SIZE / 16.0f;
                        iconPose.scale(scale, scale, scale);

                        if (glowberry$tryRenderStaticGui(mc.getItemRenderer(), segment.iconStack(), light, iconPose, buffers, level)) {
                            renderedAnyIcon = true;
                        } else {
                            String fallback = segment.iconStack().getItem().toString();
                            self.drawInBatch(fallback, cursorX, y, color, shadow, matrix, buffers, displayMode, backgroundColor, light);
                            cursorX += self.width(fallback);
                            continue;
                        }
                    } else {
                        String fallback = segment.iconStack().getItem().toString();
                        self.drawInBatch(fallback, cursorX, y, color, shadow, matrix, buffers, displayMode, backgroundColor, light);
                        cursorX += self.width(fallback);
                        continue;
                    }

                    cursorX += GLOWBERRY$ICON_ADVANCE;
                    continue;
                }

                String plain = segment.text();
                if (!plain.isEmpty()) {
                    self.drawInBatch(plain, cursorX, y, color, shadow, matrix, buffers, displayMode, backgroundColor, light);
                    cursorX += self.width(plain);
                }
            }

            if (renderedAnyIcon) {
                ci.cancel();
            }
        } finally {
            GLOWBERRY$INLINE_ICON_REENTRY.set(false);
        }
    }

    @Unique
    private static boolean glowberry$tryRenderStaticGui(
            ItemRenderer itemRenderer,
            ItemStack stack,
            int light,
            PoseStack pose,
            MultiBufferSource buffers,
            Level level
    ) {
        Method method = glowberry$getRenderStaticMethod();
        if (method == null) {
            return false;
        }

        try {
            method.invoke(itemRenderer, stack, ItemDisplayContext.GUI, light, OverlayTexture.NO_OVERLAY, pose, buffers, level, 0);
            return true;
        } catch (Exception ignored) {
            // Keep text rendering alive even if icon rendering fails.
            return false;
        }
    }

    @Unique
    private static String glowberry$toPlainString(FormattedCharSequence text) {
        if (text == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            out.appendCodePoint(codePoint);
            return true;
        });
        return out.toString();
    }

    @Unique
    private static Method glowberry$getRenderStaticMethod() {
        if (GLOWBERRY$RENDER_STATIC_LOOKED_UP) {
            return GLOWBERRY$RENDER_STATIC_METHOD;
        }
        GLOWBERRY$RENDER_STATIC_LOOKED_UP = true;

        try {
            GLOWBERRY$RENDER_STATIC_METHOD = ItemRenderer.class.getMethod(
                    "renderStatic",
                    ItemStack.class,
                    ItemDisplayContext.class,
                    int.class,
                    int.class,
                    PoseStack.class,
                    MultiBufferSource.class,
                    Level.class,
                    int.class
            );
        } catch (Exception ignored) {
            GLOWBERRY$RENDER_STATIC_METHOD = null;
        }

        return GLOWBERRY$RENDER_STATIC_METHOD;
    }
}

