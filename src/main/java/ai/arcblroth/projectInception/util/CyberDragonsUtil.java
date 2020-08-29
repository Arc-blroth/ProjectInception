package ai.arcblroth.projectInception.util;

// Class shamelessly stolen from myself at Arc-blroth/CyberneticDragons
public class CyberDragonsUtil {

    // Color converting code also shamelessly stolen from myself at Arc-blroth/BosstrovesRevenge
    public static double[] rgbToHsb(int color) {
        double red =  (color >> 16) & 0xFF;
        double green = (color >> 8) & 0xFF;
        double blue = color & 0xFF;
        double h, s, b;
        double maxRGB = Math.max(red, Math.max(green, blue));
        double minRGB = Math.min(red, Math.min(green, blue));
        double delta = maxRGB - minRGB;
        b = maxRGB;
        if(maxRGB != 0) {
            s = 255D * delta / maxRGB;
            if(red == maxRGB) {
                h = ((green - blue) / delta) % 6;
            } else if(green == maxRGB) {
                h = (blue - red) / delta + 2D;
            } else if(blue == maxRGB) {
                h = (red - green) / delta + 4D;
            } else {
                throw new RuntimeException("The colors are off the charts!");
            }
        } else {
            s = 0;
            h = -1;
        }
        h *= 60;
        if(h < 0) h += 360;
        if(new Double(h).isNaN()) h = 0;
        return new double[] {h / 360D, s / 255D, b / 255D};
    }

    public static int hsbToRgb(double[] color) {
        int red = 0, green = 0, blue = 0;
        double newHue = color[0] * 360D / 60D;
        double newSat = color[1] * 255D;
        double newBri = color[2] * 255D;
        if(newSat == 0D) {
            red = (int)Math.round(newBri);
            green = (int)Math.round(newBri);
            blue = (int)Math.round(newBri);
        } else {
            double maxRGB = newBri;
            double delta = newSat * maxRGB / 255D;
            if(newHue > 3) {
                blue = (int)Math.round(maxRGB);
                if(newHue > 4) {
                    green = (int)Math.round(maxRGB - delta);
                    red = (int)(Math.round((newHue - 4) * delta) + green);
                } else {
                    red = (int)Math.round(maxRGB - delta);
                    green = (int)(red - Math.round((newHue - 4) * delta));
                }
            } else if(newHue > 1) {
                green = (int)Math.round(maxRGB);
                if(newHue > 2) {
                    red = (int)Math.round(maxRGB - delta);
                    blue = (int)(Math.round((newHue - 2) * delta) + red);
                } else {
                    blue = (int)Math.round(maxRGB - delta);
                    red = (int)(blue - Math.round((newHue - 2) * delta));
                }
            } else if(newHue > -1) {
                red = (int)Math.round(maxRGB);
                if(newHue > 0) {
                    blue = (int)Math.round(maxRGB - delta);
                    green = (int)(Math.round(newHue * delta) + blue);
                } else {
                    green = (int)Math.round(maxRGB - delta);
                    blue = (int)(green - Math.round(newHue * delta));
                }
            }
        }
        return ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

}
