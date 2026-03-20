package ru.friz1iks.mixin.player;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Пустой mixin для PlayerEntityRenderer.
 * Основная логика кастомных моделей в MixinEntityRendererCustomModels.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer {
    // Логика перенесена в MixinEntityRendererCustomModels
}
