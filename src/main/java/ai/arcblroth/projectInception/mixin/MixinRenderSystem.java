package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import ai.arcblroth.projectInception.mc.QueueProtocol;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.openhft.chronicle.core.UnsafeMemory;
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    @Unique private static ByteBuffer projectInceptionOutput;

    @Inject(method = "initRenderer", at = @At("RETURN"))
    private static void initChronicleQueue(CallbackInfo ci) {
        if(ProjectInception.IS_INNER) {
            ProjectInceptionEarlyRiser.initChronicleQueues(
                    new File(MinecraftClient.getInstance().runDirectory, "projectInception" + File.separator + ProjectInceptionEarlyRiser.INSTANCE_PREFIX)
            );
        }
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
            ProjectInception.toParentQueue.acquireAppender().writeBytes(b -> {
                b.writeByte(QueueProtocol.MessageType.IMAGE.header);
                b.writeInt(fboWidth);
                b.writeInt(fboHeight);
                b.writeBoolean(!(MinecraftClient.getInstance().mouse.isCursorLocked() || ProjectInception.focusedInstance != null));
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
