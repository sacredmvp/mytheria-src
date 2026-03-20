package moscow.mytheria.framework.objects.gradient.impl;

import moscow.mytheria.framework.objects.gradient.Gradient;
import moscow.mytheria.utility.colors.ColorRGBA;

class DiagonalGradient extends Gradient {
   public DiagonalGradient(ColorRGBA startColor, ColorRGBA endColor) {
      super(startColor, endColor, endColor, startColor);
   }

   public DiagonalGradient rotate() {
      return new DiagonalGradient(this.topRightColor, this.bottomLeftColor);
   }
}
