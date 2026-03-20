package moscow.mytheria.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.Render3DEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.ColorSetting;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.utility.colors.ColorRGBA;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

@ModuleInfo(
   name = "ESP",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.esp"
)
public class EntityGlow extends BaseModule {
   // Цели
   private final BooleanSetting players = new BooleanSetting(this, "modules.settings.esp.players").enabled(true);
   private final BooleanSetting mobs = new BooleanSetting(this, "modules.settings.esp.mobs").enabled(false);
   private final BooleanSetting invisibles = new BooleanSetting(this, "modules.settings.esp.invisibles").enabled(true);
   
   private final BooleanSetting modernStyle = new BooleanSetting(this, "modules.settings.esp.modern_style").enabled(false);
   
   private final ColorSetting playerColor = new ColorSetting(this, "modules.settings.esp.player_color").color(new ColorRGBA(0, 255, 0, 255));
   private final ColorSetting friendColor = new ColorSetting(this, "modules.settings.esp.friend_color").color(new ColorRGBA(85, 255, 85, 255));
   private final ColorSetting mobColor = new ColorSetting(this, "modules.settings.esp.mob_color").color(new ColorRGBA(255, 0, 0, 255));
   
   private final SliderSetting lineWidth = new SliderSetting(this, "modules.settings.esp.line_width")
      .step(0.5F)
      .min(1F)
      .max(10F)
      .currentValue(2F);
   
   private final BooleanSetting colorByHealth = new BooleanSetting(this, "modules.settings.esp.color_by_health").enabled(false);
   
   private final SliderSetting range = new SliderSetting(this, "modules.settings.esp.range")
      .step(1F)
      .min(16F)
      .max(256F)
      .currentValue(128F);
   
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (!this.isEnabled() || mc.player == null || mc.world == null) return;
      
      MatrixStack ms = event.getMatrices();
      Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
      float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
      
      for (Entity entity : mc.world.getEntities()) {
         if (entity == mc.player || !(entity instanceof LivingEntity living)) continue;
         
         if (!living.isAlive() || living.isDead()) continue;
         
         // Проверка дистанции
         double distance = mc.player.distanceTo(living);
         if (distance > range.getCurrentValue()) continue;
         
         if (living.isInvisible() && !invisibles.isEnabled()) continue;
         
         boolean isPlayer = entity instanceof PlayerEntity;
         boolean isMob = entity instanceof MobEntity;
         
         if (isPlayer && !players.isEnabled()) continue;
         if (isMob && !mobs.isEnabled()) continue;
         if (!isPlayer && !isMob) continue;
         
         boolean isFriend = false;
         if (isPlayer) {
            PlayerEntity player = (PlayerEntity) entity;
            isFriend = Mytheria.getInstance().getFriendManager().isFriend(player.getName().getString());
         }
         
         ColorRGBA color;
         if (isFriend) {
            color = friendColor.getColor();
         } else if (colorByHealth.isEnabled()) {
            float healthPercent = living.getHealth() / living.getMaxHealth();
            if (healthPercent > 0.66f) {
               color = new ColorRGBA(0, 255, 0, 255);
            } else if (healthPercent > 0.33f) {
               color = new ColorRGBA(255, 255, 0, 255);
            } else {
               color = new ColorRGBA(255, 0, 0, 255);
            }
         } else {
            color = isPlayer ? playerColor.getColor() : mobColor.getColor();
         }
         
         double x = MathHelper.lerp(tickDelta, living.prevX, living.getX()) - cameraPos.x;
         double y = MathHelper.lerp(tickDelta, living.prevY, living.getY()) - cameraPos.y;
         double z = MathHelper.lerp(tickDelta, living.prevZ, living.getZ()) - cameraPos.z;
         
         GL11.glDepthFunc(519);
         RenderSystem.depthMask(false);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableCull();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         
         if (modernStyle.isEnabled()) {
            drawModernBox(buffer, ms, x, y, z, living.getWidth(), living.getHeight(), color, distance);
         } else {
            drawBox(buffer, ms, x, y, z, living.getWidth(), living.getHeight(), color);
         }
         
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         
         GL11.glDepthFunc(515);
         RenderSystem.depthMask(true);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
      }
   };
   
   private void drawBox(BufferBuilder buffer, MatrixStack ms, double x, double y, double z, float width, float height, ColorRGBA color) {
      float halfWidth = width / 2.0F;
      
      float minX = -halfWidth;
      float maxX = halfWidth;
      float minY = 0;
      float maxY = height;
      float minZ = -halfWidth;
      float maxZ = halfWidth;
      
      float r = color.getRed() / 255f;
      float g = color.getGreen() / 255f;
      float b = color.getBlue() / 255f;
      float a = color.getAlpha() / 255f;
      
      ms.push();
      ms.translate(x, y, z);
      
      var matrix = ms.peek().getPositionMatrix();
      
      // Нижняя грань
      buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
      buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
      
      buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
      buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
      
      buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
      buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
      
      buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
      buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
      
      // Верхняя грань
      buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
      buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
      
      buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
      buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
      
      buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
      buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
      
      buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
      buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
      
      // Вертикальные линии
      buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a);
      buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a);
      
      buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a);
      buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a);
      
      buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a);
      buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a);
      
      buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a);
      buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a);
      
      ms.pop();
   }
   
   private void drawModernBox(BufferBuilder buffer, MatrixStack ms, double x, double y, double z, float width, float height, ColorRGBA color, double distance) {
      float halfWidth = width / 2.0F;
      
      float minX = -halfWidth;
      float maxX = halfWidth;
      float minY = 0;
      float maxY = height;
      float minZ = -halfWidth;
      float maxZ = halfWidth;
      
      float r = color.getRed() / 255f;
      float g = color.getGreen() / 255f;
      float b = color.getBlue() / 255f;
      float a = color.getAlpha() / 255f;
      
      float pulse = (float) (Math.sin(System.currentTimeMillis() / 600.0) * 0.1 + 0.9);
      
      ms.push();
      ms.translate(x, y, z);
      
      var matrix = ms.peek().getPositionMatrix();
      
      float cornerLength = Math.min(width, height) * 0.35f;
      
      // НИЖНИЕ УГЛЫ
      // Передний левый
      buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX + cornerLength, minY, minZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX, minY + cornerLength, minZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, minX, minY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX, minY, minZ + cornerLength).color(r, g, b, a * pulse);
      
      // Передний правый
      buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX - cornerLength, minY, minZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX, minY + cornerLength, minZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, maxX, minY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX, minY, minZ + cornerLength).color(r, g, b, a * pulse);
      
      // Задний левый
      buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX + cornerLength, minY, maxZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX, minY + cornerLength, maxZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, minX, minY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX, minY, maxZ - cornerLength).color(r, g, b, a * pulse);
      
      // Задний правый
      buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX - cornerLength, minY, maxZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX, minY + cornerLength, maxZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX, minY, maxZ - cornerLength).color(r, g, b, a * pulse);
      
      // ВЕРХНИЕ УГЛЫ
      // Передний левый
      buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX + cornerLength, maxY, minZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX, maxY - cornerLength, minZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, minX, maxY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX, maxY, minZ + cornerLength).color(r, g, b, a * pulse);
      
      // Передний правый
      buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX - cornerLength, maxY, minZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX, maxY - cornerLength, minZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX, maxY, minZ + cornerLength).color(r, g, b, a * pulse);
      
      // Задний левый
      buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX + cornerLength, maxY, maxZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX, maxY - cornerLength, maxZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, minX, maxY, maxZ - cornerLength).color(r, g, b, a * pulse);
      
      // Задний правый
      buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX - cornerLength, maxY, maxZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX, maxY - cornerLength, maxZ).color(r, g, b, a * pulse);
      
      buffer.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a * pulse);
      buffer.vertex(matrix, maxX, maxY, maxZ - cornerLength).color(r, g, b, a * pulse);
      
      ms.pop();
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }
}
