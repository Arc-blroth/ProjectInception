package ai.arcblroth.projectInception.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Window;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
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

    @Inject(method = "flipFrame", at = @At("RETURN"))
    private static void readFrame(long window, CallbackInfo ci) {
        ByteBuffer output;
        try {
            System.out.println("here");
            glReadBuffer(GL_FRONT_LEFT);
            Window windowObj = MinecraftClient.getInstance().getWindow();
            int fboWidth = windowObj.getFramebufferWidth();
            int fboHeight = windowObj.getFramebufferHeight();
            output = BufferUtils.createByteBuffer(fboWidth * fboHeight * 4);
            glReadPixels(0, 0, fboWidth, fboHeight, GL_RGBA, GL_UNSIGNED_BYTE, output);
            NativeImage nativeImage = new NativeImage(NativeImage.Format.ABGR, fboWidth, fboHeight, true, memAddress(output));
            File yeet = new File(MinecraftClient.getInstance().runDirectory, "yeet.png");
            yeet.createNewFile();
            nativeImage.writeFile(yeet);
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }

}
