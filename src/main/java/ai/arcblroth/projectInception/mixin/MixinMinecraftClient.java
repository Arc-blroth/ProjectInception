package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.duck.IAmAKeyboard;
import ai.arcblroth.projectInception.duck.IPreventMouseFromStackOverflow;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ai.arcblroth.projectInception.QueueProtocol.*;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Shadow public Mouse mouse;
    @Shadow public Keyboard keyboard;
    private ExcerptTailer projectInceptionTailer;
    private ArrayList<Message> inputEvents;

    @Inject(method = "run", at = @At("HEAD"))
    private void prepareParent2ChildTailer(CallbackInfo ci) {
        if(ProjectInception.IS_INNER) {
            ProjectInception.LOGGER.log(Level.INFO, "Building tailer...");
            projectInceptionTailer = ProjectInception.outputQueue.createTailer("parent2ChildQueueReader");
            projectInceptionTailer.direction(TailerDirection.FORWARD);
            inputEvents = new ArrayList<>();
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void updateQueuedInputEvents(CallbackInfo ci) {
        if(ProjectInception.IS_INNER) {
            inputEvents.clear();
            while(true) {
                try(DocumentContext dc = projectInceptionTailer.readingDocument()) {
                    if(dc.isPresent()) {
                        Bytes<?> bytes = dc.wire().bytes();
                        Message message = readParent2ChildMessage(bytes);
                        MessageType type = message.getMessageType();
                        if(!type.equals(MessageType.I_HAVE_NO_IDEA)
                        && !type.equals(MessageType.IMAGE)) {
                            inputEvents.add(message);
                        }
                    } else {
                        dc.rollbackOnClose();
                        break;
                    }
                }
            }
            if(this.mouse instanceof IPreventMouseFromStackOverflow) {
                ((IPreventMouseFromStackOverflow) this.mouse).projectInceptionUpdateMouseEvents(inputEvents);
            }
            if(this.keyboard instanceof IAmAKeyboard) {
                ((IAmAKeyboard) this.keyboard).projectInceptionUpdateKeyboardEvents(inputEvents);
            }
        }
    }

}
