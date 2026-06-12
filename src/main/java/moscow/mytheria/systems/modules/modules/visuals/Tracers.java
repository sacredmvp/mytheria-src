package moscow.mytheria.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.Render3DEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.SettingsContainer;
import moscow.mytheria.systems.setting.settings.SelectSetting;
import moscow.mytheria.systems.target.TargetSettings;
import moscow.mytheria.utility.colors.Colors;
import moscow.mytheria.utility.render.Draw3DUtility;
import moscow.mytheria.utility.render.RenderUtility;
import moscow.mytheria.utility.render.Utils;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Tracers",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.tracers"
)
public class Tracers extends BaseModule {
   private final SelectSetting targets = new SelectSetting(
      (SettingsContainer) this,
      "modules.settings.tracers.targets",
      "modules.settings.tracers.targets.description"
   );
   
   private final SelectSetting.Value players = new SelectSetting.Value(this.targets, "modules.settings.tracers.targets.players").select();
   private final SelectSetting.Value animals = new SelectSetting.Value(this.targets, "modules.settings.tracers.targets.animals");
   private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets, "modules.settings.tracers.targets.mobs");
   private final SelectSetting.Value invisibles = new SelectSetting.Value(this.targets, "modules.settings.tracers.targets.invisibles").select();
   private final SelectSetting.Value nakedPlayers = new SelectSetting.Value(this.targets, "modules.settings.tracers.targets.naked_players").select();
   private final SelectSetting.Value friends = new SelectSetting.Value(this.targets, "modules.settings.tracers.targets.friends").select();

   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (mc.player == null || mc.world == null) {
         return;
      }

      try {
         MatrixStack matrices = event.getMatrices();
         float tickDelta = event.getTickDelta();
         Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
         
         TargetSettings targetSettings = new TargetSettings.Builder()
            .targetPlayers(this.players.isSelected())
            .targetAnimals(this.animals.isSelected())
            .targetInvisibles(this.invisibles.isSelected())
            .targetFriends(this.friends.isSelected())
            .targetNakedPlayers(this.nakedPlayers.isSelected())
            .targetMobs(this.mobs.isSelected())
            .build();

         matrices.push();
         RenderUtility.setupRender3D(true);
         RenderUtility.prepareMatrices(matrices);
         RenderSystem.enableDepthTest();
         RenderSystem.lineWidth(1.5F);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         
         // Начальная точка - глаза игрока
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d startPos = new Vec3d(
            eyePos.x - cameraPos.x,
            eyePos.y - cameraPos.y,
            eyePos.z - cameraPos.z
         );
         
         int count = 0;
         for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            
            LivingEntity livingEntity = (LivingEntity) entity;
            if (!targetSettings.isEntityValid(entity)) continue;

            count++;
            
            Vec3d entityPos = Utils.getInterpolatedPos(livingEntity, tickDelta);
            double targetY = entityPos.y + livingEntity.getHeight() / 2.0;
            
            // Конечная точка - центр сущности относительно камеры
            Vec3d endPos = new Vec3d(
               entityPos.x - cameraPos.x,
               targetY - cameraPos.y,
               entityPos.z - cameraPos.z
            );
            
            Draw3DUtility.drawLine(matrices, builder, startPos, endPos, Colors.WHITE);
         }
         
         if (count > 0) {
            RenderUtility.buildBuffer(builder);
         }
         
         RenderUtility.endRender3D();
         matrices.pop();
         
      } catch (Exception e) {
         e.printStackTrace();
      }
   };
}
