package moscow.mytheria.mixin.minecraft.client.gui.overlay;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.modules.modules.player.Freelook;
import moscow.mytheria.systems.modules.modules.visuals.Removals;
import moscow.mytheria.utility.interfaces.ICameraEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Inject(
      method = {"getSubmersionType()Lnet/minecraft/block/enums/CameraSubmersionType;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getSubmergedFluidState(CallbackInfoReturnable<CameraSubmersionType> ci) {
      Removals removals = Mytheria.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getWater().isSelected()) {
         ci.setReturnValue(CameraSubmersionType.NONE);
      }
   }

   @ModifyArgs(
      method = {"update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
      )
   )
   private void modifyRotation(Args args) {
      // Проверяем Freelook
      if (Freelook.isActive) {
         args.set(0, Freelook.x);
         args.set(1, Freelook.y);
         return;
      }
      
      // Проверяем Aura - свободная камера работает ТОЛЬКО когда есть цель
      var aura = Mytheria.getInstance().getModuleManager().getModule("Aura");
      if (aura != null && aura.isEnabled()) {
         try {
            // Проверяем что есть цель
            var method = aura.getClass().getMethod("shouldUseFreeCam");
            boolean shouldUseFreeCam = (boolean) method.invoke(aura);
            
            if (shouldUseFreeCam) {
               MinecraftClient mc = MinecraftClient.getInstance();
               if (mc.player != null && mc.player instanceof ICameraEntity cameraEntity) {
                  // Используем углы свободной камеры вместо углов игрока
                  args.set(0, cameraEntity.getCameraYaw());
                  args.set(1, cameraEntity.getCameraPitch());
               }
            }
         } catch (Exception e) {
            // Игнорируем ошибки
         }
      }
   }
}
