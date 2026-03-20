package moscow.mytheria.systems.event.handlers;

import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.network.ServerConnectionEvent;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.utility.interfaces.IMinecraft;
import net.fabricmc.loader.api.FabricLoader;

public class ServerConnectionHandler implements IMinecraft {
   private boolean messageSent = false;
   private boolean connected;
   private final EventListener<ServerConnectionEvent> onServerConnection = event -> {
      this.connected = true;
      this.messageSent = false;
   };
   private final EventListener<ClientPlayerTickEvent> onClientPlayerTick = event -> {
      if (this.connected
         && !this.messageSent
         && mc.player != null
         && mc.player.age > 100
         && mc.getCurrentServerEntry() != null
         && FabricLoader.getInstance().isModLoaded("viafabricplus")) {
      }
   };

   public ServerConnectionHandler() {
      Mytheria.getInstance().getEventManager().subscribe(this);
   }
}
