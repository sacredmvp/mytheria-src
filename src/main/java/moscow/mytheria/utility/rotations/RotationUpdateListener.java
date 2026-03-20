package moscow.mytheria.utility.rotations;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.event.impl.player.InputEvent;
import moscow.mytheria.systems.event.impl.render.Render3DEvent;

public class RotationUpdateListener {
   private final EventListener<ClientPlayerTickEvent> onTick = event -> Mytheria.getInstance().getRotationHandler().update();
   private final EventListener<Render3DEvent> onRender = event -> Mytheria.getInstance().getRotationHandler().updateRender(event.getTickDelta());
   private final EventListener<InputEvent> onInputEvent = event -> {
      RotationHandler rotationHandler = Mytheria.INSTANCE.getRotationHandler();
      RotationTask currentTask = rotationHandler.getCurrentTask();
      if (!rotationHandler.isIdling() && currentTask != null && currentTask.getMoveCorrection() == MoveCorrection.SILENT) {
         event.setYaw(Mytheria.getInstance().getRotationHandler().getCurrentRotation().getYaw());
      }
   };

   public RotationUpdateListener() {
      Mytheria.getInstance().getEventManager().subscribe(this);
   }
}
