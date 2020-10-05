package ai.arcblroth.projectInception.client.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.client.AbstractGameInstance;
import ai.arcblroth.projectInception.postlaunch.PostLaunchEntrypoint;
import ai.arcblroth.projectInception.postlaunch.ProgressBar;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;
import org.panda_lang.pandomium.util.os.PandomiumOS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.stream.Collectors;

import static ai.arcblroth.projectInception.client.mc.QueueProtocol.*;

public class CEFInitializer implements PostLaunchEntrypoint {

    @Override
    public void onPostLaunch(ProgressBar bar) {
        try {
            bar.setText("Project Inception - Loading CEF");
            if(!ProjectInception.IS_INNER) {
                String gameDir = MinecraftClient.getInstance().runDirectory.getAbsolutePath();
                ArrayList<String> commandLine = ProjectInceptionEarlyRiser.newCommandLineForForking(false);
                boolean addedNativesFolder = false;
                String nativesPath = new File(MinecraftClient.getInstance().runDirectory, "inception-cef" + File.separator + "natives").getAbsolutePath();
                for (ListIterator<String> iterator = commandLine.listIterator(); iterator.hasNext(); ) {
                    String s = iterator.next();
                    if (s.startsWith("-Djava.library.path=")) {
                        iterator.remove();
                        iterator.add(s + File.pathSeparator + nativesPath);
                        addedNativesFolder = true;
                        break;
                    }
                }
                if(!addedNativesFolder) {
                    commandLine.add("-Djava.library.path=" + nativesPath);
                }
                commandLine.add("-D" + ProjectInceptionEarlyRiser.ARG_IS_INNER + "=true");
                commandLine.add("ai.arcblroth.taterwebz.TaterwebzChild");
                List<String> cmdArgs = Arrays.asList(ProjectInception.ARGUMENTS);
                commandLine.addAll(cmdArgs);
                if (!commandLine.contains("--gameDir")) {
                    Collections.addAll(commandLine, "--gameDir", gameDir);
                }
                if(PandomiumOS.isLinux()) {
                    String libprojectinception = extractLibProjectInception(nativesPath);
                    ArrayList<String> preCommandLine = new ArrayList<>();
                    preCommandLine.add("/usr/bin/env");
                    preCommandLine.add("LD_PRELOAD=" + libprojectinception);
                    preCommandLine.add("PROJECT_INCEPTION_PROC_SELF_EXE=" + nativesPath + File.separator + "jcef_helper");
                    commandLine.addAll(0, preCommandLine);
                }
                System.out.println("\n\n\n\n");
                System.out.println(commandLine.stream().map(s -> "\"" + s + "\" ").collect(Collectors.joining()));
                ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS = new ProcessBuilder(commandLine).inheritIO().start();
            } else {
                bar.setProgress(0.5F);
            }
            ChronicleQueue childQueue = ProjectInceptionEarlyRiser.buildQueue(
                    new File(MinecraftClient.getInstance().runDirectory, "projectInception" + File.separator + ProjectInceptionEarlyRiser.TATERWEBZ_PREFIX)
            );
            ProjectInceptionClient.TATERWEBZ_CHILD_QUEUE = childQueue;
            if(!ProjectInception.IS_INNER) {
                ExcerptTailer tailer = childQueue.createTailer("postlaunch").direction(TailerDirection.FORWARD);
                tailer.toEnd();
                final int framerateLimit = MinecraftClient.getInstance().getWindow().getFramerateLimit();
                OwoMessage crash = null;
                waitForInit:
                while (ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS.isAlive()) {
                    while (true) {
                        try (DocumentContext dc = tailer.readingDocument()) {
                            if (dc.isPresent()) {
                                Message message = readChild2ParentMessage(dc.wire().bytes());
                                if (message instanceof LoadProgressMessage) {
                                    LoadProgressMessage lpMessage = (LoadProgressMessage) message;
                                    bar.setProgress(lpMessage.progress);
                                    bar.setText(lpMessage.text);
                                    if (lpMessage.done) {
                                        break waitForInit;
                                    }
                                } else if (message instanceof OwoMessage) {
                                    crash = (OwoMessage) message;
                                    break waitForInit;
                                }
                            } else {
                                dc.rollbackOnClose();
                                break;
                            }
                        }
                    }
                    if (framerateLimit != 0) {
                        sleep(1000 / framerateLimit);
                    } else {
                        sleep(1);
                    }
                }
                if (crash != null) {
                    final OwoMessage finalCrash = crash;
                    bar.setProgress(1);
                    bar.setText("Project Inception - CEF Initialization Failed: " + finalCrash.throwable.getLocalizedMessage());
                    finalCrash.throwable.printStackTrace();
                    sleep(1000);
                    RenderSystem.recordRenderCall(() -> {
                        CrashReport crashReport = new CrashReport("CEF Initialization Failed", finalCrash.throwable);
                        CrashReportSection detailSection = crashReport.addElement(finalCrash.title);
                        for (String[] detail : finalCrash.details) {
                            if (detail.length == 2) {
                                detailSection.add(detail[0], detail[1]);
                            }
                        }
                        detailSection.trimStackTraceEnd(detailSection.getStackTrace().length);
                        throw new CrashException(crashReport);
                    });
                } else if (!ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS.isAlive()) {
                    bar.setProgress(1);
                    bar.setText("Project Inception - CEF Initialization Failed: Unexpected Termination");
                    sleep(1000);
                    RenderSystem.recordRenderCall(() -> {
                        CrashReport crashReport = new CrashReport(
                                "CEF Initialization Failed: Unexpected Termination (Exit Code " + ProjectInceptionClient.TATERWEBZ_CHILD_PROCESS.exitValue() + ")",
                                new Throwable()
                        );
                        throw new CrashException(crashReport);
                    });
                }
            } else {
                bar.setProgress(1);
            }
            AbstractGameInstance.registerShutdownHook();
        } catch (Throwable e) {
            bar.setProgress(1);
            e.printStackTrace();
            bar.setText("Project Inception - CEF Initialization Failed: " + e.getLocalizedMessage());
            sleep(1000);
            RenderSystem.recordRenderCall(() -> {
                CrashReport crashReport = new CrashReport("CEF Initialization Failed", e);
                throw new CrashException(crashReport);
            });
        }
    }

    private String extractLibProjectInception(String nativesPath) throws Throwable {
        String file = "libprojectinception" + (MinecraftClient.getInstance().is64Bit() ? "-x64" : "") + ".so";
        ModContainer modContainer = FabricLoader.getInstance().getModContainer(ProjectInception.MODID).get();

        File nativesFolder = new File(nativesPath);
        File out = new File(nativesFolder, file);
        if (!out.exists()) {
            if(!nativesFolder.exists()) {
                if (!nativesFolder.mkdirs()) {
                    throw new IOException("Could not make natives directory");
                }
            }
            if (!out.createNewFile()) {
                throw new IOException("Could not extract libprojectinception");
            }

            try (InputStream is = modContainer.getRootPath().resolve(file).toUri().toURL().openStream()) {
                try (FileChannel foc = new FileOutputStream(out).getChannel()) {
                    foc.transferFrom(Channels.newChannel(is), 0, Long.MAX_VALUE);
                }
            }
        }
        return out.getAbsolutePath();
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
