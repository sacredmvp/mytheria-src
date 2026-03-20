package moscow.mytheria.systems.modules.modules.player;

import java.util.function.Predicate;
import moscow.mytheria.mixin.minecraft.client.IMinecraftClient;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.utility.inventory.ItemSlot;
import moscow.mytheria.utility.inventory.group.SlotGroup;
import moscow.mytheria.utility.inventory.group.SlotGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;

@ModuleInfo(
   name = "Auto Eat",
   category = ModuleCategory.PLAYER
)
public class AutoEat extends BaseModule {
   private boolean eating;
   private final SliderSetting food = new SliderSetting(this, "modules.settings.auto_eat.food").step(1.0F).min(1.0F).max(20.0F).currentValue(15.0F);
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (mc.player.getHungerManager().getFoodLevel() <= this.food.getCurrentValue()) {
         SlotGroup<ItemSlot> search = SlotGroups.inventory().and(SlotGroups.hotbar());
         ItemSlot foodSlot = search.findItem((Predicate<ItemStack>)(stack -> stack.getItem().getDefaultStack().contains(DataComponentTypes.FOOD)));
         if (!mc.player.getOffHandStack().contains(DataComponentTypes.FOOD) && foodSlot != null) {
            foodSlot.moveToOffHand();
         }

         this.eating = true;
         if (mc.currentScreen != null && !mc.player.isUsingItem()) {
            ((IMinecraftClient)mc).idoItemUse();
         } else {
            mc.options.useKey.setPressed(true);
         }
      } else if (this.eating) {
         this.eating = false;
         mc.options.useKey.setPressed(false);
      }
   };
}
