package org.cef.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import org.cef.CefClient;
import org.lwjgl.BufferUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.nio.ByteBuffer;

public class ProjectInceptionBrowser extends CefBrowserOsr {

    private JFrame frame;
    private ProjectInceptionCefRenderer renderer;
    private ByteBuffer lwjglByteBuffer;

    public ProjectInceptionBrowser(CefClient cefClient, String url, boolean transparent, int width, int height, CefRequestContext cefRequestContext) {
        this(cefClient, url, transparent, width, height, cefRequestContext, null, null);
    }

    public ProjectInceptionBrowser(CefClient cefClient, String url, boolean transparent, int width, int height, CefRequestContext cefRequestContext, CefBrowserOsr cefBrowserOsr, Point point) {
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
        this.renderer = new ProjectInceptionCefRenderer(transparent);
        this.renderer.initialize();
        SwingUtilities.invokeLater(() -> {
            this.frame = new JFrame("");
            this.frame.add(this.canvas_);
            this.frame.setSize(width, height);
            this.frame.setVisible(true);
        });
    }

    public int getTexture() {
        return renderer.getTextureId();
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        // This buffer will not persist long enough
        // before we can run on the render thread.
        // We MUST clone the buffer here.
        buffer.rewind();
        if(lwjglByteBuffer == null || buffer.capacity() != lwjglByteBuffer.capacity()) {
            lwjglByteBuffer = BufferUtils.createByteBuffer(buffer.capacity());
            lwjglByteBuffer.put(buffer);
            lwjglByteBuffer.rewind();
            buffer.rewind();
        } else {
            lwjglByteBuffer.rewind();
            lwjglByteBuffer.put(buffer);
            lwjglByteBuffer.rewind();
        }

        RenderSystem.recordRenderCall(() -> {
            System.out.println("here");
            renderer.onPaint(popup, dirtyRects, lwjglByteBuffer, width, height);
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
        this.frame.dispose();
        renderer.cleanup();
    }

}