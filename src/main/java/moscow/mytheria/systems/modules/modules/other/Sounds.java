package moscow.mytheria.systems.modules.modules.other;

import lombok.Generated;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.network.ReceivePacketEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.utility.sounds.ClientSounds;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(
   name = "Sounds",
   category = ModuleCategory.OTHER,
   enabledByDefault = true,
   desc = "Добавляет звуки клиента"
)
public class Sounds extends BaseModule {
   private final SliderSetting volume = new SliderSetting(this, "Громкость звука").step(0.1F).min(0.1F).max(1.0F).currentValue(1.0F);
   private final EventListener<ReceivePacketEvent> receivePacket = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet) {
         String msg = packet.content().getString();
         if (msg.contains("Вы успешно купили") || msg.contains("отправлено игроку")) {
            ClientSounds.APPLEPAY.play(this.volume.getCurrentValue(), 1.0F);
         }
      }
   };

   @Generated
   public SliderSetting getVolume() {
      return this.volume;
   }

   @Generated
   public EventListener<ReceivePacketEvent> getReceivePacket() {
      return this.receivePacket;
   }
}
