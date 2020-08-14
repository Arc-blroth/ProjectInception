package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.QueueProtocol;
import net.minecraft.client.MinecraftClient;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static ai.arcblroth.projectInception.QueueProtocol.*;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    private ExcerptTailer projectInceptionTailer;

    @Inject(method = "run", at = @At("HEAD"))
    private void prepareParent2ChildTailer(CallbackInfo ci) {
        if(ProjectInception.IS_INNER) {
            ProjectInception.LOGGER.log(Level.INFO, "Building tailer...");
            projectInceptionTailer = ProjectInception.outputQueue.createTailer("parent2ChildQueueReader");
            projectInceptionTailer.direction(TailerDirection.FORWARD);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void prepareParent2ChildQueueMessages(CallbackInfo ci) {
        if(ProjectInception.IS_INNER) {
            while(true) {
                try(DocumentContext dc = projectInceptionTailer.readingDocument()) {
                    if(dc.isPresent()) {
                        Bytes<?> bytes = dc.wire().bytes();
                        Message message = readParent2ChildMessage(bytes);
                        if(!message.getMessageType().equals(MessageType.I_HAVE_NO_IDEA)
                        && !message.getMessageType().equals(MessageType.IMAGE)) {
                            ProjectInception.parent2ChildMessagesToHandle.add(message);
                        }
                    } else {
                        dc.rollbackOnClose();
                        break;
                    }
                }
            }
        }
    }

}
