package ai.arcblroth.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import ai.arcblroth.taterwebz.util.HeadlessProgressBar;
import ai.arcblroth.taterwebz.util.NotKnotClassLoader;
import org.panda_lang.pandomium.loader.PandomiumProgressListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class CEFDownloader {

    public static final String PANDOMIUM_NATIVES_BASE = "https://repo.panda-lang.org/org/panda-lang/pandomium-natives/pandomium-natives-";
    public static final String PANDOMIUM_CORE = "https://repo.panda-lang.org/org/panda-lang/pandomium/67.0.6/pandomium-67.0.6.jar";

    @SuppressWarnings({"deprecation", "unused"})
    public void onPostLaunch(NotKnotClassLoader classLoader) {
        try {
            HeadlessProgressBar bar = new HeadlessProgressBar();
            bar.setProgress(0.1F);
            bar.setText("Project Inception - Downloading CEF [1/4]");
            bar.update();
            File coreDest = new File(TaterwebzChild.OPTIONS.runDirectory, "inception-cef" + File.separator + "pandomium.jar");
            if(!coreDest.exists()) {
                download(new URL(PANDOMIUM_CORE), coreDest);
            }
            classLoader.addURL(coreDest.toURL());
            String nativesJarPath = getNativesJarPath();
            if(nativesJarPath == null) {
                throw new UnsupportedOperationException("Unsupported OS");
            }
            bar.setProgress(0.2F);
            bar.setText("Project Inception - Downloading CEF [2/4]");
            bar.update();
            File nativesDest = new File(TaterwebzChild.OPTIONS.runDirectory, "inception-cef" + File.separator + "pandomium-natives.jar");
            if(!nativesDest.exists()) {
                download(new URL(nativesJarPath), nativesDest);
            }
            classLoader.addURL(nativesDest.toURL());
            bar.setProgress(0.4F);
            bar.setText("Project Inception - Downloading CEF [3/4]");
            bar.update();
            TaterwebzPandomium pandomium = new TaterwebzPandomium();
            pandomium.getLoader().addProgressListener((state, progress) -> {
                if (state == PandomiumProgressListener.State.RUNNING) {
                    ProjectInception.LOGGER.info(String.format("Loading Pandomium %d%%", progress));
                    bar.setProgress(0.4F + progress / (100 / 0.4F));
                }
            });
            pandomium.initialize(classLoader);
            bar.setProgress(0.8F);
            bar.setText("Project Inception - Initializing CEF [4/4]");
            bar.update();
            TaterwebzPandomium.PANDOMIUM = pandomium;
            bar.setProgress(0.9F);
            bar.update();
            TaterwebzPandomium.PANDOMIUM_CLIENT = pandomium.createClient();
            bar.setProgress(1F);
            bar.setDone(true);
            bar.update();
            pandomium.loop();
        } catch (Throwable e) {
            QueueProtocol.OwoMessage crash = new QueueProtocol.OwoMessage();
            crash.throwable = e;
            try {
                TaterwebzPandomium.addDetailsToCrashReport(crash);
            } catch (NoClassDefFoundError e2) {
                crash.title = "Pandomium Details";
                crash.details = new String[][] {
                        new String[] {"~~ NoClassDefFoundError ~~", e2.getLocalizedMessage()}
                };
            }
            e.printStackTrace();
            QueueProtocol.writeChild2ParentMessage(crash, ProjectInception.toParentQueue.acquireAppender());
            throw new RuntimeException(e);
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
        ProjectInception.LOGGER.info("Downloading " + source.toString() + " to " + destination.getAbsolutePath());
        destination.getParentFile().mkdirs();
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

}
