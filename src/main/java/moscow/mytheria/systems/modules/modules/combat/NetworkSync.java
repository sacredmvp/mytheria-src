package moscow.mytheria.systems.modules.modules.combat;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.render.Render3DEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.SelectSetting;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.utility.interfaces.IMinecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.*;

@ModuleInfo(
   name = "AimAssist",
   category = ModuleCategory.COMBAT,
   desc = "modules.descriptions.aimassist"
)
public class NetworkSync extends BaseModule implements IMinecraft {
    
    // Настройки скорости
    private final SliderSetting horizontalSpeed;
    private final SliderSetting verticalSpeed;
    
    // Настройки таргетинга
    private final SliderSetting fov;
    private final SliderSetting distance;
    private final SelectSetting targetPriority;
    private final SelectSetting.Value priorityDistance;
    private final SelectSetting.Value priorityAngle;
    private final SelectSetting.Value priorityHealth;
    private final SelectSetting aimPosition;
    private final SelectSetting.Value positionHead;
    private final SelectSetting.Value positionBody;
    private final SelectSetting.Value positionLegs;
    
    // Настройки поведения
    private final BooleanSetting verticalAim;
    private final BooleanSetting onlyWeapon;
    private final BooleanSetting predictMovement;
    private final BooleanSetting randomMovement;
    private final SliderSetting spreadAmount;
    private final SliderSetting missChance;
    
    // Настройки паттернов
    private final SelectSetting smoothingMode;
    private final SelectSetting.Value smoothingLinear;
    private final SelectSetting.Value smoothingSpring;
    private final SelectSetting.Value smoothingBezier;
    private final SelectSetting.Value smoothingExponential;
    private final SliderSetting patternIntensity;
    
    // Мультипоинт
    private final BooleanSetting multiPoint;
    private final SliderSetting multiPointCount;
    
    // Дополнительные настройки
    private final BooleanSetting breakOnKill;
    private final SliderSetting breakDelay;
    
    // === SMOOTHING (60 FPS spring-damper система) ===
    private float velocityYaw = 0.0f;
    private float velocityPitch = 0.0f;
    private float noiseTime = 0.0f;
    private long lastFrameTime = 0;
    private final Random random = new Random();

    // === TARGET LOCK & PREDICTION ===
    private PlayerEntity currentTarget = null;
    private Vec3d lastTargetPos = null;
    private Vec3d targetVelocity = Vec3d.ZERO;
    private long targetLockTime = 0;
    private static final long TARGET_LOCK_DURATION = 4000;
    
    // === MULTIPOINT ===
    private int currentMultiPointIndex = 0;
    private long lastMultiPointSwitch = 0;
    private static final long MULTIPOINT_SWITCH_INTERVAL = 150;
    
    // === BEZIER ===
    private float bezierProgress = 0f;
    private float bezierStartYaw = 0f;
    private float bezierStartPitch = 0f;
    private float bezierTargetYaw = 0f;
    private float bezierTargetPitch = 0f;
    
    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (!this.isEnabled() || mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) {
            resetAimState();
            return;
        }
        renderAimAssist();
    };
    
    public NetworkSync() {
        // Скорость
        horizontalSpeed = new SliderSetting(this, "modules.settings.aimassist.horizontal_speed")
            .step(0.1F)
            .min(0.5F)
            .max(10F)
            .currentValue(3.0F);
        
        verticalSpeed = new SliderSetting(this, "modules.settings.aimassist.vertical_speed")
            .step(0.1F)
            .min(0.3F)
            .max(8F)
            .currentValue(2.2F);
        
        // Таргетинг
        fov = new SliderSetting(this, "modules.settings.aimassist.fov")
            .step(5F)
            .min(10F)
            .max(360F)
            .currentValue(90F);
        
        distance = new SliderSetting(this, "modules.settings.aimassist.distance")
            .step(0.5F)
            .min(1F)
            .max(10F)
            .currentValue(4.5F);
        
        targetPriority = new SelectSetting(this, "modules.settings.aimassist.target_priority");
        priorityDistance = new SelectSetting.Value(targetPriority, "modules.settings.aimassist.priority.distance").select();
        priorityAngle = new SelectSetting.Value(targetPriority, "modules.settings.aimassist.priority.angle");
        priorityHealth = new SelectSetting.Value(targetPriority, "modules.settings.aimassist.priority.health");
        
        aimPosition = new SelectSetting(this, "modules.settings.aimassist.aim_position");
        positionHead = new SelectSetting.Value(aimPosition, "modules.settings.aimassist.position.head");
        positionBody = new SelectSetting.Value(aimPosition, "modules.settings.aimassist.position.body").select();
        positionLegs = new SelectSetting.Value(aimPosition, "modules.settings.aimassist.position.legs");
        
        // Поведение
        verticalAim = new BooleanSetting(this, "modules.settings.aimassist.vertical_aim").enabled(false);
        onlyWeapon = new BooleanSetting(this, "modules.settings.aimassist.only_weapon").enabled(true);
        predictMovement = new BooleanSetting(this, "modules.settings.aimassist.predict_movement").enabled(false);
        randomMovement = new BooleanSetting(this, "modules.settings.aimassist.random_movement").enabled(true);
        
        spreadAmount = new SliderSetting(this, "modules.settings.aimassist.spread_amount")
            .step(0.5F)
            .min(0F)
            .max(10F)
            .currentValue(2.0F);
        
        missChance = new SliderSetting(this, "modules.settings.aimassist.miss_chance")
            .step(1F)
            .min(0F)
            .max(25F)
            .currentValue(5F);
        
        // Паттерны
        smoothingMode = new SelectSetting(this, "modules.settings.aimassist.smoothing_mode");
        smoothingLinear = new SelectSetting.Value(smoothingMode, "modules.settings.aimassist.smoothing.linear");
        smoothingSpring = new SelectSetting.Value(smoothingMode, "modules.settings.aimassist.smoothing.spring_damper").select();
        smoothingBezier = new SelectSetting.Value(smoothingMode, "modules.settings.aimassist.smoothing.bezier");
        smoothingExponential = new SelectSetting.Value(smoothingMode, "modules.settings.aimassist.smoothing.exponential");
        
        patternIntensity = new SliderSetting(this, "modules.settings.aimassist.pattern_intensity")
            .step(0.1F)
            .min(0F)
            .max(5F)
            .currentValue(2.0F);
        
        // Мультипоинт
        multiPoint = new BooleanSetting(this, "modules.settings.aimassist.multipoint").enabled(true);
        
        multiPointCount = new SliderSetting(this, "modules.settings.aimassist.multipoint_count")
            .step(1F)
            .min(3F)
            .max(9F)
            .currentValue(3F);
        
        // Дополнительно
        breakOnKill = new BooleanSetting(this, "modules.settings.aimassist.break_on_kill").enabled(true);
        
        breakDelay = new SliderSetting(this, "modules.settings.aimassist.break_delay")
            .step(1F)
            .min(5F)
            .max(40F)
            .currentValue(20F);
    }
    
    @Override
    public void onEnable() {
        resetAimState();
    }
    
    @Override
    public void onDisable() {
        resetAimState();
    }
    
    private void resetAimState() {
        velocityYaw = 0;
        velocityPitch = 0;
        lastFrameTime = 0;
        currentTarget = null;
        lastTargetPos = null;
        targetVelocity = Vec3d.ZERO;
        targetLockTime = 0;
        noiseTime = 0;
        currentMultiPointIndex = 0;
        lastMultiPointSwitch = 0;
        bezierProgress = 0;
    }
    
    private boolean isTargetValid(PlayerEntity target) {
        if (target == null) return false;
        if (target == mc.player) return false;
        if (!target.isAlive()) return false;
        if (target.isRemoved()) return false;

        // Проверка на друга
        if (Mytheria.getInstance().getFriendManager().isFriend(target.getName().getString())) {
            return false;
        }

        double dist = mc.player.distanceTo(target);
        if (dist > distance.getCurrentValue() || dist < 0.5) return false;

        float angle = (float) getAngleTo(target);
        if (angle > fov.getCurrentValue() * 0.85f) return false;

        return true;
    }
    
    private PlayerEntity findOrKeepTarget() {
        long currentTime = System.currentTimeMillis();

        if (currentTarget != null) {
            if (isTargetValid(currentTarget)) {
                if (currentTime - targetLockTime < TARGET_LOCK_DURATION) {
                    return currentTarget;
                }
            } else {
                currentTarget = null;
                lastTargetPos = null;
                targetVelocity = Vec3d.ZERO;
                targetLockTime = 0;
            }
        }

        PlayerEntity newTarget = findTarget();

        if (newTarget != null && newTarget != currentTarget) {
            currentTarget = newTarget;
            lastTargetPos = null;
            targetVelocity = Vec3d.ZERO;
            targetLockTime = currentTime;
        } else if (newTarget == null) {
            currentTarget = null;
            lastTargetPos = null;
            targetVelocity = Vec3d.ZERO;
            targetLockTime = 0;
        }

        return currentTarget;
    }
    
    private void renderAimAssist() {
        if (mc.player == null || mc.world == null) return;

        long currentTime = System.nanoTime();
        float deltaTime;
        if (lastFrameTime == 0) {
            deltaTime = 0.016f;
        } else {
            deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f;
            deltaTime = MathHelper.clamp(deltaTime, 0.001f, 0.1f);
        }
        lastFrameTime = currentTime;

        noiseTime += (0.05f + random.nextFloat() * 0.02f) * deltaTime * 60f;

        if (random.nextInt(100) < missChance.getCurrentValue()) {
            decayVelocity(deltaTime);
            return;
        }

        if (onlyWeapon.isEnabled() && !hasWeaponInHand()) {
            decayVelocity(deltaTime);
            return;
        }

        PlayerEntity target = findOrKeepTarget();
        if (target == null) {
            decayVelocity(deltaTime);
            return;
        }

        if (predictMovement.isEnabled()) {
            updateTargetVelocity(target);
        }

        float[] angles = calculateAimAngles(target);
        if (angles == null) {
            decayVelocity(deltaTime);
            return;
        }

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff = wrapDegrees(angles[0] - currentYaw);
        float pitchDiff = angles[1] - currentPitch;

        if (Math.abs(yawDiff) > fov.getCurrentValue() / 2) {
            decayVelocity(deltaTime);
            return;
        }

        applySmoothAim(deltaTime, yawDiff, pitchDiff, currentYaw, currentPitch);
    }
    
    private void applySmoothAim(float dt, float yawDiff, float pitchDiff, float currentYaw, float currentPitch) {
        if (smoothingLinear.isSelected()) {
            applyLinearAim(dt, yawDiff, pitchDiff, currentYaw, currentPitch);
        } else if (smoothingSpring.isSelected()) {
            applySpringDamperAim(dt, yawDiff, pitchDiff, currentYaw, currentPitch);
        } else if (smoothingBezier.isSelected()) {
            applyBezierAim(dt, yawDiff, pitchDiff, currentYaw, currentPitch);
        } else if (smoothingExponential.isSelected()) {
            applyExponentialAim(dt, yawDiff, pitchDiff, currentYaw, currentPitch);
        }
    }
    
    private void applyLinearAim(float dt, float yawDiff, float pitchDiff, float currentYaw, float currentPitch) {
        float moveYaw = yawDiff * horizontalSpeed.getCurrentValue() * dt * 12f;
        float movePitch = pitchDiff * verticalSpeed.getCurrentValue() * dt * 8.5f;
        
        if (verticalAim.isEnabled() && Math.abs(pitchDiff) > 0.5f) {
            float pitchFactor = 1.0f - (float)Math.exp(-Math.abs(pitchDiff) * 0.15f);
            movePitch *= pitchFactor;
        }
        
        moveYaw = MathHelper.clamp(moveYaw, -horizontalSpeed.getCurrentValue() * 2f, horizontalSpeed.getCurrentValue() * 2f);
        movePitch = MathHelper.clamp(movePitch, -verticalSpeed.getCurrentValue() * 1.5f, verticalSpeed.getCurrentValue() * 1.5f);
        
        if (Math.abs(moveYaw) < 0.02f) moveYaw = 0;
        if (Math.abs(movePitch) < 0.015f) movePitch = 0;
        
        if (Math.abs(moveYaw) > 0.001f || Math.abs(movePitch) > 0.001f) {
            mc.player.setYaw(currentYaw + moveYaw);
            if (verticalAim.isEnabled()) {
                mc.player.setPitch(MathHelper.clamp(currentPitch + movePitch, -90f, 90f));
            }
        }
    }
    
    private void applyBezierAim(float dt, float yawDiff, float pitchDiff, float currentYaw, float currentPitch) {
        float targetYaw = currentYaw + yawDiff;
        float targetPitch = currentPitch + pitchDiff;
        
        if (bezierProgress >= 1f || Math.abs(wrapDegrees(targetYaw - bezierTargetYaw)) > 5f) {
            bezierStartYaw = currentYaw;
            bezierStartPitch = currentPitch;
            bezierTargetYaw = targetYaw;
            bezierTargetPitch = targetPitch;
            bezierProgress = 0f;
        }
        
        float speed = (horizontalSpeed.getCurrentValue() + verticalSpeed.getCurrentValue()) * 0.5f;
        bezierProgress += dt * speed * 1.5f;
        bezierProgress = Math.min(bezierProgress, 1f);
        
        float t = bezierProgress;
        float eased = (float) (1 - Math.pow(1 - t, 3));
        
        float newYaw = bezierStartYaw + wrapDegrees(bezierTargetYaw - bezierStartYaw) * eased;
        float newPitch = bezierStartPitch + (bezierTargetPitch - bezierStartPitch) * eased;
        
        mc.player.setYaw(newYaw);
        if (verticalAim.isEnabled()) {
            mc.player.setPitch(MathHelper.clamp(newPitch, -90f, 90f));
        }
    }
    
    private void applyExponentialAim(float dt, float yawDiff, float pitchDiff, float currentYaw, float currentPitch) {
        float factorYaw = 1f - (float) Math.exp(-horizontalSpeed.getCurrentValue() * dt * 10f);
        float factorPitch = 1f - (float) Math.exp(-verticalSpeed.getCurrentValue() * dt * 8f);
        
        float moveYaw = yawDiff * factorYaw;
        float movePitch = pitchDiff * factorPitch;
        
        if (verticalAim.isEnabled() && Math.abs(pitchDiff) > 0.5f) {
            movePitch *= 0.85f;
        }
        
        if (Math.abs(moveYaw) < 0.02f) moveYaw = 0;
        if (Math.abs(movePitch) < 0.015f) movePitch = 0;
        
        if (Math.abs(moveYaw) > 0.001f || Math.abs(movePitch) > 0.001f) {
            mc.player.setYaw(currentYaw + moveYaw);
            if (verticalAim.isEnabled()) {
                mc.player.setPitch(MathHelper.clamp(currentPitch + movePitch, -90f, 90f));
            }
        }
    }
    
    private void applySpringDamperAim(float dt, float yawDiff, float pitchDiff, float currentYaw, float currentPitch) {
        float stiffnessYaw = horizontalSpeed.getCurrentValue() * 12f;
        float stiffnessPitch = verticalSpeed.getCurrentValue() * 9f;
        float dampingYaw = 8f;
        float dampingPitch = 10f;

        float forceYaw = yawDiff * stiffnessYaw;
        float forcePitch = pitchDiff * stiffnessPitch;

        velocityYaw += forceYaw * dt;
        velocityPitch += forcePitch * dt;

        float dampFactorYaw = (float) Math.exp(-dampingYaw * dt);
        float dampFactorPitch = (float) Math.exp(-dampingPitch * dt);
        velocityYaw *= dampFactorYaw;
        velocityPitch *= dampFactorPitch;

        float maxVelYaw = horizontalSpeed.getCurrentValue() * 60f;
        float maxVelPitch = verticalSpeed.getCurrentValue() * 40f;
        velocityYaw = MathHelper.clamp(velocityYaw, -maxVelYaw, maxVelYaw);
        velocityPitch = MathHelper.clamp(velocityPitch, -maxVelPitch, maxVelPitch);

        float moveYaw = velocityYaw * dt;
        float movePitch = velocityPitch * dt;

        float maxMoveYaw = horizontalSpeed.getCurrentValue() * 1.5f;
        float maxMovePitch = verticalSpeed.getCurrentValue() * 1.0f;
        moveYaw = MathHelper.clamp(moveYaw, -maxMoveYaw, maxMoveYaw);
        movePitch = MathHelper.clamp(movePitch, -maxMovePitch, maxMovePitch);

        if (randomMovement.isEnabled() && spreadAmount.getCurrentValue() > 0) {
            float wobbleYaw = noise(noiseTime) * spreadAmount.getCurrentValue() * 0.008f;
            float wobblePitch = noise(noiseTime * 0.7f) * spreadAmount.getCurrentValue() * 0.003f;
            moveYaw += wobbleYaw;
            if (verticalAim.isEnabled()) {
                movePitch += wobblePitch;
            }
        }
        
        if (patternIntensity.getCurrentValue() > 0.01f) {
            float patternYaw = noise(noiseTime * 0.5f) * patternIntensity.getCurrentValue() * 0.01f;
            float patternPitch = noise(noiseTime * 0.3f) * patternIntensity.getCurrentValue() * 0.006f;
            moveYaw += patternYaw;
            if (verticalAim.isEnabled()) {
                movePitch += patternPitch;
            }
        }

        if (Math.abs(moveYaw) < 0.02f) moveYaw = 0;
        if (Math.abs(movePitch) < 0.015f) movePitch = 0;

        if (Math.abs(moveYaw) > 0.001f || Math.abs(movePitch) > 0.001f) {
            mc.player.setYaw(currentYaw + moveYaw);
            if (verticalAim.isEnabled()) {
                mc.player.setPitch(MathHelper.clamp(currentPitch + movePitch, -90f, 90f));
            }
        }
    }
    
    private void decayVelocity(float dt) {
        float decay = (float) Math.exp(-12f * dt);
        velocityYaw *= decay;
        velocityPitch *= decay;
    }
    
    private void updateTargetVelocity(PlayerEntity target) {
        Vec3d currentPos = target.getPos();
        if (lastTargetPos != null) {
            Vec3d newVel = currentPos.subtract(lastTargetPos);
            targetVelocity = targetVelocity.multiply(0.7).add(newVel.multiply(0.3));
        }
        lastTargetPos = currentPos;
    }
    
    private PlayerEntity findTarget() {
        List<PlayerEntity> targets = new ArrayList<>();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;
            
            if (Mytheria.getInstance().getFriendManager().isFriend(player.getName().getString())) {
                continue;
            }

            double dist = mc.player.distanceTo(player);
            if (dist > distance.getCurrentValue() || dist < 0.5) continue;

            if (!canSeeTarget(player)) continue;

            targets.add(player);
        }

        if (targets.isEmpty()) return null;

        if (priorityDistance.isSelected()) {
            targets.sort(Comparator.comparingDouble(p -> mc.player.distanceTo(p)));
        } else if (priorityAngle.isSelected()) {
            targets.sort(Comparator.comparingDouble(this::getAngleTo));
        } else if (priorityHealth.isSelected()) {
            targets.sort(Comparator.comparingDouble(PlayerEntity::getHealth));
        }

        PlayerEntity bestTarget = targets.get(0);
        if (mc.player != null) {
            float cooldown = mc.player.getAttackCooldownProgress(0.5f);
            if (cooldown >= 0.85f) {
                for (PlayerEntity target : targets) {
                    if (canHitTarget(target)) {
                        return target;
                    }
                }
            }
        }

        return bestTarget;
    }
    
    private boolean canHitTarget(PlayerEntity target) {
        if (mc.player == null) return false;
        
        double dist = mc.player.distanceTo(target);
        if (dist > 3.5) return false;
        
        double angle = getAngleTo(target);
        return angle <= 45.0;
    }
    
    private double getAngleTo(PlayerEntity target) {
        Vec3d diff = target.getPos().subtract(mc.player.getPos());
        float targetYaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        return Math.abs(wrapDegrees(targetYaw - mc.player.getYaw()));
    }
    
    private boolean canSeeTarget(PlayerEntity target) {
        if (mc.player == null || mc.world == null) return false;
        
        Vec3d eyePos = mc.player.getEyePos();
        Box targetBox = target.getBoundingBox();
        
        Vec3d targetCenter = targetBox.getCenter();
        if (hasLineOfSight(eyePos, targetCenter)) return true;
        
        Vec3d targetHead = new Vec3d(targetCenter.x, targetBox.maxY - 0.1, targetCenter.z);
        if (hasLineOfSight(eyePos, targetHead)) return true;
        
        Vec3d targetFeet = new Vec3d(targetCenter.x, targetBox.minY + 0.1, targetCenter.z);
        if (hasLineOfSight(eyePos, targetFeet)) return true;
        
        double offset = (targetBox.getMaxPos().x - targetBox.getMinPos().x) * 0.3;
        Vec3d[] corners = {
            new Vec3d(targetCenter.x + offset, targetCenter.y, targetCenter.z),
            new Vec3d(targetCenter.x - offset, targetCenter.y, targetCenter.z),
            new Vec3d(targetCenter.x, targetCenter.y, targetCenter.z + offset),
            new Vec3d(targetCenter.x, targetCenter.y, targetCenter.z - offset)
        };
        
        for (Vec3d corner : corners) {
            if (hasLineOfSight(eyePos, corner)) return true;
        }
        
        return false;
    }
    
    private boolean hasLineOfSight(Vec3d from, Vec3d to) {
        RaycastContext context = new RaycastContext(
            from,
            to,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            mc.player
        );
        
        BlockHitResult result = mc.world.raycast(context);
        return result.getType() == HitResult.Type.MISS;
    }
    
    private float[] calculateAimAngles(PlayerEntity target) {
        double eyeY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        float height = target.getHeight();
        
        float aimHeight;
        if (positionHead.isSelected()) {
            aimHeight = height * 0.85f;
        } else if (positionBody.isSelected()) {
            aimHeight = height * 0.5f;
        } else if (positionLegs.isSelected()) {
            aimHeight = 0.2f;
        } else {
            aimHeight = height * 0.85f; // По умолчанию голова
        }

        Vec3d targetPos = target.getPos().add(0, aimHeight, 0);
        
        if (multiPoint.isEnabled()) {
            int pointCount = (int) multiPointCount.getCurrentValue();
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastMultiPointSwitch > MULTIPOINT_SWITCH_INTERVAL) {
                int newIndex;
                do {
                    newIndex = random.nextInt(pointCount);
                } while (newIndex == currentMultiPointIndex && pointCount > 1);
                
                currentMultiPointIndex = newIndex;
                lastMultiPointSwitch = currentTime;
            }
            
            float width = target.getWidth();
            double angle = (Math.PI * 2 * currentMultiPointIndex) / pointCount;
            double offsetX = Math.cos(angle) * width * 0.4;
            double offsetZ = Math.sin(angle) * width * 0.4;
            
            targetPos = targetPos.add(offsetX, 0, offsetZ);
        }

        if (predictMovement.isEnabled()) {
            float predictionFactor = MathHelper.clamp(mc.player.distanceTo(target) * 0.1f, 0f, 0.5f);
            targetPos = targetPos.add(targetVelocity.multiply(predictionFactor * 3));
        }

        double dx = targetPos.x - mc.player.getX();
        double dy = targetPos.y - eyeY;
        double dz = targetPos.z - mc.player.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        if (horizDist < 0.01) return null;

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = MathHelper.clamp((float) Math.toDegrees(Math.atan2(-dy, horizDist)), -90f, 90f);

        return new float[]{yaw, pitch};
    }
    
    private float wrapDegrees(float angle) {
        angle = angle % 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }
    
    private float noise(float t) {
        return (float)(Math.sin(t * 1.7) * 0.5 + Math.sin(t * 3.1) * 0.3 + Math.sin(t * 7.3) * 0.2);
    }

    private boolean hasWeaponInHand() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem ||
               item instanceof TridentItem || item.toString().toLowerCase().contains("mace");
    }
}
