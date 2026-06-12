package moscow.mytheria.systems.modules.modules.visuals;

import java.util.HashSet;
import java.util.Set;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.SelectSetting;
import moscow.mytheria.utility.game.EntityUtility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

@ModuleInfo(
   name = "XRay",
   category = ModuleCategory.VISUALS,
   desc = "Скрывает все блоки, кроме выбранных"
)
public class OreScanner extends BaseModule {
   private final SelectSetting blocks = new SelectSetting(this, "modules.settings.xray.blocks");
   private final Set<Block> selectedBlocksCache = new HashSet<>();
   private long cacheStamp = -1L;

   public OreScanner() {
      // Руды
      new SelectSetting.Value(this.blocks, "modules.settings.xray.diamond_ore").select();
      new SelectSetting.Value(this.blocks, "modules.settings.xray.emerald_ore").select();
      new SelectSetting.Value(this.blocks, "modules.settings.xray.iron_ore");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.gold_ore");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.ancient_debris").select();
      new SelectSetting.Value(this.blocks, "modules.settings.xray.lapis_ore");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.redstone_ore");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.coal_ore");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.copper_ore");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.nether_quartz_ore");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.nether_gold_ore");
      
      // Контейнеры
      new SelectSetting.Value(this.blocks, "modules.settings.xray.chest").select();
      new SelectSetting.Value(this.blocks, "modules.settings.xray.ender_chest").select();
      new SelectSetting.Value(this.blocks, "modules.settings.xray.shulker_box").select();
      new SelectSetting.Value(this.blocks, "modules.settings.xray.barrel").select();
      
      // Механизмы
      new SelectSetting.Value(this.blocks, "modules.settings.xray.dispenser");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.dropper");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.piston");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.sticky_piston");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.observer");
      new SelectSetting.Value(this.blocks, "modules.settings.xray.hopper");
      
      // Специальные
      new SelectSetting.Value(this.blocks, "modules.settings.xray.spawner").select();
      new SelectSetting.Value(this.blocks, "modules.settings.xray.structure_block").select();
      new SelectSetting.Value(this.blocks, "modules.settings.xray.jigsaw").select();
   }

   @Override
   public void onEnable() {
      if (!EntityUtility.isInGame()) {
         return;
      }
      this.invalidateCache();
      this.reloadRenderer();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.reloadRenderer();
      super.onDisable();
   }

   @Override
   public void tick() {
      if (System.currentTimeMillis() - this.cacheStamp > 500L) {
         this.invalidateCache();
      }
      super.tick();
   }

   private void reloadRenderer() {
      if (mc.worldRenderer != null) {
         mc.worldRenderer.reload();
      }
   }

   private void invalidateCache() {
      this.selectedBlocksCache.clear();
      for (SelectSetting.Value v : this.blocks.getSelectedValues()) {
         String name = v.getName();
         Block block = getBlockByName(name);
         if (block != null) {
            this.selectedBlocksCache.add(block);
         }
      }
      this.cacheStamp = System.currentTimeMillis();
   }
   
   private Block getBlockByName(String name) {
      switch (name) {
         // Руды
         case "modules.settings.xray.diamond_ore":
            selectedBlocksCache.add(Blocks.DIAMOND_ORE);
            return Blocks.DEEPSLATE_DIAMOND_ORE;
         case "modules.settings.xray.emerald_ore":
            selectedBlocksCache.add(Blocks.EMERALD_ORE);
            return Blocks.DEEPSLATE_EMERALD_ORE;
         case "modules.settings.xray.iron_ore":
            selectedBlocksCache.add(Blocks.IRON_ORE);
            return Blocks.DEEPSLATE_IRON_ORE;
         case "modules.settings.xray.gold_ore":
            selectedBlocksCache.add(Blocks.GOLD_ORE);
            selectedBlocksCache.add(Blocks.DEEPSLATE_GOLD_ORE);
            return Blocks.NETHER_GOLD_ORE;
         case "modules.settings.xray.ancient_debris":
            return Blocks.ANCIENT_DEBRIS;
         case "modules.settings.xray.lapis_ore":
            selectedBlocksCache.add(Blocks.LAPIS_ORE);
            return Blocks.DEEPSLATE_LAPIS_ORE;
         case "modules.settings.xray.redstone_ore":
            selectedBlocksCache.add(Blocks.REDSTONE_ORE);
            return Blocks.DEEPSLATE_REDSTONE_ORE;
         case "modules.settings.xray.coal_ore":
            selectedBlocksCache.add(Blocks.COAL_ORE);
            return Blocks.DEEPSLATE_COAL_ORE;
         case "modules.settings.xray.copper_ore":
            selectedBlocksCache.add(Blocks.COPPER_ORE);
            return Blocks.DEEPSLATE_COPPER_ORE;
         case "modules.settings.xray.nether_quartz_ore":
            return Blocks.NETHER_QUARTZ_ORE;
         case "modules.settings.xray.nether_gold_ore":
            return Blocks.NETHER_GOLD_ORE;
            
         // Контейнеры
         case "modules.settings.xray.chest":
            selectedBlocksCache.add(Blocks.CHEST);
            return Blocks.TRAPPED_CHEST;
         case "modules.settings.xray.ender_chest":
            return Blocks.ENDER_CHEST;
         case "modules.settings.xray.shulker_box":
            selectedBlocksCache.add(Blocks.SHULKER_BOX);
            selectedBlocksCache.add(Blocks.WHITE_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.ORANGE_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.MAGENTA_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.LIGHT_BLUE_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.YELLOW_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.LIME_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.PINK_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.GRAY_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.LIGHT_GRAY_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.CYAN_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.PURPLE_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.BLUE_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.BROWN_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.GREEN_SHULKER_BOX);
            selectedBlocksCache.add(Blocks.RED_SHULKER_BOX);
            return Blocks.BLACK_SHULKER_BOX;
         case "modules.settings.xray.barrel":
            return Blocks.BARREL;
            
         // Механизмы
         case "modules.settings.xray.dispenser":
            return Blocks.DISPENSER;
         case "modules.settings.xray.dropper":
            return Blocks.DROPPER;
         case "modules.settings.xray.piston":
            return Blocks.PISTON;
         case "modules.settings.xray.sticky_piston":
            return Blocks.STICKY_PISTON;
         case "modules.settings.xray.observer":
            return Blocks.OBSERVER;
         case "modules.settings.xray.hopper":
            return Blocks.HOPPER;
            
         // Специальные
         case "modules.settings.xray.spawner":
            return Blocks.SPAWNER;
         case "modules.settings.xray.structure_block":
            return Blocks.STRUCTURE_BLOCK;
         case "modules.settings.xray.jigsaw":
            return Blocks.JIGSAW;
            
         default:
            return null;
      }
   }

   public boolean isBlockEnabled(Block block) {
      if (this.cacheStamp < 0L) {
         this.invalidateCache();
      }
      return this.selectedBlocksCache.contains(block);
   }
}
