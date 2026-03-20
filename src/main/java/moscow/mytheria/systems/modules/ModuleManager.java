package moscow.mytheria.systems.modules;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.event.impl.render.HudRenderEvent;
import moscow.mytheria.systems.event.impl.window.KeyPressEvent;
import moscow.mytheria.systems.event.impl.window.MouseEvent;
import moscow.mytheria.systems.modules.exception.UnknownModuleException;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.modules.modules.combat.ItemRadius;
import moscow.mytheria.systems.modules.modules.combat.ShiftTap;
import moscow.mytheria.systems.modules.modules.combat.TotemTracker;
import moscow.mytheria.systems.modules.modules.combat.DebugHelper;
import moscow.mytheria.systems.modules.modules.movement.AutoSprint;
import moscow.mytheria.systems.modules.modules.other.Assist;
import moscow.mytheria.systems.modules.modules.other.AutoAccept;
import moscow.mytheria.systems.modules.modules.other.AutoJoin;
import moscow.mytheria.systems.modules.modules.other.CoordInvite;
import moscow.mytheria.systems.modules.modules.other.DeathCords;
import moscow.mytheria.systems.modules.modules.other.ItemHighlighter;
import moscow.mytheria.systems.modules.modules.other.ItemPickup;
import moscow.mytheria.systems.modules.modules.other.NameProtect;
import moscow.mytheria.systems.modules.modules.other.Sounds;
import moscow.mytheria.systems.modules.modules.other.SpecBind;
import moscow.mytheria.systems.modules.modules.player.AutoEat;
import moscow.mytheria.systems.modules.modules.player.AutoInvisible;
import moscow.mytheria.systems.modules.modules.player.AutoSwap;
import moscow.mytheria.systems.modules.modules.player.ElytraUtils;
import moscow.mytheria.systems.modules.modules.player.Freelook;
import moscow.mytheria.systems.modules.modules.player.InvUtils;
import moscow.mytheria.systems.modules.modules.player.MiddleClick;
import moscow.mytheria.systems.modules.modules.player.MineHelper;
import moscow.mytheria.systems.modules.modules.player.PlayerUtils;
import moscow.mytheria.systems.modules.modules.player.TapeMouse;
import moscow.mytheria.systems.modules.modules.visuals.Ambience;
import moscow.mytheria.systems.modules.modules.visuals.AspectRatio;
import moscow.mytheria.systems.modules.modules.visuals.BlockOverlay;
import moscow.mytheria.systems.modules.modules.visuals.CustomFog;
import moscow.mytheria.systems.modules.modules.visuals.CustomHitBox;
import moscow.mytheria.systems.modules.modules.visuals.HitEffect;
import moscow.mytheria.systems.modules.modules.visuals.HitParticles;
import moscow.mytheria.systems.modules.modules.visuals.Interface;
import moscow.mytheria.systems.modules.modules.visuals.ItemPhysics;
import moscow.mytheria.systems.modules.modules.visuals.KillEffects;
import moscow.mytheria.systems.modules.modules.visuals.MenuModule;
import moscow.mytheria.systems.modules.modules.visuals.ObjectInfo;
import moscow.mytheria.systems.modules.modules.visuals.PVPAI;
import moscow.mytheria.systems.modules.modules.visuals.Prediction;
import moscow.mytheria.systems.modules.modules.visuals.Removals;
import moscow.mytheria.systems.modules.modules.visuals.SwingAnimation;
import moscow.mytheria.systems.modules.modules.visuals.TNTTimer;
import moscow.mytheria.systems.modules.modules.visuals.TargetESP;
import moscow.mytheria.systems.modules.modules.visuals.ViewModel;
import moscow.mytheria.systems.modules.modules.visuals.World;
import moscow.mytheria.systems.modules.modules.visuals.Zoom;
import net.minecraft.client.MinecraftClient;
import ru.kotopushka.compiler.sdk.annotations.CompileBytecode;

public class ModuleManager {
   private final List<Module> modules = new ArrayList<>();
   private final EventListener<ClientPlayerTickEvent> tickListener;
   private final EventListener<HudRenderEvent> moduleWidgetRenderer;
   private final EventListener<KeyPressEvent> onKeyPress = event -> {
      if (MinecraftClient.getInstance().currentScreen == null) {
         for (Module module : this.getModules()) {
            // Не срабатывает бинд если модуль скрыт
            if (!module.isHidden() && module.getKey() == event.getKey() && module.getKey() != -1 && event.getAction() == 1) {
               module.toggle();
            }
         }
      }
   };
   private final EventListener<MouseEvent> onMouseButtonPress = event -> {
      if (MinecraftClient.getInstance().currentScreen == null) {
         for (Module module : this.getModules()) {
            // Не срабатывает бинд если модуль скрыт
            if (!module.isHidden() && module.getKey() == event.getButton() && module.getKey() != -1 && event.getAction() == 1) {
               module.toggle();
            }
         }
      }
   };

   public ModuleManager(EventListener<ClientPlayerTickEvent> tickListener, EventListener<HudRenderEvent> moduleWidgetRenderer) {
      this.tickListener = tickListener;
      this.moduleWidgetRenderer = moduleWidgetRenderer;
      Mytheria.getInstance().getEventManager().subscribe(this);
   }

   @CompileBytecode
   public void registerModules() {
      // Скрытые модули (регистрируем с новыми именами классов)
      BaseModule auraModule = new moscow.mytheria.systems.modules.modules.combat.AutoClicker();
      this.register(auraModule);
      auraModule.setHidden(true); // Скрыт по умолчанию
      
      BaseModule espModule = new moscow.mytheria.systems.modules.modules.visuals.EntityGlow();
      this.register(espModule);
      espModule.setHidden(true); // Скрыт по умолчанию
      
      BaseModule nameTagsModule = new moscow.mytheria.systems.modules.modules.visuals.PlayerLabels();
      this.register(nameTagsModule);
      nameTagsModule.setHidden(true); // Скрыт по умолчанию
      
      BaseModule triggerBotModule = new moscow.mytheria.systems.modules.modules.combat.DebugHelper();
      this.register(triggerBotModule);
      triggerBotModule.setHidden(true); // Скрыт по умолчанию
      
      BaseModule aimAssistModule = new moscow.mytheria.systems.modules.modules.combat.NetworkSync();
      this.register(aimAssistModule);
      aimAssistModule.setHidden(true); // Скрыт по умолчанию
      
      BaseModule panicModule = new moscow.mytheria.systems.modules.modules.other.SafeMode();
      this.register(panicModule);
      panicModule.setHidden(true); // Скрыт по умолчанию
      
      // Остальные модули
      this.register(new TotemTracker());
      this.register(new AutoSprint());
      this.register(new MenuModule());
      this.register(new Removals());
      this.register(new Ambience());
      this.register(new SwingAnimation());
      this.register(new ItemRadius());
      this.register(new TNTTimer());
      this.register(new ViewModel());
      this.register(new Interface());
      this.register(new HitEffect());
      this.register(new TargetESP());
      this.register(new CustomFog());
      this.register(new AspectRatio());
      this.register(new Zoom());
      this.register(new CustomHitBox());
      this.register(new moscow.mytheria.systems.modules.modules.visuals.CustomHotbar());
      this.register(new World());
      this.register(new KillEffects());
      this.register(new Prediction());
      this.register(new ShiftTap());
      this.register(new AutoInvisible());
      this.register(new MineHelper());
      this.register(new MiddleClick());
      this.register(new InvUtils());
      this.register(new AutoEat());
      this.register(new PlayerUtils());
      this.register(new ItemPickup());
      this.register(new ItemHighlighter());
      this.register(new ObjectInfo());
      this.register(new NameProtect());
      this.register(new ElytraUtils());
      this.register(new Freelook());
      this.register(new TapeMouse());
      this.register(new AutoAccept());
      this.register(new DeathCords());
      this.register(new AutoSwap());
      this.register(new AutoJoin());
      this.register(new Assist());
      this.register(new Sounds());
      this.register(new CoordInvite());
      this.register(new SpecBind());
      this.register(new BlockOverlay());
      this.register(new HitParticles());
      this.register(new ItemPhysics());
      this.register(new PVPAI());
   }

   @CompileBytecode
   public void enableModules() {
      for (Module module : this.modules) {
         if (module.getInfo().enabledByDefault()) {
            module.enable();
         }
      }

      Mytheria.LOGGER.info("Enabled default modules");
   }

   public void register(BaseModule module) {
      this.modules.add(module);
   }

   public <T extends Module> T getModule(String name) {
      return (T)this.modules
         .stream()
         .filter(module -> module.getName().replace(" ", "").equalsIgnoreCase(name) || module.getName().equalsIgnoreCase(name))
         .findFirst()
         .orElseThrow(() -> new UnknownModuleException(name));
   }

   public <T extends Module> T getModule(Class<T> clazz) {
      return (T)this.modules
         .stream()
         .filter(module -> module.getClass().equals(clazz))
         .findFirst()
         .orElseThrow(() -> new UnknownModuleException(clazz.getSimpleName()));
   }

   public <T extends Module> T getModuleSafe(Class<T> clazz) {
      return (T)this.modules.stream().filter(module -> module.getClass().equals(clazz)).findFirst().orElse(null);
   }

   public void disableAllModules() {
      for (Module module : this.modules) {
         if (module.isEnabled()) {
            module.disable();
         }
      }
   }

   @Generated
   public List<Module> getModules() {
      return this.modules;
   }

   @Generated
   public EventListener<ClientPlayerTickEvent> getTickListener() {
      return this.tickListener;
   }

   @Generated
   public EventListener<HudRenderEvent> getModuleWidgetRenderer() {
      return this.moduleWidgetRenderer;
   }

   @Generated
   public EventListener<KeyPressEvent> getOnKeyPress() {
      return this.onKeyPress;
   }

   @Generated
   public EventListener<MouseEvent> getOnMouseButtonPress() {
      return this.onMouseButtonPress;
   }
}
