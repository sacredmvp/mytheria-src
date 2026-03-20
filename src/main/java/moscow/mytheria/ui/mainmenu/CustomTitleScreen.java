package moscow.mytheria.ui.mainmenu;

import java.util.ArrayList;
import java.util.List;
import moscow.mytheria.Mytheria;
import moscow.mytheria.framework.base.CustomScreen;
import moscow.mytheria.framework.base.UIContext;
import moscow.mytheria.framework.msdf.Font;
import moscow.mytheria.framework.msdf.Fonts;
import moscow.mytheria.framework.objects.BorderRadius;
import moscow.mytheria.framework.objects.MouseButton;
import moscow.mytheria.systems.localization.Localizator;
import moscow.mytheria.systems.modules.modules.other.Sounds;
import moscow.mytheria.utility.animation.base.Animation;
import moscow.mytheria.utility.animation.base.Easing;
import moscow.mytheria.utility.colors.ColorRGBA;
import moscow.mytheria.utility.game.TextUtility;
import moscow.mytheria.utility.interfaces.IMinecraft;
import moscow.mytheria.utility.math.MathUtility;
import moscow.mytheria.utility.render.DrawUtility;
import moscow.mytheria.utility.render.obj.Rect;
import moscow.mytheria.utility.sounds.ClientSounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import ru.kotopushka.compiler.sdk.annotations.Compile;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

public class CustomTitleScreen extends CustomScreen implements IMinecraft {
   private static boolean once;
   private static final List<CustomButton> buttons = new ArrayList<>();
   private boolean active = true;
   private final Animation activeAnimation = new Animation(1000L, 1.0F, Easing.FIGMA_EASE_IN_OUT);
   private final ColorRGBA dateColor = new ColorRGBA(171.0F, 254.0F, 255.0F);
   private final ColorRGBA timeColor = new ColorRGBA(203.0F, 254.0F, 255.0F);

   @Compile
   @VMProtect(
      type = VMProtectType.MUTATION
   )
   protected void init() {
      String basePath = "image/mainmenu/icons/";
      if (!once) {
         if (Mytheria.getInstance().getModuleManager().getModule(Sounds.class).isEnabled()) {
            ClientSounds.WELCOME.play(Mytheria.getInstance().getModuleManager().getModule(Sounds.class).getVolume().getCurrentValue());
         }

         buttons.add(new CustomButton(basePath + "single.png", 12.0F, () -> mc.setScreen(new SelectWorldScreen(this))));
         buttons.add(new CustomButton(basePath + "multi.png", 12.0F, () -> mc.setScreen(new MultiplayerScreen(this))));
         buttons.add(new CustomButton(basePath + "settings.png", 12.0F, () -> mc.setScreen(new OptionsScreen(this, mc.options))));
         buttons.add(new CustomButton(basePath + "quit.png", 14.0F, mc::stop));
         once = true;
      }

      super.init();
   }

   @Override
   public void render(UIContext context) {
      if (Fonts.isInitialized()) {
         Font timeFont = Fonts.ROUND_BOLD.getFont(65.0F);
         Font dateFont = Fonts.MEDIUM.getFont(16.0F);
         Font unlockFont = Fonts.REGULAR.getFont(10.0F);
         float textAlpha = 255.0F * (0.5F + 0.5F * this.activeAnimation.getValue());
         float timeOffset = MathUtility.interpolate(this.height / 2.0F - 20.0F, 80.0, this.activeAnimation.getValue());
         new Rect(-this.width / 2.0F, -this.width / 3.0F, this.width * 1.5F, this.width);
         this.activeAnimation.update(this.active);
         context.drawRoundedRect(0.0F, 0.0F, this.width, this.height, BorderRadius.ZERO, ColorRGBA.BLACK);
         context.drawCenteredText(timeFont, TextUtility.getCurrentTime(), this.width / 2.0F, timeOffset, ColorRGBA.WHITE.withAlpha(textAlpha));
         context.drawCenteredText(
            dateFont,
            TextUtility.getFormattedDateDigital(),
            this.width / 2.0F,
            timeOffset + 70.0F,
            new ColorRGBA(150.0F, 150.0F, 150.0F).withAlpha(textAlpha)
         );
         context.drawRoundedRect(
            this.width / 2.0F - 36.0F,
            this.height - 5 - 3.0F * this.activeAnimation.getValue(),
            72.0F,
            3.0F,
            BorderRadius.all(1.0F),
            ColorRGBA.WHITE.withAlpha(255.0F * this.activeAnimation.getValue())
         );
         context.drawCenteredText(
            unlockFont,
            Localizator.translate("mainmenu.next"),
            this.width / 2.0F,
            this.height - 15 + 3.0F * this.activeAnimation.getValue(),
            ColorRGBA.WHITE.withAlpha(155.0F * (1.0F - this.activeAnimation.getValue()))
         );
         DrawUtility.blurProgram.draw();
         float offset = 0.0F;

         for (CustomButton button : buttons) {
            button.getActiveAnim().update(buttons.size() - buttons.indexOf(button) > (1.0F - this.activeAnimation.getValue()) * buttons.size() + 0.5F);
            button.set(this.width / 2.0F - 69.0F + offset, this.height - 80.0F - 10.0F * button.getActiveAnim().getValue(), 30.0F, 30.0F);
            offset += button.getWidth() + 6.0F;
            button.draw(context);
         }

         if (this.shouldShowIsland()) {
            Mytheria.getInstance().getHud().getIsland().render(context);
         }
      }
   }

   @Compile
   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (!this.shouldShowIsland() || !Mytheria.getInstance().getHud().getIsland().handleClick((float)mouseX, (float)mouseY, button.getButtonIndex())) {
         for (CustomButton customButton : buttons) {
            if (customButton.hovered(mouseX, mouseY) && customButton.getActiveAnim().getValue() == 1.0F) {
               customButton.click(mouseX, mouseY, button.getButtonIndex());
               return;
            }
         }

         super.onMouseClicked(mouseX, mouseY, button);
      }
   }

   @Compile
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 69) {
         Mytheria.getInstance().getThemeManager().switchTheme();
      }

      if (Screen.hasControlDown() && keyCode == 82) {
         MinecraftClient.getInstance().setScreen(new MultiplayerScreen(this));
      }

      if (Screen.hasControlDown() && keyCode == 84) {
         MinecraftClient.getInstance().setScreen(new SelectWorldScreen(this));
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   private boolean shouldShowIsland() {
      return Mytheria.getInstance().getMusicTracker().haveActiveSession();
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
   }
}
