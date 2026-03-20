package moscow.mytheria.protection.client;

import moscow.mytheria.Mytheria;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

public class MinecraftClientMixinProtection {
   @VMProtect(
      type = VMProtectType.MUTATION
   )
   public static void init() {
      Mytheria.INSTANCE.initialize();
   }

   @VMProtect(
      type = VMProtectType.MUTATION
   )
   public static void shutdown() {
      Mytheria.INSTANCE.shutdown();
   }

   public static void updateTitle(CallbackInfoReturnable<String> cir) {
      if (!Mytheria.INSTANCE.isPanic()) {
         String title = "%s %s %s".formatted("Mytheria", "2.0", "(Release)");
         cir.setReturnValue(title);
      }
   }
}
