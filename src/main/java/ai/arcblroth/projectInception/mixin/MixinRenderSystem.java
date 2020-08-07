package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.queue.ChronicleQueue;
import org.apache.logging.log4j.Level;
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    private static ByteBuffer projectInceptionOutput;

    @Inject(method = "initRenderer", at = @At("RETURN"))
    private static void initChronicleQueue(CallbackInfo ci) {
        // Because we need to reuse this queue, we don't wrap this in a try
        // with resources. The queue is closed in MixinWindow#closeChronicleQueue.
        ProjectInception.LOGGER.log(Level.INFO, "Initializing queue...");
        ProjectInception.queue = ChronicleQueue.singleBuilder(
                MinecraftClient.getInstance().runDirectory + "/projectInception"
        ).build();
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
            ProjectInception.queue.acquireAppender().writeBytes(b -> {
                UnsafeMemory.UNSAFE.copyMemory(
                        memAddress(projectInceptionOutput),
                        b.addressForWrite(b.writePosition()),
                        projectInceptionOutput.capacity()
                );
            });
        }
    }

}
