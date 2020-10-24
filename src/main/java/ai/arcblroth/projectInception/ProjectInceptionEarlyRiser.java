package ai.arcblroth.projectInception;

import ai.arcblroth.projectInception.config.ProjectInceptionConfig;
import com.chocohead.mm.api.ClassTinkerers;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;

public class ProjectInceptionEarlyRiser implements Runnable {

    public static final String ARG_IS_INNER;
    public static final String ARG_DISPLAY_WIDTH;
    public static final String ARG_DISPLAY_HEIGHT;
    public static final String ARG_INSTANCE_PREFIX;

    public static final Logger LOGGER = LogManager.getLogger("ProjectInception");
    public static final boolean IS_INNER;
    public static final String INSTANCE_PREFIX;
    public static final String TATERWEBZ_PREFIX;
    public static final String BROWSER_PREFIX;
    public static String[] ARGUMENTS = new String[0];

    private static String OUR_CONFIG = null;

    static {
        final String className = ProjectInceptionEarlyRiser.class.getName();
        ARG_IS_INNER = className + ".IS_INNER";
        ARG_DISPLAY_WIDTH = className + ".DISPLAY_WIDTH";
        ARG_DISPLAY_HEIGHT = className + ".DISPLAY_HEIGHT";
        ARG_INSTANCE_PREFIX = className + ".INSTANCE_PREFIX";

        IS_INNER = System.getProperty(ARG_IS_INNER) != null
                && System.getProperty(ARG_IS_INNER).equals("true");
        INSTANCE_PREFIX = System.getProperty(ARG_INSTANCE_PREFIX) != null
                ? System.getProperty(ARG_INSTANCE_PREFIX)
                : "inst";
        TATERWEBZ_PREFIX = "taterwebz-child-process";
        BROWSER_PREFIX = System.getProperty(ARG_INSTANCE_PREFIX) != null
                ? INSTANCE_PREFIX + "-browser"
                : "browser";
    }

    private static File generateLoggingConfiguration(String namespace) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            if(OUR_CONFIG == null) {
                try {
                    OUR_CONFIG = IOUtils.toString(
                            FabricLoader.getInstance().getModContainer("minecraft").get()
                                    .getPath("log4j2.xml").toUri().toURL().openStream()
                            , StandardCharsets.UTF_8);
                } catch (IOException e) {
                    ProjectInception.LOGGER.warn(e);
                }
            }
            if(OUR_CONFIG == null) {
                return null;
            }
            Document document = db.parse(new ByteArrayInputStream(OUR_CONFIG.getBytes()));
            NodeList rrafs = document.getElementsByTagName("RollingRandomAccessFile");
            for (int i = 0; i < rrafs.getLength(); i++) {
                Node rraf = rrafs.item(i);
                Node fileName = rraf.getAttributes().getNamedItem("fileName");
                Node filePattern = rraf.getAttributes().getNamedItem("filePattern");
                if (fileName != null && fileName.getNodeValue() != null) {
                    String[] fileNameParts = fileName.getNodeValue().split("[/\\\\]");
                    fileNameParts[fileNameParts.length - 1] = "project-inception-" + namespace + "-" + fileNameParts[fileNameParts.length - 1];
                    fileName.setNodeValue(String.join(File.separator, fileNameParts));
                }
                if (filePattern != null && filePattern.getNodeValue() != null) {
                    String[] filePatternParts = filePattern.getNodeValue().split("[/\\\\]");
                    filePatternParts[filePatternParts.length - 1] = "project-inception-" + namespace + "-" + filePatternParts[filePatternParts.length - 1];
                    filePattern.setNodeValue(String.join(File.separator, filePatternParts));
                }
            }
            DOMSource domSource = new DOMSource(document);
            File f = new File(MinecraftClient.getInstance().runDirectory,
                    "logs" + File.separator + "projectInceptionTemp" + File.separator + namespace + ".xml");
            f.getParentFile().mkdirs();
            f.createNewFile();
            f.deleteOnExit();
            try(FileOutputStream fos = new FileOutputStream(f)) {
                StreamResult result = new StreamResult(fos);
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.transform(domSource, result);
            }
            return f;
        } catch (Exception e) {
            ProjectInception.LOGGER.warn(e);
            return null;
        }
    }

    public static ArrayList<String> newCommandLineForForking(String loggingNamespace, boolean useOriginalClasspath) {
        ArrayList<String> commandLine = new ArrayList<>();
        commandLine.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        commandLine.addAll(jvmArgs);
        if(!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            if (!useOriginalClasspath || !jvmArgs.stream().anyMatch(s -> s.contains("-cp") || s.contains("-classpath"))) {
                commandLine.add("-cp");
                List<String> classpath = new ArrayList<>();
                classpath.add(System.getProperty("java.class.path"));
                try {
                    classpath.add(FabricLauncherBase.minecraftJar.toString());
                    FabricLoader.getInstance().getAllMods().stream()
                            .filter(m -> m.getMetadata().getId().matches("fabric.*|project_inception|mm"))
                            .map(c -> (ModContainer)c)
                            .map(ModContainer::getOriginUrl)
                            .filter(u -> u.getProtocol().equals("file"))
                            .map(u -> {
                                try {
                                    return Paths.get(u.toURI());
                                } catch (URISyntaxException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .map(p -> p.toFile().toString())
                            .forEach(classpath::add);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                commandLine.add(String.join(File.pathSeparator, classpath));
            }
        } else {
            ListIterator<String> iter = commandLine.listIterator();
            while(iter.hasNext()) {
                String s = iter.next();
                if(s.startsWith("-agentlib")) {
                    iter.remove();
                    iter.add(s.replace("server=n", "server=y"));
                }
                if(s.startsWith("-Djava.class.path")) {
                    iter.remove();
                }
            }
            commandLine.add("-cp");
            commandLine.add(System.getProperty("java.class.path"));
        }
        File configFile = generateLoggingConfiguration(loggingNamespace);
        if(configFile != null) {
            commandLine.removeIf(s -> s.startsWith("-Dlog4j.configurationFile="));
            commandLine.add("-Dlog4j.configurationFile=" + configFile.getAbsolutePath());
        }
        return commandLine;
    }

    @Override
    public void run() {
        if(IS_INNER) {
            LOGGER.log(Level.INFO, "Initializing for inner instance...");
        } else {
            LOGGER.log(Level.INFO, "Initializing for parent instance...");
        }
        ProjectInceptionConfig.load();
        ProjectInceptionConfig.save();
        if(IS_INNER) {
            // Force the window to not appear
            final String windowClassName = FabricLoader.getInstance().isDevelopmentEnvironment()
                    ? "net/minecraft/client/util/Window"
                    : "net/minecraft/class_1041";
            ClassTinkerers.addTransformation(windowClassName, classNode -> {
                classNode.methods.stream().filter(method -> method.name.equals("<init>")).forEach(method -> {
                    ListIterator<AbstractInsnNode> insns = method.instructions.iterator();
                    MethodInsnNode glfwDefaultWindowHints = (MethodInsnNode) findInsn(method, insn ->
                            insn instanceof MethodInsnNode && ((MethodInsnNode) insn).name.equals("glfwDefaultWindowHints"));
                    moveAfter(insns, glfwDefaultWindowHints);
                    insns.add(new LdcInsnNode(GLFW.GLFW_VISIBLE));
                    insns.add(new InsnNode(Opcodes.ICONST_0));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/lwjgl/glfw/GLFW", "glfwWindowHint", "(II)V"));
                    insns.add(new LdcInsnNode(GLFW.GLFW_RESIZABLE));
                    insns.add(new InsnNode(Opcodes.ICONST_0));
                    insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/lwjgl/glfw/GLFW", "glfwWindowHint", "(II)V"));
                });
            });
        }
        LOGGER.log(Level.INFO, "Project Inception early initialization done.");
    }

    // from EntrypointPatch
    private static AbstractInsnNode findInsn(MethodNode node, Predicate<AbstractInsnNode> predicate) {
        for(int i = node.instructions.size() - 1; i >= 0; --i) {
            AbstractInsnNode insn = node.instructions.get(i);
            if (predicate.test(insn)) {
                return insn;
            }
        }
        return null;
    }

    private static void moveAfter(ListIterator<AbstractInsnNode> it, AbstractInsnNode targetNode) {
        while(it.hasNext()) {
            AbstractInsnNode node = it.next();
            if (node == targetNode) {
                return;
            }
        }
    }

    public static void initChronicleQueues(File queueDir) {
        yeetChronicleQueues(queueDir, true);
        // Because we need to reuse this queue, we don't wrap this in a try
        // with resources. The queue is closed in MixinWindow#closeChronicleQueue.
        LOGGER.log(Level.INFO, "Initializing fromParentQueue...");
        ProjectInception.toParentQueue = ProjectInceptionEarlyRiser.buildQueue(queueDir);
    }

    public static ChronicleQueue buildQueue(File queueDir) {
        return ChronicleQueue
                .singleBuilder(queueDir)
                .rollCycle(RollCycles.MINUTELY)
                .build();
    }

    public static void yeetChronicleQueues(File queueDir, boolean allowCrash) {
        if(!ProjectInceptionEarlyRiser.IS_INNER) {
            if (queueDir.exists()) {
                try {
                    FileUtils.deleteDirectory(queueDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    if(allowCrash) {
                        throw new CrashException(new CrashReport("[Project Inception] Couldn't delete old queues!", e));
                    }
                }
            }
        }
    }

}
