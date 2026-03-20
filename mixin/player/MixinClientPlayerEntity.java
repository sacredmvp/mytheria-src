package ru.friz1iks.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.friz1iks.events.Event;
import ru.friz1iks.events.impl.move.EventMotion;
import ru.friz1iks.events.impl.EventUpdate;
import ru.friz1iks.events.impl.move.EventNoSlow;
import ru.friz1iks.events.impl.player.EventSprint;
import ru.friz1iks.manager.IMinecraft;
import ru.friz1iks.manager.Manager;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity implements IMinecraft {

    @Shadow
    public abstract void move(MovementType type, Vec3d movement);

    @Unique
    private float preYaw;
    @Unique
    private float prePitch;
    @Unique
    private float packetYaw;
    @Unique
    private float packetPitch;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        Event.call(new EventUpdate());
        preYaw = mc.player.getYaw();
        prePitch = mc.player.getPitch();

    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void onSendMovementPacketsHead(CallbackInfo ci) {
        EventMotion event = new EventMotion( mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround());
        Event.call(event);

        if (event.isCancel()) {
            ci.cancel();
            return;
        }
        mc.player.setYaw(event.getYaw());
        mc.player.setPitch(event.getPitch());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendMovementPackets()V", shift = At.Shift.AFTER))
    private void afterSendMovementPackets(CallbackInfo ci) {
        packetYaw = mc.player.getYaw();
        packetPitch = mc.player.getPitch();
        mc.player.setYaw(preYaw);
        mc.player.setPitch(prePitch);
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocksHook(double x, double d, CallbackInfo ci) {
        if (Manager.FUNCTION_MANAGER.noPush.state && Manager.FUNCTION_MANAGER.noPush.mods.get("Блоки")) {
            ci.cancel();
        }
    }

    @Unique
    private EventNoSlow getNoSlowEvent() {
        EventNoSlow eventNoSlow = new EventNoSlow();
        Event.call(eventNoSlow);
        return eventNoSlow;
    }

    @Redirect(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/Input;movementSideways:F", opcode = Opcodes.PUTFIELD, ordinal = 0))
    private void redirectMovementSideways(Input input, float value) {
        EventNoSlow event = getNoSlowEvent();
        if (event.isCancel()) {
            // Применяем множитель скорости вместо полного замедления
            float multiplier = event.getSlowdownMultiplier();
            if (multiplier >= 1.0f) {
                // Полная скорость - не применяем замедление
                return;
            }
            // Частичное замедление
            input.movementSideways = input.movementSideways * multiplier;
        } else {
            input.movementSideways = value;
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/Input;movementForward:F", opcode = Opcodes.PUTFIELD, ordinal = 0))
    private void redirectMovementForward(Input input, float value) {
        EventNoSlow event = getNoSlowEvent();
        if (event.isCancel()) {
            // Применяем множитель скорости вместо полного замедления
            float multiplier = event.getSlowdownMultiplier();
            if (multiplier >= 1.0f) {
                // Полная скорость - не применяем замедление
                return;
            }
            // Частичное замедление
            input.movementForward = input.movementForward * multiplier;
        } else {
            input.movementForward = value;
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setSprinting(Z)V", ordinal = 0))
    private void redirectSetSprinting(ClientPlayerEntity player, boolean sprinting) {
        EventNoSlow event = getNoSlowEvent();
        // Если NoSlow отменён ИЛИ спринт разрешён - не сбрасываем спринт
        if (event.isCancel() || event.isSprintAllowed()) {
            // Не вызываем setSprinting(false) - сохраняем спринт
            return;
        }
        player.setSprinting(sprinting);
    }
    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean hookSprintStart(boolean original) {
        var event = new EventSprint(original);
        return event.isSprinting();
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;canSprint()Z"))
    private boolean hookSprintStop(boolean original) {
        var event = new EventSprint(original);
        Event.call(event);
        return event.isSprinting();
    }
}
