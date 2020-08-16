package ai.arcblroth.projectInception;

import com.chocohead.mm.api.ClassTinkerers;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.util.ListIterator;
import java.util.function.Predicate;

public class ProjectInceptionEarlyRiser implements Runnable {

    public static final String ARG_IS_INNER;
    public static final String ARG_DISPLAY_WIDTH;
    public static final String ARG_DISPLAY_HEIGHT;

    static {
        final String className = ProjectInceptionEarlyRiser.class.getName();
        ARG_IS_INNER = className + ".IS_INNER";
        ARG_DISPLAY_WIDTH = className + ".DISPLAY_WIDTH";
        ARG_DISPLAY_HEIGHT = className + ".DISPLAY_HEIGHT";
    }

    public static final int DISPLAY_SCALE = 64;

    // This make the child process not actually init Minecraft
    // so that I can test things without destroying my computer
    public static final boolean USE_FAUX_INNER = false;

    public static final Logger LOGGER = LogManager.getLogger("ProjectInception");
    public static final boolean IS_INNER = System.getProperty(ARG_IS_INNER) != null
            && System.getProperty(ARG_IS_INNER).equals("true");
    public static String[] ARGUMENTS = new String[0];

    @Override
    public void run() {
        if(IS_INNER) {
            LOGGER.log(Level.INFO, "Initializing for inner instance...");
        } else {
            LOGGER.log(Level.INFO, "Initializing for parent instance...");
        }
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
                });
            });
        }
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
        LOGGER.log(Level.INFO, "Initializing queue...");
        ProjectInception.outputQueue = ChronicleQueue
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
