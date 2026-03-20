package moscow.mytheria.ui.hud.impl.island.impl;

import moscow.mytheria.framework.base.CustomDrawContext;
import moscow.mytheria.systems.setting.settings.SelectSetting;
import moscow.mytheria.ui.hud.impl.island.TimerStatus;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.game.server.ServerUtility;

public class PVPStatus extends TimerStatus {
   public PVPStatus(SelectSetting setting) {
      super(setting, "pvp");
   }

   @Override
   public void draw(CustomDrawContext context) {
      this.update("s", ServerUtility.ctTime, "Вы в PVP режиме", new ColorRGBA(185.0F, 28.0F, 28.0F));
      super.draw(context);
   }

   @Override
   public boolean canShow() {
      return ServerUtility.hasCT;
   }
}
