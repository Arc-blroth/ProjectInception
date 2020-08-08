package ai.arcblroth.projectInception;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.wire.DocumentContext;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress;

class Scratch {
    public static void main(String[] args) {
        try(ChronicleQueue queue = ChronicleQueue
                .singleBuilder("test")
                .rollCycle(RollCycles.HOURLY) // hopefully no one has more than 70,000 fps
                .build()) {
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(4);
            byteBuffer.put(1, (byte) 1);
            byteBuffer.put(2, (byte) 2);
            byteBuffer.put(3, (byte) 3);
            queue.acquireAppender().writeBytes(b -> {
                UnsafeMemory.UNSAFE.copyMemory(
                        memAddress(byteBuffer),
                        b.addressForWrite(b.writePosition()),
                        byteBuffer.capacity()
                );
                b.writeSkip(byteBuffer.capacity());
            });
            System.out.println(queue.dump());

            ExcerptTailer tailer = queue.createTailer();
            tailer.toEnd();
            tailer.moveToIndex(tailer.index() - 1);
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