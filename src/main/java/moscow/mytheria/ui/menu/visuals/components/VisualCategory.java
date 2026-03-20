package moscow.mytheria.ui.menu.visuals.components;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.modules.Module;
import moscow.mytheria.ui.hud.HudElement;
import moscow.mytheria.ui.menu.api.MenuCategory;
import moscow.mytheria.utility.animation.base.Animation;
import moscow.mytheria.utility.animation.base.Easing;

public class VisualCategory {
   private final String name;
   private final MenuCategory menuCategory;
   private final List<VisualToggle> toggles = new ArrayList<>();
   private final List<HudToggle> hudToggles = new ArrayList<>();
   private final Animation selectedAnimation = new Animation(300L, Easing.BAKEK);

   public VisualCategory(String name, MenuCategory menuCategory) {
      this.name = name;
      this.menuCategory = menuCategory;
   }

   public void init() {
      this.toggles.clear();
      this.hudToggles.clear();
      if (this.menuCategory != null) {
         for (Module module : Mytheria.getInstance().getModuleManager().getModules()) {
            if (module.getCategory() == this.menuCategory.getCategory() && !module.isHidden()) {
               this.toggles.add(new VisualToggle(module));
            }
         }
      } else if (this.name.equals("HUD")) {
         for (HudElement element : Mytheria.getInstance().getHud().getElements()) {
            this.hudToggles.add(new HudToggle(element));
         }
      } else if (this.name.equals("Utilities")) {
      }
   }

   public boolean isHudCategory() {
      return this.name.equals("HUD");
   }

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public List<VisualToggle> getToggles() {
      return this.toggles;
   }

   @Generated
   public List<HudToggle> getHudToggles() {
      return this.hudToggles;
   }

   @Generated
   public Animation getSelectedAnimation() {
      return this.selectedAnimation;
   }

   @Generated
   public MenuCategory getMenuCategory() {
      return this.menuCategory;
   }
}
