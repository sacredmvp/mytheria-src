package moscow.mytheria.systems.modules.modules.combat;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.ModeSetting;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

@ModuleInfo(
   name = "Aura",
   category = ModuleCategory.COMBAT,
   desc = "modules.descriptions.aura"
)
public class AutoClicker extends BaseModule {
   private final SliderSetting range = new SliderSetting(this, "modules.settings.aura.range")
      .step(0.1F)
      .min(3.0F)
      .max(6.0F)
      .currentValue(3.8F);
   
   private final BooleanSetting players = new BooleanSetting(this, "modules.settings.aura.players").enabled(true);
   private final BooleanSetting mobs = new BooleanSetting(this, "modules.settings.aura.mobs").enabled(false);
   private final BooleanSetting rotate = new BooleanSetting(this, "modules.settings.aura.rotate").enabled(true);
   
   private final ModeSetting serverMode = new ModeSetting(this, "modules.settings.aura.server");
   private final ModeSetting.Value spaceTimeServer = new ModeSetting.Value(serverMode, "modules.settings.aura.server.spacetime").select();
   private final ModeSetting.Value spookyTimeServer = new ModeSetting.Value(serverMode, "modules.settings.aura.server.spookytime_anarchy");
   
   private final ModeSetting attackMode = new ModeSetting(this, "modules.settings.aura.attackmode");
   private final ModeSetting.Value normalMode = new ModeSetting.Value(attackMode, "modules.settings.aura.normal").select();
   private final ModeSetting.Value onlyCritsMode = new ModeSetting.Value(attackMode, "modules.settings.aura.onlycrits");
   private final ModeSetting.Value maceMode = new ModeSetting.Value(attackMode, "modules.settings.aura.macemode");
   
   private LivingEntity target = null;
   private final Random random = new Random();
   private boolean cameraInitializedForTarget = false;
   
   private float lastYaw = 0;
   private float lastPitch = 0;
   private int hitCount = 0;
   private long startTime = 0;
   private int rotationPauseTicks = 0;
   private float overshootYawCorrection = 0;
   private float overshootPitchCorrection = 0;
   
   private final moscow.mytheria.utility.rotation.SyncHandler syncHandler = new moscow.mytheria.utility.rotation.SyncHandler();
   
   private int sprintResetState = 0;
   private int sprintResetTimer = 0;
   
   private float shakeTime = 0;
   
   public boolean shouldUseFreeCam() {
      return this.isEnabled() && rotate.isEnabled() && target != null;
   }
   
   public LivingEntity getTarget() {
      return target;
   }
   
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (!this.isEnabled() || mc.player == null || mc.world == null) return;
      
      if (sprintResetState > 0) {
         sprintResetTimer--;
         if (sprintResetTimer <= 0) {
            if (sprintResetState == 1) {
               mc.player.setSprinting(true);
               sprintResetState = 0;
            }
         }
      }
      
      LivingEntity oldTarget = target;
      
      if (target != null && (!target.isAlive() || target.isDead() || target.getHealth() <= 0 || target.isRemoved())) {
         target = null;
         cameraInitializedForTarget = false;
      }
      
      target = findTarget();
      
      if (oldTarget != null && target == null) {
         if (mc.player instanceof moscow.mytheria.utility.interfaces.ICameraEntity cameraEntity) {
            mc.player.setYaw(cameraEntity.getCameraYaw());
            mc.player.setPitch(cameraEntity.getCameraPitch());
         }
         cameraInitializedForTarget = false;
      }
      
      if (target == null) return;
      
      double dist = getStrictDistance(target);
      
      if (!cameraInitializedForTarget) {
         if (mc.player instanceof moscow.mytheria.utility.interfaces.ICameraEntity cameraEntity) {
            cameraEntity.setCameraYaw(mc.player.getYaw());
            cameraEntity.setCameraPitch(mc.player.getPitch());
         }
         cameraInitializedForTarget = true;
      }
      
      if (rotate.isEnabled()) {
         if (serverMode.is(spaceTimeServer)) {
            spaceTimeRotation(target);
         } else if (serverMode.is(spookyTimeServer)) {
            spookyTimeRotation(target);
         }
      }
      
      if (dist <= range.getCurrentValue() && sprintResetState == 0) {
         if (attackMode.is(maceMode)) {
            if (!isHoldingMace()) return;
            if (!(target instanceof PlayerEntity)) return;
            if (!canHitTarget(target)) return;
            
            mc.interactionManager.attackEntity(mc.player, target);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.resetLastAttackedTicks();
            
            hitCount++;
            return;
         }
         
         float cooldownProgress = mc.player.getAttackCooldownProgress(0.5f);
         if (cooldownProgress < 0.95f) return;
         
         if (attackMode.is(onlyCritsMode) && !canPerformCriticalHit()) return;
         
         if (!canHitTarget(target)) return;
         
         boolean isCrit = canPerformCriticalHit();
         
         if (isCrit && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            sprintResetState = 1;
            sprintResetTimer = 2 + random.nextInt(2);
         }
         
         mc.interactionManager.attackEntity(mc.player, target);
         mc.player.swingHand(Hand.MAIN_HAND);
         mc.player.resetLastAttackedTicks();
         
         hitCount++;
      }
   };
   
   private void spaceTimeRotation(LivingEntity target) {
      if (rotationPauseTicks > 0) {
         rotationPauseTicks--;
         return;
      }
      
      long uniqueSeed = System.nanoTime() ^ (hitCount * 31L);
      
      float baseHeight = 0.5f;
      float heightOffset = target.getHeight() * baseHeight;
      
      double timeOffset = System.currentTimeMillis() / 350.0;
      double smoothX = Math.sin(timeOffset * 2.5) * 0.15;
      double smoothY = Math.cos(timeOffset * 2.0) * 0.08;
      double smoothZ = Math.sin(timeOffset * 2.3) * 0.15;
      
      Vec3d targetPos = new Vec3d(
         target.getX() + smoothX,
         target.getY() + heightOffset + smoothY,
         target.getZ() + smoothZ
      );
      
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d vec = targetPos.subtract(eyePos);
      
      float yawToTarget = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
      float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));
      
      float yawDelta = MathHelper.wrapDegrees(yawToTarget - lastYaw);
      float pitchDelta = pitchToTarget - lastPitch;
      
      double distanceToTarget = getStrictDistance(target);
      float maxRange = range.getCurrentValue();
      
      float distanceFactor = 1.0f - (float)(distanceToTarget / maxRange);
      distanceFactor = (float)Math.pow(distanceFactor, 0.5);
      
      float angularDist = (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
      
      float baseInterpSpeed = 0.15f + distanceFactor * 0.25f;
      
      float angularSpeedBoost = Math.min(angularDist / 25.0f, 1.2f);
      float interpSpeed = baseInterpSpeed * (0.8f + angularSpeedBoost * 0.5f);
      
      interpSpeed = MathHelper.clamp(interpSpeed, 0.12f, 0.55f);
      
      float jitterAmount = 0.0f;
      if (angularDist > 8.0f) {
         float jitterIntensity = Math.min(angularDist / 40.0f, 1.0f);
         jitterAmount = (float)(Math.random() - 0.5) * 0.08f * jitterIntensity;
      }
      
      float targetYaw = yawToTarget + jitterAmount;
      float targetPitch = MathHelper.clamp(pitchToTarget, -89.0F, 89.0F);
      
      float yawResult = lastYaw + MathHelper.wrapDegrees(targetYaw - lastYaw) * interpSpeed;
      float pitchResult = lastPitch + (targetPitch - lastPitch) * interpSpeed;
      
      float gcd = getGCD();
      yawResult -= (yawResult - lastYaw) % gcd;
      pitchResult -= (pitchResult - lastPitch) % gcd;
      
      yawResult += overshootYawCorrection;
      pitchResult += overshootPitchCorrection;
      overshootYawCorrection *= 0.7f;
      overshootPitchCorrection *= 0.7f;
      
      if (angularDist > 35.0f && (uniqueSeed & 0xFF) < 2) {
         float intensity = (float)Math.min(angularDist / 100.0, 0.15);
         overshootYawCorrection = (float)(Math.random() - 0.5) * intensity * 0.5f;
         overshootPitchCorrection = (float)(Math.random() - 0.5) * intensity * 0.3f;
      }
      
      if (angularDist > 15.0f && (uniqueSeed & 0x3FFF) < 1) {
         rotationPauseTicks = 1;
      }
      
      lastYaw = yawResult;
      lastPitch = pitchResult;
      
      mc.player.setYaw(yawResult);
      mc.player.setPitch(pitchResult);
   }
   
   private void spookyTimeRotation(LivingEntity target) {
      syncHandler.applyRotation(target);
      
      lastYaw = syncHandler.lastYaw;
      lastPitch = syncHandler.lastPitch;
   }
   
   private Vec3d getClosestPoint(LivingEntity entity) {
      Vec3d eyePos = mc.player.getEyePos();
      Box box = entity.getBoundingBox();
      
      double x = MathHelper.clamp(eyePos.x, box.minX, box.maxX);
      double y = MathHelper.clamp(eyePos.y, box.minY, box.maxY);
      double z = MathHelper.clamp(eyePos.z, box.minZ, box.maxZ);
      
      return new Vec3d(x, y, z);
   }
   
   private double getStrictDistance(LivingEntity entity) {
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d closest = getClosestPoint(entity);
      return eyePos.distanceTo(closest);
   }
   
   private float getGCD() {
      double sens = mc.options.getMouseSensitivity().getValue();
      double f = sens * 0.6 + 0.2;
      return (float) (f * f * f * 8.0 * 0.15);
   }
   
   private boolean canPerformCriticalHit() {
      if (mc.player == null) return false;
      
      return mc.player.fallDistance > 0.0f && !mc.player.isOnGround() 
             && !mc.player.isTouchingWater() && !mc.player.hasVehicle()
             && !mc.player.isClimbing();
   }
   
   private boolean isHoldingMace() {
      if (mc.player == null) return false;
      ItemStack heldStack = mc.player.getMainHandStack();
      return !heldStack.isEmpty() && heldStack.getItem().toString().toLowerCase().contains("mace");
   }
   
   private boolean canHitTarget(LivingEntity target) {
      if (mc.player == null || target == null) return false;
      
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d lookVec = mc.player.getRotationVec(1.0f);
      Vec3d endPos = eyePos.add(lookVec.multiply(range.getCurrentValue()));
      
      Box targetBox = target.getBoundingBox().expand(0.3);
      
      return targetBox.raycast(eyePos, endPos).isPresent();
   }
   
   private LivingEntity findTarget() {
      LivingEntity closest = null;
      double closestDist = range.getCurrentValue();
      
      boolean maceOnlyPlayers = attackMode.is(maceMode);
      
      for (Entity entity : mc.world.getEntities()) {
         if (entity == mc.player || !(entity instanceof LivingEntity living)) continue;
         
         if (!living.isAlive() || living.isDead() || living.getHealth() <= 0) continue;
         
         boolean isPlayer = entity instanceof PlayerEntity;
         boolean isMob = entity instanceof MobEntity;
         
         if (maceOnlyPlayers && !isPlayer) continue;
         
         if (isPlayer && !players.isEnabled()) continue;
         if (isMob && !mobs.isEnabled()) continue;
         if (!isPlayer && !isMob) continue;
         
         if (isPlayer) {
            PlayerEntity player = (PlayerEntity) entity;
            if (Mytheria.getInstance().getFriendManager().isFriend(player.getName().getString())) {
               continue;
            }
         }
         
         double dist = getStrictDistance(living);
         if (dist < closestDist) {
            closest = living;
            closestDist = dist;
         }
      }
      
      return closest;
   }

   @Override
   public void onEnable() {
      target = null;
      cameraInitializedForTarget = false;
      lastYaw = mc.player != null ? mc.player.getYaw() : 0;
      lastPitch = mc.player != null ? mc.player.getPitch() : 0;
      hitCount = 0;
      startTime = System.currentTimeMillis();
      rotationPauseTicks = 0;
      overshootYawCorrection = 0;
      overshootPitchCorrection = 0;
      shakeTime = 0;
      sprintResetState = 0;
      sprintResetTimer = 0;
      
      syncHandler.reset();
   }

   @Override
   public void onDisable() {
      target = null;
      cameraInitializedForTarget = false;
      
      if (mc.player != null) {
         if (mc.player instanceof moscow.mytheria.utility.interfaces.ICameraEntity cameraEntity) {
            mc.player.setYaw(cameraEntity.getCameraYaw());
            mc.player.setPitch(cameraEntity.getCameraPitch());
         }
      }
   }
}
