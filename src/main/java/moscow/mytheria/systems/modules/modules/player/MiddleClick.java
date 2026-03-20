package moscow.mytheria.systems.modules.modules.player;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.event.impl.window.KeyPressEvent;
import moscow.mytheria.systems.event.impl.window.MouseEvent;
import moscow.mytheria.systems.friends.FriendManager;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.notifications.NotificationType;
import moscow.mytheria.systems.setting.settings.BindSetting;
import moscow.mytheria.systems.setting.settings.SelectSetting;
import moscow.mytheria.utility.inventory.InventoryUtility;
import moscow.mytheria.utility.inventory.group.SlotGroup;
import moscow.mytheria.utility.inventory.group.SlotGroups;
import moscow.mytheria.utility.inventory.slots.HotbarSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

@ModuleInfo(
   name = "Middle Click",
   category = ModuleCategory.PLAYER,
   desc = "Выполняет действие при нажатии на колесико мыши"
)
public class MiddleClick extends BaseModule {
   private final SelectSetting actions = new SelectSetting(this, "Действие").min(1);
   private final SelectSetting.Value clickPearl = new SelectSetting.Value(this.actions, "Бросать жемчуг").select();
   private final SelectSetting.Value clickFriend = new SelectSetting.Value(this.actions, "Добавлять друзей");
   private final BindSetting clickFriendKey = new BindSetting(this, "Клавиша друзей", () -> !this.clickFriend.isSelected());
   private final BindSetting clickPearlKey = new BindSetting(this, "Клавиша перла", () -> !this.clickPearl.isSelected());
   private boolean pendingPearlSwap = false;
   private final EventListener<KeyPressEvent> onKeyPressEvent = event -> this.handleKey(event.getKey(), event.getAction());
   private final EventListener<MouseEvent> onMouseEvent = event -> this.handleKey(event.getButton(), event.getAction());
   private final EventListener<ClientPlayerTickEvent> onSwapTick = event -> {
      if (this.pendingPearlSwap && mc.player != null) {
         SlotGroup<HotbarSlot> hotbar = SlotGroups.hotbar();
         HotbarSlot pearlSlot = hotbar.findItem(Items.ENDER_PEARL);
         if (pearlSlot != null) {
            InventoryUtility.selectHotbarSlot(pearlSlot, false);
         } else {
            Mytheria.getInstance()
               .getNotificationManager()
               .addNotificationOther(NotificationType.ERROR, "Предмет не найден", "Вам необходимо иметь Жемчужный глаз в хотбаре");
         }

         this.pendingPearlSwap = false;
      }
   };

   private void handleKey(int key, int action) {
      if (mc.currentScreen == null && action == 1) {
         if (mc.options.attackKey.isPressed()) {
            return;
         }

         if (this.clickFriend.isSelected() && this.clickFriendKey.isKey(key)) {
            if (mc.targetedEntity instanceof PlayerEntity) {
               String nick = mc.targetedEntity.getName().getString();
               FriendManager friend = Mytheria.getInstance().getFriendManager();
               if (friend.isFriend(nick)) {
                  friend.remove(nick);
               } else {
                  friend.add(nick);
               }
            }

            return;
         }

         if (this.clickPearl.isSelected() && this.clickPearlKey.isKey(key)) {
            if (!this.pendingPearlSwap) {
               this.pendingPearlSwap = true;
            }

            return;
         }
      }
   }
}
