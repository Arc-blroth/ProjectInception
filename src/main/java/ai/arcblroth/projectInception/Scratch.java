package ai.arcblroth.projectInception;

import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.wire.DocumentContext;

class Scratch {
    public static void main(String[] args) {
        try(ChronicleQueue queue = ChronicleQueue
                .singleBuilder("test")
                .rollCycle(RollCycles.HOURLY) // hopefully no one has more than 70,000 fps
                .build()) {
            queue.acquireAppender().writeBytes(b -> b.writeByte(QueueProtocol.MessageType.IMAGE.header));
            QueueProtocol.MouseSetPosMessage mpMessage = new QueueProtocol.MouseSetPosMessage();
            mpMessage.x = 69;
            mpMessage.y = 420;
            QueueProtocol.writeParent2ChildMessage(mpMessage, queue.acquireAppender());
            System.out.println(queue.dump());

            ExcerptTailer tailer = queue.createTailer();
            tailer.toEnd();
            tailer.moveToIndex(tailer.index() - 1);
            while(tailer.index() != 0 && !QueueProtocol.peekMessageType(tailer).equals(QueueProtocol.MessageType.IMAGE)) {
                tailer.moveToIndex(tailer.index() - 1);
            }
            System.out.println(QueueProtocol.peekMessageType(tailer));
            try (DocumentContext dc = tailer.readingDocument()) {
                System.out.println(dc.isPresent());
                if (dc.isPresent()) {
                    Bytes bytes = dc.wire().bytes();
                    System.out.println(bytes.toHexString());
                }
            }
        }
    }
}