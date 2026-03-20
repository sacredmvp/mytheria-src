package moscow.mytheria.systems.friends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.utility.game.EntityUtility;
import moscow.mytheria.utility.game.MessageUtility;
import moscow.mytheria.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class FriendManager implements IMinecraft {
   private final List<String> friends = new ArrayList<>();

   public void add(String name) {
      if (Mytheria.getInstance().getTargetManager().getTarget().contains(name)) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.target")));
      } else if (this.friends.contains(name)) {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.exists", name)));
      } else if (name.equalsIgnoreCase(mc.getSession().getUsername())) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.self")));
      } else {
         this.friends.add(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.added", name)));
         if (EntityUtility.isInGame()) {
            Mytheria.getInstance().getFileManager().writeFile("client");
         }
      }
   }

   public void remove(String name) {
      if (this.friends.contains(name)) {
         this.friends.remove(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.removed", name)));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.not_exists", name)));
      }

      Mytheria.getInstance().getFileManager().writeFile("client");
   }

   @Compile
   public void clear() {
      if (this.friends.isEmpty()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.empty")));
      } else {
         this.friends.clear();
         MessageUtility.info(Text.of("Список друзей успешно очищен!"));
         Mytheria.getInstance().getFileManager().writeFile("client");
      }
   }

   public List<String> listFriends() {
      return Collections.unmodifiableList(this.friends);
   }

   public boolean isFriend(String name) {
      return this.friends.contains(name);
   }
}
