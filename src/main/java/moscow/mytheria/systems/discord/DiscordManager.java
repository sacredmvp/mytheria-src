package moscow.mytheria.systems.discord;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.RichPresence;
import moscow.mytheria.utility.interfaces.IMinecraft;

public class DiscordManager implements IMinecraft {
   private final IPCClient client = new IPCClient(1464622534585745479L);
   private final long startTime = System.currentTimeMillis() / 1000L;

   private RichPresence getPresence() {
      return new RichPresence()
         .setDetails("Версия: 2.0")
         .setState("Роль: ADMIN")
         .setStartTimestamp(this.startTime)
         .setLargeImageKey("animlogo_v2")
         .setButtons(new RichPresence.Button("кряк от", "https://t.me/AestheticDLC"));
   }

   public void connect() {
      this.client.setListener(new IPCListener() {
         @Override
         public void onReady(IPCClient client) {
            client.sendRichPresence(DiscordManager.this.getPresence());
         }
      });
      this.client.connect();
   }

   public void disconnect() {
      if (this.client != null && this.client.isConnected()) {
         this.client.close();
      }
   }
}
