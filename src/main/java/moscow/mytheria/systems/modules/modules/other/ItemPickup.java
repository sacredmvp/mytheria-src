package moscow.mytheria.systems.modules.modules.other;

import java.util.Map;
import java.util.Map.Entry;
import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.game.PickupEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.notifications.NotificationType;
import moscow.mytheria.utility.game.ItemUtility;
import moscow.mytheria.utility.game.MessageUtility;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@ModuleInfo(
   name = "Item Pickup",
   category = ModuleCategory.OTHER,
   enabledByDefault = true,
   desc = "Уведомляет вас при поднятии донатного предмета"
)
public class ItemPickup extends BaseModule {
   private final Map<String, String> don = Map.of(
      "krush-helmet",
      "Вы подобрали Шлем крушителя!",
      "krush-chestplate",
      "Вы подобрали Нагрудник крушителя!",
      "krush-leggings",
      "Вы подобрали Поножи крушителя!",
      "krush-boots",
      "Вы подобрали Ботинки крушителя!",
      "krush-sword",
      "Вы подобрали донатный предмет: Меч крушителя"
   );
   private final EventListener<PickupEvent> onPickupEvent = event -> {
      ItemStack stack = event.getItemStack();

      for (Entry<String, String> entry : this.don.entrySet()) {
         if (ItemUtility.checkDonItem(stack, entry.getKey())) {
            MessageUtility.info(Text.of(entry.getValue()));
            return;
         }
      }

      if (ItemUtility.isDonItem(stack)) {
         String name = stack.getName().getString();
         Mytheria.getInstance()
            .getNotificationManager()
            .addNotificationOther(NotificationType.INFO, "Донатный предмет", "Вы подобрали донатный предмет: " + name);
      }
   };
}
