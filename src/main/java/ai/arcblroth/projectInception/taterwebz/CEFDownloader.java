package ai.arcblroth.projectInception.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.postlaunch.PostLaunchEntrypoint;
import ai.arcblroth.projectInception.postlaunch.ProgressBar;
import com.chocohead.mm.api.ClassTinkerers;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.panda_lang.pandomium.loader.PandomiumProgressListener;
import org.panda_lang.pandomium.wrapper.PandomiumBrowser;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class CEFDownloader implements PostLaunchEntrypoint {

    public static final String PANDOMIUM_NATIVES_BASE = "https://repo.panda-lang.org/org/panda-lang/pandomium-natives/pandomium-natives-";
    public static final String PANDOMIUM_CORE = "https://repo.panda-lang.org/org/panda-lang/pandomium/67.0.6/pandomium-67.0.6.jar";

    @Override
    @SuppressWarnings("deprecation")
    public void onPostLaunch(ProgressBar bar) {
        try {
            bar.setText("Project Inception - Loading CEF");
            bar.setProgress(0.05F);
            if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
                // In a dev env, we dynamically load Pandomium
                // so that it gets loaded on Knot rather than
                // the AppClassLoader
                if(FabricLoader.getInstance().isDevelopmentEnvironment()) {
                    bar.setText("Project Inception - Downloading CEF [0/4]");
                }
                ProjectInception.LOGGER.info("Detected dev, adding url " + PANDOMIUM_CORE);
                File coreDest = new File(MinecraftClient.getInstance().runDirectory, "inception-cef" + File.separator + "pandomium.jar");
                if(!coreDest.exists()) {
                    download(new URL(PANDOMIUM_CORE), coreDest);
                }
                ClassTinkerers.addURL(coreDest.toURL());
            }
            String nativesJarPath = getNativesJarPath();
            if(nativesJarPath == null) {
                throw new UnsupportedOperationException("Unsupported OS");
            }
            bar.setProgress(0.1F);
            ProjectInception.LOGGER.info("Adding natives url " + nativesJarPath);
            bar.setText("Project Inception - Downloading CEF [1/4]");
            File nativesDest = new File(MinecraftClient.getInstance().runDirectory, "inception-cef" + File.separator + "pandomium-natives.jar");
            if(!nativesDest.exists()) {
                download(new URL(nativesJarPath), nativesDest);
            }
            ClassTinkerers.addURL(nativesDest.toURL());
            bar.setProgress(0.2F);
            bar.setText("Project Inception - Linking CEF [2/4]");
            bar.setProgress(0.3F);
            bar.setText("Project Inception - Downloading CEF [3/4]");
            TaterwebzPandomium pandomium = new TaterwebzPandomium();
            pandomium.getLoader().addProgressListener((state, progress) -> {
                if (state == PandomiumProgressListener.State.RUNNING) {
                    ProjectInception.LOGGER.info(String.format("Loading Pandomium %d%%", progress));
                    bar.setProgress(0.3F + progress / (100 / 0.5F));
                }
            });
            bar.setText("Project Inception - Initializing CEF [4/4]");
            pandomium.initialize();
            ProjectInceptionClient.PANDOMIUM = pandomium;
            bar.setProgress(0.9F);
            ProjectInceptionClient.PANDOMIUM_CLIENT = pandomium.createClient();
            bar.setProgress(1F);
        } catch (Throwable e) {
            bar.setProgress(1);
            e.printStackTrace();
            bar.setText("Project Inception - CEF Initialization Failed: " + e.getLocalizedMessage());
            sleep(1000);
            RenderSystem.recordRenderCall(() -> {
                if (e instanceof CrashException) {
                    throw (CrashException)e;
                } else {
                    CrashReport crashReport = new CrashReport("CEF Initialization Failed", e);
                    TaterwebzPandomium.addDetailsToCrashReport(crashReport);
                    throw new CrashException(crashReport);
                }
            });
        }
    }

    private static String getNativesJarPath() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.startsWith("windows")) {
            return PANDOMIUM_NATIVES_BASE + "win64/67.0/pandomium-natives-win64-67.0.jar";
        } else if (os.startsWith("linux")) {
            return PANDOMIUM_NATIVES_BASE + "linux64/67.0.3/pandomium-natives-linux64-67.0.3.jar";
        } else if(os.startsWith("mac")) {
            return PANDOMIUM_NATIVES_BASE + "macosx64/67.0.5/pandomium-natives-macosx64-67.0.5.jar";
        } else {
            return null;
        }
    }

    private static void download(URL source, File destination) throws IOException {
        if(destination.exists()) {
            destination.delete();
        }
        HttpURLConnection connection = (HttpURLConnection)source.openConnection();
        // This is the same User-Agent that Pandomium uses to trick its own maven
        // see PandomiumDownloader#download
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        connection.connect();
        ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
        FileOutputStream fos = new FileOutputStream(destination);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        rbc.close();
        fos.close();
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
