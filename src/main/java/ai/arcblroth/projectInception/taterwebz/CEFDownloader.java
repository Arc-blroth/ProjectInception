package ai.arcblroth.projectInception.taterwebz;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.postlaunch.PostLaunchEntrypoint;
import ai.arcblroth.projectInception.postlaunch.ProgressBar;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class CEFDownloader implements PostLaunchEntrypoint {

    public static final String CEF_BUILDSERVER = "http://opensource.spotify.com/cefbuilds/";
    public static final String CEF_WINDOWS_x32 = "cef_binary_85.2.11+g0202816+chromium-85.0.4183.83_windows32_client.tar.bz2";
    public static final String CEF_WINDOWS_x64 = "cef_binary_85.2.11+g0202816+chromium-85.0.4183.83_windows64_client.tar.bz2";
    public static final String CEF_LINUX_x32 = "cef_binary_84.4.1+gfdc7504+chromium-84.0.4147.105_linux32_client.tar.bz2";
    public static final String CEF_LINUX_x64 = "cef_binary_84.4.1+gfdc7504+chromium-84.0.4147.105_linux64_client.tar.bz2";
    public static final String CEF_LINUX_ARM_x32 = "cef_binary_84.4.1+gfdc7504+chromium-84.0.4147.105_linuxarm_client.tar.bz2";
    public static final String CEF_LINUX_ARM_x64 = "cef_binary_84.4.1+gfdc7504+chromium-84.0.4147.105_linuxarm64_client.tar.bz2";
    public static final String CEF_MACOS_x64 = "cef_binary_85.2.11+g0202816+chromium-85.0.4183.83_macosx64_client.tar.bz2";
    public static final String EXTRACTION_MARKER = ".inception_extraction_marker";

    @Override
    public void onPostLaunch(ProgressBar bar) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            bar.setProgress(1);
            bar.setText("Project Inception - CEF Initalization Failed: " + throwable.getLocalizedMessage());
            sleep(1000);
        });

        bar.setProgress(0.1);
        bar.setText("Project Inception - Downloading CEF");
        String cefDistribution = getCefDistribution();
        if(cefDistribution == null) {
            bar.setProgress(1);
            bar.setText("Project Inception - Could not download CEF, OS not recognized");
            sleep(1000);
        } else {
            try {
                URL cefDistroUrl = new URL(CEF_BUILDSERVER.concat(cefDistribution));
                File cefFolder = new File(MinecraftClient.getInstance().runDirectory, "inception-cef");
                cefFolder.mkdir();
                File extractionMarker = new File(cefFolder, EXTRACTION_MARKER);
                if(!extractionMarker.exists()) {
                    File extractDest = new File(cefFolder, "cef_binary_client.tar.bz2");
                    if (!extractDest.exists()) {
                        ProjectInception.LOGGER.info("Downloading " + cefDistroUrl.toString());
                        ReadableByteChannel rbc = Channels.newChannel(cefDistroUrl.openStream());
                        FileOutputStream fos = new FileOutputStream(extractDest);
                        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                    }
                    bar.setProgress(0.4);
                    bar.setText("Project Inception - Extracting CEF");
                    ProjectInception.LOGGER.info("Extracting CEF");
                    try (FileInputStream fis = new FileInputStream(extractDest)) {
                        BZip2CompressorInputStream bz2is = new BZip2CompressorInputStream(fis);
                        BufferedInputStream bis = new BufferedInputStream(bz2is);
                        ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(bis);
                        ArchiveEntry entry;
                        while ((entry = ais.getNextEntry()) != null) {
                            ProjectInception.LOGGER.info("Extracting tar entry " + entry.getName());
                            if (!ais.canReadEntryData(entry)) {
                                ProjectInception.LOGGER.warn("Could not read tar entry " + entry.getName());
                                continue;
                            }
                            File f = new File(cefFolder, entry.getName());
                            if (entry.isDirectory()) {
                                if (!f.isDirectory() && !f.mkdirs()) {
                                    throw new IOException("Failed to create directory " + f);
                                }
                            } else {
                                bar.setText(String.format("Project Inception - Extracting CEF [%s]", f.getName()));
                                File parent = f.getParentFile();
                                if (!parent.isDirectory() && !parent.mkdirs()) {
                                    throw new IOException("Failed to create directory " + parent);
                                }
                                try (FileOutputStream fos = new FileOutputStream(f)) {
                                    ReadableByteChannel rbc = Channels.newChannel(ais);
                                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        bar.setProgress(1);
                        bar.setText("Project Inception - Could not extract CEF: " + e.getLocalizedMessage());
                        sleep(1000);
                        return;
                    }
                    bar.setProgress(0.6);
                    bar.setText("Project Inception - Relocating CEF");
                    Path cefFolderPath = cefFolder.toPath();
                    Path internalCefFolder = new File(cefFolder, cefDistribution.substring(0, cefDistribution.length() - ".tar.bz2".length())).toPath();
                    Files.walk(internalCefFolder).forEach(p -> {
                        try {
                            File f = cefFolderPath.resolve(internalCefFolder.relativize(p)).toFile();
                            if(p.toFile().isDirectory())  {
                                f.mkdirs();
                            } else {
                                if(p.toFile().exists()) {
                                    bar.setText(String.format("Project Inception - Relocating CEF [%s]", f.getName()));
                                    if (f.exists()) {
                                        f.delete();
                                    }
                                    ProjectInception.LOGGER.info("Relocating " + p + " to " + f.toPath());
                                    Files.move(p, f.toPath());
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    bar.setProgress(0.8);
                    FileUtils.deleteDirectory(internalCefFolder.toFile());
                    extractionMarker.createNewFile();
                }
                bar.setProgress(0.8);
                bar.setText("Project Inception - Initializing CEF");
                ProjectInception.LOGGER.info("Linking CEF");
                LibCEF.init(new File(cefFolder, "Release"));
                bar.setProgress(1);
            } catch (Exception e) {
                e.printStackTrace();
                bar.setProgress(1);
                bar.setText("Project Inception - Could not setup CEF: " + e.getLocalizedMessage());
                sleep(1000);
            }
        }
    }

    private String getCefDistribution() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);
        if(os.contains("win")) {
            if(arch.contains("64")) {
                return CEF_WINDOWS_x64;
            } else {
                return CEF_WINDOWS_x32;
            }
        } else if(os.contains("mac") || os.contains("darwin")) {
            return CEF_MACOS_x64;
        } else if(os.contains("nux")) {
            if(arch.contains("arm")) {
                if(arch.contains("64")) {
                    return CEF_LINUX_ARM_x64;
                } else {
                    return CEF_LINUX_ARM_x32;
                }
            } else if(arch.contains("64")) {
                return CEF_LINUX_x64;
            } else {
                return CEF_LINUX_x32;
            }
        } else {
            return null;
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
