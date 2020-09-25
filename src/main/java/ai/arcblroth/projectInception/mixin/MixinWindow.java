package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.glfwHideWindow;

@Mixin(Window.class)
public class MixinWindow {

    @Shadow @Final private long handle;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void hideWindow(CallbackInfo ci) {
        // This is used as a fallback in case the
        // somewhat more brittle transformation
        // in ProjectInceptionEarlyRiser fails
        if(ProjectInception.IS_INNER) {
            glfwHideWindow(handle);
        }
    }

}
