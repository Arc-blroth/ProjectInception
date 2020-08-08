package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    private static ByteBuffer projectInceptionOutput;

    @Inject(method = "initRenderer", at = @At("RETURN"))
    private static void initChronicleQueue(CallbackInfo ci) {
        File queueDir = new File(MinecraftClient.getInstance().runDirectory, "projectInception");
        if(!ProjectInception.IS_INNER) {
            if (queueDir.exists()) {
                try {
                    FileUtils.deleteDirectory(queueDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new CrashException(new CrashReport("[Project Inception] Couldn't delete old queues!", e));
                }
            }
        }
        // Because we need to reuse this queue, we don't wrap this in a try
        // with resources. The queue is closed in MixinWindow#closeChronicleQueue.
        ProjectInception.LOGGER.log(Level.INFO, "Initializing queue...");
        ProjectInception.outputQueue = ChronicleQueue
                .singleBuilder(queueDir)
                .rollCycle(RollCycles.HOURLY) // hopefully no one has more than 70,000 fps
                .build();
    }

    @Inject(method = "flipFrame", at = @At("RETURN"))
    private static void readFrame(long window, CallbackInfo ci) {
        if(ProjectInception.IS_INNER) {
            glReadBuffer(GL_FRONT_LEFT);
            Window windowObj = MinecraftClient.getInstance().getWindow();
            int fboWidth = windowObj.getFramebufferWidth();
            int fboHeight = windowObj.getFramebufferHeight();
            if (projectInceptionOutput == null
                    || projectInceptionOutput.capacity() < fboWidth * fboHeight * 4) {
                projectInceptionOutput = BufferUtils.createByteBuffer(fboWidth * fboHeight * 4);
            }
            glReadPixels(0, 0, fboWidth, fboHeight, GL_RGBA, GL_UNSIGNED_BYTE, projectInceptionOutput);
            ProjectInception.outputQueue.acquireAppender().writeBytes(b -> {
                b.writeInt(fboWidth);
                b.writeInt(fboHeight);
                UnsafeMemory.UNSAFE.copyMemory(
                        memAddress(projectInceptionOutput),
                        b.addressForWrite(b.writePosition()),
                        projectInceptionOutput.capacity()
                );
                b.writeSkip(projectInceptionOutput.capacity());
            });
        }
    }

}
