package moscow.mytheria.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.CustomDrawContext;
import moscow.mytheria.framework.msdf.Font;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.msdf.MsdfRenderer;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.HudRenderEvent;
import moscow.mytheria.systems.event.impl.render.Render3DEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.SettingsContainer;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.SelectSetting;
import moscow.mytheria.systems.target.TargetSettings;
import moscow.mytheria.utility.animation.base.Animation;
import moscow.mytheria.utility.animation.base.Easing;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.colors.Colors;
import moscow.mytheria.utility.render.Draw3DUtility;
import moscow.mytheria.utility.render.DrawUtility;
import moscow.mytheria.utility.render.RenderUtility;
import moscow.mytheria.utility.render.Utils;
import moscow.mytheria.utility.render.batching.impl.IconBatching;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@ModuleInfo(
   name = "Arrows",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.arrows"
)
public class Arrows extends BaseModule {
   private final BooleanSetting lines = new BooleanSetting(this, "modules.settings.arrows.lines").enabled(false);
   
   private final SelectSetting targets = new SelectSetting(
      (SettingsContainer) this,
      "modules.settings.arrows.targets",
      "modules.settings.arrows.targets.description"
   );
   
   private final SelectSetting.Value players = new SelectSetting.Value(this.targets, "modules.settings.arrows.targets.players").select();
   private final SelectSetting.Value animals = new SelectSetting.Value(this.targets, "modules.settings.arrows.targets.animals");
   private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets, "modules.settings.arrows.targets.mobs");
   private final SelectSetting.Value villagers = new SelectSetting.Value(this.targets, "modules.settings.arrows.targets.villagers");
   private final SelectSetting.Value invisibles = new SelectSetting.Value(this.targets, "modules.settings.arrows.targets.invisibles").select();
   private final SelectSetting.Value nakedPlayers = new SelectSetting.Value(this.targets, "modules.settings.arrows.targets.naked_players").select();
   private final SelectSetting.Value friends = new SelectSetting.Value(this.targets, "modules.settings.arrows.targets.friends").select();
   
   private final Map<Entity, ArrowsAnimation> animations = new HashMap<>();

   private final EventListener<HudRenderEvent> onHud = event -> {
      if (mc.player == null || mc.world == null || this.lines.isEnabled()) {
         return;
      }

      CustomDrawContext context = event.getContext();
      MatrixStack ms = context.getMatrices();
      
      TargetSettings targetSettings = new TargetSettings.Builder()
         .targetPlayers(this.players.isSelected())
         .targetAnimals(this.animals.isSelected())
         .targetInvisibles(this.invisibles.isSelected())
         .targetFriends(this.friends.isSelected())
         .targetNakedPlayers(this.nakedPlayers.isSelected())
         .targetMobs(this.mobs.isSelected())
         .build();

      HashSet<Entity> toRemove = new HashSet<>();
      
      for (Map.Entry<Entity, ArrowsAnimation> entry : this.animations.entrySet()) {
         Entity entity = entry.getKey();
         ArrowsAnimation animation = entry.getValue();
         
         boolean isVillager = entity instanceof VillagerEntity && this.villagers.isSelected();
         boolean shouldShow = mc.world.hasEntity(entity) && 
            entity instanceof LivingEntity && 
            (isVillager || targetSettings.isEntityValid(entity));
         
         animation.showing.update(shouldShow);
         animation.showing.setDuration(500L);
         
         if (animation.showing.getValue() == 0.0f && !shouldShow) {
            toRemove.add(entity);
         }
      }

      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof LivingEntity)) continue;
         
         boolean isVillager = entity instanceof VillagerEntity && this.villagers.isSelected();
         if (!isVillager && !targetSettings.isEntityValid(entity)) continue;
         if (this.animations.containsKey(entity)) continue;
         
         this.animations.put(entity, new ArrowsAnimation());
      }

      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
      RenderSystem.disableCull();
      
      ms.push();
      IconBatching iconBatching = new IconBatching(VertexFormats.POSITION_TEXTURE_COLOR, context.getMatrices());
      ms.translate(sr.getScaledWidth() / 2.0f, sr.getScaledHeight() / 2.0f, 0.0f);
      
      Font font = Fonts.MEDIUM.getFont(14.0F);

      for (Map.Entry<Entity, ArrowsAnimation> arrow : this.animations.entrySet()) {
         if (arrow.getValue().showing.getValue() <= 0.0f) continue;
         
         Entity entity = arrow.getKey();
         float angle = this.calculateAngle(entity, event.getTickDelta());
         
         ms.push();
         ms.translate(0.0f, 0.0f, 0.0f);
         RenderUtility.rotate(ms, 0.0f, 0.0f, angle);
         RenderUtility.scale(ms, 0.0f, 0.0f, 2.0f - arrow.getValue().showing.getValue());
         
         boolean isFriend = Mytheria.getInstance().getFriendManager().isFriend(entity.getName().getString());
         boolean isVillager = entity instanceof VillagerEntity;
         
         ColorRGBA arrowColor;
         if (isVillager) {
            arrowColor = Colors.RED;
         } else if (isFriend) {
            arrowColor = Colors.GREEN;
         } else {
            arrowColor = Colors.ACCENT;
         }
         
         context.drawTexture(
            Mytheria.id("textures/arrow.png"),
            -10.0f,
            40.0f,
            20.0f,
            20.0f,
            arrowColor.mulAlpha(arrow.getValue().showing.getValue())
         );
         
         ms.pop();
      }

      iconBatching.draw();

      for (Entity entity : toRemove) {
         this.animations.remove(entity);
      }

      ms.pop();
      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.disableDepthTest();
   };

   private final EventListener<Render3DEvent> on3DRender = event -> {
      if (mc.player == null || mc.world == null || !this.lines.isEnabled()) {
         return;
      }

      MatrixStack matrices = event.getMatrices();
      
      TargetSettings targetSettings = new TargetSettings.Builder()
         .targetPlayers(this.players.isSelected())
         .targetAnimals(this.animals.isSelected())
         .targetInvisibles(this.invisibles.isSelected())
         .targetFriends(this.friends.isSelected())
         .targetNakedPlayers(this.nakedPlayers.isSelected())
         .targetMobs(this.mobs.isSelected())
         .build();

      RenderUtility.setupRender3D(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof LivingEntity)) continue;
         
         LivingEntity livingEntity = (LivingEntity) entity;
         boolean isVillager = entity instanceof VillagerEntity && this.villagers.isSelected();
         
         if (!isVillager && !targetSettings.isEntityValid(entity)) continue;

         Vec3d entityPos = Utils.getInterpolatedPos(livingEntity, event.getTickDelta());
         ColorRGBA lineColor = isVillager ? Colors.RED : Colors.WHITE;
         Draw3DUtility.renderLineFromPlayer(matrices, builder, entityPos.add(0.0, livingEntity.getHeight() / 2.0, 0.0), lineColor);
      }

      RenderUtility.buildBuffer(builder);
      RenderUtility.endRender3D();
   };

   private float calculateAngle(Entity entity, float partialTicks) {
      Vec3d pos = Utils.getInterpolatedPos(entity, partialTicks).subtract(mc.gameRenderer.getCamera().getPos());
      
      double cos = MathHelper.cos((float) (mc.gameRenderer.getCamera().getYaw() * (Math.PI / 180)));
      double sin = MathHelper.sin((float) (mc.gameRenderer.getCamera().getYaw() * (Math.PI / 180)));
      
      double rotY = -(pos.z * cos - pos.x * sin);
      double rotX = -(pos.x * cos + pos.z * sin);
      
      return (float) (Math.atan2(rotY, rotX) * 180.0 / Math.PI - 90.0);
   }

   static class ArrowsAnimation {
      Animation showing = new Animation(300L, Easing.BAKEK);
      Animation rotating = new Animation(300L, Easing.BAKEK);
   }
}
