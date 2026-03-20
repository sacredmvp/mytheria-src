package moscow.mytheria.utility.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SyncHandler {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    public float lastYaw = 0;
    public float lastPitch = 0;
    public float acceleration = 0;
    public boolean isBack = false;
    
    public void applyRotation(LivingEntity target) {
        if (mc.player == null || target == null) return;
        
        Box box = target.getBoundingBox();
        Vec3d point = box.getCenter();
        Vec3d eyes = mc.player.getEyePos();
        Vec3d vec = point.subtract(eyes);
        
        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));
        
        if (mc.player.isGliding()) {
            if (!isBack) {
                acceleration += 0.005F;
                if (acceleration >= 0.13F) {
                    isBack = true;
                }
            } else {
                if (acceleration >= -0.02F) {
                    acceleration -= 0.005F;
                }
                if (acceleration <= -0.02F) {
                    isBack = false;
                }
            }
        } else {
            Vec3d lookVec = mc.player.getRotationVector();
            boolean canSeeTarget = rayTrace(lookVec, 1488.0, box);
            
            if (!canSeeTarget) {
                acceleration += 0.0015F;
            } else if (acceleration > 0.0F) {
                acceleration -= 0.01F;
            }
        }
        
        float deltaYaw = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float deltaPitch = targetPitch - lastPitch;
        
        float newYaw = lastYaw + deltaYaw;
        float newPitch = lastPitch + deltaPitch;
        
        float gcd = getGCD();
        newYaw -= (newYaw - lastYaw) % gcd;
        newPitch -= (newPitch - lastPitch) % gcd;
        
        float cameraYaw = mc.gameRenderer.getCamera().getYaw();
        float cameraPitch = mc.gameRenderer.getCamera().getPitch();
        
        float deltaYaw2 = MathHelper.wrapDegrees(cameraYaw - lastYaw);
        float deltaPitch2 = cameraPitch - lastPitch;
        
        if (mc.options.getPerspective().isFirstPerson() == false && mc.options.getPerspective().isFrontView()) {
            deltaYaw2 = MathHelper.wrapDegrees(cameraYaw - 180.0F - lastYaw);
            deltaPitch2 = -cameraPitch - lastPitch;
        }
        
        lastYaw = newYaw;
        lastPitch = newPitch;
        
        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
    }
    
    public void reset() {
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
        acceleration = 0.15F;
        isBack = false;
    }
    
    private boolean rayTrace(Vec3d direction, double distance, Box targetBox) {
        Vec3d start = mc.player.getEyePos();
        Vec3d end = start.add(direction.multiply(distance));
        return targetBox.raycast(start, end).isPresent();
    }
    
    private float getGCD() {
        double sens = mc.options.getMouseSensitivity().getValue();
        double f = sens * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }
}
