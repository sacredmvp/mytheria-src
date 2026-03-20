package moscow.mytheria.mixin.minecraft.client.gui.overlay;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.modules.modules.visuals.CustomHotbar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HotbarMixin {
   
   @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
   private void hideHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      try {
         CustomHotbar customHotbar = Mytheria.getInstance().getModuleManager().getModule(CustomHotbar.class);
         if (customHotbar != null && customHotbar.isEnabled()) {
            ci.cancel();
         }
      } catch (Exception e) {
         // Игнорируем ошибки
      }
   }
}
