package ai.arcblroth.projectInception;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;

/**
 * A message sent through a Chronicle Queue
 * has the basic format:
 * <br><br>
 * <pre><code>
 *     1 byte MESSAGE TYPE HEADER
 *     n byte CONTENT
 * </code></pre>
 * There are several types of messages, each
 * with different content formats.
 * <ol start="0">
 *     <li>
 *         {@link MessageType#IMAGE} | child &rarr; parent | RGBA encoded image.<br>
 *         <code>int fboWidth</code><br>
 *         <code>int fboHeight</code><br>
 *         <code>ByteBuffer image // size fboWidth * fboHeight * 4</code><br>
 *     </li>
 *     <li>
 *         {@link MessageType#MOUSE_BUTTON} | parent &rarr; child | see <code>net.minecraft.client.Mouse#onMouseButton</code><br>
 *         <code>int button</code><br>
 *         <code>int message</code><br>
 *         <code>int mods</code><br>
 *     </li>
 *     <li>
 *         {@link MessageType#MOUSE_SCROLL} | parent &rarr; child | see <code>net.minecraft.client.Mouse#onMouseScroll</code><br>
 *         <code>double horizontal</code><br>
 *         <code>double vertical</code><br>
 *     </li>
 *     <li>
 *         {@link MessageType#MOUSE_MOVE} | parent &rarr; child | see <code>net.minecraft.client.Mouse#onCursorPos</code><br>
 *         <code>double x</code><br>
 *         <code>double y</code><br>
 *     </li>
 *     <li>
 *         {@link MessageType#MOUSE_SET_POS} | parent &rarr; child | coordinates are decimals of the window dimensions
 *         <code>double x</code><br>
 *         <code>double y</code><br>
 *     </li>
 *     <li>
 *         {@link MessageType#KEYBOARD_KEY} | parent &rarr; child | see <code>net.minecraft.client.Keyboard#onKey</code><br>
 *         <code>int key</code><br>
 *         <code>int scancode</code><br>
 *         <code>int action</code><br>
 *         <code>int mods</code><br>
 *     </li>
 *     <li>
 *         {@link MessageType#KEYBOARD_CHAR} | parent &rarr; child | see <code>net.minecraft.client.Keyboard#onChar</code><br>
 *         <code>int codepoint</code><br>
 *         <code>int mods</code><br>
 *     </li>
 * </ol>
 */
public class QueueProtocol {

    public enum MessageType {
        I_HAVE_NO_IDEA(0),
        IMAGE(1),
        MOUSE_BUTTON(2),
        MOUSE_SCROLL(3),
        MOUSE_MOVE(4),
        MOUSE_SET_POS(5),
        KEYBOARD_KEY(6),
        KEYBOARD_CHAR(7);

        public final byte header;

        MessageType(int header) {
            this.header = (byte)header;
        }

        public static MessageType fromHeader(byte header) {
            for(MessageType type : MessageType.values()) {
                if(header == type.header) return type;
            }
            return MessageType.I_HAVE_NO_IDEA;
        }
    }

    public static class Message {
        public MessageType getMessageType() { return MessageType.I_HAVE_NO_IDEA; }
    }

    // Images are handled exceptionally for SPEEED
    // public static final class ImageMessage extends Message {
    //     public int fboWidth;
    //     public int fboHeight;
    //     public ByteBuffer image;
    //     @Override public MessageType getMessageType() { return MessageType.IMAGE; }
    // }

    public static final class MouseButtonMessage extends Message {
        public int button;
        public int message;
        public int mods;
        @Override public MessageType getMessageType() { return MessageType.MOUSE_BUTTON; }
    }

    public static final class MouseScrollMessage extends Message {
        public double horizontal;
        public double vertical;
        @Override public MessageType getMessageType() { return MessageType.MOUSE_SCROLL; }
    }

    public static final class MouseMoveMessage extends Message {
        public double x;
        public double y;
        @Override public MessageType getMessageType() { return MessageType.MOUSE_MOVE; }
    }

    public static final class MouseSetPosMessage extends Message {
        public double x; // these are relative to the screen width and height
        public double y;
        @Override public MessageType getMessageType() { return MessageType.MOUSE_SET_POS; }
    }

    public static final class KeyboardKeyMessage extends Message {
        public int key;
        public int scancode;
        public int action;
        public int mods;
        @Override public MessageType getMessageType() { return MessageType.KEYBOARD_KEY; }
    }

    public static final class KeyboardCharMessage extends Message {
        public int codepoint;
        public int mods;
        @Override public MessageType getMessageType() { return MessageType.KEYBOARD_CHAR; }
    }

    public static MessageType peekMessageType(ExcerptTailer tailer) {
        long index = tailer.index();
        if(index == 0) return MessageType.I_HAVE_NO_IDEA;
        try (DocumentContext dc = tailer.readingDocument()) {
            dc.rollbackOnClose();
            if(dc.isPresent()) {
                byte header = dc.wire().bytes().readByte();
                return MessageType.fromHeader(header);
            }
            return MessageType.I_HAVE_NO_IDEA;
        }
    }

    public static void writeParent2ChildMessage(Message message, ExcerptAppender appender) {
        MessageType type = message.getMessageType();
        if(type.equals(MessageType.I_HAVE_NO_IDEA)) throw new NullPointerException("Unknown message type");
        if(type.equals(MessageType.IMAGE)) throw new IllegalStateException("ImageMessage is not a parent -> child message");
        appender.writeBytes(b -> {
            b.writeByte(type.header);
            if(message instanceof MouseButtonMessage) {
                MouseButtonMessage mbMessage = (MouseButtonMessage) message;
                b.writeInt(mbMessage.button);
                b.writeInt(mbMessage.message);
                b.writeInt(mbMessage.mods);
            } else if(message instanceof MouseScrollMessage) {
                MouseScrollMessage msMessage = (MouseScrollMessage) message;
                b.writeDouble(msMessage.horizontal);
                b.writeDouble(msMessage.vertical);
            } else if(message instanceof MouseMoveMessage) {
                MouseMoveMessage mmMessage = (MouseMoveMessage) message;
                b.writeDouble(mmMessage.x);
                b.writeDouble(mmMessage.y);
            } else if(message instanceof MouseSetPosMessage) {
                MouseSetPosMessage mpMessage = (MouseSetPosMessage) message;
                b.writeDouble(mpMessage.x);
                b.writeDouble(mpMessage.y);
            } else if(message instanceof KeyboardKeyMessage) {
                KeyboardKeyMessage keMessage = (KeyboardKeyMessage) message;
                b.writeInt(keMessage.action);
                b.writeInt(keMessage.key);
                b.writeInt(keMessage.mods);
            } else if(message instanceof KeyboardCharMessage) {
                KeyboardCharMessage kcMessage = (KeyboardCharMessage) message;
                b.writeInt(kcMessage.codepoint);
                b.writeInt(kcMessage.mods);
            }
        });
    }

    public static Message readParent2ChildMessage(ExcerptTailer tailer) {
        long index = tailer.index();
        if(index == 0) return new Message();
        try (DocumentContext dc = tailer.readingDocument()) {
            if(dc.isPresent()) {
                Bytes<?> bytes = dc.wire().bytes();
                return readParent2ChildMessage(bytes);
            }
            return new Message();
        }
    }

    public static Message readParent2ChildMessage(Bytes<?> bytes) {
        byte header = bytes.readByte();
        MessageType messageType = MessageType.fromHeader(header);
        if(messageType.equals(MessageType.I_HAVE_NO_IDEA)) return new Message();
        if(messageType.equals(MessageType.IMAGE)) return new Message();
        if(messageType.equals(MessageType.MOUSE_BUTTON)) {
            MouseButtonMessage mbMessage = new MouseButtonMessage();
            mbMessage.button = bytes.readInt();
            mbMessage.message = bytes.readInt();
            mbMessage.mods = bytes.readInt();
            return mbMessage;
        } else if(messageType.equals(MessageType.MOUSE_SCROLL)) {
            MouseScrollMessage msMessage = new MouseScrollMessage();
            msMessage.horizontal = bytes.readDouble();
            msMessage.vertical = bytes.readDouble();
            return msMessage;
        } else if(messageType.equals(MessageType.MOUSE_MOVE)) {
            MouseMoveMessage mmMessage = new MouseMoveMessage();
            mmMessage.x = bytes.readDouble();
            mmMessage.y = bytes.readDouble();
            return mmMessage;
        } else if(messageType.equals(MessageType.MOUSE_SET_POS)) {
            MouseSetPosMessage mpMessage = new MouseSetPosMessage();
            mpMessage.x = bytes.readDouble();
            mpMessage.y = bytes.readDouble();
            return mpMessage;
        } else if(messageType.equals(MessageType.KEYBOARD_KEY)) {
            KeyboardKeyMessage keMessage = new KeyboardKeyMessage();
            keMessage.action = bytes.readInt();
            keMessage.key = bytes.readInt();
            keMessage.mods = bytes.readInt();
            return keMessage;
        } else if(messageType.equals(MessageType.KEYBOARD_CHAR)) {
            KeyboardCharMessage kcMessage = new KeyboardCharMessage();
            kcMessage.codepoint = bytes.readInt();
            kcMessage.mods = bytes.readInt();
            return kcMessage;
        } else {
            throw new RuntimeException("Why did you add something to my enum!?");
        }
    }

}