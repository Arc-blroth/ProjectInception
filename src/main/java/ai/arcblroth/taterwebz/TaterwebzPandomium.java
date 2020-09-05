package ai.arcblroth.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.mc.QueueProtocol;
import org.cef.CefApp;
import org.cef.browser.ProjectInceptionBrowser;
import org.panda_lang.pandomium.Pandomium;
import org.panda_lang.pandomium.loader.PandomiumProgressListener;
import org.panda_lang.pandomium.settings.PandomiumSettings;
import org.panda_lang.pandomium.settings.PandomiumSettingsBuilder;
import org.panda_lang.pandomium.wrapper.PandomiumCEF;
import org.panda_lang.pandomium.wrapper.PandomiumClient;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

public class TaterwebzPandomium extends Pandomium {

    public static TaterwebzPandomium PANDOMIUM;
    public static PandomiumClient PANDOMIUM_CLIENT;

    public TaterwebzPandomium() {
        super(((Supplier<PandomiumSettings>) () -> {
            InetSocketAddress proxyAddr = TaterwebzChild.OPTIONS.proxyAddress;
            File nativesFolder = new File("natives");
            boolean shouldDeleteNativesFolder = !nativesFolder.exists();
            PandomiumSettingsBuilder settingsBuilder = PandomiumSettings.getDefaultSettingsBuilder();
            settingsBuilder.nativeDirectory("inception-cef" + File.separator + "natives");
            if (shouldDeleteNativesFolder) {
                nativesFolder.delete();
            }
            if (proxyAddr != null) {
                settingsBuilder.proxy(proxyAddr.getHostName(), proxyAddr.getPort());
            }
            settingsBuilder.loadAsync(false);
            return settingsBuilder.build();
        }).get());
    }

    public void loop() {
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Throwable e) {
            QueueProtocol.OwoMessage crash = new QueueProtocol.OwoMessage();
            crash.throwable = e;
            QueueProtocol.writeChild2ParentMessage(crash, ProjectInception.toParentQueue.acquireAppender());
            throw new RuntimeException(e);
        }
    }

    public static ProjectInceptionBrowser createBrowser(String url, int width, int height) {
        ProjectInception.LOGGER.info("Creating browser with url " + url);
        if (PANDOMIUM_CLIENT.getCefClient().isDisposed_) {
            throw new IllegalStateException("Can't create browser. CefClient is disposed.");
        }
        return new ProjectInceptionBrowser(PANDOMIUM_CLIENT.getCefClient(), url, false, width, height, null);
    }

    public static void doMessageLoopWork() {
        if (CefApp.getState() != CefApp.CefAppState.TERMINATED) {
            CefApp.self.N_DoMessageLoopWork();
        }
    }

    public static void addDetailsToCrashReport(QueueProtocol.OwoMessage crash) {
        crash.title = "Pandomium Details";
        crash.details = new String[][] {
                new String[] {"Pandomium Version", getVersion()},
                new String[] {"Chromium Version", getChromiumVersion()},
                new String[] {"CEF Version", getCefVersion()}
        };
    }

}
