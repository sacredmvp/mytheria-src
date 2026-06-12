package moscow.mytheria.systems.modules.modules.other;

import moscow.mytheria.systems.event.EventListener;
import moscow.mytheria.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.mytheria.systems.modules.api.ModuleCategory;
import moscow.mytheria.systems.modules.api.ModuleInfo;
import moscow.mytheria.systems.modules.impl.BaseModule;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.systems.setting.settings.ButtonSetting;
import moscow.mytheria.systems.setting.settings.SliderSetting;
import moscow.mytheria.systems.setting.settings.StringSetting;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
   name = "AutoChat",
   category = ModuleCategory.OTHER,
   desc = "modules.descriptions.autochat"
)
public class AutoChat extends BaseModule {
   private final List<ChatMessage> messages = new ArrayList<>();
   private int currentMessageIndex = 0;
   private long lastSentTime = 0;
   
   private final SliderSetting delaySeconds = new SliderSetting(
      this,
      "modules.settings.autochat.delay",
      "modules.settings.autochat.delay.description"
   ).min(1.0F).max(300.0F).step(1.0F).currentValue(60.0F);
   
   private final BooleanSetting randomOrder = new BooleanSetting(
      this,
      "modules.settings.autochat.random_order"
   ).enabled(false);
   
   private final BooleanSetting onlyWhenActive = new BooleanSetting(
      this,
      "modules.settings.autochat.only_when_active"
   ).enabled(false);
   
   // Сообщения (до 10 штук)
   private final StringSetting message1 = new StringSetting(this, "modules.settings.autochat.message1").text("");
   private final BooleanSetting enabled1 = new BooleanSetting(this, "modules.settings.autochat.enabled1").enabled(true);
   
   private final StringSetting message2 = new StringSetting(this, "modules.settings.autochat.message2").text("");
   private final BooleanSetting enabled2 = new BooleanSetting(this, "modules.settings.autochat.enabled2").enabled(false);
   
   private final StringSetting message3 = new StringSetting(this, "modules.settings.autochat.message3").text("");
   private final BooleanSetting enabled3 = new BooleanSetting(this, "modules.settings.autochat.enabled3").enabled(false);
   
   private final StringSetting message4 = new StringSetting(this, "modules.settings.autochat.message4").text("");
   private final BooleanSetting enabled4 = new BooleanSetting(this, "modules.settings.autochat.enabled4").enabled(false);
   
   private final StringSetting message5 = new StringSetting(this, "modules.settings.autochat.message5").text("");
   private final BooleanSetting enabled5 = new BooleanSetting(this, "modules.settings.autochat.enabled5").enabled(false);
   
   private final StringSetting message6 = new StringSetting(this, "modules.settings.autochat.message6").text("");
   private final BooleanSetting enabled6 = new BooleanSetting(this, "modules.settings.autochat.enabled6").enabled(false);
   
   private final StringSetting message7 = new StringSetting(this, "modules.settings.autochat.message7").text("");
   private final BooleanSetting enabled7 = new BooleanSetting(this, "modules.settings.autochat.enabled7").enabled(false);
   
   private final StringSetting message8 = new StringSetting(this, "modules.settings.autochat.message8").text("");
   private final BooleanSetting enabled8 = new BooleanSetting(this, "modules.settings.autochat.enabled8").enabled(false);
   
   private final StringSetting message9 = new StringSetting(this, "modules.settings.autochat.message9").text("");
   private final BooleanSetting enabled9 = new BooleanSetting(this, "modules.settings.autochat.enabled9").enabled(false);
   
   private final StringSetting message10 = new StringSetting(this, "modules.settings.autochat.message10").text("");
   private final BooleanSetting enabled10 = new BooleanSetting(this, "modules.settings.autochat.enabled10").enabled(false);
   
   private final ButtonSetting testButton = new ButtonSetting(
      this,
      "modules.settings.autochat.test"
   ).action(this::sendNextMessage);

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null || mc.world == null) {
         return;
      }
      
      if (onlyWhenActive.isEnabled() && mc.currentScreen != null) {
         return;
      }
      
      updateMessageList();
      
      if (messages.isEmpty()) {
         return;
      }
      
      long currentTime = System.currentTimeMillis();
      long delayMs = (long) (delaySeconds.getCurrentValue() * 1000);
      
      if (currentTime - lastSentTime >= delayMs) {
         sendNextMessage();
         lastSentTime = currentTime;
      }
   };
   
   @Override
   public void onEnable() {
      super.onEnable();
      lastSentTime = System.currentTimeMillis();
      currentMessageIndex = 0;
   }
   
   private void updateMessageList() {
      messages.clear();
      
      if (enabled1.isEnabled() && !message1.getText().isEmpty()) {
         messages.add(new ChatMessage(message1.getText()));
      }
      if (enabled2.isEnabled() && !message2.getText().isEmpty()) {
         messages.add(new ChatMessage(message2.getText()));
      }
      if (enabled3.isEnabled() && !message3.getText().isEmpty()) {
         messages.add(new ChatMessage(message3.getText()));
      }
      if (enabled4.isEnabled() && !message4.getText().isEmpty()) {
         messages.add(new ChatMessage(message4.getText()));
      }
      if (enabled5.isEnabled() && !message5.getText().isEmpty()) {
         messages.add(new ChatMessage(message5.getText()));
      }
      if (enabled6.isEnabled() && !message6.getText().isEmpty()) {
         messages.add(new ChatMessage(message6.getText()));
      }
      if (enabled7.isEnabled() && !message7.getText().isEmpty()) {
         messages.add(new ChatMessage(message7.getText()));
      }
      if (enabled8.isEnabled() && !message8.getText().isEmpty()) {
         messages.add(new ChatMessage(message8.getText()));
      }
      if (enabled9.isEnabled() && !message9.getText().isEmpty()) {
         messages.add(new ChatMessage(message9.getText()));
      }
      if (enabled10.isEnabled() && !message10.getText().isEmpty()) {
         messages.add(new ChatMessage(message10.getText()));
      }
   }
   
   private void sendNextMessage() {
      if (mc.player == null || messages.isEmpty()) {
         return;
      }
      
      int index;
      if (randomOrder.isEnabled()) {
         index = (int) (Math.random() * messages.size());
      } else {
         index = currentMessageIndex % messages.size();
         currentMessageIndex++;
      }
      
      String message = messages.get(index).getText();
      if (message.startsWith("/")) {
         mc.player.networkHandler.sendCommand(message.substring(1));
      } else {
         mc.player.networkHandler.sendChatMessage(message);
      }
   }
   
   private static class ChatMessage {
      private final String text;
      
      public ChatMessage(String text) {
         this.text = text;
      }
      
      public String getText() {
         return text;
      }
   }
}
