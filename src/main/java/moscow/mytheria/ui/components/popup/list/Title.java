package moscow.mytheria.ui.components.popup.list;

import com.mojang.blaze3d.systems.RenderSystem;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.msdf.Font;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.ui.components.popup.PopupComponent;
import moscow.mytheria.utility.colors.Colors;
import moscow.mytheria.utility.gui.GuiUtility;

public class Title extends PopupComponent {
   private final String text;

   public Title(String text) {
      this.text = text;
   }

   @Override
   protected void renderComponent(UIContext context) {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float nameLeftPadding = 8.0F;
      float nameHeight = nameFont.height();
      context.drawFadeoutText(
         nameFont,
         this.text != null && !this.text.trim().isEmpty() ? Localizator.translate(this.text) : " ",
         this.x + nameLeftPadding,
         this.y + GuiUtility.getMiddleOfBox(nameHeight, this.height),
         Colors.getTextColor().withAlpha(RenderSystem.getShaderColor()[3] * 255.0F),
         0.8F,
         1.0F,
         this.width - 12.0F
      );
   }

   @Override
   public float getHeight() {
      return this.height = 18.0F;
   }
}
