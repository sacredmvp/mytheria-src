package moscow.mytheria.systems.modules.modules.other;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.network.ReceivePacketEvent;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.notifications.NotificationType;
import moscow.mytheria.systems.setting.settings.ModeSetting;
import moscow.mytheria.utility.game.server.ServerUtility;
import moscow.mytheria.utility.inventory.group.SlotGroup;
import moscow.mytheria.utility.inventory.group.SlotGroups;
import moscow.mytheria.utility.inventory.slots.HotbarSlot;
import moscow.mytheria.utility.time.Timer;
import net.minecraft.util.Hand;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(
   name = "Auto Join",
   category = ModuleCategory.OTHER,
   desc = "Автоматически заходит на режим"
)
public class AutoJoin extends BaseModule {
   private final String griefString = "306";
   private final ModeSetting mode = new ModeSetting(this, "Режим");
   private final ModeSetting.Value duels = new ModeSetting.Value(this.mode, "Дуэли SpookyTime").select();
   private final ModeSetting.Value grief = new ModeSetting.Value(this.mode, "Гриф RW/FT/Spooky");
   private final Timer timer = new Timer();
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (ServerUtility.isST() && this.duels.isSelected()) {
         SlotGroup<HotbarSlot> search = SlotGroups.hotbar();
         HotbarSlot compass = search.findItem(Items.COMPASS);
         HotbarSlot sword = search.findItem(Items.DIAMOND_SWORD);
         if (compass != null && this.timer.finished(300L)) {
            mc.player.getInventory().selectedSlot = compass.getSlotId();
            mc.interactionManager
               .sendSequencedPacket(
                  mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch())
               );
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && mc.currentScreen.getTitle().getString().contains("Выберите режим")) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 14, 0, SlotActionType.QUICK_MOVE, mc.player);
            }

            this.timer.reset();
         }

         if (sword != null) {
            Mytheria.getInstance().getNotificationManager().addNotificationOther(NotificationType.SUCCESS, "Успешный вход", "Вы успешно вошли на дуэли");
            this.toggle();
         }
      }

      if ((ServerUtility.isFT() || ServerUtility.isRW() || ServerUtility.isST()) && this.grief.isSelected()) {
         mc.player.networkHandler.sendChatCommand("an306");
      }
   };
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet && (ServerUtility.isFT() || ServerUtility.isRW() || ServerUtility.isST()) && this.grief.isSelected()) {
         String message = packet.content().getString().toLowerCase();
         if (ServerUtility.isFT() ? message.contains("вы уже подключены к этому серверву!") : message.contains("вы уже подключены на этот сервер!")) {
            this.toggle();
         }
      }
   };
}
