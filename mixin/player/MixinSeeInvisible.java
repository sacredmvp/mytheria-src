package ru.friz1iks.mixin.player;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.friz1iks.manager.Manager;
import ru.friz1iks.modules.render.SeeInvisible;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinSeeInvisible<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    private void onRenderPre(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SeeInvisible seeInvisible = Manager.FUNCTION_MANAGER.seeInvisible;
        if (seeInvisible == null || !seeInvisible.state) return;

        // Проверяем, невидима ли сущность
        if (state.invisible) {
            // Если включена опция "только игроки", проверяем тип
            if (seeInvisible.isOnlyPlayers() && !(state instanceof PlayerEntityRenderState)) {
                return;
            }

            // Устанавливаем прозрачность для рендера
            float alpha = seeInvisible.getAlpha();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    private void onRenderPost(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        SeeInvisible seeInvisible = Manager.FUNCTION_MANAGER.seeInvisible;
        if (seeInvisible == null || !seeInvisible.state) return;

        // Восстанавливаем цвет после рендера невидимой сущности
        if (state.invisible) {
            if (seeInvisible.isOnlyPlayers() && !(state instanceof PlayerEntityRenderState)) {
                return;
            }

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
    }
}
