package ru.friz1iks.mixin.display;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.friz1iks.manager.HudManager;
import ru.friz1iks.manager.Manager;
import ru.friz1iks.util.color.ColorUtil;
import ru.friz1iks.util.render.RenderUtil;

import java.awt.*;

@Mixin(InGameHud.class)
public abstract class MixinHotbar {
    
    @Shadow @Final private MinecraftClient client;
    
    @Unique private static final int HOTBAR_SLOT_COUNT = 9;
    @Unique private static final int SLOT_SIZE = 22;
    @Unique private static final int SLOT_SPACING = 1;
    @Unique private static final float CORNER_RADIUS = 3f;
    
    // Анимация выделения слота
    @Unique private static int targetSlot = 0;
    @Unique private static float currentPosition = 0;
    @Unique private static long lastUpdateTime = System.currentTimeMillis();
    
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!HudManager.hotbarEnabled) return;
        
        PlayerEntity player = client.player;
        if (player == null) return;
        
        ci.cancel();
        
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        
        int i = screenWidth / 2;
        Arm handside = player.getMainArm().getOpposite();
        
        int blitStandartY = screenHeight - 22;
        int blitItemY = blitStandartY + 3;
        
        // Обновляем целевую позицию для анимации
        int newTargetSlot = player.getInventory().selectedSlot;
        if (newTargetSlot != targetSlot) {
            targetSlot = newTargetSlot;
            lastUpdateTime = System.currentTimeMillis();
        }
        
        // Вычисляем текущую позицию с плавным переходом (lerp анимация)
        long currentTime = System.currentTimeMillis();
        float delta = (currentTime - lastUpdateTime) / 200f; // 200ms - время анимации
        delta = Math.min(delta, 1.0f);
        currentPosition = lerp(currentPosition, targetSlot, delta);
        
        int i2 = i - 91;
        int k2 = 182;
        int l1 = 22;
        
        // Основной фон хотбара
        RenderUtil.drawRoundedRect(context.getMatrices(), i2, blitStandartY, k2, l1 - 2, 4.0f, new Color(0, 0, 0, 150).getRGB());
        
        // Анимированное выделение слота с использованием currentPosition
        float selectorX = i - 91 - 1 + currentPosition * 20;
        int selectorY = blitStandartY - 1;
        int selectorWidth = 24;
        int selectorHeight = 22;
        
        // Обводка выделенного слота с цветом темы
        int themeColor = ColorUtil.getColorHud(0);
        RenderUtil.drawRoundedRect(context.getMatrices(), selectorX + 2, selectorY + 2, selectorWidth - 4, selectorHeight - 4, 2.0f, ColorUtil.reAlphaInt(themeColor, 100));
        drawRoundedRectOutline(context, selectorX + 2, selectorY + 2, selectorWidth - 4, selectorHeight - 4, 2.0f, 1.2f, themeColor);
        
        // Offhand слот
        ItemStack offhandStack = player.getOffHandStack();
        if (!offhandStack.isEmpty()) {
            if (handside == Arm.LEFT) {
                int offhandX = i - 91 - 28;
                int offhandWidth = 20;
                int offhandHeight = 21;
                RenderUtil.drawRoundedRect(context.getMatrices(), offhandX, blitStandartY, offhandWidth, offhandHeight - 1, 3.0f, new Color(0, 0, 0, 150).getRGB());
            } else {
                int offhandX = i + 91 + 4;
                int offhandWidth = 20;
                int offhandHeight = 21;
                RenderUtil.drawRoundedRect(context.getMatrices(), offhandX, blitStandartY, offhandWidth, offhandHeight - 1, 3.0f, new Color(0, 0, 0, 150).getRGB());
            }
        }
        
        // Рендер предметов в хотбаре
        for (int slot = 0; slot < 9; slot++) {
            int itemX = i - 90 + slot * 20 + 2;
            int itemY = blitItemY - 1;
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                context.drawItem(stack, itemX, itemY);
                context.drawStackOverlay(client.textRenderer, stack, itemX, itemY);
            }
        }
        
        // Рендер offhand предмета
        if (!offhandStack.isEmpty()) {
            int offhandItemY = blitItemY - 1;
            if (handside == Arm.LEFT) {
                context.drawItem(offhandStack, i - 91 - 26, offhandItemY);
                context.drawStackOverlay(client.textRenderer, offhandStack, i - 91 - 26, offhandItemY);
            } else {
                context.drawItem(offhandStack, i + 91 + 10, offhandItemY);
                context.drawStackOverlay(client.textRenderer, offhandStack, i + 91 + 10, offhandItemY);
            }
        }
        
    }
    
    @Unique
    private float lerp(float start, float end, float delta) {
        return start + (end - start) * delta;
    }
    
    @Unique
    private void drawRoundedRectOutline(DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color) {
        // Верхняя линия
        RenderUtil.drawRoundedRect(context.getMatrices(), x, y, width, thickness, radius, color);
        // Нижняя линия
        RenderUtil.drawRoundedRect(context.getMatrices(), x, y + height - thickness, width, thickness, radius, color);
        // Левая линия
        RenderUtil.drawRoundedRect(context.getMatrices(), x, y, thickness, height, radius, color);
        // Правая линия
        RenderUtil.drawRoundedRect(context.getMatrices(), x + width - thickness, y, thickness, height, radius, color);
    }
}
