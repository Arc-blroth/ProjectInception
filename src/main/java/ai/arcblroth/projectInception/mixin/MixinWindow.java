package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class MixinWindow {

    @Inject(method = "close", at = @At("RETURN"))
    private void closeChronicleQueue(CallbackInfo ci) {
        if(ProjectInception.queue != null
        && !ProjectInception.queue.isClosed()) {
            ProjectInception.queue.close();
        }
    }

}
