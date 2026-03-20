package ru.friz1iks.mixin.display;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.friz1iks.manager.Manager;
import ru.friz1iks.modules.render.ItemColor;
import ru.friz1iks.util.render.RenderUtil;

import java.awt.*;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenItemColor {

    @Unique
    private long lastTime = System.currentTimeMillis();

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onDrawSlotPre(DrawContext context, Slot slot, CallbackInfo ci) {
        ItemColor itemColor = Manager.FUNCTION_MANAGER.itemColor;
        if (itemColor == null || !itemColor.state) return;

        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return;

        String itemId = Registries.ITEM.getId(stack.getItem()).getPath();

        if (itemColor.isItemEnabled(itemId)) {
            int color = itemColor.getItemColor(itemId);

            // Быстрее мерцание (было 1000мс, теперь 400мс) и ярче (было 0.3+0.7, теперь 0.2+0.8)
            long currentTime = System.currentTimeMillis();
            float pulse = (float) (Math.sin((currentTime % 400) / 400.0 * Math.PI * 2) * 0.2 + 0.8);

            // Ярче цвета (увеличиваем яркость)
            int r = Math.min(255, (int) (((color >> 16) & 0xFF) * 1.2f * pulse));
            int g = Math.min(255, (int) (((color >> 8) & 0xFF) * 1.2f * pulse));
            int b = Math.min(255, (int) ((color & 0xFF) * 1.2f * pulse));
            int alpha = (int) (160 * pulse); // Было 100, теперь 160 - ярче

            int finalColor = (alpha << 24) | (r << 16) | (g << 8) | b;

            int x = slot.x;
            int y = slot.y;

            // Подсветка слота с закругленными углами
            RenderUtil.drawRoundedRect(context.getMatrices(), x, y, 16, 16, 3, finalColor);
        }
    }
}
