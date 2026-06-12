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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import net.minecraft.world.BlockView;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Shadow
   private boolean thirdPerson;
   
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

   @Inject(
      method = {"update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"},
      at = {@At("TAIL")}
   )
   private void onUpdateTail(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
      // Оптимизация: проверяем instance один раз
      moscow.mytheria.systems.modules.modules.movement.CameraView cameraView = 
         moscow.mytheria.systems.modules.modules.movement.CameraView.instance;
      if (cameraView != null && cameraView.isActive()) {
         this.thirdPerson = true;
      }
   }

   @ModifyArgs(
      method = {"update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V"
      )
   )
   private void modifyPosition(Args args, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
      // Оптимизация: кэшируем instance
      moscow.mytheria.systems.modules.modules.movement.CameraView cameraView = 
         moscow.mytheria.systems.modules.modules.movement.CameraView.instance;
      if (cameraView != null && cameraView.isActive()) {
         // Используем интерполированную позицию для плавного движения
         args.set(0, cameraView.getCamX(tickDelta));
         args.set(1, cameraView.getCamY(tickDelta));
         args.set(2, cameraView.getCamZ(tickDelta));
      }
   }

   @ModifyArgs(
      method = {"update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V"
      )
   )
   private void modifyRotation(Args args, BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta) {
      // Оптимизация: проверяем CameraView первым (самый частый случай)
      moscow.mytheria.systems.modules.modules.movement.CameraView cameraView = 
         moscow.mytheria.systems.modules.modules.movement.CameraView.instance;
      if (cameraView != null && cameraView.isActive()) {
         // Используем интерполированные углы для плавного поворота
         args.set(0, cameraView.getCamYaw(tickDelta));
         args.set(1, cameraView.getCamPitch(tickDelta));
         return;
      }
      
      // Проверяем Freelook
      if (Freelook.isActive) {
         args.set(0, Freelook.x);
         args.set(1, Freelook.y);
         return;
      }
      
      // Проверяем Aura (самый редкий случай - проверяем последним)
      var aura = Mytheria.getInstance().getModuleManager().getModule("Aura");
      if (aura != null && aura.isEnabled()) {
         try {
            var method = aura.getClass().getMethod("shouldUseFreeCam");
            boolean shouldUseFreeCam = (boolean) method.invoke(aura);
            
            if (shouldUseFreeCam) {
               MinecraftClient mc = MinecraftClient.getInstance();
               if (mc.player != null && mc.player instanceof ICameraEntity cameraEntity) {
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
