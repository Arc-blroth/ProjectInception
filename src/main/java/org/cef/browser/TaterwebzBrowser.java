package org.cef.browser;

import ai.arcblroth.projectInception.ProjectInception;
import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import ai.arcblroth.taterwebz.TaterwebzPandomium;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.queue.ChronicleQueue;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import sun.nio.ch.DirectBuffer;

import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.*;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.ByteBuffer;

public class TaterwebzBrowser extends CefBrowser_N implements CefRenderHandler {

    public GLAutoDrawable canvas_;
    public long window_handle_;
    public Rectangle browser_rect_;
    public Point screenPoint_;
    public boolean isTransparent_;
    public Canvas fauxComponent;
    private boolean isInitYet = false;

    private final ChronicleQueue queue;

    public TaterwebzBrowser(CefClient cefClient, String url, boolean transparent, int width, int height, CefRequestContext cefRequestContext, ChronicleQueue queue) {
        this(cefClient, url, transparent, width, height, cefRequestContext, queue, null, null);
    }

    public TaterwebzBrowser(CefClient cefClient, String url, boolean transparent, int width, int height, CefRequestContext cefRequestContext, ChronicleQueue queue, CefBrowserOsr cefBrowserOsr, Point point) {
        super(cefClient, url, cefRequestContext, cefBrowserOsr, point);
        this.window_handle_ = 0L;
        this.browser_rect_ = new Rectangle(0, 0, width, height);
        this.screenPoint_ = new Point(0, 0);
        this.isTransparent_ = transparent;
        createGLCanvas();

        if (getParentBrowser() == null) {
            createBrowser(getClient(), getWindowHandle(), getUrl(), isTransparent_, null, getRequestContext());
        }
        browser_rect_.setBounds(0, 0, width, height);

        this.queue = queue;
    }

    private void createGLCanvas() {
        GLProfile profile = GLProfile.getMaxFixedFunc(true);
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setOnscreen(false);
        capabilities.setHardwareAccelerated(true);

        GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
        this.canvas_ = factory.createOffscreenAutoDrawable(factory.getDefaultDevice(), capabilities, new DefaultGLCapabilitiesChooser(), this.browser_rect_.width, this.browser_rect_.height);

        this.fauxComponent = new Canvas() {
            @Override
            public boolean isShowing() {
                return true;
            }

            @Override
            public boolean isVisible() {
                return true;
            }

            @Override
            public boolean isDisplayable() {
                return true;
            }

            @Override
            public boolean isFocusable() {
                return true;
            }

            @Override
            public Rectangle getBounds() {
                return browser_rect_;
            }
        };
    }

    private synchronized long getWindowHandle() {
        if (this.window_handle_ == 0L) {
            NativeSurface var1 = this.canvas_.getNativeSurface();
            if (var1 != null) {
                var1.lockSurface();
                this.window_handle_ = this.getWindowHandle(var1.getSurfaceHandle());
                var1.unlockSurface();
            }
        }
        return this.window_handle_;
    }

    public CefBrowser_N createDevToolsBrowser(CefClient var1, String var2, CefRequestContext var3, CefBrowser_N var4, Point var5) {
        return new TaterwebzBrowser(var1, var2, this.isTransparent_, this.browser_rect_.width, this.browser_rect_.height, var3, this.queue);
    }

    public CefRenderHandler getRenderHandler() {
        return this;
    }

    public void render() {
        if(!isInitYet) {
            if(N_CefHandle != 0) {
                //ProjectInception.LOGGER.de("init render()");
                wasResized(browser_rect_.width, browser_rect_.height);
            }
            isInitYet = true;
        }
    }

    @Override
    public void onPaint(CefBrowser cefBrowser, boolean popup, Rectangle[] rectangles, ByteBuffer byteBuffer, int width, int height) {
        if(popup) return;
        //ProjectInception.LOGGER.info("painting");
        byteBuffer.rewind();
        queue.acquireAppender().writeBytes(b -> {
            b.writeByte(QueueProtocol.MessageType.IMAGE.header);
            b.writeInt(width);
            b.writeInt(height);
            b.writeBoolean(true);
            // This image is BGRA, but we need RGBA
            for(int i = 0; i < byteBuffer.capacity() / 4; i++) {
                b.writeByte(byteBuffer.get(i * 4 + 2));
                b.writeByte(byteBuffer.get(i * 4 + 1));
                b.writeByte(byteBuffer.get(i * 4));
                b.writeByte(byteBuffer.get(i * 4 + 3));
            }
        });
    }

    @Override
    public Rectangle getViewRect(CefBrowser var1) {
        return this.browser_rect_;
    }

    @Override
    public Point getScreenPoint(CefBrowser var1, Point var2) {
        Point var3 = new Point(this.screenPoint_);
        var3.translate(var2.x, var2.y);
        return var3;
    }

    @Override
    public void onPopupShow(CefBrowser var1, boolean var2) {}

    @Override
    public void onPopupSize(CefBrowser var1, Rectangle var2) {}

    @Override
    public void onCursorChange(CefBrowser var1, final int var2) {}

    @Override
    public boolean startDragging(CefBrowser var1, CefDragData var2, int var3, int var4, int var5) {
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser var1, int var2) {}

    @Override
    public Component getUIComponent() {
        return this.fauxComponent;
    }

    public void click(double hitX, double hitY) {
        //MouseEvent mouseEvent = new MouseEvent(
        //        canvas_,
        //        MouseEvent.MOUSE_CLICKED,
        //        System.currentTimeMillis(),
        //        0,
        //        (int)Math.round(hitX * browser_rect_.width),
        //        (int)Math.round(hitY * browser_rect_.height),
        //        1,
        //        false
        //);
        //sendMouseEvent(mouseEvent);
    }

    public void dispose() {
        canvas_.destroy();
        queue.close();
    }

}