package moscow.mytheria.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
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
import moscow.mytheria.systems.setting.settings.SelectSetting;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.game.WorldUtility;
import moscow.mytheria.utility.render.Draw3DUtility;
import moscow.mytheria.utility.render.DrawUtility;
import moscow.mytheria.utility.render.RenderUtility;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.*;

@ModuleInfo(
   name = "BlockInfo",
   category = ModuleCategory.VISUALS,
   desc = "modules.descriptions.blockinfo"
)
public class BlockInfo extends BaseModule {
   private static final Box FULL_BOX = new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
   
   private final SelectSetting blocks = new SelectSetting(this, "modules.settings.blockinfo.blocks");
   private final Set<BlockEntityType<?>> selectedBlocksCache = new HashSet<>();
   private long cacheStamp = -1L;
   
   // Контейнеры по умолчанию
   private static final BlockEntityType<?>[] DEFAULT_SELECTED = new BlockEntityType<?>[]{
      BlockEntityType.CHEST,
      BlockEntityType.TRAPPED_CHEST,
      BlockEntityType.ENDER_CHEST,
      BlockEntityType.BARREL,
      BlockEntityType.SHULKER_BOX,
      BlockEntityType.HOPPER,
      BlockEntityType.FURNACE,
      BlockEntityType.BLAST_FURNACE,
      BlockEntityType.SMOKER,
      BlockEntityType.DISPENSER,
      BlockEntityType.DROPPER,
      BlockEntityType.MOB_SPAWNER,
      BlockEntityType.JIGSAW, // Блок-пазл
      BlockEntityType.STRUCTURE_BLOCK // Структурный блок
   };
   
   // Другое
   private final BooleanSetting minecarts = new BooleanSetting(this, "modules.settings.blockinfo.minecarts").enabled(true);
   private final BooleanSetting throughWalls = new BooleanSetting(this, "modules.settings.blockinfo.through_walls").enabled(true);
   
   // Неймтеги на блоки
   private final BooleanSetting showNameTags = new BooleanSetting(this, "modules.settings.blockinfo.show_nametags").enabled(false);
   private final SliderSetting nameTagScale = new SliderSetting(this, "modules.settings.blockinfo.nametag_scale", () -> !this.showNameTags.isEnabled())
      .step(0.1F)
      .min(0.5F)
      .max(3.0F)
      .currentValue(1.0F);
   
   // Параметры
   private final SliderSetting radius = new SliderSetting(this, "modules.settings.blockinfo.radius")
      .step(4F)
      .min(8F)
      .max(128F)
      .currentValue(32F);
   private final SliderSetting lineWidth = new SliderSetting(this, "modules.settings.blockinfo.line_width")
      .step(0.5F)
      .min(1F)
      .max(5F)
      .currentValue(2F);

   // Списки для рендера
   private final Set<BlockPos> renderedPositions = new HashSet<>();
   private final List<BlockEntity> blockEntityList = new ArrayList<>();

   public BlockInfo() {
      // Создаем настройки для всех типов блок-энтити
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.chest").select();
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.ender_chest").select();
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.barrel").select();
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.shulker_box").select();
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.hopper");
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.furnace");
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.dispenser");
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.dropper");
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.spawner").select();
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.jigsaw").select();
      new SelectSetting.Value(this.blocks, "modules.settings.blockinfo.structure_block").select();
   }
   
   private BlockEntityType<?> getBlockEntityTypeByName(String name) {
      switch (name) {
         case "modules.settings.blockinfo.chest": return BlockEntityType.CHEST;
         case "modules.settings.blockinfo.ender_chest": return BlockEntityType.ENDER_CHEST;
         case "modules.settings.blockinfo.barrel": return BlockEntityType.BARREL;
         case "modules.settings.blockinfo.shulker_box": return BlockEntityType.SHULKER_BOX;
         case "modules.settings.blockinfo.hopper": return BlockEntityType.HOPPER;
         case "modules.settings.blockinfo.furnace": return BlockEntityType.FURNACE;
         case "modules.settings.blockinfo.dispenser": return BlockEntityType.DISPENSER;
         case "modules.settings.blockinfo.dropper": return BlockEntityType.DROPPER;
         case "modules.settings.blockinfo.spawner": return BlockEntityType.MOB_SPAWNER;
         case "modules.settings.blockinfo.jigsaw": return BlockEntityType.JIGSAW;
         case "modules.settings.blockinfo.structure_block": return BlockEntityType.STRUCTURE_BLOCK;
         default: return null;
      }
   }

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (!this.isEnabled() || mc.player == null || mc.world == null) return;
      
      try {
         // Обновляем кеш если нужно
         if (System.currentTimeMillis() - this.cacheStamp > 500L) {
            invalidateCache();
         }
         
         MatrixStack matrices = event.getMatrices();
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getPos();
         
         // Очищаем списки
         blockEntityList.clear();
         renderedPositions.clear();
         
         int maxDist = (int) this.radius.getCurrentValue();
         int maxDistSq = maxDist * maxDist;
         
         // Собираем блок-энтити (без дубликатов по позиции)
         for (BlockEntity blockEntity : WorldUtility.blockEntities) {
            BlockPos pos = blockEntity.getPos();
            if (renderedPositions.contains(pos)) continue;
            if (mc.player.squaredDistanceTo(pos.toCenterPos()) > maxDistSq) continue;
            if (isBlockEntityEnabled(blockEntity.getType())) {
               blockEntityList.add(blockEntity);
               renderedPositions.add(pos);
            }
         }
         
         // Рендерим
         RenderSystem.enableBlend();
         if (this.throughWalls.isEnabled()) {
            RenderSystem.disableDepthTest();
         } else {
            RenderSystem.enableDepthTest();
         }
         RenderSystem.disableCull();
         RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(this.lineWidth.getCurrentValue());
         
         BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         
         // Рендерим блок-энтити
         for (BlockEntity blockEntity : blockEntityList) {
            Color color = getBlockEntityColor(blockEntity.getType());
            BlockPos pos = blockEntity.getPos();
            Box box = FULL_BOX.offset(pos);
            Draw3DUtility.renderOutlinedBox(matrices, linesBuffer, 
               box.offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()), 
               toColorRGBA(color));
         }
         
         // Рендерим вагонетки
         if (this.minecarts.isEnabled()) {
            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof ChestMinecartEntity && entity.squaredDistanceTo(mc.player) <= maxDistSq) {
                  Box box = entity.getBoundingBox();
                  Draw3DUtility.renderOutlinedBox(matrices, linesBuffer,
                     box.offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()),
                     toColorRGBA(new Color(255, 215, 0)));
               }
            }
         }
         
         RenderUtility.buildBuffer(linesBuffer);
         
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.disableBlend();
         
         // Рендерим неймтеги если включено
         if (this.showNameTags.isEnabled()) {
            for (BlockEntity blockEntity : blockEntityList) {
               BlockEntityType<?> type = blockEntity.getType();
               // Показываем неймтеги только для спавнеров, джигсоу и структурных блоков
               if (type == BlockEntityType.MOB_SPAWNER || 
                   type == BlockEntityType.JIGSAW || 
                   type == BlockEntityType.STRUCTURE_BLOCK) {
                  drawBlockNameTag(matrices, blockEntity, cameraPos);
               }
            }
         }
         
      } catch (Exception e) {
         e.printStackTrace();
      }
   };
   
   @Override
   public void onDisable() {
      blockEntityList.clear();
      renderedPositions.clear();
   }
   
   private void invalidateCache() {
      this.selectedBlocksCache.clear();
      for (SelectSetting.Value v : this.blocks.getSelectedValues()) {
         try {
            BlockEntityType<?> type = getBlockEntityTypeByName(v.getName());
            if (type != null) {
               this.selectedBlocksCache.add(type);
               // Добавляем связанные типы
               if (type == BlockEntityType.CHEST) {
                  this.selectedBlocksCache.add(BlockEntityType.TRAPPED_CHEST);
               }
               if (type == BlockEntityType.FURNACE) {
                  this.selectedBlocksCache.add(BlockEntityType.BLAST_FURNACE);
                  this.selectedBlocksCache.add(BlockEntityType.SMOKER);
               }
            }
         } catch (Throwable ignored) {
         }
      }
      this.cacheStamp = System.currentTimeMillis();
   }
   
   public boolean isBlockEntityEnabled(BlockEntityType<?> type) {
      if (this.cacheStamp < 0L) {
         invalidateCache();
      }
      return this.selectedBlocksCache.contains(type);
   }
   
   private Color getBlockEntityColor(BlockEntityType<?> type) {
      if (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST) {
         return new Color(215, 134, 11);
      }
      if (type == BlockEntityType.ENDER_CHEST) {
         return new Color(138, 43, 226);
      }
      if (type == BlockEntityType.BARREL) {
         return new Color(131, 90, 33);
      }
      if (type == BlockEntityType.SHULKER_BOX) {
         return new Color(0, 194, 255);
      }
      if (type == BlockEntityType.HOPPER) {
         return new Color(120, 120, 120);
      }
      if (type == BlockEntityType.FURNACE || type == BlockEntityType.BLAST_FURNACE || type == BlockEntityType.SMOKER) {
         return new Color(150, 150, 150);
      }
      if (type == BlockEntityType.DISPENSER || type == BlockEntityType.DROPPER) {
         return new Color(100, 100, 100);
      }
      if (type == BlockEntityType.MOB_SPAWNER) {
         return new Color(41, 250, 41);
      }
      if (type == BlockEntityType.JIGSAW) {
         return new Color(255, 100, 255);
      }
      if (type == BlockEntityType.STRUCTURE_BLOCK) {
         return new Color(100, 100, 255);
      }
      return new Color(255, 255, 255);
   }
   
   private String getBlockName(BlockEntityType<?> type) {
      if (type == BlockEntityType.MOB_SPAWNER) return "Спавнер";
      if (type == BlockEntityType.JIGSAW) return "Блок-дамагер";
      if (type == BlockEntityType.STRUCTURE_BLOCK) return "Прогрузчик";
      if (type == BlockEntityType.CHEST || type == BlockEntityType.TRAPPED_CHEST) return "Сундук";
      if (type == BlockEntityType.ENDER_CHEST) return "Эндер-сундук";
      if (type == BlockEntityType.BARREL) return "Бочка";
      if (type == BlockEntityType.SHULKER_BOX) return "Шалкер";
      if (type == BlockEntityType.HOPPER) return "Воронка";
      if (type == BlockEntityType.FURNACE) return "Печь";
      if (type == BlockEntityType.BLAST_FURNACE) return "Плавильня";
      if (type == BlockEntityType.SMOKER) return "Коптильня";
      if (type == BlockEntityType.DISPENSER) return "Раздатчик";
      if (type == BlockEntityType.DROPPER) return "Выбрасыватель";
      return "Блок";
   }
   
   private void drawBlockNameTag(MatrixStack ms, BlockEntity blockEntity, Vec3d cameraPos) {
      BlockPos pos = blockEntity.getPos();
      Vec3d blockCenter = pos.toCenterPos();
      
      ms.push();
      
      // Позиция чуть выше блока
      ms.translate(
         blockCenter.x - cameraPos.x,
         blockCenter.y - cameraPos.y + 0.7, // Чуть выше блока
         blockCenter.z - cameraPos.z
      );
      
      // Поворот к камере
      ms.multiply(new Quaternionf().rotationY(-mc.gameRenderer.getCamera().getYaw() * (float)(Math.PI / 180.0)));
      ms.multiply(new Quaternionf().rotationX(mc.gameRenderer.getCamera().getPitch() * (float)(Math.PI / 180.0)));
      
      // Масштаб
      float scale = 0.02F * this.nameTagScale.getCurrentValue();
      ms.scale(-scale, -scale, scale);
      
      // Используем MSDF шрифт
      Font font = Fonts.MEDIUM.getFont(16.0F);
      
      String name = getBlockName(blockEntity.getType());
      
      float nameWidth = font.width(name);
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
      ColorRGBA bgColor = new ColorRGBA(30, 30, 30, 150);
      DrawUtility.drawRoundedRect(
         ms,
         x1,
         y1,
         bgWidth,
         bgHeight,
         BorderRadius.all(4.0F),
         bgColor
      );
      
      // Текст с MSDF рендером
      Color blockColor = getBlockEntityColor(blockEntity.getType());
      ColorRGBA textColor = new ColorRGBA(
         (float) blockColor.getRed(),
         (float) blockColor.getGreen(),
         (float) blockColor.getBlue(),
         255.0F
      );
      
      MsdfRenderer.renderText(
         font.getFont(),
         name,
         font.getSize(),
         textColor.getRGB(),
         ms.peek().getPositionMatrix(),
         -nameWidth / 2,
         -font.height() / 2 + 1,
         0.01F
      );
      
      GL11.glDepthFunc(515);
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      
      ms.pop();
   }
   
   private moscow.mytheria.utility.colors.ColorRGBA toColorRGBA(Color color) {
      return new moscow.mytheria.utility.colors.ColorRGBA(
         (float) color.getRed(),
         (float) color.getGreen(),
         (float) color.getBlue(),
         (float) color.getAlpha()
      );
   }
}
