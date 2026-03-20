package ru.friz1iks.mixin.display;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.friz1iks.manager.HudManager;

@Mixin(InGameHud.class)
public class MixinScoreboard {
    
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", 
            at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        // Отменяем ванильный рендер если включен кастомный scoreboard
        if (HudManager.scoreboardEnabled) {
            ci.cancel();
        }
    }
}
