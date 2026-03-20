package ru.friz1iks.mixin.player;

import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.friz1iks.AxienDLC;
import ru.friz1iks.events.Event;
import ru.friz1iks.events.impl.input.EventMouse;
import ru.friz1iks.manager.IMinecraft;
import ru.friz1iks.manager.Manager;

@Mixin(Mouse.class)
public class MixinMouse implements IMinecraft {
    @Inject(method = "onMouseButton", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;player:Lnet/minecraft/client/network/ClientPlayerEntity;", ordinal = 0))
    private void beforeSpectatorCheck(long window, int button, int action, int mods, CallbackInfo ci) {
        boolean bl = action == 1;
        if (!bl) return;
        AxienDLC main = new AxienDLC();
        main.keyPress(-100 + button);

        Event.call(new EventMouse(button));
    }
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        // Обработка кликов по Dynamic Island (ЛКМ)
        if (button == 0 && action == 1 && mc.currentScreen == null && mc.player != null) {
            var hud = Manager.FUNCTION_MANAGER.hud;
            if (hud != null && hud.state) {
                double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
                double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
                hud.handleDynamicIslandClick(mouseX, mouseY);
            }
        }
        
        if (mc.currentScreen instanceof ChatScreen) {
            Manager.DRAG_MANAGER.draggables.values().forEach(dragging -> {
                if (dragging.getModule() != null && dragging.getModule().state) {
                    dragging.onRelease(button);
                }
            });
        }

    }
}