package moscow.mytheria.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.msdf.Font;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.msdf.MsdfRenderer;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.Render3DEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.ColorSetting;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.render.DrawUtility;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.*;

@ModuleInfo(
   name = "NameTags",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.nametags"
)
public class PlayerLabels extends BaseModule {
   // Цели
   private final BooleanSetting players = new BooleanSetting(this, "modules.settings.nametags.players").enabled(true);
   private final BooleanSetting mobs = new BooleanSetting(this, "modules.settings.nametags.mobs").enabled(false);
   private final BooleanSetting invisibles = new BooleanSetting(this, "modules.settings.nametags.invisibles").enabled(true);
   
   // Отображение
   private final BooleanSetting showHealth = new BooleanSetting(this, "modules.settings.nametags.show_health").enabled(true);
   private final BooleanSetting showDistance = new BooleanSetting(this, "modules.settings.nametags.show_distance").enabled(false);
   private final BooleanSetting showArmor = new BooleanSetting(this, "modules.settings.nametags.show_armor").enabled(true);
   private final BooleanSetting showEffects = new BooleanSetting(this, "modules.settings.nametags.show_effects").enabled(true);
   private final BooleanSetting showEnchants = new BooleanSetting(this, "modules.settings.nametags.show_enchants").enabled(true);
   private final BooleanSetting showSphere = new BooleanSetting(this, "modules.settings.nametags.show_sphere").enabled(true);
   private final BooleanSetting staticSize = new BooleanSetting(this, "modules.settings.nametags.static_size").enabled(false);
   
   // Настройки размера
   private final SliderSetting scale = new SliderSetting(this, "modules.settings.nametags.scale")
      .step(0.05F)
      .min(0.1F)
      .max(2.0F)
      .currentValue(1.0F);
   
   // Цвета
   private final ColorSetting nameColor = new ColorSetting(this, "modules.settings.nametags.name_color")
      .color(new ColorRGBA(255, 255, 255, 255));
   
   private final ColorSetting friendColor = new ColorSetting(this, "modules.settings.nametags.friend_color")
      .color(new ColorRGBA(85, 255, 85, 255));
   
   private final ColorSetting healthColor = new ColorSetting(this, "modules.settings.nametags.health_color")
      .color(new ColorRGBA(255, 85, 85, 255));
   
   private final ColorSetting distanceColor = new ColorSetting(this, "modules.settings.nametags.distance_color")
      .color(new ColorRGBA(170, 170, 170, 255));
   
   private final ColorSetting backgroundColor = new ColorSetting(this, "modules.settings.nametags.background_color")
      .color(new ColorRGBA(30, 30, 30, 150));
   
   private final BooleanSetting colorByHealth = new BooleanSetting(this, "modules.settings.nametags.color_by_health").enabled(false);
   
   // Дистанция
   private final SliderSetting range = new SliderSetting(this, "modules.settings.nametags.range")
      .step(1F)
      .min(16F)
      .max(256F)
      .currentValue(128F);
   
   private static final Set<String> IMPORTANT_ENCHANTS = Set.of(
      "Protection", "Защита",
      "Unbreaking", "Прочность",
      "Looting", "Добыча",
      "Fortune", "Удача",
      "Efficiency", "Эффективность",
      "Power", "Сила",
      "Feather Falling", "Невесомость",
      "Thorns", "Шипы",
      "Silk Touch", "Шёлковое касание",
      "Respiration", "Подводное дыхание",
      "Mending", "Починка",
      "Knockback", "Отдача",
      "Curse of Vanishing", "Проклятие утраты"
   );
   
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
         
         // Проверка невидимости
         if (living.isInvisible() && !invisibles.isEnabled()) continue;
         
         boolean isPlayer = entity instanceof PlayerEntity;
         boolean isMob = entity instanceof MobEntity;
         
         if (isPlayer && !players.isEnabled()) continue;
         if (isMob && !mobs.isEnabled()) continue;
         if (!isPlayer && !isMob) continue;
         
         // Интерполированная позиция
         double x = MathHelper.lerp(tickDelta, living.prevX, living.getX()) - cameraPos.x;
         double y = MathHelper.lerp(tickDelta, living.prevY, living.getY()) - cameraPos.y;
         double z = MathHelper.lerp(tickDelta, living.prevZ, living.getZ()) - cameraPos.z;
         
         // Рисуем нейм тег
         drawNameTag(ms, living, x, y, z, distance);
      }
   };

   /**
    * Рисует нейм тег над сущностью
    */
   private void drawNameTag(MatrixStack ms, LivingEntity entity, double x, double y, double z, double distance) {
      ms.push();
      
      // Позиция над головой
      ms.translate(x, y + entity.getHeight() + 0.5, z);
      
      // Поворот к камере
      ms.multiply(new Quaternionf().rotationY(-mc.gameRenderer.getCamera().getYaw() * (float)(Math.PI / 180.0)));
      ms.multiply(new Quaternionf().rotationX(mc.gameRenderer.getCamera().getPitch() * (float)(Math.PI / 180.0)));
      
      // Масштаб
      float baseScale = staticSize.isEnabled() ? 0.025F : (float)Math.max(0.02, Math.min(distance * 0.002, 0.05));
      float finalScale = baseScale * scale.getCurrentValue();
      ms.scale(-finalScale, -finalScale, finalScale);
      
      Font font = Fonts.MEDIUM.getFont(16.0F);
      
      if (entity instanceof PlayerEntity player) {
         renderPlayerTag(ms, player, font, distance);
      } else {
         renderMobTag(ms, entity, font, distance);
      }
      
      GL11.glDepthFunc(515);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      
      ms.pop();
   }

   /**
    * Рендер тега игрока
    */
   private void renderPlayerTag(MatrixStack ms, PlayerEntity player, Font font, double distance) {
      boolean isFriend = Mytheria.getInstance().getFriendManager().isFriend(player.getName().getString());
      
      String friendPrefix = isFriend ? Formatting.GRAY + "[" + Formatting.GREEN + "F" + Formatting.GRAY + "] " : "";
      
      float health = player.getHealth() + player.getAbsorptionAmount();
      String hpText = Formatting.GRAY + " [" + (health < 300 ? Formatting.RED.toString() + (int) health : Formatting.RED + "Unknown") + Formatting.GRAY + "]" + Formatting.RESET;
      
      String name = player.getName().getString();
      
      // Префикс команды (донат)
      Text prefix = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getPrefix() : Text.literal("");
      
      // Вычисляем ширину - все элементы одного размера
      float textScale = 1.0f; // Единый масштаб для всех элементов
      float friendWidth = friendPrefix.isEmpty() ? 0 : mc.textRenderer.getWidth(Text.literal(friendPrefix)) * textScale;
      float prefixWidth = mc.textRenderer.getWidth(prefix) * textScale;
      float nameHpWidth = mc.textRenderer.getWidth(Text.literal(name + (showHealth.isEnabled() ? hpText : ""))) * textScale;
      
      if (showDistance.isEnabled()) {
         String distText = " [" + String.format("%.1f", distance) + "m]";
         nameHpWidth += mc.textRenderer.getWidth(Text.literal(distText)) * textScale;
      }
      
      float totalWidth = friendWidth + prefixWidth + nameHpWidth;
      
      float padding = 6.0F;
      float bgWidth = totalWidth + padding * 2;
      float bgHeight = mc.textRenderer.fontHeight * textScale + padding * 1.5F;
      
      float x1 = -bgWidth / 2;
      float y1 = -bgHeight / 2;
      
      GL11.glDepthFunc(519);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      
      // Тень
      DrawUtility.drawShadow(
         ms,
         x1 - 2,
         y1 - 2,
         bgWidth + 4,
         bgHeight + 4,
         8.0F,
         BorderRadius.all(4.0F),
         ColorRGBA.BLACK.withAlpha(100.0F)
      );
      
      // Фон
      DrawUtility.drawRoundedRect(
         ms,
         x1,
         y1,
         bgWidth,
         bgHeight,
         BorderRadius.all(4.0F),
         backgroundColor.getColor()
      );
      
      // Рисуем весь текст в едином масштабе
      ms.push();
      ms.translate(x1 + padding, y1 + padding * 0.75f, 0);
      ms.scale(textScale, textScale, 1.0f);
      
      float dx = 0;
      
      // Префикс друга
      if (!friendPrefix.isEmpty()) {
         mc.textRenderer.draw(
            Text.literal(friendPrefix), 
            dx, 
            0, 
            -1, 
            false, 
            ms.peek().getPositionMatrix(), 
            mc.getBufferBuilders().getEntityVertexConsumers(), 
            TextRenderer.TextLayerType.NORMAL, 
            0, 
            0xF000F0
         );
         dx += friendWidth / textScale;
      }
      
      // Префикс команды (донат) - тот же размер что и все остальное
      if (!prefix.getString().isEmpty()) {
         mc.textRenderer.draw(
            prefix, 
            dx, 
            0, 
            -1, 
            false, 
            ms.peek().getPositionMatrix(), 
            mc.getBufferBuilders().getEntityVertexConsumers(), 
            TextRenderer.TextLayerType.NORMAL, 
            0, 
            0xF000F0
         );
         dx += prefixWidth / textScale;
      }
      
      // Имя + здоровье + дистанция
      ColorRGBA nameColorFinal = isFriend ? friendColor.getColor() : nameColor.getColor();
      String fullText = name;
      if (showHealth.isEnabled()) fullText += hpText;
      if (showDistance.isEnabled()) fullText += " [" + String.format("%.1f", distance) + "m]";
      
      mc.textRenderer.draw(
         Text.literal(fullText), 
         dx, 
         0, 
         nameColorFinal.getRGB(), 
         false, 
         ms.peek().getPositionMatrix(), 
         mc.getBufferBuilders().getEntityVertexConsumers(), 
         TextRenderer.TextLayerType.NORMAL, 
         0, 
         0xF000F0
      );
      
      ms.pop();
      
      // Рисуем эффекты
      if (showEffects.isEnabled()) {
         renderEffects(ms, player, font);
      }
      
      // Рисуем броню
      if (showArmor.isEnabled()) {
         ms.push();
         ms.translate(0, -bgHeight / 2 - 18, 0);
         renderPlayerItems(ms, player, font);
         ms.pop();
      }
   }

   /**
    * Рендер тега моба
    */
   private void renderMobTag(MatrixStack ms, LivingEntity entity, Font font, double distance) {
      String name = entity.getName().getString();
      String hpText = "";
      
      if (showHealth.isEnabled()) {
         float health = entity.getHealth();
         hpText = " " + Formatting.RED + String.format("%.1f", health);
      }
      
      if (showDistance.isEnabled()) {
         hpText += " [" + String.format("%.1f", distance) + "m]";
      }
      
      float nameWidth = font.width(name + hpText);
      float padding = 6.0F;
      float bgWidth = nameWidth + padding * 2;
      float bgHeight = font.height() + padding * 1.5F;
      
      float x1 = -bgWidth / 2;
      float y1 = -bgHeight / 2;
      
      GL11.glDepthFunc(519);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      
      // Тень
      DrawUtility.drawShadow(
         ms,
         x1 - 2,
         y1 - 2,
         bgWidth + 4,
         bgHeight + 4,
         8.0F,
         BorderRadius.all(4.0F),
         ColorRGBA.BLACK.withAlpha(100.0F)
      );
      
      // Фон
      DrawUtility.drawRoundedRect(
         ms,
         x1,
         y1,
         bgWidth,
         bgHeight,
         BorderRadius.all(4.0F),
         backgroundColor.getColor()
      );
      
      // Текст
      MsdfRenderer.renderText(
         font.getFont(),
         name + hpText,
         font.getSize(),
         nameColor.getColor().getRGB(),
         ms.peek().getPositionMatrix(),
         -nameWidth / 2,
         -font.height() / 2 + 1,
         0.01F
      );
   }

   /**
    * Рендер эффектов
    */
   private void renderEffects(MatrixStack ms, PlayerEntity player, Font font) {
      ms.push();
      ms.translate(0, font.height() / 2 + 10, 0);
      
      int offsetY = 0;
      for (StatusEffectInstance effect : player.getStatusEffects()) {
         String name = effect.getEffectType().value().getName().getString();
         int lvl = effect.getAmplifier() + 1;
         int sec = effect.getDuration() / 20;
         String text = name + (lvl > 1 ? " " + lvl : "") + " | " + sec / 60 + ":" + String.format("%02d", sec % 60);
         
         float width = font.width(text);
         
         MsdfRenderer.renderText(
            font.getFont(),
            text,
            font.getSize(),
            0xFFFFFF,
            ms.peek().getPositionMatrix(),
            -width / 2,
            offsetY,
            0.01F
         );
         offsetY += 9;
      }
      
      ms.pop();
   }

   /**
    * Рендер предметов игрока
    */
   private void renderPlayerItems(MatrixStack ms, PlayerEntity player, Font font) {
      List<ItemStack> stacks = new ArrayList<>(6);
      stacks.add(player.getMainHandStack());
      player.getArmorItems().forEach(stacks::add);
      stacks.add(player.getOffHandStack());
      stacks.removeIf(i -> i.isEmpty() || i.getItem() instanceof AirBlockItem);
      
      if (stacks.isEmpty()) return;
      
      float itemSize = 16f;
      float spacing = 2f;
      float totalWidth = stacks.size() * itemSize + (stacks.size() - 1) * spacing;
      float startX = -totalWidth / 2;
      
      float currentX = startX;
      
      for (ItemStack stack : stacks) {
         // Рисуем предмет (упрощенно - просто слот)
         DrawUtility.drawRoundedRect(
            ms,
            currentX,
            -itemSize / 2,
            itemSize,
            itemSize,
            BorderRadius.all(2.0F),
            new ColorRGBA(60, 60, 60, 220)
         );
         
         // Рисуем чары
         if (showEnchants.isEnabled() && !stack.getEnchantments().isEmpty()) {
            List<Object2IntMap.Entry<RegistryEntry<Enchantment>>> enchantments = new ArrayList<>(stack.getEnchantments().getEnchantmentEntries());
            
            enchantments.removeIf(entry -> {
               Text name = Enchantment.getName(entry.getKey(), entry.getIntValue());
               String full = name.getString();
               return IMPORTANT_ENCHANTS.stream().noneMatch(full::contains);
            });
            
            if (!enchantments.isEmpty()) {
               ms.push();
               ms.translate(currentX, -itemSize / 2 - 2, 0);
               ms.scale(0.7f, 0.7f, 1.0f);
               
               int enchantY = 0;
               for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments) {
                  RegistryEntry<Enchantment> regEntry = entry.getKey();
                  int level = entry.getIntValue();
                  Text enchantText = Enchantment.getName(regEntry, level);
                  
                  String display = getShortName(enchantText, level);
                  
                  MsdfRenderer.renderText(
                     font.getFont(),
                     display,
                     font.getSize() * 0.7f,
                     0xFFFFFF,
                     ms.peek().getPositionMatrix(),
                     0,
                     enchantY - enchantments.size() * 8,
                     0.01F
                  );
                  enchantY += 8;
               }
               
               ms.pop();
            }
         }
         
         currentX += itemSize + spacing;
      }
   }

   /**
    * Автоматическое сокращение чар
    */
   private String getShortName(Text description, int level) {
      String full = description.getString();
      String[] words = full.split(" ");
      String shortName;
      
      if (words.length == 1) {
         shortName = words[0].substring(0, Math.min(2, words[0].length())).toUpperCase();
      } else {
         shortName = "";
         for (String w : words) {
            if (!w.isEmpty()) shortName += w.charAt(0);
         }
         shortName = shortName.toUpperCase();
      }
      
      return shortName + " " + level;
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }
}
