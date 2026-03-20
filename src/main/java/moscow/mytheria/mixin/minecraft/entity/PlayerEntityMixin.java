package moscow.mytheria.mixin.minecraft.entity;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.impl.game.AttackEvent;
import moscow.mytheria.systems.event.impl.game.PostAttackEvent;
import moscow.mytheria.utility.rotations.RotationHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerEntity.class})
public class PlayerEntityMixin {
   @Inject(
      method = {"attack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void attackAHook2(Entity target, CallbackInfo ci) {
      AttackEvent event = new AttackEvent(target);
      Mytheria.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"attack"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void attackAHook(Entity target, CallbackInfo ci) {
      PostAttackEvent event = new PostAttackEvent(target);
      Mytheria.getInstance().getEventManager().triggerEvent(event);
   }

   @Inject(
      method = {"isPushedByFluids"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void removePushFromFluids(CallbackInfoReturnable<Boolean> cir) {
   }

   @Redirect(
      method = {"travel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
      )
   )
   private Vec3d redirectGetRotationVectorInTravel(PlayerEntity instance) {
      RotationHandler rotationHandler = Mytheria.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getRotationVector() : rotationHandler.getCurrentRotation().getRotationVector();
   }
}
