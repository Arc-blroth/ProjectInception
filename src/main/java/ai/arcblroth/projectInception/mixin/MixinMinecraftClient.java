package ai.arcblroth.projectInception.mixin;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.QueueProtocol;
import net.minecraft.client.MinecraftClient;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;
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
        projectInceptionTailer = ProjectInception.outputQueue.createTailer("parent2ChildQueueReader");
        projectInceptionTailer.direction(TailerDirection.FORWARD);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void prepareParent2ChildQueueMessages(CallbackInfo ci) {
        if(ProjectInception.IS_INNER) {
            if(projectInceptionTailer.index() > 0) {
                while(true) {
                    System.out.println("handling message");
                    try(DocumentContext dc = projectInceptionTailer.readingDocument()) {
                        if(dc.isPresent()) {
                            Bytes<?> bytes = dc.wire().bytes();
                            Message message = readParent2ChildMessage(bytes);
                            if(!message.getMessageType().equals(MessageType.I_HAVE_NO_IDEA)
                            && !message.getMessageType().equals(MessageType.IMAGE)) {
                                System.out.println(message.getMessageType());
                                ProjectInception.parent2ChildMessagesToHandle.add(message);
                            }
                        } else {
                            break;
                        }
                    }
                }
                System.out.println("done handling messages");
                // we move past the end by 1
                projectInceptionTailer.moveToIndex(projectInceptionTailer.index() - 1);
            }
        }
    }

}
