package moscow.mytheria.systems.modules.modules.movement;

import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.SliderSetting;

@ModuleInfo(
   name = "CameraView",
   category = ModuleCategory.MOVEMENT,
   desc = "modules.descriptions.cameraview"
)
public class CameraView extends BaseModule {
   private final SliderSetting speedXZ = new SliderSetting(this, "modules.settings.cameraview.speed_xz")
      .step(0.1F)
      .min(0.1F)
      .max(10.0F)
      .currentValue(4.0F);
   private final SliderSetting speedY = new SliderSetting(this, "modules.settings.cameraview.speed_y")
      .step(0.1F)
      .min(0.1F)
      .max(10.0F)
      .currentValue(4.0F);
   private final BooleanSetting fastFly = new BooleanSetting(this, "modules.settings.cameraview.fast_fly").enabled(true);
   private final SliderSetting fastMultiplier = new SliderSetting(this, "modules.settings.cameraview.fast_multiplier", () -> !this.fastFly.isEnabled())
      .step(0.5F)
      .min(1.5F)
      .max(10.0F)
      .currentValue(3.0F);

   // Текущая и предыдущая позиция для интерполяции
   private double camX, camY, camZ;
   private double prevCamX, prevCamY, prevCamZ;
   private float camYaw, camPitch;
   private float prevCamYaw, prevCamPitch;
   private boolean active;

   public static CameraView instance;

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (!active || mc.player == null) {
         return;
      }
      
      // Сохраняем предыдущую позицию для интерполяции
      prevCamX = camX;
      prevCamY = camY;
      prevCamZ = camZ;
      
      // Сохраняем предыдущие углы для интерполяции
      prevCamYaw = camYaw;
      prevCamPitch = camPitch;

      // Обновляем углы камеры
      camYaw = mc.player.getYaw();
      camPitch = mc.player.getPitch();

      // Базовая скорость как в режиме наблюдателя
      double baseSpeed = 0.1; // Базовая скорость движения
      double speedMultiplier = this.speedXZ.getCurrentValue() * 0.1;
      
      // Проверяем ускорение (как в спектаторе - sprint ускоряет)
      boolean sprintPressed = mc.options.sprintKey.isPressed();
      if (this.fastFly.isEnabled() && sprintPressed) {
         speedMultiplier *= this.fastMultiplier.getCurrentValue();
      }

      // Кэшируем состояния клавиш
      boolean forward = mc.options.forwardKey.isPressed();
      boolean back = mc.options.backKey.isPressed();
      boolean left = mc.options.leftKey.isPressed();
      boolean right = mc.options.rightKey.isPressed();
      boolean jump = mc.options.jumpKey.isPressed();
      boolean sneak = mc.options.sneakKey.isPressed();

      // Быстрый выход если нет движения
      if (!forward && !back && !left && !right && !jump && !sneak) {
         return;
      }

      // Предвычисляем тригонометрию
      double yawRad = Math.toRadians(camYaw);
      double pitchRad = Math.toRadians(camPitch);
      
      double sinYaw = Math.sin(yawRad);
      double cosYaw = Math.cos(yawRad);
      double sinPitch = Math.sin(pitchRad);
      double cosPitch = Math.cos(pitchRad);

      double dx = 0, dy = 0, dz = 0;
      
      // Движение вперед/назад с учетом pitch (как в спектаторе)
      if (forward) {
         dx -= sinYaw * cosPitch * speedMultiplier;
         dy -= sinPitch * speedMultiplier;
         dz += cosYaw * cosPitch * speedMultiplier;
      }
      if (back) {
         dx += sinYaw * cosPitch * speedMultiplier;
         dy += sinPitch * speedMultiplier;
         dz -= cosYaw * cosPitch * speedMultiplier;
      }
      
      // Движение влево/вправо (всегда горизонтально)
      if (left) {
         dx += cosYaw * speedMultiplier;
         dz += sinYaw * speedMultiplier;
      }
      if (right) {
         dx -= cosYaw * speedMultiplier;
         dz -= sinYaw * speedMultiplier;
      }
      
      // Вертикальное движение (всегда строго вверх/вниз)
      double verticalSpeed = this.speedY.getCurrentValue() * 0.1;
      if (this.fastFly.isEnabled() && sprintPressed) {
         verticalSpeed *= this.fastMultiplier.getCurrentValue();
      }
      
      if (jump) { dy += verticalSpeed; }
      if (sneak) { dy -= verticalSpeed; }

      // Обновляем позицию
      camX += dx;
      camY += dy;
      camZ += dz;
   };

   public CameraView() {
      instance = this;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      
      if (mc.player == null || mc.world == null) {
         this.setEnabled(false, true);
         return;
      }
      
      // Инициализация позиции камеры
      camX = mc.player.getX();
      camY = mc.player.getY();
      camZ = mc.player.getZ();
      prevCamX = camX;
      prevCamY = camY;
      prevCamZ = camZ;
      camYaw = mc.player.getYaw();
      camPitch = mc.player.getPitch();
      prevCamYaw = camYaw;
      prevCamPitch = camPitch;

      // Скрываем HUD
      mc.options.hudHidden = true;

      // Останавливаем движение игрока если он двигался
      if (mc.player != null) {
         mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
      }

      active = true;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      active = false;

      // Восстанавливаем HUD
      if (mc.options != null) {
         mc.options.hudHidden = false;
      }
   }

   @Override
   public void tick() {
      if (!active || mc.player == null) {
         if (active) {
            this.setEnabled(false, true);
         }
         return;
      }
   }

   public boolean isActive() {
      return active;
   }

   // Интерполированные позиции для плавного движения
   public double getCamX(float partialTicks) {
      return prevCamX + (camX - prevCamX) * partialTicks;
   }

   public double getCamY(float partialTicks) {
      return prevCamY + (camY - prevCamY) * partialTicks;
   }

   public double getCamZ(float partialTicks) {
      return prevCamZ + (camZ - prevCamZ) * partialTicks;
   }
   
   // Интерполированные углы для плавного поворота
   public float getCamYaw(float partialTicks) {
      return prevCamYaw + (camYaw - prevCamYaw) * partialTicks;
   }
   
   public float getCamPitch(float partialTicks) {
      return prevCamPitch + (camPitch - prevCamPitch) * partialTicks;
   }

   // Без интерполяции (для обратной совместимости)
   public double getCamX() { return camX; }
   public double getCamY() { return camY; }
   public double getCamZ() { return camZ; }
   public float getCamYaw() { return camYaw; }
   public float getCamPitch() { return camPitch; }
}
