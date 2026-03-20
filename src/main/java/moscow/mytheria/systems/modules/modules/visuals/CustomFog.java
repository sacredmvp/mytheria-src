package moscow.mytheria.systems.modules.modules.visuals;

import lombok.Generated;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.ColorSetting;
import moscow.mytheria.systems.setting.settings.RangeSetting;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.colors.Colors;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;

@ModuleInfo(
   name = "Custom Fog",
   category = ModuleCategory.VISUALS
)
public class CustomFog extends BaseModule {
   private final RangeSetting distance = new RangeSetting(this, "modules.settings.custom_fog.distance")
      .min(1.0F)
      .max(100.0F)
      .step(1.0F)
      .firstValue(10.0F)
      .secondValue(50.0F);
   private final BooleanSetting syncWithTheme = new BooleanSetting(this, "modules.settings.custom_fog.sync_with_theme").enabled(true);
   private final ColorSetting fogColor = new ColorSetting(this, "modules.settings.custom_fog.color", () -> this.syncWithTheme.isEnabled())
      .color(Colors.getAccentColor())
      .alpha(true);

   public boolean shouldModifyFog(Camera camera) {
      if (this.isEnabled() && mc.world != null && mc.player != null) {
         Entity entity = camera.getFocusedEntity();
         if (camera.getSubmersionType() == CameraSubmersionType.WATER) {
            return false;
         } else if (camera.getSubmersionType() == CameraSubmersionType.LAVA) {
            return false;
         } else if (camera.getSubmersionType() == CameraSubmersionType.POWDER_SNOW) {
            return false;
         } else {
            if (entity instanceof LivingEntity livingEntity) {
               if (livingEntity.hasStatusEffect(StatusEffects.BLINDNESS)) {
                  return false;
               }

               if (livingEntity.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   @Generated
   public RangeSetting getDistance() {
      return this.distance;
   }

   @Generated
   public ColorSetting getFogColor() {
      return this.fogColor;
   }

   public ColorRGBA getFogColorValue() {
      return this.syncWithTheme.isEnabled() ? Colors.getAccentColor() : this.fogColor.getColor();
   }
}
