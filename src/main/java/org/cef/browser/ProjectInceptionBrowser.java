package org.cef.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.nio.ByteBuffer;

public class ProjectInceptionBrowser extends CefBrowser_N implements CefRenderHandler {

    private ProjectInceptionCefRenderer renderer;
    private Framebuffer framebuffer;
    private final Rectangle browserRect = new Rectangle(0, 0, 1, 1); // Work around CEF issue #1437.
    private final Point screenPoint = new Point(0, 0);
    private final boolean isTransparent;

    public ProjectInceptionBrowser(CefClient client, String url, boolean transparent, CefRequestContext context, int width, int height) {
        this(client, url, transparent, context, width, height, null, null);
    }

    private ProjectInceptionBrowser(CefClient client, String url, boolean transparent, CefRequestContext context, int width, int height, ProjectInceptionBrowser parent, Point inspectAt) {
        super(client, url, context, parent, inspectAt);
        isTransparent = transparent;
        renderer = new ProjectInceptionCefRenderer(transparent);
        createGLCanvas();
        browserRect.setSize(width, height);
        wasResized(width, height);
    }

    @Override
    public Component getUIComponent() {
        return null;
    }

    public int getTexture() {
        return framebuffer.method_30277();
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    @Override
    public CefBrowser_N createDevToolsBrowser(CefClient client, String url, CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        return new ProjectInceptionBrowser(client, url, isTransparent, context, 1, 1, this, inspectAt);
    }

    @SuppressWarnings("serial")
    private void createGLCanvas() {
        RenderSystem.assertThread(RenderSystem::isOnRenderThread);
        framebuffer = new Framebuffer(browserRect.width, browserRect.height, true, false);
        createBrowserIfRequired(getParentBrowser() != null);
        framebuffer.beginWrite(false);
        renderer.initialize();
        framebuffer.endWrite();
    }

    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return browserRect;
    }

    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        Point screenPoint = new Point(this.screenPoint);
        screenPoint.translate(viewPoint.x, viewPoint.y);
        return screenPoint;
    }

    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        if (!show) {
            renderer.clearPopupRects();
            invalidate();
        }
    }

    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        renderer.onPopupSize(size);
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        framebuffer.beginWrite(false);
        renderer.onPaint(popup, dirtyRects, buffer, width, height);
        renderer.render();
        framebuffer.endWrite();
    }

    @Override
    public void onCursorChange(CefBrowser browser, final int cursorType) {

    }

    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        return false;
    }

    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {

    }

    public void click(double hitX, double hitY) {
        MouseEvent mouseEvent = new MouseEvent(
                null,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                (int)Math.round(hitX * browserRect.width),
                (int)Math.round(hitY * browserRect.height),
                1,
                false
        );
        sendMouseEvent(mouseEvent);
    }

    private void createBrowserIfRequired(boolean hasParent) {
        long windowHandle = 0;
        if (hasParent) {
            windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
        }

        if (getNativeRef("CefBrowser") == 0) {
            if (getParentBrowser() != null) {
                createDevTools(getParentBrowser(), getClient(), windowHandle, isTransparent, null, getInspectAt());
            } else {
                createBrowser(getClient(), windowHandle, getUrl(), isTransparent, null, getRequestContext());
            }
        } else {
            // OSR windows cannot be reparented after creation.
            setFocus(true);
        }
    }

    public void dispose() {
        renderer.cleanup();
    }

}