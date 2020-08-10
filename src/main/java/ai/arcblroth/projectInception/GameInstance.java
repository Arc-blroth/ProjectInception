package ai.arcblroth.projectInception;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;
import org.apache.logging.log4j.Level;
import org.lwjgl.BufferUtils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GameInstance {

    private static int instanceCounter = 0;
    private static final ArrayList<GameInstance> instancesToCleanUp = new ArrayList<>();
    private int instanceNumber;
    private Process process;
    private final Object processLock = new Object();
    private boolean isProcessBeingKilled = false;
    private final ArrayList<String> commandLine;
    private final ExcerptTailer tailer;
    private int lastWidth = 0;
    private int lastHeight = 0;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            instancesToCleanUp.forEach(g -> {
                ProjectInception.LOGGER.log(Level.INFO, "Destroying game instances on exit...");
                g.stop(false);
            });
        }));
    }

    public GameInstance() {
        instanceNumber = instanceCounter;
        instanceCounter++;
        instancesToCleanUp.add(this);

        commandLine = new ArrayList<>();
        commandLine.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        commandLine.addAll(jvmArgs);
        if(!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            if (!jvmArgs.stream().anyMatch(s -> s.contains("-cp") || s.contains("-classpath"))) {
                commandLine.add("-cp");
                commandLine.add(System.getProperty("java.class.path"));
            }
            if (!jvmArgs.stream().anyMatch(s -> s.contains("-Djava.library.path"))) {
                commandLine.add("-Djava.library.path=" + System.getProperty("java.library.path"));
            }
            commandLine.add("-DprojectInceptionInner=true");
            commandLine.add(ProjectInception.MAIN_CLASS);
            List<String> cmdArgs = Arrays.asList(ProjectInception.ARGUMENTS);
            commandLine.addAll(cmdArgs);
            if(!cmdArgs.contains("--disableMultiplayer")) {
                commandLine.add("--disableMultiplayer");
            }
        } else {
            commandLine.removeIf(s -> s.startsWith("-javaagent") || s.startsWith("-agentlib"));
            commandLine.add("-cp");
            commandLine.add(System.getProperty("java.class.path"));
            if(System.getProperty("java.library.path").length() > 0) {
                commandLine.add("-Djava.library.path=" + System.getProperty("java.library.path"));
            }
            commandLine.add("-DprojectInceptionInner=true");
            commandLine.add("net.fabricmc.devlaunchinjector.Main");
            commandLine.add("--disableMultiplayer");
        }

        this.tailer = ProjectInception.outputQueue.createTailer().direction(TailerDirection.NONE);
    }

    public void start() {
        if(process == null || !process.isAlive()) {
            try {
                ProjectInception.LOGGER.log(Level.INFO, "Running command line: " + String.join(" ", commandLine));
                process = new ProcessBuilder(commandLine).inheritIO().start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop(boolean async) {
        Runnable stopFunc = () -> {
            if(this.process != null && this.process.isAlive()) {
                synchronized (processLock) {
                    isProcessBeingKilled = true;
                }
                ProjectInception.LOGGER.log(Level.DEBUG, "Destroying game instance #" + this.instanceNumber);
                this.process.destroy();
                if(this.process.isAlive()) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ignored) {}
                    if (this.process.isAlive()) {
                        this.process.destroyForcibly();
                    }
                }
                this.process = null;
                synchronized (processLock) {
                    isProcessBeingKilled = false;
                }
            }
        };
        if(async) {
            new Thread(stopFunc).start();
        } else {
            stopFunc.run();
        }
    }

    public ByteBuffer getLastTexture(ByteBuffer in) {
        synchronized (processLock) {
            if(isProcessBeingKilled) return null;
            if (process == null || !process.isAlive()) return null;
            this.tailer.toEnd();
            if (this.tailer.index() == 0) return in;
            this.tailer.moveToIndex(this.tailer.index() - 1);
            try (DocumentContext dc = this.tailer.readingDocument()) {
                if (dc.isPresent()) {
                    Bytes<?> bytes = dc.wire().bytes();
                    lastWidth = bytes.readInt();
                    lastHeight = bytes.readInt();
                    if (in != null) {
                        in.rewind();
                    }
                    if (in == null || in.capacity() < lastWidth * lastHeight * 4) {
                        in = BufferUtils.createByteBuffer(lastWidth * lastHeight * 4);
                    }
                    bytes.read(in);
                    in.rewind();
                    for (int i = 0; i < in.capacity(); i += 4) {
                        // on the sending side alpha is zero, so
                        // we make sure it gets set to 100% here
                        in.put(i + 3, (byte) 255);
                    }
                }
                return in;
            }
        }
    }

    public void click(double hitX, double hitY) {

    }

    public int getLastWidth() {
        return lastWidth;
    }

    public int getLastHeight() {
        return lastHeight;
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

}
