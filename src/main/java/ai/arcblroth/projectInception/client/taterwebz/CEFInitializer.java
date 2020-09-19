package ai.arcblroth.projectInception.client.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionClient;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.client.AbstractGameInstance;
import ai.arcblroth.projectInception.postlaunch.PostLaunchEntrypoint;
import ai.arcblroth.projectInception.postlaunch.ProgressBar;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ai.arcblroth.projectInception.client.mc.QueueProtocol.*;

public class CEFInitializer implements PostLaunchEntrypoint {

    @Override
    @SuppressWarnings("deprecation")
    public void onPostLaunch(ProgressBar bar) {
        try {
            bar.setText("Project Inception - Loading CEF");
            if(!ProjectInception.IS_INNER) {
                ArrayList<String> commandLine = ProjectInceptionEarlyRiser.newCommandLineForForking(false);
                commandLine.add("-D" + ProjectInceptionEarlyRiser.ARG_IS_INNER + "=true");
                commandLine.add("ai.arcblroth.taterwebz.TaterwebzChild");
                List<String> cmdArgs = Arrays.asList(ProjectInception.ARGUMENTS);
                commandLine.addAll(cmdArgs);
                if (!commandLine.contains("--gameDir")) {
                    Collections.addAll(commandLine, "--gameDir", MinecraftClient.getInstance().runDirectory.getAbsolutePath());
                }
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

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
