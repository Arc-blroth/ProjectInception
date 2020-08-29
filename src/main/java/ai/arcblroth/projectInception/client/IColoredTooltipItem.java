package ai.arcblroth.projectInception.client;

import ai.arcblroth.projectInception.util.CyberDragonsUtil;
import net.minecraft.text.TextColor;

public interface IColoredTooltipItem {

    public int getColor();

    public default boolean isRainbow() {
        return false;
    }

    public default TextColor getFinalColor() {
        int color = getColor();
        if(isRainbow()) {
            double[] hsb = CyberDragonsUtil.rgbToHsb(color);
            hsb[0] += ((System.currentTimeMillis() % 4000) / 4000F);
            hsb[0] %= 1F;
            color = CyberDragonsUtil.hsbToRgb(hsb);
        }
        int finalColor = color;
        return TextColor.fromRgb(finalColor);
    }

}
