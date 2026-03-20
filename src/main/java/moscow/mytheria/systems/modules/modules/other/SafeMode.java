package moscow.mytheria.systems.modules.modules.other;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.modules.Module;
import moscow.mytheria.systems.modules.ModuleManager;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.StringSetting;

@ModuleInfo(
   name = "Panic",
   category = ModuleCategory.OTHER,
   desc = "modules.descriptions.panic"
)
public class SafeMode extends BaseModule {
   private final StringSetting secretWord = new StringSetting(this, "modules.settings.panic.secret").text("restore");
   private final StringSetting unlockWord = new StringSetting(this, "modules.settings.panic.unlock").text("zxc");
   
   private boolean auraWasEnabled = false;
   private boolean espWasEnabled = false;
   private boolean nameTagsWasEnabled = false;
   private boolean triggerBotWasEnabled = false;
   private boolean aimAssistWasEnabled = false;
   private boolean panicActivated = false;
   private static boolean modulesUnlocked = false;

   @Override
   public void onEnable() {
      ModuleManager moduleManager = Mytheria.getInstance().getModuleManager();
      
      // Сохраняем и выключаем Aura
      Module auraModule = moduleManager.getModule("Aura");
      if (auraModule != null) {
         auraWasEnabled = auraModule.isEnabled();
         if (auraWasEnabled) {
            auraModule.toggle();
         }
         
         // Скрываем Aura
         if (auraModule instanceof BaseModule baseAura) {
            baseAura.setHidden(true);
         }
      }
      
      // Сохраняем и выключаем ESP
      Module espModule = moduleManager.getModule("ESP");
      if (espModule != null) {
         espWasEnabled = espModule.isEnabled();
         if (espWasEnabled) {
            espModule.toggle();
         }
         
         // Скрываем ESP
         if (espModule instanceof BaseModule baseEsp) {
            baseEsp.setHidden(true);
         }
      }
      
      // Сохраняем и выключаем NameTags
      Module nameTagsModule = moduleManager.getModule("NameTags");
      if (nameTagsModule != null) {
         nameTagsWasEnabled = nameTagsModule.isEnabled();
         if (nameTagsWasEnabled) {
            nameTagsModule.toggle();
         }
         
         // Скрываем NameTags
         if (nameTagsModule instanceof BaseModule baseNameTags) {
            baseNameTags.setHidden(true);
         }
      }
      
      // Сохраняем и выключаем TriggerBot
      Module triggerBotModule = moduleManager.getModule("TriggerBot");
      if (triggerBotModule != null) {
         triggerBotWasEnabled = triggerBotModule.isEnabled();
         if (triggerBotWasEnabled) {
            triggerBotModule.toggle();
         }
         
         // Скрываем TriggerBot
         if (triggerBotModule instanceof BaseModule baseTriggerBot) {
            baseTriggerBot.setHidden(true);
         }
      }
      
      // Сохраняем и выключаем AimAssist
      Module aimAssistModule = moduleManager.getModule("AimAssist");
      if (aimAssistModule != null) {
         aimAssistWasEnabled = aimAssistModule.isEnabled();
         if (aimAssistWasEnabled) {
            aimAssistModule.toggle();
         }
         
         // Скрываем AimAssist
         if (aimAssistModule instanceof BaseModule baseAimAssist) {
            baseAimAssist.setHidden(true);
         }
      }
      
      panicActivated = true;
      modulesUnlocked = false;
      
      // Скрываем Panic
      this.setHidden(true);
   }

   @Override
   public void onDisable() {
      restoreModules();
   }
   
   public void restoreModules() {
      if (!panicActivated) return;
      
      ModuleManager moduleManager = Mytheria.getInstance().getModuleManager();
      
      // Восстанавливаем Aura
      Module auraModule = moduleManager.getModule("Aura");
      if (auraModule != null) {
         if (auraModule instanceof BaseModule baseAura) {
            baseAura.setHidden(false);
         }
         if (auraWasEnabled && !auraModule.isEnabled()) {
            auraModule.toggle();
         }
      }
      
      // Восстанавливаем ESP
      Module espModule = moduleManager.getModule("ESP");
      if (espModule != null) {
         if (espModule instanceof BaseModule baseEsp) {
            baseEsp.setHidden(false);
         }
         if (espWasEnabled && !espModule.isEnabled()) {
            espModule.toggle();
         }
      }
      
      // Восстанавливаем NameTags
      Module nameTagsModule = moduleManager.getModule("NameTags");
      if (nameTagsModule != null) {
         if (nameTagsModule instanceof BaseModule baseNameTags) {
            baseNameTags.setHidden(false);
         }
         if (nameTagsWasEnabled && !nameTagsModule.isEnabled()) {
            nameTagsModule.toggle();
         }
      }
      
      // Восстанавливаем TriggerBot
      Module triggerBotModule = moduleManager.getModule("TriggerBot");
      if (triggerBotModule != null) {
         if (triggerBotModule instanceof BaseModule baseTriggerBot) {
            baseTriggerBot.setHidden(false);
         }
         if (triggerBotWasEnabled && !triggerBotModule.isEnabled()) {
            triggerBotModule.toggle();
         }
      }
      
      // Восстанавливаем AimAssist
      Module aimAssistModule = moduleManager.getModule("AimAssist");
      if (aimAssistModule != null) {
         if (aimAssistModule instanceof BaseModule baseAimAssist) {
            baseAimAssist.setHidden(false);
         }
         if (aimAssistWasEnabled && !aimAssistModule.isEnabled()) {
            aimAssistModule.toggle();
         }
      }
      
      // Показываем Panic
      this.setHidden(false);
      
      panicActivated = false;
      
      // Выключаем Panic
      if (this.isEnabled()) {
         this.toggle();
      }
   }
   
   public String getSecretWord() {
      return secretWord.getText();
   }
   
   public String getUnlockWord() {
      return unlockWord.getText();
   }
   
   public boolean isPanicActivated() {
      return panicActivated;
   }
   
   public static boolean areModulesUnlocked() {
      return modulesUnlocked;
   }
   
   public static void unlockModules() {
      modulesUnlocked = true;
      
      // Показываем скрытые модули
      ModuleManager moduleManager = Mytheria.getInstance().getModuleManager();
      
      Module auraModule = moduleManager.getModule("Aura");
      if (auraModule instanceof BaseModule baseAura) {
         baseAura.setHidden(false);
      }
      
      Module espModule = moduleManager.getModule("ESP");
      if (espModule instanceof BaseModule baseEsp) {
         baseEsp.setHidden(false);
      }
      
      Module nameTagsModule = moduleManager.getModule("NameTags");
      if (nameTagsModule instanceof BaseModule baseNameTags) {
         baseNameTags.setHidden(false);
      }
      
      Module triggerBotModule = moduleManager.getModule("TriggerBot");
      if (triggerBotModule instanceof BaseModule baseTriggerBot) {
         baseTriggerBot.setHidden(false);
      }
      
      Module aimAssistModule = moduleManager.getModule("AimAssist");
      if (aimAssistModule instanceof BaseModule baseAimAssist) {
         baseAimAssist.setHidden(false);
      }
      
      Module panicModule = moduleManager.getModule("Panic");
      if (panicModule instanceof BaseModule basePanic) {
         basePanic.setHidden(false);
      }
   }
}
