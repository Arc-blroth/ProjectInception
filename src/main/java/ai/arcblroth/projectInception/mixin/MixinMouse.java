package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static ai.arcblroth.projectInception.QueueProtocol.*;

@Mixin(Mouse.class)
public abstract class MixinMouse {

    @Shadow private MinecraftClient client;
    @Shadow private boolean hasResolutionChanged;

    @Inject(method = "updateMouse", at = @At("HEAD"))
    private void updateQueuedMouseEvents(CallbackInfo ci) {
        if(ProjectInception.IS_INNER) {
            long windowHandle = client.getWindow().getHandle();
            ProjectInception.parent2ChildMessagesToHandle.removeIf(message -> {
                System.out.println(message.getMessageType());
               if(message instanceof MouseButtonMessage) {
                   MouseButtonMessage mbMessage = (MouseButtonMessage) message;
                   System.out.println(mbMessage.button + " " + (mbMessage.message == 1));
                   onMouseButton(windowHandle, mbMessage.button, mbMessage.message, mbMessage.mods);
                   return true;
               } else if(message instanceof MouseScrollMessage) {
                   MouseScrollMessage msMessage = (MouseScrollMessage) message;
                   onMouseScroll(windowHandle, msMessage.horizontal, msMessage.vertical);
               } else if(message instanceof MouseMoveMessage) {
                   MouseMoveMessage mmMessage = (MouseMoveMessage) message;
                   onCursorPos(
                           windowHandle,
                           mmMessage.x * this.client.getWindow().getWidth(),
                           mmMessage.y * this.client.getWindow().getHeight()
                   );
               } else if(message instanceof MouseSetPosMessage) {
                   MouseSetPosMessage mpMessage = (MouseSetPosMessage) message;
                   System.out.println(mpMessage.x + " " + mpMessage.y);
                   this.hasResolutionChanged = true;
                   onCursorPos(
                           windowHandle,
                           mpMessage.x * this.client.getWindow().getWidth(),
                           mpMessage.y * this.client.getWindow().getHeight()
                   );
               }
               return false;
            });
        }
    }

    @Shadow
    private void onMouseButton(long window, int button, int action, int mods) {}

    @Shadow
    private void onMouseScroll(long window, double horizontal, double vertical) {}

    @Shadow
    private void onCursorPos(long window, double x, double y) {}

}
