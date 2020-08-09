package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.ProjectInceptionEarlyRiser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(Window.class)
public class MixinWindow {

    @Inject(method = "close", at = @At("RETURN"))
    private void closeChronicleQueue(CallbackInfo ci) {
        if(ProjectInception.outputQueue != null
        && !ProjectInception.outputQueue.isClosed()) {
            ProjectInception.outputQueue.close();
        }
        File queueDir = new File(MinecraftClient.getInstance().runDirectory, "projectInception");
        ProjectInceptionEarlyRiser.yeetChronicleQueues(queueDir, false);
    }

}
