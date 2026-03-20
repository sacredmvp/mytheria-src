package moscow.mytheria.ui.menu.api;

import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.HudRenderEvent;
import moscow.mytheria.systems.modules.modules.visuals.MenuModule;
import moscow.mytheria.ui.menu.MenuScreen;
import moscow.mytheria.ui.menu.dropdown.DropDownScreen;
import moscow.mytheria.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;

public class MenuCloseListener implements IMinecraft {
   private final EventListener<HudRenderEvent> onHudRender = event -> {
      MenuScreen menuScreen = Mytheria.getInstance().getMenuScreen();
      if (mc.currentScreen == null) {
         MenuModule menuModule = Mytheria.getInstance().getModuleManager().getModule(MenuModule.class);
         if (!(menuScreen instanceof DropDownScreen)) {
            Mytheria.getInstance().setMenuScreen(new DropDownScreen());
         }
      }

      if (menuScreen != null) {
         menuScreen.getMenuAnimation().update(menuScreen.isClosing() ? 0.0F : 1.0F);
         if (!(mc.currentScreen instanceof MenuScreen) && Mytheria.getInstance().getModuleManager().getModule(MenuModule.class).isEnabled()) {
            Mytheria.getInstance().getModuleManager().getModule(MenuModule.class).setEnabled(false);
         }

         if (menuScreen.getMenuAnimation().getValue() > 0.1F && !(mc.currentScreen instanceof MenuScreen) && menuScreen.isClosing()) {
            UIContext context = UIContext.of(event.getContext(), -1, -1, MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));
            menuScreen.render(context);
         }
      }
   };

   public MenuCloseListener() {
      Mytheria.getInstance().getEventManager().subscribe(this);
   }
}
