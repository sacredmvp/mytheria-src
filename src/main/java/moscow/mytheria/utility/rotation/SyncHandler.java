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
    
    public void applyRotation(LivingEntity target) {
        if (mc.player == null || target == null) return;
        
        Box box = target.getBoundingBox();
        Vec3d point = box.getCenter();
        Vec3d eyes = mc.player.getEyePos();
        Vec3d vec = point.subtract(eyes);
        
        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(vec.y, Math.sqrt(vec.x * vec.x + vec.z * vec.z))));
        
        float gcd = getGCD();
        targetYaw -= (targetYaw - lastYaw) % gcd;
        targetPitch -= (targetPitch - lastPitch) % gcd;
        
        lastYaw = targetYaw;
        lastPitch = targetPitch;
        
        mc.player.setYaw(targetYaw);
        mc.player.setPitch(targetPitch);
    }
    
    public void reset() {
        if (mc.player != null) {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }
    
    private float getGCD() {
        double sens = mc.options.getMouseSensitivity().getValue();
        double f = sens * 0.6 + 0.2;
        return (float) (f * f * f * 8.0 * 0.15);
    }
}
