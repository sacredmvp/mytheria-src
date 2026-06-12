package moscow.mytheria.mixin.minecraft.client.network;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.impl.game.CloseScreenEvent;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEndEvent;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.event.impl.player.SlowDownEvent;
import moscow.mytheria.systems.modules.modules.player.Freelook;
import moscow.mytheria.systems.modules.modules.player.InvUtils;
import moscow.mytheria.utility.interfaces.IMinecraft;
import moscow.mytheria.utility.mixins.ClientPlayerEntityAddition;
import moscow.mytheria.utility.rotations.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerEntity.class})
public class ClientPlayerEntityMixin implements ClientPlayerEntityAddition, IMinecraft {
   @Unique
   private int groundTicks = 0;

   @Redirect(
      method = {"tickMovement()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
      ),
      require = 0
   )
   private boolean onIsUsingItemRedirect(ClientPlayerEntity player) {
      SlowDownEvent slowDownEvent = new SlowDownEvent();
      Mytheria.getInstance().getEventManager().triggerEvent(slowDownEvent);
      return player.isUsingItem() && player.getVehicle() == null && !slowDownEvent.isCancelled();
   }

   @WrapWithCondition(
      method = {"closeScreen()V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V"
      )}
   )
   private boolean preventCloseScreen(MinecraftClient instance, Screen screen) {
      Mytheria.getInstance().getEventManager().triggerEvent(new CloseScreenEvent(screen));
      return true;
   }

   @Inject(
      method = {"pushOutOfBlocks(DD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void removePushOutFromBlocks(double x, double z, CallbackInfo ci) {
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("HEAD")}
   )
   public void triggerTickEvent(CallbackInfo ci) {
      Mytheria.getInstance().getEventManager().triggerEvent(new ClientPlayerTickEvent());
      Freelook freelook = Mytheria.getInstance().getModuleManager().getModule(Freelook.class);
      if (freelook != null && freelook.isEnabled() && mc.player != null) {
      }
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("RETURN")}
   )
   public void triggerTickEndEvent(CallbackInfo ci) {
      Mytheria.getInstance().getEventManager().triggerEvent(new ClientPlayerTickEndEvent());
   }



   @Inject(
      method = {"tickMovement()V"},
      at = {@At("HEAD")}
   )
   public void updateOnGroundTicks(CallbackInfo ci) {
      if (mc.player != null && mc.player.isOnGround()) {
         this.groundTicks++;
      } else {
         this.groundTicks = 0;
      }
   }

   @Redirect(
      method = {"sendMovementPackets()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"
      )
   )
   public float replaceMovePacketYaw(ClientPlayerEntity instance) {
      RotationHandler rotationHandler = Mytheria.getInstance().getRotationHandler();
      float yaw = rotationHandler.isIdling() ? instance.getYaw() : rotationHandler.getCurrentRotation().getYaw();
      rotationHandler.getServerRotation().setYaw(yaw);
      return yaw;
   }

   @Redirect(
      method = {"sendMovementPackets()V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"
      )
   )
   public float replaceMovePacketPitch(ClientPlayerEntity instance) {
      RotationHandler rotationHandler = Mytheria.getInstance().getRotationHandler();
      float pitch = rotationHandler.isIdling() ? instance.getPitch() : rotationHandler.getCurrentRotation().getPitch();
      rotationHandler.getServerRotation().setYaw(pitch);
      return pitch;
   }

   @Inject(
      method = {"dropSelectedItem(Z)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
      InvUtils slotLock = Mytheria.getInstance().getModuleManager().getModuleSafe(InvUtils.class);
      if (slotLock != null && slotLock.isEnabled() && slotLock.getSlotLock().isSelected() && slotLock.isLocked(mc.player.getInventory().selectedSlot)) {
         cir.setReturnValue(false);
         cir.cancel();
      }
   }

   @Override
   public int Mytheria$getOnGroundTicks() {
      return this.groundTicks;
   }
}
