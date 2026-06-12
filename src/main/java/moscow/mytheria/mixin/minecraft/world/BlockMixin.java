package moscow.mytheria.mixin.minecraft.world;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.modules.modules.visuals.OreScanner;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {
   @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
   private static void mytheria$onShouldDrawSide(BlockState state, BlockState stateFrom, Direction side, CallbackInfoReturnable<Boolean> cir) {
      try {
         OreScanner xray = Mytheria.getInstance().getModuleManager().getModuleSafe(OreScanner.class);
         if (xray != null && xray.isEnabled()) {
            if (xray.isBlockEnabled(state.getBlock())) {
               cir.setReturnValue(true);
            } else {
               cir.setReturnValue(false);
            }
         }
      } catch (Throwable ignored) {
      }
   }
}
