package ai.arcblroth.projectInception.postlaunch;

import net.minecraft.client.gui.screen.SplashScreen;

public class ProgressBar {

    private final SplashScreen splash;
    private float progress = 0F;
    private String text = "";

    public ProgressBar(SplashScreen splash) {
        this.splash = splash;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setProgress(double progress) {
        setProgress((float) progress);
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}
