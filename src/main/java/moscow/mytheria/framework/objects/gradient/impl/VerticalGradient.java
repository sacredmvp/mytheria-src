package moscow.mytheria.framework.objects.gradient.impl;

import moscow.mytheria.framework.objects.gradient.Gradient;
import moscow.mytheria.utility.colors.ColorRGBA;

public class VerticalGradient extends Gradient {
   public VerticalGradient(ColorRGBA startColor, ColorRGBA endColor) {
      super(startColor, endColor, startColor, endColor);
   }

   public VerticalGradient rotate() {
      return new VerticalGradient(this.bottomRightColor, this.topLeftColor);
   }
}
