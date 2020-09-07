package org.cef.browser;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import net.minecraft.client.MinecraftClient;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.queue.ChronicleQueue;
import org.cef.CefClient;
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Unique;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress;

public class TaterwebzBrowser extends CefBrowserOsr {

    private final ChronicleQueue queue;
    private JFrame frame;

    public TaterwebzBrowser(CefClient cefClient, String url, boolean transparent, int width, int height, CefRequestContext cefRequestContext, ChronicleQueue queue) {
        this(cefClient, url, transparent, width, height, cefRequestContext, queue, null, null);
    }

    public TaterwebzBrowser(CefClient cefClient, String url, boolean transparent, int width, int height, CefRequestContext cefRequestContext, ChronicleQueue queue, CefBrowserOsr cefBrowserOsr, Point point) {
        super(cefClient, url, transparent, cefRequestContext, cefBrowserOsr, point);
        //this.jWindow = new JWindow(new JFrame("") {
        //    @Override
        //    public boolean isShowing() {
        //        return true;
        //    }
        //    @Override
        //    public boolean isFocused() {
        //        return true;
        //    }
        //});
        this.frame = new JFrame("");
        this.frame.add(this.canvas_);
        this.frame.setSize(width, height);
        this.frame.setVisible(true);

        this.queue = queue;
    }

    @Override
    public void onPaint(CefBrowser cefBrowser, boolean popup, Rectangle[] rectangles, ByteBuffer byteBuffer, int i, int i1) {
        if(popup) return;
        System.out.println("painting");
        int width = browser_rect_.width;
        int height = browser_rect_.height;
        byteBuffer.rewind();
        queue.acquireAppender().writeBytes(b -> {
            b.writeByte(QueueProtocol.MessageType.IMAGE.header);
            b.writeInt(width);
            b.writeInt(height);
            b.writeBoolean(true);
            UnsafeMemory.UNSAFE.copyMemory(
                    UnsafeMemory.unsafeGetLong(byteBuffer, Jvm.arrayByteBaseOffset()),
                    b.addressForWrite(b.writePosition()),
                    byteBuffer.capacity()
            );
            b.writeSkip(byteBuffer.capacity());
        });
    }

    public void click(double hitX, double hitY) {
        MouseEvent mouseEvent = new MouseEvent(
                canvas_,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                (int)Math.round(hitX * browser_rect_.width),
                (int)Math.round(hitY * browser_rect_.height),
                1,
                false
        );
        sendMouseEvent(mouseEvent);
    }

    public void dispose() {
        frame.dispose();
        queue.close();
    }

}