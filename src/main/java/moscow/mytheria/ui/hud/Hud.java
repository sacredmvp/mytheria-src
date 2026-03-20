package moscow.mytheria.ui.hud;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.framework.objects.MouseButton;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.ChatRenderEvent;
import moscow.mytheria.systems.event.impl.render.HudRenderEvent;
import moscow.mytheria.systems.event.impl.window.ChatClickEvent;
import moscow.mytheria.systems.event.impl.window.ChatKeyPressEvent;
import moscow.mytheria.systems.event.impl.window.ChatReleaseEvent;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.systems.notifications.NotificationType;
import moscow.mytheria.ui.components.animated.AnimatedText;
import moscow.mytheria.ui.components.popup.Popup;
import moscow.mytheria.ui.hud.impl.ArmorHud;
import moscow.mytheria.ui.hud.impl.Effects;
import moscow.mytheria.ui.hud.impl.InventoryHud;
import moscow.mytheria.ui.hud.impl.KeyBinds;
import moscow.mytheria.ui.hud.impl.TargetHud;
import moscow.mytheria.ui.hud.impl.Watermark;
import moscow.mytheria.ui.hud.impl.island.DynamicIsland;
import moscow.mytheria.ui.hud.inline.impl.PlayerElement;
import moscow.mytheria.ui.hud.inline.impl.WorldElement;
import moscow.mytheria.ui.menu.visuals.VisualsScreen;
import moscow.mytheria.utility.animation.base.Easing;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.game.cursor.CursorType;
import moscow.mytheria.utility.game.cursor.CursorUtility;
import moscow.mytheria.utility.gui.GuiUtility;
import moscow.mytheria.utility.interfaces.IMinecraft;
import moscow.mytheria.utility.interfaces.IScaledResolution;
import moscow.mytheria.utility.render.RenderUtility;
import moscow.mytheria.utility.time.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.kotopushka.compiler.sdk.annotations.CompileBytecode;

public class Hud implements IMinecraft, IScaledResolution {
   private final List<HudElement> elements = new ArrayList<>();
   private final List<Popup> popups = new ArrayList<>();
   public DynamicIsland island;
   private final HudHistoryManager historyManager = new HudHistoryManager();
   private final Grid grid = new Grid();
   private String desc = "";
   private AnimatedText descText;
   private final Timer timer = new Timer();
   private final EventListener<HudRenderEvent> onHud = event -> {
      UIContext context = UIContext.of(
         event.getContext(),
         mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().getX(),
         mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().getY(),
         MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false)
      );
      if (this.descText == null) {
         this.descText = new AnimatedText(Fonts.REGULAR.getFont(10.0F), 10.0F, 300L, Easing.BAKEK).centered();
      }

      this.desc = "";
      this.grid.draw(context);
      this.grid.update();

      for (HudElement element : this.elements) {
         element.render(context);
         if (element.getSelecting().getValue() >= 0.0F) {
            float anim = element.getAnimation().getValue() * element.getVisible().getValue();
            float scale = 0.5F + anim * 0.5F - 0.05F * element.getSelecting().getValue();
            element.getLoadingAnim().setDuration(1500L);
            element.getLoadingAnim().update(1.0F);
            if (element.getLoadingAnim().getValue() == 1.0F) {
               element.getLoadingAnim().setValue(0.0F);
            }

            RenderUtility.scale(context.getMatrices(), element.getX() + element.getWidth() / 2.0F, element.getY() + element.getHeight() / 2.0F, scale);
            context.drawLoadingRect(
               element.getX(),
               element.getY(),
               element.getWidth(),
               element instanceof HudList ? Math.max(20.0F, element.getHeight()) : element.getHeight(),
               element.getLoadingAnim().getValue() * 2.2F - 0.5F,
               BorderRadius.all(element instanceof DynamicIsland ? 7.0F : 6.0F),
               ColorRGBA.WHITE.withAlpha(100.0F * element.getSelecting().getValue())
            );
            RenderUtility.end(context.getMatrices());
         }
      }

      this.descText.pos(sr.getScaledWidth() / 2.0F, 30.0F);
      if (!this.desc.contains(".description")) {
         this.descText.update(this.desc);
         this.descText.render(context);
      }

      for (Popup popup : this.popups) {
         if (!(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof HudEditorScreen)) {
            popup.setShowing(false);
         }
      }

      if (!(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof HudEditorScreen)) {
         CursorUtility.set(CursorType.DEFAULT);
      }

      this.popups.removeIf(popupx -> popupx.getAnimation().getValue() == 0.0F && !popupx.isShowing());
   };
   private final EventListener<ChatRenderEvent> onPostHud = event -> {
      UIContext context = UIContext.of(
         event.getContext(),
         mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().getX(),
         mc.currentScreen == null ? -1 : (int)GuiUtility.getMouse().getY(),
         MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false)
      );
      context.getMatrices().push();
      context.getMatrices().translate(0.0F, 0.0F, 2000.0F);

      for (Popup popup : this.popups) {
         if (popup.getY() + popup.getHeight() > sr.getScaledHeight()) {
            popup.setY(sr.getScaledHeight() - 10.0F - popup.getHeight());
         }

         popup.render(context);
      }

      context.getMatrices().pop();
   };
   private final EventListener<ChatKeyPressEvent> onKeyPress = event -> {
      int modifiers = event.getModifiers();
      int keyCode = event.getKeyCode();
      if (keyCode == 90 && (modifiers & 2) != 0) {
         Mytheria.getInstance().getHud().getHistoryManager().undo();
      } else if (keyCode == 89 && (modifiers & 2) != 0) {
         Mytheria.getInstance().getHud().getHistoryManager().redo();
      }
   };
   private final EventListener<ChatClickEvent> onClick = event -> {
      for (Popup popup : this.popups) {
         popup.onMouseClicked(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
         if (popup.isHovered(event.getX(), event.getY())) {
            return;
         }

         popup.setShowing(false);
      }

      for (HudElement element : this.elements) {
         element.onMouseClicked(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
         if (element.isHovered(event.getX(), event.getY()) && element.isShowing() || element.isDragging()) {
            return;
         }
      }

      if (event.getButton() == 1 && !this.disabledElements().isEmpty() && !(mc.currentScreen instanceof VisualsScreen)) {
         Popup popup = new Popup(event.getX(), event.getY(), 90.0F, 6.0F).title("Что добавляем?").separator();

         for (HudElement elementx : this.disabledElements()) {
            popup.button(Localizator.translate(elementx.getName()), elementx.getIcon(), popup1 -> {
               elementx.pos(event.getX(), event.getY());
               elementx.setShowing(true);
               popup1.setShowing(false);
               Mytheria.getInstance().getFileManager().writeFile("client");
            });
         }

         this.popups.add(popup);
      } else if (event.getButton() == 1 && this.disabledElements().isEmpty() && this.timer.finished(600L)) {
         Mytheria.getInstance()
            .getNotificationManager()
            .addNotificationOther(NotificationType.ERROR, "Элементов нет", "Элементы закончились, добавлять больше нечего");
         this.timer.reset();
      }
   };
   private final EventListener<ChatReleaseEvent> onRelease = event -> {
      for (Popup popup : this.popups) {
         popup.onMouseReleased(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
         if (popup.isHovered(event.getX(), event.getY())) {
            return;
         }
      }

      for (HudElement element : this.elements) {
         element.onMouseReleased(event.getX(), event.getY(), MouseButton.fromButtonIndex(event.getButton()));
      }
   };

   @CompileBytecode
   private void initialize() {
      Mytheria.getInstance().getEventManager().subscribe(this);
      this.elements
         .addAll(
            List.of(
               new Effects(),
               new KeyBinds(),
               new TargetHud(),
               new ArmorHud(),
               new InventoryHud(),
               this.island = new DynamicIsland(),
               new WorldElement(),
               new PlayerElement(),
               new Watermark()
            )
         );
   }

   public Hud() {
      this.initialize();
   }

   public List<HudElement> enabledElements() {
      return this.elements.stream().filter(HudElement::isShowing).toList();
   }

   public List<HudElement> disabledElements() {
      return this.elements.stream().filter(element -> !element.isShowing()).toList();
   }

   public <T extends HudElement> T getElementByName(String name) {
      return (T)this.elements.stream().filter(element -> element.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   @Generated
   public List<HudElement> getElements() {
      return this.elements;
   }

   @Generated
   public List<Popup> getPopups() {
      return this.popups;
   }

   @Generated
   public DynamicIsland getIsland() {
      return this.island;
   }

   @Generated
   public HudHistoryManager getHistoryManager() {
      return this.historyManager;
   }

   @Generated
   public Grid getGrid() {
      return this.grid;
   }

   @Generated
   public String getDesc() {
      return this.desc;
   }

   @Generated
   public AnimatedText getDescText() {
      return this.descText;
   }

   @Generated
   public Timer getTimer() {
      return this.timer;
   }

   @Generated
   public EventListener<HudRenderEvent> getOnHud() {
      return this.onHud;
   }

   @Generated
   public EventListener<ChatRenderEvent> getOnPostHud() {
      return this.onPostHud;
   }

   @Generated
   public EventListener<ChatKeyPressEvent> getOnKeyPress() {
      return this.onKeyPress;
   }

   @Generated
   public EventListener<ChatClickEvent> getOnClick() {
      return this.onClick;
   }

   @Generated
   public EventListener<ChatReleaseEvent> getOnRelease() {
      return this.onRelease;
   }

   @Generated
   public void setDesc(String desc) {
      this.desc = desc;
   }
}
