package moscow.mytheria.systems.modules.modules.visuals;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.modules.modules.other.Sounds;
import moscow.mytheria.systems.setting.settings.ModeSetting;
import moscow.mytheria.ui.menu.MenuScreen;
import moscow.mytheria.ui.menu.api.MenuCloseListener;
import moscow.mytheria.utility.sounds.ClientSounds;

@ModuleInfo(
   name = "Menu",
   category = ModuleCategory.VISUALS,
   key = 344,
   desc = "modules.descriptions.menu"
)
public class MenuModule extends BaseModule {
   private static final MenuCloseListener menuCloseListener = new MenuCloseListener();
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.menu.mode");
   private final ModeSetting.Value dropdown = new ModeSetting.Value(this.mode, "modules.settings.menu.mode.dropdown");

   @Override
   public void onEnable() {
      if (!(mc.currentScreen instanceof MenuScreen)) {
         MenuScreen menuScreen = Mytheria.getInstance().getMenuScreen();
         mc.setScreen(menuScreen);
         Sounds soundsModule = Mytheria.getInstance().getModuleManager().getModule(Sounds.class);
         if (soundsModule.isEnabled()) {
            ClientSounds.CLICKGUI_OPEN.play(soundsModule.getVolume().getCurrentValue());
         }

         super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (mc.currentScreen instanceof MenuScreen) {
         mc.setScreen(null);
         Mytheria.getInstance().getMenuScreen().setClosing(true);
      }

      super.onDisable();
   }
}
