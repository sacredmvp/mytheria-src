package moscow.mytheria.systems.modules.modules.visuals;

import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.Render3DEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.ColorSetting;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.colors.Colors;
import moscow.mytheria.utility.render.Chinahat;
import moscow.mytheria.utility.render.RenderUtility;

@ModuleInfo(
   name = "Chinahat",
   desc = "modules.descriptions.chinahat",
   category = ModuleCategory.VISUALS
)
public class ChinahatModule extends BaseModule {
   private final SliderSetting heightOffset = new SliderSetting(this, "modules.settings.chinahat.height_offset")
      .min(-0.5F)
      .max(0.5F)
      .step(0.05F)
      .currentValue(0.1F);
   private final SliderSetting width = new SliderSetting(this, "modules.settings.chinahat.width").min(0.2F).max(3.0F).step(0.1F).currentValue(0.7F);
   private final SliderSetting height = new SliderSetting(this, "modules.settings.chinahat.height").min(0.05F).max(0.8F).step(0.05F).currentValue(0.25F);
   private final BooleanSetting syncWithTheme = new BooleanSetting(this, "modules.settings.chinahat.sync_with_theme").enabled(true);
   private final ColorSetting color = new ColorSetting(this, "modules.settings.chinahat.color", () -> this.syncWithTheme.isEnabled())
      .color(new ColorRGBA(100.0F, 150.0F, 200.0F, 255.0F))
      .alpha(true);
   private final SliderSetting alpha = new SliderSetting(this, "modules.settings.chinahat.alpha").min(0.0F).max(1.0F).step(0.05F).currentValue(0.85F);
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (mc.world != null && mc.player != null) {
         RenderUtility.setupRender3D(true);
         Chinahat.render(
            mc.player,
            event.getMatrices(),
            mc.gameRenderer.getCamera(),
            event.getTickDelta(),
            this.width.getCurrentValue(),
            this.height.getCurrentValue(),
            this.syncWithTheme.isEnabled() ? Colors.getAccentColor() : this.color.getColor(),
            0.3F + this.alpha.getCurrentValue() * 0.7F,
            true,
            this.heightOffset.getCurrentValue()
         );
         RenderUtility.endRender3D();
      }
   };

   public SliderSetting getWidth() {
      return this.width;
   }

   public SliderSetting getHeight() {
      return this.height;
   }

   public ColorSetting getColor() {
      return this.color;
   }

   public SliderSetting getAlpha() {
      return this.alpha;
   }

   public SliderSetting getHeightOffset() {
      return this.heightOffset;
   }
}
