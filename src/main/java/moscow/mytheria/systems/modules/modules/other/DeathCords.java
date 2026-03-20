package moscow.mytheria.systems.modules.modules.other;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.waypoints.WayPointsManager;
import moscow.mytheria.utility.game.MessageUtility;
import net.minecraft.text.Text;
import net.minecraft.client.gui.screen.DeathScreen;

@ModuleInfo(
   name = "Death Cords",
   category = ModuleCategory.OTHER,
   desc = "Отправляет координаты смерти в чат"
)
public class DeathCords extends BaseModule {
   private boolean death;
   private final BooleanSetting wayDeath = new BooleanSetting(this, "Ставить метку");
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (!(mc.currentScreen instanceof DeathScreen) || mc.player == null) {
         this.death = true;
      } else if (this.death) {
         int xCord = (int)mc.player.getX();
         int yCord = (int)mc.player.getY();
         int zCord = (int)mc.player.getZ();
         MessageUtility.info(Text.of("Координаты смерти: " + xCord + " " + yCord + " " + zCord));
         if (this.wayDeath.isEnabled()) {
            WayPointsManager wayPointsManager = Mytheria.getInstance().getWayPointsManager();
            if (wayPointsManager.contains("Death")) {
               wayPointsManager.del("Death");
            }

            wayPointsManager.add("Death", xCord, yCord, zCord);
         }

         this.death = false;
      }
   };
}
