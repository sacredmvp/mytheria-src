package moscow.mytheria.systems.modules.modules.visuals;

import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.HudRenderEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.render.DrawUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

@ModuleInfo(
   name = "Custom Hotbar",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.customhotbar"
)
public class CustomHotbar extends BaseModule {
   // Анимация выделения слота
   private static int targetSlot = 0;
   private static float currentPosition = 0;
   private static long lastUpdateTime = System.currentTimeMillis();
   
   // Настройки позиции
   private final SliderSetting xOffset = new SliderSetting(this, "modules.settings.customhotbar.x_offset")
      .step(1F)
      .min(-500F)
      .max(500F)
      .currentValue(0F);
   
   private final SliderSetting yOffset = new SliderSetting(this, "modules.settings.customhotbar.y_offset")
      .step(1F)
      .min(-500F)
      .max(500F)
      .currentValue(0F);
   
   // Настройки масштаба
   private final SliderSetting scale = new SliderSetting(this, "modules.settings.customhotbar.scale")
      .step(0.1F)
      .min(0.5F)
      .max(2.0F)
      .currentValue(1.0F);
   
   private final EventListener<HudRenderEvent> onHudRender = event -> {
      if (!this.isEnabled() || mc.player == null) return;
      
      DrawContext context = event.getContext();
      int screenWidth = mc.getWindow().getScaledWidth();
      int screenHeight = mc.getWindow().getScaledHeight();
      
      // Применяем масштаб
      float currentScale = scale.getCurrentValue();
      context.getMatrices().push();
      context.getMatrices().scale(currentScale, currentScale, 1.0F);
      
      // Вычисляем позицию с учетом масштаба
      int baseX = (int)((screenWidth / 2 - 91) / currentScale) + (int)xOffset.getCurrentValue();
      int baseY = (int)((screenHeight - 22) / currentScale) + (int)yOffset.getCurrentValue();
      
      PlayerInventory inventory = mc.player.getInventory();
      
      // Обновляем целевую позицию для анимации
      int newTargetSlot = inventory.selectedSlot;
      if (newTargetSlot != targetSlot) {
         targetSlot = newTargetSlot;
         lastUpdateTime = System.currentTimeMillis();
      }
      
      // Вычисляем текущую позицию с плавным переходом (lerp анимация)
      long currentTime = System.currentTimeMillis();
      float delta = (currentTime - lastUpdateTime) / 200F; // 200ms анимация
      delta = Math.min(delta, 1.0f);
      currentPosition = lerp(currentPosition, targetSlot, delta);
      
      // Размеры
      int hotbarWidth = 182;
      int hotbarHeight = 20;
      int slotSize = 20;
      
      // Рисуем основной фон хотбара с тенью
      DrawUtility.drawShadow(
         context.getMatrices(),
         baseX - 2,
         baseY - 2,
         hotbarWidth + 4,
         hotbarHeight + 4,
         8.0F,
         BorderRadius.all(4.0F),
         ColorRGBA.BLACK.withAlpha(100.0F)
      );
      
      DrawUtility.drawRoundedRect(
         context.getMatrices(),
         baseX,
         baseY,
         hotbarWidth,
         hotbarHeight,
         BorderRadius.all(4.0F),
         new ColorRGBA(0, 0, 0, 150)
      );
      
      // Анимированное выделение слота - красивый скругленный квадрат с тенью
      float selectorX = baseX + currentPosition * 20;
      float selectorY = baseY;
      float selectorWidth = slotSize;
      float selectorHeight = hotbarHeight;
      
      // Тень для выделенного слота
      DrawUtility.drawShadow(
         context.getMatrices(),
         selectorX - 1,
         selectorY - 1,
         selectorWidth + 2,
         selectorHeight + 2,
         6.0F,
         BorderRadius.all(4.0F),
         new ColorRGBA(100, 150, 255, 120)
      );
      
      // Красивый скругленный квадрат
      DrawUtility.drawRoundedRect(
         context.getMatrices(),
         selectorX,
         selectorY,
         selectorWidth,
         selectorHeight,
         BorderRadius.all(4.0F),
         new ColorRGBA(100, 150, 255, 100)
      );
      
      // Рисуем предметы в хотбаре
      for (int i = 0; i < 9; i++) {
         int x = baseX + i * 20 + 2;
         int y = baseY + 2;
         
         ItemStack stack = inventory.getStack(i);
         
         if (!stack.isEmpty()) {
            context.drawItem(stack, x, y);
            context.drawStackOverlay(mc.textRenderer, stack, x, y);
         }
      }
      
      // Рисуем offhand
      ItemStack offhandStack = mc.player.getOffHandStack();
      if (!offhandStack.isEmpty()) {
         int offhandX = baseX + hotbarWidth + 6;
         int offhandY = baseY;
         int offhandSize = 20;
         
         // Фон offhand с тенью
         DrawUtility.drawShadow(
            context.getMatrices(),
            offhandX - 2,
            offhandY - 2,
            offhandSize + 4,
            offhandSize + 4,
            8.0F,
            BorderRadius.all(4.0F),
            ColorRGBA.BLACK.withAlpha(100.0F)
         );
         
         DrawUtility.drawRoundedRect(
            context.getMatrices(),
            offhandX,
            offhandY,
            offhandSize,
            offhandSize,
            BorderRadius.all(4.0F),
            new ColorRGBA(0, 0, 0, 150)
         );
         
         // Предмет
         context.drawItem(offhandStack, offhandX + 2, offhandY + 2);
         context.drawStackOverlay(mc.textRenderer, offhandStack, offhandX + 2, offhandY + 2);
      }
      
      context.getMatrices().pop();
   };
   
   private float lerp(float start, float end, float delta) {
      return start + (end - start) * delta;
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }
}
