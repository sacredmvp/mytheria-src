package moscow.mytheria.systems.commands.commands;

import java.util.List;
import moscow.mytheria.Mytheria;
import moscow.mytheria.systems.commands.Command;
import moscow.mytheria.systems.commands.CommandBuilder;
import moscow.mytheria.systems.commands.ParameterBuilder;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.systems.modules.Module;
import moscow.mytheria.utility.game.MessageUtility;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class ToggleCommand {
   @Compile
   public Command command() {
      List<String> moduleNames = Mytheria.getInstance()
         .getModuleManager()
         .getModules()
         .stream()
         .filter(module -> !module.isHidden())
         .map(module -> module.getName().replace(" ", ""))
         .toList();
      return CommandBuilder.begin("toggle")
         .aliases("t")
         .desc("commands.toggle.description")
         .param("module", (ParameterBuilder<Module> p) -> p.validator(ParameterBuilder.MODULE).suggests(moduleNames))
         .handler(
            context -> {
               Module module = (Module)context.arguments().getFirst();
               module.toggle();
               MessageUtility.info(
                  Text.of(Localizator.translate("commands.toggle." + (module.isEnabled() ? "enabled" : "disabled"), module.getName()))
               );
            }
         )
         .build();
   }
}
