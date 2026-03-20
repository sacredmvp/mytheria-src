package moscow.mytheria.ui.hud.impl.island.impl;

import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.CustomDrawContext;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.systems.setting.settings.SelectSetting;
import moscow.mytheria.ui.hud.impl.island.DynamicIsland;
import moscow.mytheria.ui.hud.impl.island.IslandStatus;
import moscow.mytheria.utility.colors.Colors;

public class DefaultStatus extends IslandStatus {
   public DefaultStatus(SelectSetting setting) {
      super(setting, "default");
   }

   @Override
   public void draw(CustomDrawContext context) {
      DynamicIsland island = Mytheria.getInstance().getHud().getIsland();
      float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
      float y = 7.0F;
      float width = this.size.width = 20.0F + Fonts.MEDIUM.getFont(7.0F).width(Mytheria.NAME);
      float height = this.size.height = 15.0F;
      context.drawRoundedRect(x - 6.0F + 10.0F * this.animation.getValue(), y + 4.0F, 7.0F, 7.0F, BorderRadius.all(3.0F), Colors.getAccentColor());
      context.drawText(Fonts.MEDIUM.getFont(7.0F), Mytheria.NAME, x + 25.0F - 10.0F * this.animation.getValue(), y + 5.0F, Colors.getTextColor());
   }

   @Override
   public boolean canShow() {
      return true;
   }
}
