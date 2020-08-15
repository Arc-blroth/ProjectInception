package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.QueueProtocol;
import ai.arcblroth.projectInception.duck.IAmAKeyboard;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static ai.arcblroth.projectInception.QueueProtocol.*;

@Mixin(Keyboard.class)
public class MixinKeyboard implements IAmAKeyboard {

    @Shadow private MinecraftClient client;
    private final KeyboardKeyMessage projectInceptionKeMessage = new KeyboardKeyMessage();
    private final KeyboardCharMessage projectInceptionKcMessage = new KeyboardCharMessage();

    @Override
    public void projectInceptionUpdateKeyboardEvents(List<QueueProtocol.Message> events) {
        if(ProjectInception.IS_INNER) {
            long windowHandle = client.getWindow().getHandle();
            events.removeIf(message -> {
                if (message instanceof KeyboardKeyMessage) {
                    KeyboardKeyMessage keMessage = (KeyboardKeyMessage) message;
                    onKey(windowHandle, keMessage.key, keMessage.scancode, keMessage.action, keMessage.mods);
                    return true;
                } else if (message instanceof KeyboardCharMessage) {
                    KeyboardCharMessage kcMessage = (KeyboardCharMessage) message;
                    onChar(windowHandle, kcMessage.codepoint, kcMessage.mods);
                    return true;
                }
                return false;
            });
        }
    }

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onParentKey(long window, int key, int scancode, int i, int j, CallbackInfo ci) {
        if(window == this.client.getWindow().getHandle()) {
            if(ProjectInception.focusedInstance != null) {
                projectInceptionKeMessage.key = key;
                projectInceptionKeMessage.scancode = scancode;
                projectInceptionKeMessage.action = i;
                projectInceptionKeMessage.mods = j;
                ProjectInception.focusedInstance.sendParent2ChildMessage(projectInceptionKeMessage);
            }
        }
    }

    @Inject(method = "onChar", at = @At("HEAD"))
    private void onParentChar(long window, int i, int j, CallbackInfo ci) {
        if(window == this.client.getWindow().getHandle()) {
            if(ProjectInception.focusedInstance != null) {
                projectInceptionKcMessage.codepoint = i;
                projectInceptionKcMessage.mods = j;
                ProjectInception.focusedInstance.sendParent2ChildMessage(projectInceptionKcMessage);
            }
        }
    }

    @Shadow
    public void onKey(long window, int key, int scancode, int i, int j) {}

    @Shadow
    private void onChar(long window, int i, int j) {}

}
