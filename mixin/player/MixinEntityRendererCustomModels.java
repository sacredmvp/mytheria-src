package ru.friz1iks.mixin.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.friz1iks.manager.Manager;
import ru.friz1iks.models.CustomModelRenderer;
import ru.friz1iks.modules.render.CustomModels;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinEntityRendererCustomModels<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Unique
    private static CustomModelRenderer customModelRenderer;

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", 
            at = @At("HEAD"), cancellable = true)
    private void renderCustomModel(S state, MatrixStack matrices, 
                                   VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }
        
        CustomModels customModels = Manager.FUNCTION_MANAGER.customModels;
        if (customModels == null || !customModels.state) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        
        String playerName = playerState.name;
        boolean isSelf = mc.player != null && mc.player.getName().getString().equals(playerName);
        boolean isFriend = Manager.FRIEND_MANAGER.isFriend(playerName);

        boolean shouldRenderCustom;
        if (customModels.onlySelf.get()) {
            shouldRenderCustom = isSelf || (isFriend && customModels.friends.get());
        } else {
            shouldRenderCustom = true;
        }

        if (!shouldRenderCustom) {
            return;
        }

        if (customModelRenderer == null) {
            customModelRenderer = new CustomModelRenderer();
        }

        String preset = customModels.sizePreset.get();
        float extraScaleX = customModels.getScaleX();
        float extraScaleY = customModels.getScaleY();
        float extraScaleZ = customModels.getScaleZ();
        
        // Пресеты размеров
        // Обычный = 2 блока, Маленький = 1 блок, Большой = 3 блока
        float presetScaleX = 1.0f;
        float presetScaleY = 1.0f;
        float presetScaleZ = 1.0f;
        
        switch (preset) {
            case "Обычный" -> {
                presetScaleX = 1.0f;
                presetScaleY = 1.0f;
                presetScaleZ = 1.0f;
            }
            case "Маленький" -> {
                presetScaleX = 0.5f;
                presetScaleY = 0.5f;
                presetScaleZ = 0.5f;
            }
            case "Большой" -> {
                presetScaleX = 1.5f;
                presetScaleY = 1.5f;
                presetScaleZ = 1.5f;
            }
            case "Широкий" -> {
                presetScaleX = 2.0f;
                presetScaleY = 1.0f;
                presetScaleZ = 2.0f;
            }
            case "Высокий" -> {
                presetScaleX = 0.7f;
                presetScaleY = 2.0f;
                presetScaleZ = 0.7f;
            }
        }
        
        float finalScaleX = presetScaleX * extraScaleX;
        float finalScaleY = presetScaleY * extraScaleY;
        float finalScaleZ = presetScaleZ * extraScaleZ;

        matrices.push();
        
        // Поворот тела по yaw (модель смотрит в направлении тела)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - playerState.bodyYaw));
        
        // Разворот на 180 если включено
        if (customModels.reversed.get()) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
        }

        // Применяем масштаб
        matrices.scale(finalScaleX, finalScaleY, finalScaleZ);

        String mode = customModels.mode.get();
        
        // Рендерим кастомную модель
        customModelRenderer.render(playerState, matrices, vertexConsumers, light, mode, 
                customModels.friendHighlight.get() && isFriend);

        matrices.pop();
        
        ci.cancel();
    }
}
