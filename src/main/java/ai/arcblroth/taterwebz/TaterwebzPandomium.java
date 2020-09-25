package ai.arcblroth.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import ai.arcblroth.taterwebz.util.NotKnotClassLoader;
import com.google.common.net.UrlEscapers;
import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.util.cache.TempJarCache;
import jogamp.common.Debug;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.TaterwebzBrowser;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandlerAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.panda_lang.pandomium.Pandomium;
import org.panda_lang.pandomium.settings.PandomiumSettings;
import org.panda_lang.pandomium.settings.PandomiumSettingsBuilder;
import org.panda_lang.pandomium.wrapper.PandomiumClient;

import javax.swing.*;
import java.io.File;
import java.net.*;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.TreeMap;
import java.util.function.Supplier;

import static ai.arcblroth.projectInception.client.mc.QueueProtocol.*;

public class TaterwebzPandomium extends Pandomium {

    public static TaterwebzPandomium PANDOMIUM;
    public static PandomiumClient PANDOMIUM_CLIENT;

    private TreeMap<String, TaterwebzBrowser> browsers;

    public TaterwebzPandomium() {
        super(((Supplier<PandomiumSettings>) () -> {
            InetSocketAddress proxyAddr = TaterwebzChild.OPTIONS.proxyAddress;
            File nativesFolder = new File("natives");
            boolean shouldDeleteNativesFolder = !nativesFolder.exists();
            PandomiumSettingsBuilder settingsBuilder = PandomiumSettings.getDefaultSettingsBuilder();
            settingsBuilder.nativeDirectory(TaterwebzChild.OPTIONS.runDirectory.getAbsolutePath() + File.separator + "inception-cef" + File.separator + "natives");
            if (shouldDeleteNativesFolder) {
                nativesFolder.delete();
            }
            if (proxyAddr != null) {
                settingsBuilder.proxy(proxyAddr.getHostName(), proxyAddr.getPort());
            }
            settingsBuilder.loadAsync(false);
            PandomiumSettings settings = settingsBuilder.build();
            settings.getCefSettings().log_severity = CefSettings.LogSeverity.LOGSEVERITY_DISABLE;
            settings.getCefSettings().windowless_rendering_enabled = true;
            return settings;
        }).get());
        browsers = new TreeMap<>();
    }

    public void initialize(NotKnotClassLoader classLoader) {
        super.initialize();

        // System.setProperty("jogamp.debug.JNILibLoader", "true");
        // System.setProperty("jogamp.debug.NativeLibrary", "true");
        // System.setProperty("jogamp.debug.TempJarCache", "true");
        // System.setProperty("jogamp.debug.JarUtil", "true");

        // Patch JOGL native searching because for some reason
        // this breaks when we use a separate process
        classLoader.addClassTransformer("com.jogamp.common.jvm.JNILibLoaderBase", classNode -> {
            classNode.methods.forEach(methodNode -> {
                if(methodNode.name.equals("addNativeJarLibsWithTempJarCache")) {
                    ListIterator<AbstractInsnNode> insns = methodNode.instructions.iterator();
                    while(insns.hasNext()) {
                        AbstractInsnNode insn = insns.next();
                        if(insn instanceof MethodInsnNode && ((MethodInsnNode) insn).name.equals("addNativeJarLibsImpl")) {
                            VarInsnNode aload11 = (VarInsnNode) insn.getPrevious();
                            VarInsnNode aload9 = (VarInsnNode) aload11.getPrevious();
                            aload11.var = aload9.var;
                            break;
                        }
                    }
                }
            });
        });

        // By default, JOGL breaks if we have spaces in the jar path
        classLoader.addClassTransformer("com.jogamp.common.util.JarUtil", classNode -> {
            classNode.methods.forEach(methodNode -> {
                if(methodNode.name.equals("getJarURI")) {
                    ListIterator<AbstractInsnNode> insns = methodNode.instructions.iterator();
                    while(insns.hasNext()) {
                        AbstractInsnNode insn = insns.next();
                        if(insn instanceof MethodInsnNode && ((MethodInsnNode) insn).name.equals("toURI")) {
                            MethodInsnNode mInsn = ((MethodInsnNode) insn);
                            mInsn.setOpcode(Opcodes.INVOKESTATIC);
                            mInsn.owner = "ai/arcblroth/taterwebz/TaterwebzPandomium";
                            mInsn.name  = "getJarUtilURI";
                            mInsn.desc  = "(Ljava/net/URL;)Ljava/net/URI;";
                        }
                    }
                }
            });
        });

        TempJarCache.initSingleton();
        if(!TempJarCache.isInitialized()) {
            throw new RuntimeException("Could not initialize JOGL TempJarCache!");
        }
        if(!JNILibLoaderBase.addNativeJarLibs(new Class[]{Debug.class}, null)) {
            throw new RuntimeException("Could not initialize JOGL natives!");
        }

    }

    public void postInitialize() {
        // Disable all context menus
        PANDOMIUM_CLIENT.getCefClient().addContextMenuHandler(new CefContextMenuHandlerAdapter() {
            @Override
            public void onBeforeContextMenu(CefBrowser cefBrowser, CefFrame cefFrame, CefContextMenuParams cefContextMenuParams, CefMenuModel cefMenuModel) {
                cefMenuModel.clear();
            }
        });

    }

    public void loop() {
        ExcerptTailer tailer = ProjectInception.toParentQueue.createTailer().direction(TailerDirection.FORWARD);
        tailer.toEnd();
        ArrayList<RequestBrowserMessage> browserRequests = new ArrayList<>();

        //RequestBrowserMessage test = new RequestBrowserMessage();
        //test.createOrDestroy = true;
        //test.width = 256;
        //test.height = 256;
        //test.uuid = "test";
        //QueueProtocol.writeParent2ChildMessage(test, ProjectInception.toParentQueue.acquireAppender());

        //CefBrowser browser2 = PANDOMIUM_CLIENT.getCefClient().createBrowser("https://google.com/", true, true);
        //JFrame frame = new JFrame();
        //frame.add(browser2.getUIComponent());
        //frame.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CefApp.getInstance().shutdown();
        }));

        try {
            while(true) {
                browserRequests.clear();
                while(true) {
                    try(DocumentContext dc = tailer.readingDocument()) {
                        if(dc.isPresent()) {
                            Bytes<?> bytes = dc.wire().bytes();
                            Message message = readParent2ChildMessage(bytes);
                            MessageType type = message.getMessageType();
                            if(type == QueueProtocol.MessageType.REQUEST_BROWSER) {
                                browserRequests.add((RequestBrowserMessage) message);
                            }
                        } else {
                            dc.rollbackOnClose();
                            break;
                        }
                    }
                }

                for(RequestBrowserMessage rbMessage : browserRequests) {
                    if(rbMessage.createOrDestroy) {
                        browsers.put(rbMessage.uuid, createBrowser(rbMessage.initialURL, rbMessage.width, rbMessage.height, rbMessage.uuid));
                    } else {
                        TaterwebzBrowser browser = browsers.remove(rbMessage.uuid);
                        if(browser != null) {
                            browser.setCloseAllowed();
                            browser.close(true);
                            browser.dispose();
                        }
                    }
                }

                SwingUtilities.invokeAndWait(() -> {
                    try {
                        for (TaterwebzBrowser browser : browsers.values()) {
                            browser.handleEvents();
                            browser.render();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                CefApp.getInstance().N_DoMessageLoopWork();

                Thread.sleep(1000 / 60);
            }
        } catch (Throwable e) {
            QueueProtocol.OwoMessage crash = new QueueProtocol.OwoMessage();
            crash.throwable = e;
            QueueProtocol.writeChild2ParentMessage(crash, ProjectInception.toParentQueue.acquireAppender());
            throw new RuntimeException(e);
        }
    }

    public static TaterwebzBrowser createBrowser(String url, int width, int height, String uuid) {
        if(url == null || url.isEmpty()) url = "about:blank";
        ProjectInception.LOGGER.info("Creating browser \"" + uuid + "\" with url " + url);
        if (PANDOMIUM_CLIENT.getCefClient().isDisposed_) {
            throw new IllegalStateException("Can't create browser. CefClient is disposed.");
        }
        ChronicleQueue queue = ProjectInceptionEarlyRiser.buildQueue(
                new File(TaterwebzChild.OPTIONS.runDirectory, "projectInception" + File.separator + uuid)
        );
        return new TaterwebzBrowser(PANDOMIUM_CLIENT.getCefClient(), url, false, width, height, null, queue);
    }

    public static void addDetailsToCrashReport(QueueProtocol.OwoMessage crash) {
        crash.title = "Pandomium Details";
        crash.details = new String[][] {
                new String[] {"Pandomium Version", getVersion()},
                new String[] {"Chromium Version", getChromiumVersion()},
                new String[] {"CEF Version", getCefVersion()}
        };
    }

    // Redirect handler for JarUtil#getJarURI
    @SuppressWarnings("unused")
    public static URI getJarUtilURI(URL in) throws MalformedURLException, URISyntaxException {
        return new URL(UrlEscapers.urlFragmentEscaper().escape(in.toString())).toURI();
    }

}
