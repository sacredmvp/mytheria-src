package moscow.mytheria.systems.modules.modules.combat;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleInfo(
   name = "TriggerBot",
   category = ModuleCategory.COMBAT,
   desc = "modules.descriptions.iggerbottr"
)
public class DebugHelper extends BaseModule {
   private final BooleanSetting onlyPlayers = new BooleanSetting(this, "modules.settings.triggerbot.onlyplayers").enabled(true);
   private final BooleanSetting critOnly = new BooleanSetting(this, "modules.settings.triggerbot.critonly").enabled(false);
   private final BooleanSetting attackInvisible = new BooleanSetting(this, "modules.settings.triggerbot.attackinvisible").enabled(false);
   
   // Fixed values (not in settings)
   private static final float ATTACK_RANGE = 3.0F;
   private static final long MIN_DELAY = 35L;
   private static final long MAX_DELAY = 85L;
   private static final boolean CHECK_COOLDOWN = true;
   
   // Attack scheduling
   private Entity scheduledTarget = null;
   private Entity lastTarget = null;
   private long scheduledAttackTime = 0;
   private long lastAttackTime = 0;
   private long lastHitTime = 0;
   private long attackDelay = 50;
   private int consecutiveHits = 0;
   private int hitPattern = 0;
   
   // Item tracking
   private Item lastHeldItem = null;
   private long itemSwitchTime = 0;
   
   // State flags
   private boolean shouldAttack = false;
   private boolean isSprintPaused = false;
   private boolean wasSprintingBeforeAttack = false;
   
   // Executor for sprint reset
   private final ScheduledExecutorService sprintExecutor = Executors.newSingleThreadScheduledExecutor();
   
   // Constants
   private static final long ITEM_SWITCH_DELAY = 150L;
   
   public LivingEntity target = null;
   
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (!this.isEnabled() || mc.player == null || mc.world == null) {
         resetSprintState();
         return;
      }
      
      if (mc.currentScreen != null) {
         return;
      }
      
      long currentTime = System.currentTimeMillis();
      
      // Handle scheduled attacks
      if (shouldAttack && scheduledTarget != null && mc.interactionManager != null) {
         if (currentTime >= scheduledAttackTime && isValidTarget(scheduledTarget)) {
            executeAttack(scheduledTarget);
         }
         shouldAttack = false;
         scheduledTarget = null;
      }
      
      // Track item switches
      Item currentItem = mc.player.getMainHandStack().getItem();
      if (lastHeldItem != currentItem) {
         lastHeldItem = currentItem;
         itemSwitchTime = currentTime;
      }
      
      // Wait after item switch
      if (currentTime - itemSwitchTime < ITEM_SWITCH_DELAY) return;
      
      // Reset consecutive hits after timeout
      if (currentTime - lastHitTime > 2000) {
         consecutiveHits = 0;
      }
      
      // Check if player is using item
      if (isUsingItem() || !isHoldingWeapon()) return;
      
      // Get crosshair target
      HitResult hitResult = mc.crosshairTarget;
      if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
         lastTarget = null;
         resetSprintState();
         return;
      }
      
      Entity targetEntity = ((EntityHitResult) hitResult).getEntity();
      
      // Check if only players mode is enabled
      if (onlyPlayers.isEnabled() && !(targetEntity instanceof PlayerEntity)) {
         resetSprintState();
         return;
      }
      
      // Check if target is a valid player
      if (targetEntity instanceof PlayerEntity targetPlayer) {
         if (!isValidPlayer(targetPlayer)) {
            resetSprintState();
            return;
         }
      }
      
      // Check if target is alive and not invulnerable
      if (!(targetEntity instanceof LivingEntity) || !targetEntity.isAlive() || targetEntity.isInvulnerable()) {
         resetSprintState();
         return;
      }
      
      // Check distance
      double distance = mc.player.distanceTo(targetEntity);
      if (distance > ATTACK_RANGE) {
         resetSprintState();
         return;
      }
      
      // Check attack delay
      if (currentTime - lastAttackTime < attackDelay) return;
      
      float cooldownProgress = mc.player.getAttackCooldownProgress(0.5f);
      boolean isFalling = isFallingForCrit();
      boolean isOnGround = mc.player.isOnGround();
      
      if (target == lastTarget && currentTime - lastHitTime < 1000) {
         consecutiveHits++;
      } else {
         consecutiveHits = 1;
         hitPattern = 0;
      }
      
      hitPattern++;
      int patternIndex = (hitPattern - 1) % 3;
      
      if (CHECK_COOLDOWN) {
         float requiredCooldown = 0.88f;
         
         if (isHoldingMace()) {
            requiredCooldown = 0.92f;
         } else if (patternIndex < 2) {
            if (isFalling) {
               requiredCooldown = 0.85f;
            } else if (isOnGround) {
               requiredCooldown = 0.93f;
            }
         } else {
            if (isFalling) {
               requiredCooldown = 0.85f;
            } else if (isOnGround) {
               if (distance < 2.5 && cooldownProgress >= 0.98f) {
                  requiredCooldown = 0.98f;
               } else {
                  return;
               }
            }
         }
         
         if (cooldownProgress < requiredCooldown) return;
      }
      
      if (critOnly.isEnabled() && !isFalling) return;
      
      // Pause sprint always (no setting, always enabled)
      pauseSprintNow(true);
      
      scheduleAttack(targetEntity, currentTime);
   };
   
   @Override
   public void onEnable() {
      scheduledTarget = null;
      lastTarget = null;
      shouldAttack = false;
      isSprintPaused = false;
      hitPattern = 0;
   }
   
   @Override
   public void onDisable() {
      scheduledTarget = null;
      lastTarget = null;
      hitPattern = 0;
      resetSprintState();
   }
   
   private void pauseSprintNow(boolean shouldPause) {
      if (mc.player == null) return;
      
      if (shouldPause) {
         if (mc.player.isSprinting() && mc.options.forwardKey.isPressed()) {
            wasSprintingBeforeAttack = true;
            mc.player.setSprinting(false);
            isSprintPaused = true;
         } else {
            wasSprintingBeforeAttack = false;
            isSprintPaused = false;
         }
      }
   }
   
   private void scheduleSprint() {
      if (!isSprintPaused || mc.player == null) return;
      
      if (!mc.options.forwardKey.isPressed()) {
         resetSprintState();
         return;
      }
      
      long delay = 20 + (long)(Math.random() * 15);
      boolean shouldRestore = wasSprintingBeforeAttack;
      
      sprintExecutor.schedule(() -> {
         if (mc.player != null && shouldRestore && mc.currentScreen == null && mc.options.forwardKey.isPressed()) {
            mc.execute(() -> {
               if (mc.player != null) {
                  mc.player.setSprinting(true);
               }
               isSprintPaused = false;
               wasSprintingBeforeAttack = false;
            });
         }
      }, delay, TimeUnit.MILLISECONDS);
   }
   
   private void resetSprintState() {
      if (isSprintPaused && mc.player != null && mc.options.forwardKey.isPressed() && wasSprintingBeforeAttack) {
         mc.player.setSprinting(true);
      }
      isSprintPaused = false;
      wasSprintingBeforeAttack = false;
   }
   
   private void scheduleAttack(Entity target, long currentTime) {
      if (mc.player == null) return;
      
      scheduledTarget = target;
      long tickOffset = 5 + (long)(Math.random() * 15);
      
      if (target == lastTarget && currentTime - lastHitTime < 1500) {
         consecutiveHits++;
         if (consecutiveHits > 3) {
            tickOffset += (long)(Math.random() * 20);
         }
      } else {
         consecutiveHits = 1;
      }
      
      tickOffset = Math.max(5, tickOffset);
      scheduledAttackTime = currentTime + tickOffset;
      shouldAttack = true;
      lastTarget = target;
      lastHitTime = currentTime;
      lastAttackTime = currentTime;
      
      attackDelay = (long)(MIN_DELAY + Math.random() * (MAX_DELAY - MIN_DELAY));
      
      if (attackDelay < 20) attackDelay = 20;
   }
   
   private void executeAttack(Entity target) {
      if (mc.player == null || mc.interactionManager == null) return;
      
      try {
         mc.interactionManager.attackEntity(mc.player, target);
         mc.player.swingHand(Hand.MAIN_HAND);
         scheduleSprint();
      } catch (Exception e) {
         // Ignore exceptions
      }
   }
   
   private boolean isValidTarget(Entity target) {
      if (target == null || mc.player == null) return false;
      if (!(target instanceof LivingEntity livingEntity)) return false;
      return livingEntity.isAlive() && !livingEntity.isInvulnerable() && mc.player.distanceTo(target) <= ATTACK_RANGE;
   }
   
   private boolean isValidPlayer(PlayerEntity player) {
      try {
         if (player.getGameProfile() == null || player.getGameProfile().getId() == null) return false;
         
         // Check if player is a friend
         if (Mytheria.getInstance().getFriendManager().isFriend(player.getName().getString())) {
            return false;
         }
         
         // Check if player is invisible without armor
         if (!attackInvisible.isEnabled() && player.isInvisible()) {
            // Check if player has any armor
            boolean hasArmor = false;
            for (net.minecraft.item.ItemStack armorStack : player.getArmorItems()) {
               if (!armorStack.isEmpty()) {
                  hasArmor = true;
                  break;
               }
            }
            
            // If invisible and no armor, don't attack
            if (!hasArmor) {
               return false;
            }
         }
         
         String uuid = player.getGameProfile().getId().toString();
         if (uuid == null || uuid.isEmpty() || uuid.equals("00000000-0000-0000-0000-000000000000")) {
            return false;
         }
         
         String name = player.getGameProfile().getName();
         return name != null && !name.isEmpty();
      } catch (Exception e) {
         return false;
      }
   }
   
   private boolean isHoldingWeapon() {
      if (mc.player == null) return false;
      ItemStack heldStack = mc.player.getMainHandStack();
      if (heldStack.isEmpty()) return false;
      Item item = heldStack.getItem();
      return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem || item.toString().toLowerCase().contains("mace");
   }
   
   private boolean isHoldingMace() {
      if (mc.player == null) return false;
      ItemStack heldStack = mc.player.getMainHandStack();
      return !heldStack.isEmpty() && heldStack.getItem().toString().toLowerCase().contains("mace");
   }
   
   private boolean isUsingItem() {
      if (mc.player == null || !mc.player.isUsingItem()) return false;
      Item activeItem = mc.player.getStackInHand(mc.player.getActiveHand()).getItem();
      return activeItem.getComponents().contains(DataComponentTypes.FOOD) ||
             activeItem instanceof PotionItem ||
             activeItem instanceof ShieldItem ||
             activeItem instanceof BowItem ||
             activeItem instanceof CrossbowItem ||
             activeItem instanceof TridentItem ||
             activeItem instanceof FishingRodItem ||
             activeItem instanceof SpyglassItem;
   }
   
   private boolean isFallingForCrit() {
      if (mc.player == null || mc.player.hasVehicle()) return false;
      if (mc.player.isOnGround()) return false;
      if (mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing()) return false;
      
      double velocityY = mc.player.getVelocity().y;
      if (velocityY >= 0) return false;
      
      double fallSpeed = Math.abs(velocityY);
      return fallSpeed >= 0.12 && fallSpeed <= 1.3;
   }
}
