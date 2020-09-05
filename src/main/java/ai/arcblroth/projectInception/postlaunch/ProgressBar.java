package ai.arcblroth.projectInception.postlaunch;

import net.minecraft.client.gui.screen.SplashScreen;

public class ProgressBar {

    private float progress = 0F;
    private String text = "";

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}
