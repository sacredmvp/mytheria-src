package moscow.mytheria.mixin.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import moscow.mytheria.Mytheria;
import moscow.mytheria.utility.interfaces.ICameraEntity;
import moscow.mytheria.utility.interfaces.IMinecraft;
import moscow.mytheria.utility.rotations.MoveCorrection;
import moscow.mytheria.utility.rotations.RotationHandler;
import moscow.mytheria.utility.rotations.RotationTask;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public class EntityMixin implements IMinecraft, ICameraEntity {
   @Shadow
   private Box boundingBox;
   
   @Unique
   private float cameraYaw = Float.NaN; // NaN = не инициализирована
   
   @Unique
   private float cameraPitch = Float.NaN; // NaN = не инициализирована
   
   @Unique
   private boolean cameraInitialized = false;

   @ModifyExpressionValue(
      method = {"move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"
      )}
   )
   public boolean fixFalldistanceValue(boolean original) {
      return (Object)this == mc.player ? false : original;
   }

   @Redirect(
      method = {"updateVelocity(FLnet/minecraft/util/math/Vec3d;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;getYaw()F"
      )
   )
   public float movementCorrection(Entity instance) {
      RotationHandler rotationHandler = Mytheria.INSTANCE.getRotationHandler();
      RotationTask currentTask = rotationHandler.getCurrentTask();
      return currentTask != null && currentTask.getMoveCorrection() != MoveCorrection.NONE && instance instanceof ClientPlayerEntity
         ? rotationHandler.getCurrentRotation().getYaw()
         : instance.getYaw();
   }
   
   /**
    * Перехватываем изменение направления взгляда для свободной камеры.
    * Свободная камера работает ТОЛЬКО когда есть цель.
    */
   @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
   private void onChangeLookDirection(double deltaX, double deltaY, CallbackInfo ci) {
      Entity self = (Entity)(Object)this;
      
      // Проверяем что это игрок
      if (self instanceof ClientPlayerEntity && self == mc.player) {
         var aura = Mytheria.getInstance().getModuleManager().getModule("Aura");
         if (aura != null && aura.isEnabled()) {
            try {
               // Проверяем что есть цель и ротация включена
               var method = aura.getClass().getMethod("shouldUseFreeCam");
               boolean shouldUseFreeCam = (boolean) method.invoke(aura);
               
               if (shouldUseFreeCam) {
                  // Инициализируем камеру если ещё не инициализирована
                  if (!cameraInitialized) {
                     cameraYaw = mc.player.getYaw();
                     cameraPitch = mc.player.getPitch();
                     cameraInitialized = true;
                  }
                  
                  // Применяем движение мыши к камере вместо игрока
                  cameraYaw += (float) deltaX * 0.15F;
                  cameraPitch = MathHelper.clamp(cameraPitch + (float) deltaY * 0.15F, -90.0F, 90.0F);
                  ci.cancel();
               } else {
                  // Сбрасываем инициализацию когда нет цели
                  if (cameraInitialized) {
                     cameraInitialized = false;
                  }
               }
            } catch (Exception e) {
               // Игнорируем ошибки
            }
         }
      }
   }
   
   @Override
   public float getCameraYaw() {
      if (!cameraInitialized && mc.player != null) {
         return mc.player.getYaw();
      }
      return cameraYaw;
   }
   
   @Override
   public float getCameraPitch() {
      if (!cameraInitialized && mc.player != null) {
         return mc.player.getPitch();
      }
      return cameraPitch;
   }
   
   @Override
   public void setCameraYaw(float yaw) {
      this.cameraYaw = yaw;
      this.cameraInitialized = true;
   }
   
   @Override
   public void setCameraPitch(float pitch) {
      this.cameraPitch = pitch;
      this.cameraInitialized = true;
   }
}
