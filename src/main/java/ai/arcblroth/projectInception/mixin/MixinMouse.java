package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.duck.IPreventMouseFromStackOverflow;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ConcurrentModificationException;
import java.util.List;

import static ai.arcblroth.projectInception.mc.QueueProtocol.*;

@Mixin(Mouse.class)
public abstract class MixinMouse implements IPreventMouseFromStackOverflow {

    @Shadow @Final private MinecraftClient client;
    @Shadow private boolean hasResolutionChanged;
    @Unique private boolean projectInceptionPreventStackOverflowPlease = false;

    @Unique private final MouseButtonMessage projectInceptionMbMessage = new MouseButtonMessage();
    @Unique private final MouseScrollMessage projectInceptionMsMessage = new MouseScrollMessage();
    @Unique private final MouseMoveMessage projectInceptionMmMessage = new MouseMoveMessage();
    @Unique private final MouseSetPosMessage projectInceptionMpMessage = new MouseSetPosMessage();

    @Override
    public void projectInceptionUpdateMouseEvents(List<Message> events) {
        if(ProjectInception.IS_INNER) {
            projectInceptionPreventStackOverflowPlease = true;
            try {
                long windowHandle = client.getWindow().getHandle();
                events.removeIf(message -> {
                    if (message instanceof MouseButtonMessage) {
                        MouseButtonMessage mbMessage = (MouseButtonMessage) message;
                        onMouseButton(windowHandle, mbMessage.button, mbMessage.message, mbMessage.mods);
                        return true;
                    } else if (message instanceof MouseScrollMessage) {
                        MouseScrollMessage msMessage = (MouseScrollMessage) message;
                        onMouseScroll(windowHandle, msMessage.horizontal, msMessage.vertical);
                        return true;
                    } else if (message instanceof MouseMoveMessage) {
                        MouseMoveMessage mmMessage = (MouseMoveMessage) message;
                        onCursorPos(
                                windowHandle,
                                mmMessage.x * this.client.getWindow().getWidth(),
                                mmMessage.y * this.client.getWindow().getHeight()
                        );
                        return true;
                    } else if (message instanceof MouseSetPosMessage) {
                        MouseSetPosMessage mpMessage = (MouseSetPosMessage) message;
                        this.hasResolutionChanged = true;
                        onCursorPos(
                                windowHandle,
                                mpMessage.x * this.client.getWindow().getWidth(),
                                mpMessage.y * this.client.getWindow().getHeight()
                        );
                        return true;
                    }
                    return false;
                });
            } catch (ConcurrentModificationException e) {
                // I honestly have no idea why this occurs
                // as the list should only be modified in one
                // location at one time
                // perhaps forced ticks are breaking it? idk
                ProjectInception.LOGGER.log(Level.WARN, "ConcurrentModificationException whilst processing Mouse events");
            } finally {
                projectInceptionPreventStackOverflowPlease = false;
            }
        }
    }

    @Redirect(method = "onCursorPos", at = @At(value = "INVOKE", target = "net/minecraft/client/Mouse.updateMouse()V"))
    private void preventStackOverflow(Mouse xfceReference) {
        if(xfceReference instanceof IPreventMouseFromStackOverflow) {
            if(!((IPreventMouseFromStackOverflow) xfceReference).getShouldPreventStackOverflow()) {
                xfceReference.updateMouse();
            }
        }
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onParentMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if(window == this.client.getWindow().getHandle()) {
            if(ProjectInception.focusedInstance != null) {
                projectInceptionMbMessage.button = button;
                projectInceptionMbMessage.message = action;
                projectInceptionMbMessage.mods = mods;
                ProjectInception.focusedInstance.sendParent2ChildMessage(projectInceptionMbMessage);
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onParentMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if(window == this.client.getWindow().getHandle()) {
            if(ProjectInception.focusedInstance != null) {
                projectInceptionMsMessage.horizontal = horizontal;
                projectInceptionMsMessage.vertical = vertical;
                ProjectInception.focusedInstance.sendParent2ChildMessage(projectInceptionMsMessage);
            }
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void onParentCursorPos(long window, double x, double y, CallbackInfo ci) {
        if(window == this.client.getWindow().getHandle()) {
            if(ProjectInception.focusedInstance != null) {
                if(this.hasResolutionChanged) {
                    projectInceptionMpMessage.x = x / this.client.getWindow().getWidth();
                    projectInceptionMpMessage.y = y / this.client.getWindow().getHeight();
                    ProjectInception.focusedInstance.sendParent2ChildMessage(projectInceptionMpMessage);
                } else {
                    projectInceptionMmMessage.x = x / this.client.getWindow().getWidth();
                    projectInceptionMmMessage.y = y / this.client.getWindow().getHeight();
                    ProjectInception.focusedInstance.sendParent2ChildMessage(projectInceptionMmMessage);
                }
            }
        }
    }

    @Override
    public boolean getShouldPreventStackOverflow() {
        return projectInceptionPreventStackOverflowPlease;
    }

    @Shadow
    private void onMouseButton(long window, int button, int action, int mods) {}

    @Shadow
    private void onMouseScroll(long window, double horizontal, double vertical) {}

    @Shadow
    private void onCursorPos(long window, double x, double y) {}

}
