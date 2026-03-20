package moscow.mytheria.mixin.minecraft.client.gui.screen;

import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.CustomDrawContext;
import moscow.mytheria.systems.event.impl.render.ChatRenderEvent;
import moscow.mytheria.systems.event.impl.window.ChatClickEvent;
import moscow.mytheria.systems.event.impl.window.ChatReleaseEvent;
import moscow.mytheria.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ChatScreen.class})
public class ChatScreenMixin extends Screen implements IMinecraft {
   @Shadow
   protected TextFieldWidget chatField;
   @Shadow
   private ChatInputSuggestor chatInputSuggestor;

   protected ChatScreenMixin(Text title) {
      super(title);
   }

   @Inject(
      method = {"sendMessage(Ljava/lang/String;Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendMessage(String text, boolean addToHistory, CallbackInfo ci) {
      // Проверяем секретное слово для разблокировки модулей
      moscow.mytheria.systems.modules.Module panicModule = Mytheria.getInstance().getModuleManager().getModule("Panic");
      if (panicModule instanceof moscow.mytheria.systems.modules.modules.other.SafeMode safeMode) {
         // Разблокировка модулей
         if (!moscow.mytheria.systems.modules.modules.other.SafeMode.areModulesUnlocked() 
             && text.trim().equalsIgnoreCase(safeMode.getUnlockWord())) {
            moscow.mytheria.systems.modules.modules.other.SafeMode.unlockModules();
            mc.inGameHud.getChatHud().addToMessageHistory(text);
            ci.cancel();
            return;
         }
         
         // Восстановление после паники
         if (safeMode.isPanicActivated() && text.trim().equalsIgnoreCase(safeMode.getSecretWord())) {
            safeMode.restoreModules();
            mc.inGameHud.getChatHud().addToMessageHistory(text);
            ci.cancel();
            return;
         }
      }
      
      if (Mytheria.getInstance().getCommandManager().dispatch(text)) {
         mc.inGameHud.getChatHud().addToMessageHistory(text);
         ci.cancel();
      }
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"},
      at = {@At("RETURN")}
   )
   public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      Mytheria.getInstance().getEventManager().triggerEvent(new ChatRenderEvent(CustomDrawContext.of(context), delta));
   }

   @Inject(
      method = {"mouseClicked(DDI)Z"},
      at = {@At("HEAD")}
   )
   private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      Mytheria.getInstance().getEventManager().triggerEvent(new ChatClickEvent((float)mouseX, (float)mouseY, button));
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      Mytheria.getInstance().getEventManager().triggerEvent(new ChatReleaseEvent((float)mouseX, (float)mouseY, button));
      return super.mouseReleased(mouseX, mouseY, button);
   }
}
