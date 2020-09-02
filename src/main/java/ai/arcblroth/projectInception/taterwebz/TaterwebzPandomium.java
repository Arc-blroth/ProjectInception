package ai.arcblroth.projectInception.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.mixin.AccessorRenderThread;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.cef.CefApp;
import org.cef.browser.CefBrowser;
import org.cef.browser.ProjectInceptionBrowser;
import org.panda_lang.pandomium.Pandomium;
import org.panda_lang.pandomium.loader.PandomiumProgressListener;
import org.panda_lang.pandomium.settings.PandomiumSettings;
import org.panda_lang.pandomium.settings.PandomiumSettingsBuilder;
import org.panda_lang.pandomium.util.SystemUtils;
import org.panda_lang.pandomium.wrapper.PandomiumBrowser;
import org.panda_lang.pandomium.wrapper.PandomiumCEF;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

public class TaterwebzPandomium extends Pandomium {

    private final Field pcefField;

    public TaterwebzPandomium() {
        super(((Supplier<PandomiumSettings>) () -> {
            InetSocketAddress proxyAddr = (InetSocketAddress) MinecraftClient.getInstance().getNetworkProxy().address();
            File nativesFolder = new File("natives");
            boolean shouldDeleteNativesFolder = !nativesFolder.exists();
            PandomiumSettingsBuilder settingsBuilder = PandomiumSettings.getDefaultSettingsBuilder();
            settingsBuilder.nativeDirectory("inception-cef");
            if (shouldDeleteNativesFolder) {
                nativesFolder.delete();
            }
            if (proxyAddr != null) {
                settingsBuilder.proxy(proxyAddr.getHostName(), proxyAddr.getPort());
            }
            return settingsBuilder.build();
        }).get());
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            Field mainThreadField = Pandomium.class.getDeclaredField("mainThread");
            mainThreadField.setAccessible(true);
            modifiersField.setInt(mainThreadField, mainThreadField.getModifiers() & ~Modifier.FINAL);
            mainThreadField.set(this, ((AccessorRenderThread) MinecraftClient.getInstance()).projectInceptionGetRenderThread());
            pcefField = Pandomium.class.getDeclaredField("pcef");
            pcefField.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            CrashReport crashReport = new CrashReport("Couldn't initialize Project Inception Pandomium!", e);
            addDetailsToCrashReport(crashReport);
            throw new CrashException(crashReport);
        }
    }

    @Override
    public void initialize() {
        System.setProperty("java.awt.headless", "false");
        Toolkit.getDefaultToolkit();
        String originalNativePath = System.getProperty("java.library.path");
        super.getLoader().addProgressListener((state, progress) -> {
            if (state == PandomiumProgressListener.State.DONE) {
                PandomiumCEF pcef = new PandomiumCEF(this);
                try {
                    pcefField.set(this, pcef);
                    SystemUtils.injectLibraryPath(originalNativePath + File.pathSeparator + System.getProperty("java.library.path"));
                } catch (Exception e) {
                    CrashReport crashReport = new CrashReport("Couldn't initialize Project Inception Pandomium!", e);
                    addDetailsToCrashReport(crashReport);
                    throw new CrashException(crashReport);
                }
                pcef.initialize();
                // Force the browser class to load now
                // in case of link errors
                ProjectInceptionBrowser.class.getName();
                ClientTickEvents.END_CLIENT_TICK.register(TaterwebzPandomium::doMessageLoopWork);
            }
        });
        super.getLoader().load();
    }

    public static ProjectInceptionBrowser createBrowser(String url, int width, int height) {
        ProjectInception.LOGGER.info("Creating browser with url " + url);
        if (ProjectInceptionClient.PANDOMIUM_CLIENT.getCefClient().isDisposed_) {
            throw new IllegalStateException("Can't create browser. CefClient is disposed.");
        }
        return new ProjectInceptionBrowser(ProjectInceptionClient.PANDOMIUM_CLIENT.getCefClient(), url, false, width, height, null);
    }

    public static void doMessageLoopWork(MinecraftClient client) {
        if (CefApp.getState() != CefApp.CefAppState.TERMINATED) {
            CefApp.self.N_DoMessageLoopWork();
        }
    }

    public static void addDetailsToCrashReport(CrashReport crashReport) {
        crashReport.addElement("Pandomium Details")
                .add("Pandomium Version", getVersion())
                .add("Chromium Version", getChromiumVersion())
                .add("CEF Version", getCefVersion());
    }

}
