package moscow.mytheria.ui.hud.impl;

import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.systems.setting.settings.BooleanSetting;
import moscow.mytheria.ui.hud.HudElement;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.colors.Colors;
import net.minecraft.client.network.PlayerListEntry;

public class Watermark extends HudElement {
   private final BooleanSetting showRole = new BooleanSetting(this, "hud.watermark.show_role").enabled(true);
   private final BooleanSetting showPing = new BooleanSetting(this, "hud.watermark.show_ping").enabled(true);
   private final BooleanSetting showFps = new BooleanSetting(this, "hud.watermark.show_fps").enabled(true);

   public Watermark() {
      super("hud.watermark", "icons/hud/watermark.png");
   }

   @Override
   public void update(UIContext context) {
      float padding = 8.0F;
      float iconTextGap = 6.0F;
      float separatorSpacing = 6.0F;
      this.height = 22.0F;
      float calculatedWidth = padding;
      if (this.showRole.isEnabled()) {
         float dotSize = 6.0F;
         float roleFont = 8.0F;
         String role = "Media";
         calculatedWidth = padding + (dotSize + iconTextGap);
         calculatedWidth += Fonts.MEDIUM.getFont(roleFont).width(role) + separatorSpacing;
         calculatedWidth += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
      }

      if (this.showPing.isEnabled()) {
         int ping = this.getPing();
         String pingText = ping + " ms";
         float textFont = 7.5F;
         calculatedWidth += Fonts.REGULAR.getFont(textFont).width(pingText) + separatorSpacing;
         calculatedWidth += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
      }

      if (this.showFps.isEnabled()) {
         int fps = mc.getCurrentFps();
         String fpsText = fps + " FPS";
         float textFont = 7.5F;
         calculatedWidth += Fonts.REGULAR.getFont(textFont).width(fpsText);
      }

      this.width = calculatedWidth + padding;
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      float padding = 8.0F;
      float itemSpacing = 8.0F;
      float iconTextGap = 6.0F;
      float separatorSpacing = 6.0F;
      this.height = 22.0F;
      float currentX = this.x + padding;
      float centerY = this.y + this.height / 2.0F;
      context.drawClientRect(this.x, this.y, this.width, this.height, this.animation.getValue(), this.dragAnim.getValue(), 6.0F);
      if (this.showRole.isEnabled()) {
         String role = "Media";
         float dotSize = 6.0F;
         float roleFont = 8.0F;
         float roleTextHeight = Fonts.REGULAR.getFont(roleFont).height();
         context.drawRoundedRect(
            currentX, centerY - dotSize / 2.0F, dotSize, dotSize, BorderRadius.all(dotSize / 2.0F), new ColorRGBA(255.0F, 0.0F, 0.0F, 255.0F)
         );
         currentX += dotSize + iconTextGap;
         context.drawText(Fonts.MEDIUM.getFont(roleFont), role, currentX, centerY - roleTextHeight / 2.0F, Colors.getTextColor());
         currentX += Fonts.MEDIUM.getFont(roleFont).width(role) + separatorSpacing;
         float separatorHeight = Fonts.REGULAR.getFont(8.0F).height();
         context.drawText(Fonts.REGULAR.getFont(8.0F), "/", currentX, centerY - separatorHeight / 2.0F, Colors.getTextColor().withAlpha(80.0F));
         currentX += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
      }

      if (this.showPing.isEnabled()) {
         int ping = this.getPing();
         String pingText = ping + " ms";
         float textFont = 7.5F;
         float textHeight = Fonts.REGULAR.getFont(textFont).height();
         context.drawText(Fonts.REGULAR.getFont(textFont), pingText, currentX, centerY - textHeight / 2.0F, Colors.getTextColor());
         currentX += Fonts.REGULAR.getFont(textFont).width(pingText) + separatorSpacing;
         float separatorHeight = Fonts.REGULAR.getFont(8.0F).height();
         context.drawText(Fonts.REGULAR.getFont(8.0F), "/", currentX, centerY - separatorHeight / 2.0F, Colors.getTextColor().withAlpha(80.0F));
         currentX += Fonts.REGULAR.getFont(8.0F).width("/") + separatorSpacing;
      }

      if (this.showFps.isEnabled()) {
         int fps = mc.getCurrentFps();
         String fpsText = fps + " FPS";
         float textFont = 7.5F;
         float textHeight = Fonts.REGULAR.getFont(textFont).height();
         context.drawText(Fonts.REGULAR.getFont(textFont), fpsText, currentX, centerY - textHeight / 2.0F, Colors.getTextColor());
         currentX += Fonts.REGULAR.getFont(textFont).width(fpsText);
      }
   }

   private int getPing() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
         if (playerListEntry != null) {
            return playerListEntry.getLatency();
         }
      }

      return 0;
   }
}
