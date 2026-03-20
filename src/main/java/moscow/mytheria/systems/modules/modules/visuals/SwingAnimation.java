package moscow.mytheria.systems.modules.modules.visuals;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.HandRenderEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.constructions.swinganim.SwingAnimScreen;
import moscow.mytheria.systems.modules.constructions.swinganim.SwingTransformations;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.ButtonSetting;
import net.minecraft.util.Arm;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

@ModuleInfo(
   name = "Swing Animation",
   category = ModuleCategory.VISUALS,
   desc = "Изменяет анимации рук при взмахе"
)
public class SwingAnimation extends BaseModule {
   private final ButtonSetting button = new ButtonSetting(this, "swing.open_menu").action(() -> mc.setScreen(new SwingAnimScreen()));
   private final EventListener<HandRenderEvent> onHandRender = event -> {
      if (this.isEnabled()) {
         if (event.getArm() == Arm.RIGHT) {
            ItemStack itemStack = event.getItemStack();
            if (!this.shouldApplyAnimation(itemStack)) {
               return;
            }

            MatrixStack matrices = event.getMatrices();
            float swingProgress = event.getSwingProgress();
            SwingTransformations trans = Mytheria.getInstance().getSwingManager().transformations(swingProgress);
            matrices.translate(trans.getAnchorX(), trans.getAnchorY(), trans.getAnchorZ());
            matrices.translate(trans.getMoveX(), trans.getMoveY(), trans.getMoveZ());
            matrices.multiply(
               new Quaternionf()
                  .rotationXYZ((float)Math.toRadians(trans.getRotateX()), (float)Math.toRadians(trans.getRotateY()), (float)Math.toRadians(trans.getRotateZ()))
            );
            matrices.translate(-trans.getAnchorX(), -trans.getAnchorY(), -trans.getAnchorZ());
            event.cancel();
         }
      }
   };

   public boolean shouldApplyAnimation(ItemStack itemStack) {
      Item item = itemStack.getItem();
      return item != Items.AIR
         && item != Items.FILLED_MAP
         && item != Items.CROSSBOW
         && item != Items.BOW
         && item != Items.TRIDENT
         && item.getUseAction(itemStack) != UseAction.DRINK
         && item.getUseAction(itemStack) != UseAction.EAT;
   }
}
