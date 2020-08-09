package ai.arcblroth.projectInception;

import com.chocohead.mm.api.ClassTinkerers;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
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

    public static final Logger LOGGER = LogManager.getLogger("ProjectInception");
    public static final boolean IS_INNER = System.getProperty("projectInceptionInner") != null
            && System.getProperty("projectInceptionInner").equals("true");
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
            ClassTinkerers.addTransformation("net/minecraft/client/util/Window", classNode -> {
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
