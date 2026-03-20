package moscow.mytheria.systems.setting.impl;

import java.util.function.BooleanSupplier;
import lombok.Generated;
import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.config.ConfigFile;
import moscow.mytheria.systems.setting.Setting;
import moscow.mytheria.systems.setting.SettingsContainer;
import moscow.mytheria.ui.hud.HudElement;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSetting implements Setting {
   private final String name;
   @NotNull
   private final BooleanSupplier hideCondition;
   @NotNull
   private final SettingsContainer parent;

   public AbstractSetting(@NotNull SettingsContainer parent, String name, @NotNull BooleanSupplier hideCondition) {
      this.name = name;
      this.hideCondition = hideCondition;
      this.parent = parent;
      this.register(parent);
   }

   public AbstractSetting(@NotNull SettingsContainer parent, String name) {
      this(parent, name, () -> false);
   }

   @Override
   public void register(SettingsContainer parent) {
      parent.getSettings().add(this);
   }

   @Override
   public String getDescription() {
      return this.getName() + ".description";
   }

   @Generated
   @Override
   public String getName() {
      return this.name;
   }

   @NotNull
   @Generated
   @Override
   public BooleanSupplier getHideCondition() {
      return this.hideCondition;
   }

   @NotNull
   @Generated
   public SettingsContainer getParent() {
      return this.parent;
   }

   protected void autoSaveConfig() {
      try {
         Mytheria mytheria = Mytheria.getInstance();
         if (mytheria == null || mytheria.getConfigManager() == null || !mytheria.getConfigManager().isInitialized()) {
            return;
         }

         if (this.parent instanceof HudElement) {
            if (mytheria.getFileManager() != null) {
               mytheria.getFileManager().writeFile("client");
            }
         } else {
            ConfigFile autosaveConfig = mytheria.getConfigManager().getAutoSaveConfig();
            if (autosaveConfig != null) {
               Mytheria.LOGGER.info("Auto-saving config after setting '{}' change", this.name);
               autosaveConfig.save();
            }
         }
      } catch (Exception var3) {
         Mytheria.LOGGER.error("Error during auto-save for setting '{}': {}", this.name, var3.getMessage());
      }
   }
}
