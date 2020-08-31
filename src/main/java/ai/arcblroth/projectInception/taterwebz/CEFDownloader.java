package ai.arcblroth.projectInception.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.postlaunch.PostLaunchEntrypoint;
import ai.arcblroth.projectInception.postlaunch.ProgressBar;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.panda_lang.pandomium.loader.PandomiumProgressListener;
import org.panda_lang.pandomium.util.SystemUtils;

import java.io.File;

public class CEFDownloader implements PostLaunchEntrypoint {

    @Override
    public void onPostLaunch(ProgressBar bar) {
        try {
            bar.setText("Project Inception - Loading CEF");
            TaterwebzPandomium pandomium = new TaterwebzPandomium();
            pandomium.getLoader().addProgressListener((state, progress) -> {
                if (state == PandomiumProgressListener.State.RUNNING) {
                    ProjectInception.LOGGER.info(String.format("Loading Pandomium %d%%", progress));
                    bar.setProgress(progress / (100 / 0.8F));
                }
            });
            pandomium.initialize();
            ProjectInceptionClient.PANDOMIUM = pandomium;
            bar.setProgress(0.9F);
            ProjectInceptionClient.PANDOMIUM_CLIENT = pandomium.createClient();
            bar.setProgress(1F);
        } catch (Throwable e) {
            bar.setProgress(1);
            e.printStackTrace();
            bar.setText("Project Inception - CEF Initalization Failed: " + e.getLocalizedMessage());
            sleep(1000);
            RenderSystem.recordRenderCall(() -> {
                if (e instanceof CrashException) {
                    throw (CrashException)e;
                } else {
                    CrashReport crashReport = new CrashReport("CEF Initalization Failed", e);
                    TaterwebzPandomium.addDetailsToCrashReport(crashReport);
                    throw new CrashException(crashReport);
                }
            });
        }
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
