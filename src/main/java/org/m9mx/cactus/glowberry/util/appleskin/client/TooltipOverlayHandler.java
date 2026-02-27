package org.m9mx.cactus.glowberry.util.appleskin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import net.minecraft.util.FormattedCharSequence;
import org.m9mx.cactus.glowberry.feature.modules.AppleSkinModule;
import org.m9mx.cactus.glowberry.util.appleskin.api.event.TooltipOverlayEvent;
import org.m9mx.cactus.glowberry.util.appleskin.helpers.ColorHelper;
import org.m9mx.cactus.glowberry.util.appleskin.helpers.FoodHelper;
import org.m9mx.cactus.glowberry.util.appleskin.helpers.KeyHelper;
import org.m9mx.cactus.glowberry.util.appleskin.helpers.TextureHelper;
import org.m9mx.cactus.glowberry.util.appleskin.helpers.TextureHelper.FoodType;
import net.minecraft.network.chat.Component;



import java.util.ArrayList;

import java.util.List;

public class TooltipOverlayHandler
{
    public static TooltipOverlayHandler INSTANCE;

    static abstract class EmptyText implements Component
    {
        @Override
        public Style getStyle()
        {
            return Style.EMPTY;
        }
        // Fix 1: Changed 'getContent' to 'getContents' (Mojang 1.21.11 mapping)
        @Override
        public ComponentContents getContents()
        {
            return PlainTextContents.EMPTY;
        }
        static List<Component> emptySiblings = new ArrayList<>();
        @Override
        public List<Component> getSiblings()
        {
            return emptySiblings;
        }
        // Fix 2: Component interface in 1.21.11 requires this method.
        // Returning EMPTY here allows FoodOverlayTextComponent to override it later.
        @Override
        public net.minecraft.util.FormattedCharSequence getVisualOrderText()
        {
            return net.minecraft.util.FormattedCharSequence.EMPTY;
        }
    }

    // Bind to text line, because food overlay must apply line offset of all case.
    public static class FoodOverlayTextComponent extends EmptyText implements FormattedCharSequence
    {
        public FoodOverlay foodOverlay;

        FoodOverlayTextComponent(FoodOverlay foodOverlay)
        {
            this.foodOverlay = foodOverlay;
        }
        // Fix 1: Changed 'asOrderedText' to 'getVisualOrderText' (Mojang 1.21.11 mapping)
        @Override
        public FormattedCharSequence getVisualOrderText()
        {
            return this;
        }
        // Fix 2: Updated the accept logic to match the 1.21.11 Component contract
        @Override
        public boolean accept(net.minecraft.util.FormattedCharSink visitor)
        {
            // Since this is a placeholder for the icon overlay, we visit an empty string
            return StringDecomposer.iterateFormatted("", Style.EMPTY, visitor);
        }
    }

    public static class FoodOverlay implements TooltipComponent, ClientTooltipComponent {
        private FoodProperties defaultFood;
        private FoodProperties modifiedFood;
        private Consumable consumableComponent;

        private int biggestHunger;
        private float biggestSaturationIncrement;

        private int hungerBars;
        private String hungerBarsText;

        private int saturationBars;
        private String saturationBarsText;

        private ItemStack itemStack;

        FoodOverlay(ItemStack itemStack, FoodProperties defaultFood, FoodProperties modifiedFood, Consumable consumableComponent, Player player)
        {
            this.itemStack = itemStack;
            this.defaultFood = defaultFood;
            this.modifiedFood = modifiedFood;
            this.consumableComponent = consumableComponent;

            biggestHunger = Math.max(defaultFood.nutrition(), modifiedFood.nutrition());
            biggestSaturationIncrement = Math.max(defaultFood.saturation(), modifiedFood.saturation());

            hungerBars = (int) Math.ceil(Math.abs(biggestHunger) / 2f);
            if (hungerBars > 10)
            {
                hungerBarsText = "x" + ((biggestHunger < 0 ? -1 : 1) * hungerBars);
                hungerBars = 1;
            }

            saturationBars = (int) Math.ceil(Math.abs(biggestSaturationIncrement) / 2f);
            if (saturationBars > 10 || saturationBars == 0)
            {
                saturationBarsText = "x" + ((biggestSaturationIncrement < 0 ? -1 : 1) * saturationBars);
                saturationBars = 1;
            }
        }

        boolean shouldRenderHungerBars()
        {
            return hungerBars > 0;
        }

        @Override
        public int getHeight(Font textRenderer)
        {
            // hunger + spacing + saturation + arbitrary spacing,
            // for some reason 3 extra looks best
            return 9 + 1 + 7 + 3;
        }

        @Override
        public int getWidth(Font textRenderer)
        {
            int hungerBarLength = hungerBars * 9;
            if (hungerBarsText != null)
            {
                hungerBarLength += textRenderer.width(hungerBarsText);
            }
            int saturationBarLength = saturationBars * 7;
            if (saturationBarsText != null)
            {
                saturationBarLength += textRenderer.width(saturationBarsText);
            }
            return Math.max(hungerBarLength, saturationBarLength);
        }

        @Override
        public void renderImage(Font textRenderer, int x, int y, int width, int height, GuiGraphics guiGraphics)
        {
            if (TooltipOverlayHandler.INSTANCE != null)
                TooltipOverlayHandler.INSTANCE.onRenderTooltip(guiGraphics, this, x, y, textRenderer);
        }
    }

    public static void init()
    {
        INSTANCE = new TooltipOverlayHandler();
    }

    public void onItemTooltip(ItemStack hoveredStack, Player player, Item.TooltipContext context, TooltipFlag type, List tooltip)
    {
        // When hoveredStack or tooltip is null an unknown exception occurs.
        // If ModConfig.INSTANCE is null then we're probably still in the init phase
        //if (hoveredStack == null || tooltip == null || ModConfig.INSTANCE == null)
        //    return;

        if (!shouldShowTooltip(hoveredStack, type))
            return;

        FoodHelper.QueriedFoodResult queriedFoodResult = FoodHelper.query(hoveredStack, player);
        if (queriedFoodResult == null)
            return;

        FoodProperties defaultFood = queriedFoodResult.defaultFoodProperties;
        FoodProperties modifiedFood = queriedFoodResult.modifiedFoodProperties;

        // Notify everyone that we should render tooltip overlay
        TooltipOverlayEvent.Pre prerenderEvent = new TooltipOverlayEvent.Pre(hoveredStack, defaultFood, modifiedFood);
        TooltipOverlayEvent.Pre.EVENT.invoker().interact(prerenderEvent);
        if (prerenderEvent.isCanceled)
            return;

        FoodOverlay foodOverlay = new FoodOverlay(prerenderEvent.itemStack, defaultFood, modifiedFood, queriedFoodResult.consumable, player);
        if (foodOverlay.shouldRenderHungerBars())
        {
            try
            {
                tooltip.add(new FoodOverlayTextComponent(foodOverlay));
            }
            catch (UnsupportedOperationException ignored)
            {
                // The list is immutable, e.g. the item has the HIDE_TOOLTIP component.
                // In addition to checking for that component, we catch this exception
                // just in case there are other reasons the list could be immutable.
            }
        }
    }

    enum FoodOutline
    {
        NEGATIVE,
        EXTRA,
        NORMAL,
        PARTIAL,
        MISSING;

        public int argb()
        {
            return switch (this)
            {
                case NEGATIVE -> ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, 1.0f);
                case EXTRA -> ColorHelper.argbFromRGBA(0.06f, 0.32f, 0.02f, 1.0f);
                case NORMAL -> ColorHelper.argbFromRGBA(0.0f, 0.0f, 0.0f, 1.0f);
                case PARTIAL -> ColorHelper.argbFromRGBA(0.53f, 0.21f, 0.08f, 1.0f);
                case MISSING -> ColorHelper.argbFromRGBA(0.62f, 0.0f, 0.0f, 0.5f);
            };
        }

        public static FoodOutline get(int modifiedFoodHunger, int defaultFoodHunger, int i)
        {
            if (modifiedFoodHunger < 0)
                return NEGATIVE;
            else if (modifiedFoodHunger > defaultFoodHunger && defaultFoodHunger <= i)
                return EXTRA;
            else if (modifiedFoodHunger > i + 1 || defaultFoodHunger == modifiedFoodHunger)
                return NORMAL;
            else if (modifiedFoodHunger == i + 1)
                return PARTIAL;
            else
                return MISSING;
        }
    }

    public void onRenderTooltip(GuiGraphics guiGraphics, FoodOverlay foodOverlay, int toolTipX, int toolTipY, Font textRenderer)
    {
        // When matrixStack or tooltip is null an unknown exception occurs.
        // If ModConfig.INSTANCE is null then we're probably still in She init phase
        //if (context == null || ModConfig.INSTANCE == null)
        //    return;

        // Not found overlay text lines, maybe some mods removed it.
        if (foodOverlay == null)
            return;

        Matrix3x2fStack matrixStack;
        ItemStack itemStack = foodOverlay.itemStack;

        FoodProperties defaultFood = foodOverlay.defaultFood;
        FoodProperties modifiedFood = foodOverlay.modifiedFood;

        int x = toolTipX;
        int y = toolTipY;

        // Notify everyone that we should render tooltip overlay
        TooltipOverlayEvent.Render renderEvent = new TooltipOverlayEvent.Render(itemStack, x, y, guiGraphics, defaultFood, modifiedFood);
        TooltipOverlayEvent.Render.EVENT.invoker().interact(renderEvent);
        if (renderEvent.isCanceled)
            return;

        x = renderEvent.x;
        y = renderEvent.y;

        guiGraphics = renderEvent.context;
        itemStack = renderEvent.itemStack;
        matrixStack = guiGraphics.pose();

        int defaultFoodHunger = defaultFood.nutrition();
        int modifiedFoodHunger = modifiedFood.nutrition();

        // Render from right to left so that the icons 'face' the right way
        x += (foodOverlay.hungerBars - 1) * 9;

        boolean isRotten = FoodHelper.isRotten(foodOverlay.consumableComponent);

        for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2)
        {
            //contex.drawGuiTexture(RenderPipelines.GUI_TEXTURED, TextureHelper.FOOD_EMPTY_TEXTURE, x, y, 9, 9);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.FOOD_EMPTY_TEXTURE, x, y, 9, 9);

            FoodOutline outline = FoodOutline.get(modifiedFoodHunger, defaultFoodHunger, i);
            if (outline != FoodOutline.NORMAL)
            {
                //contex.drawGuiTexture(RenderPipelines.GUI_TEXTURED, TextureHelper.HUNGER_OUTLINE_SPRITE, x, y, 9, 9, outline.argb());
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.HUNGER_OUTLINE_SPRITE, x, y, 9, 9, outline.argb());
            }

            boolean isDefaultHalf = defaultFoodHunger - 1 == i;
            Identifier defaultFoodIcon = TextureHelper.getFoodTexture(isRotten, isDefaultHalf ? FoodType.HALF : FoodType.FULL);
            //contex.drawGuiTexture(RenderPipelines.GUI_TEXTURED, defaultFoodIcon, x, y, 9, 9, ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.25F));
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, defaultFoodIcon, x, y, 9, 9, ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.25F));

            if (modifiedFoodHunger > i)
            {
                boolean isModifiedHalf = modifiedFoodHunger - 1 == i;
                Identifier modifiedFoodIcon = TextureHelper.getFoodTexture(isRotten, isModifiedHalf ? FoodType.HALF : FoodType.FULL);
                //contex.drawGuiTexture(RenderPipelines.GUI_TEXTURED, modifiedFoodIcon, x, y, 9, 9);
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, modifiedFoodIcon, x, y, 9, 9);
            }

            x -= 9;
        }
        if (foodOverlay.hungerBarsText != null)
        {
            x += 18;
            matrixStack.pushMatrix();
            matrixStack.translate(x, y);
            matrixStack.scale(0.75f, 0.75f);
            //contex.drawTextWithShadow(textRenderer, foodOverlay.hungerBarsText, 2, 2, 0xFFAAAAAA);
            guiGraphics.drawString(textRenderer, foodOverlay.hungerBarsText, 2, 2, 0xFFAAAAAA);
            matrixStack.popMatrix();
        }

        x = toolTipX;
        y += 10;

        float modifiedSaturationIncrement = modifiedFood.saturation();
        float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

        // Render from right to left so that the icons 'face' the right way
        x += (foodOverlay.saturationBars - 1) * 7;

        for (int i = 0; i < foodOverlay.saturationBars * 2; i += 2)
        {
            float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

            boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
            int color = shouldBeFaded ? ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.5F) : ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, x, y, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7, 256, 256, color);
            //contex.drawTexture(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, x, y, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7, 256, 256, color);

            x -= 7;
        }
        if (foodOverlay.saturationBarsText != null)
        {
            x += 14;
            matrixStack.pushMatrix();
            matrixStack.translate(x, y);
            matrixStack.scale(0.75f, 0.75f);
            //contex.drawTextWithShadow(textRenderer, foodOverlay.saturationBarsText, 2, 1, 0xFFAAAAAA);
            guiGraphics.drawString(textRenderer, foodOverlay.saturationBarsText, 2, 1, 0xFFAAAAAA);
            matrixStack.popMatrix();
        }
    }

    private boolean shouldShowTooltip(ItemStack hoveredStack, TooltipFlag type)
    {
        if (hoveredStack.isEmpty())
        {
            return false;
        }

        // Note: The intention here is to match the logic in ItemStack.getTooltip
        if (!type.isCreative() && hoveredStack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).hideTooltip())
        {
            return false;
        }
        // Check module settings
        boolean showOnShift = AppleSkinModule.INSTANCE.showFoodValuesInTooltip.get() && KeyHelper.isShiftKeyDown();
        boolean showAlways = AppleSkinModule.INSTANCE.showFoodValuesInTooltipAlways.get();

        if (!showOnShift && !showAlways)
        {
            return false;
        }

        if (!FoodHelper.isFood(hoveredStack))
        {
            return false;
        }
        return true;
    }
}
