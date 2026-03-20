package ru.friz1iks.mixin.attack;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.friz1iks.events.Event;
import ru.friz1iks.events.impl.player.EventAttack;
import ru.friz1iks.manager.Manager;
import ru.friz1iks.modules.combat.HitBox;
import ru.friz1iks.util.math.RayTraceUtil;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinAttackPlayer {

    @Unique
    private boolean hitboxRotated = false;

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    public void attackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        // NoFriendDamage check
        if (Manager.FUNCTION_MANAGER.noFriendDamage.state) {
            if (Manager.FRIEND_MANAGER.isFriend(target.getName().getString())) {
                ci.cancel();
                return;
            }
        }

        // HitBox silent aim - поворот перед атакой
        HitBox hitBox = Manager.FUNCTION_MANAGER.xbox;
        if (hitBox.state) {
            hitboxRotated = hitBox.processAttack(target);
        }
    }

    @Inject(method = "attackEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V", shift = At.Shift.AFTER, ordinal = 0))
    private void afterSendPacket(PlayerEntity player, Entity target, CallbackInfo ci) {
        Event.call(new EventAttack(player, target));
        RayTraceUtil.markHit(target);

        // HitBox - возврат ротации после удара
        if (hitboxRotated) {
            HitBox hitBox = Manager.FUNCTION_MANAGER.xbox;
            hitBox.sendRotationBack();
            hitboxRotated = false;
        }
    }
}
