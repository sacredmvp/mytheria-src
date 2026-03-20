package moscow.mytheria.mixin.minecraft.render.entity;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.modules.modules.visuals.PlayerLabels;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererLabelMixin {
   
   @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
   private void hideLabel(EntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      try {
         PlayerLabels nameTagsModule = Mytheria.getInstance().getModuleManager().getModule(PlayerLabels.class);
         if (nameTagsModule != null && nameTagsModule.isEnabled()) {
            ci.cancel();
         }
      } catch (Exception e) {
         // Игнорируем ошибки
      }
   }
}
