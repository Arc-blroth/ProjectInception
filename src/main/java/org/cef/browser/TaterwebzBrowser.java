package org.cef.browser;

import ai.arcblroth.projectInception.client.mc.QueueProtocol;
import ai.arcblroth.taterwebz.util.GLFW2SwingKeyHandler;
import com.google.common.collect.Lists;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.panda_lang.pandomium.util.os.PandomiumOS;

import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static ai.arcblroth.projectInception.client.mc.QueueProtocol.*;
import static javax.media.opengl.GL2GL3.*;

public class TaterwebzBrowser extends CefBrowser_N implements CefRenderHandler {

    private static final Field setScancode;

    static {
        if(PandomiumOS.isWindows()) {
            try {
                setScancode = KeyEvent.class.getDeclaredField("scancode");
                setScancode.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            setScancode = null;
        }
    }

    public GLAutoDrawable canvas_;
    public CefRenderer renderer_;
    public long window_handle_;
    public Rectangle browser_rect_;
    public Point screenPoint_;
    public boolean isTransparent_;
    public Component fauxComponent;
    private boolean isInitYet = false;

    private static ByteBuffer rendererOut;

    private final ChronicleQueue queue;
    private final ExcerptTailer tailer;

    private double lastMouseX = 0.5;
    private double lastMouseY = 0.5;
    private boolean isDragging = false;
    private boolean isDragStart = false;
    private double dragStartX = 0.5;
    private double dragStartY = 0.5;
    private int lastModifiers = 0;

    private String lastURL;

    public TaterwebzBrowser(CefClient cefClient, String url, boolean transparent, int width, int height, CefRequestContext cefRequestContext, ChronicleQueue queue) {
        this(cefClient, url, transparent, width, height, cefRequestContext, queue, null, null);
    }

    public TaterwebzBrowser(CefClient cefClient, String url, boolean transparent, int width, int height, CefRequestContext cefRequestContext, ChronicleQueue queue, CefBrowserOsr cefBrowserOsr, Point point) {
        super(cefClient, url, cefRequestContext, cefBrowserOsr, point);
        this.lastURL = url_;
        this.window_handle_ = 0L;
        this.browser_rect_ = new Rectangle(0, 0, width, height);
        this.screenPoint_ = new Point(0, 0);
        this.isTransparent_ = transparent;
        this.renderer_ = new CefRenderer(isTransparent_);
        createGLCanvas();

        if (getParentBrowser() == null) {
            createBrowser(getClient(), getWindowHandle(), getUrl(), isTransparent_, null, getRequestContext());
        }
        browser_rect_.setBounds(0, 0, width, height);

        this.queue = queue;
        this.tailer = this.queue.createTailer();
    }

    private void createGLCanvas() {
        GLProfile profile = GLProfile.getMaxFixedFunc(true);
        GLCapabilities capabilities = new GLCapabilities(profile);
        capabilities.setOnscreen(false);
        capabilities.setHardwareAccelerated(true);

        GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
        this.canvas_ = factory.createOffscreenAutoDrawable(factory.getDefaultDevice(), capabilities, new DefaultGLCapabilitiesChooser(), this.browser_rect_.width, this.browser_rect_.height);
        this.canvas_.display();
        
        this.fauxComponent = new Component() {
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

    public void handleEvents() {
        while(true) {
            try(DocumentContext dc = tailer.readingDocument()) {
                if(dc.isPresent()) {
                    Bytes<?> bytes = dc.wire().bytes();
                    Message message = readParent2ChildMessage(bytes);
                    if(message instanceof SetPageMessage) {
                        SetPageMessage spMessage = (SetPageMessage) message;
                        switch (spMessage.action) {
                            case SetPageMessage.ACTION_RELOAD: {
                                reload();
                                break;
                            }
                            case SetPageMessage.ACTION_GOTO: {
                                loadURL(spMessage.url);
                                break;
                            }
                            case SetPageMessage.ACTION_BACK: {
                                if(canGoBack()) goBack();
                                break;
                            }
                            case SetPageMessage.ACTION_FORWARD: {
                                if(canGoForward()) goForward();
                                break;
                            }
                        }
                    } else if (message instanceof MouseButtonMessage) {
                        MouseButtonMessage mbMessage = (MouseButtonMessage) message;
                        sendMouseEvent(buildMouseEvent(mbMessage.button, mbMessage.message, mbMessage.mods, lastMouseX, lastMouseY));
                        if(!isDragging && mbMessage.message == GLFW.GLFW_PRESS) {
                            isDragStart = true;
                        }
                        if(mbMessage.message == GLFW.GLFW_RELEASE) {
                            isDragStart = false;
                            isDragging = false;
                        }
                    } else if (message instanceof MouseScrollMessage) {
                        MouseScrollMessage msMessage = (MouseScrollMessage) message;
                        sendMouseWheelEvent(buildScrollEvent(msMessage.horizontal, msMessage.vertical));
                    } else if (message instanceof MouseMoveMessage) {
                        MouseMoveMessage mmMessage = (MouseMoveMessage) message;
                        if(!isDragging && isDragStart) {
                            dragStartX = lastMouseX;
                            dragStartY = lastMouseY;
                            isDragging = true;
                        }
                        sendMouseEvent(buildMoveEvent(mmMessage.x, mmMessage.y, isDragging));
                    } else if (message instanceof MouseSetPosMessage) {
                        MouseSetPosMessage mpMessage = (MouseSetPosMessage) message;
                        isDragStart = false;
                        isDragging = false;
                        sendMouseEvent(buildMoveEvent(mpMessage.x, mpMessage.y, false));
                    } else if (message instanceof KeyboardKeyMessage) {
                        KeyboardKeyMessage keMessage = (KeyboardKeyMessage) message;
                        sendKeyEvent(buildKeyEvent(keMessage.key, keMessage.scancode, keMessage.action, keMessage.mods));
                    } else if (message instanceof KeyboardCharMessage) {
                        KeyboardCharMessage kcMessage = (KeyboardCharMessage) message;
                        sendKeyEvent(buildCharEvent(kcMessage.codepoint, kcMessage.mods));
                    }
                } else {
                    dc.rollbackOnClose();
                    break;
                }
            }
        }
        if(!getMainFrame().getURL().equals(lastURL)) {
            lastURL = getMainFrame().getURL();
            SetPageMessage spMessage = new SetPageMessage();
            spMessage.action = SetPageMessage.ACTION_GOTO;
            spMessage.url = lastURL;
            QueueProtocol.writeParent2ChildMessage(spMessage, queue.acquireAppender());
        }
    }

    private int processKeyModifiers(int mods, int swingModifiers) {
        if((mods & GLFW.GLFW_MOD_SHIFT) == GLFW.GLFW_MOD_SHIFT) {
            swingModifiers |= InputEvent.SHIFT_DOWN_MASK;
        }
        if((mods & GLFW.GLFW_MOD_CONTROL) == GLFW.GLFW_MOD_CONTROL) {
            swingModifiers |= InputEvent.CTRL_DOWN_MASK;
        }
        if((mods & GLFW.GLFW_MOD_ALT) == GLFW.GLFW_MOD_ALT) {
            swingModifiers |= InputEvent.ALT_DOWN_MASK;
        }
        if((mods & GLFW.GLFW_MOD_SUPER) == GLFW.GLFW_MOD_SUPER) {
            swingModifiers |= InputEvent.META_DOWN_MASK;
        }
        return swingModifiers;
    }

    private MouseEvent buildMouseEvent(int button, int message, int mods, double hitX, double hitY) {
        int swingType = 0;
        if(message == GLFW.GLFW_PRESS) {
            swingType = MouseEvent.MOUSE_PRESSED;
        } else if(message == GLFW.GLFW_RELEASE) {
            swingType = MouseEvent.MOUSE_RELEASED;
        } // JCEF doesn't handle MOUSE_CLICKED which is a yay moment

        int swingModifiers = 0;
        int swingButton = 0;
        if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            swingModifiers |= MouseEvent.BUTTON1_DOWN_MASK;
            swingButton = MouseEvent.BUTTON1;
        } else if(button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            swingModifiers |= MouseEvent.BUTTON2_DOWN_MASK;
            swingButton = MouseEvent.BUTTON2;
        } else if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            swingModifiers |= MouseEvent.BUTTON3_DOWN_MASK;
            swingButton = MouseEvent.BUTTON3;
        }
        swingModifiers = processKeyModifiers(mods, swingModifiers);
        this.lastModifiers = swingModifiers;
        this.lastMouseX = hitX;
        this.lastMouseY = hitY;

        return new MouseEvent(
                fauxComponent,
                swingType,
                System.currentTimeMillis(),
                swingModifiers,
                (int)Math.round(hitX * browser_rect_.width),
                (int)Math.round(hitY * browser_rect_.height),
                1,
                false,
                swingButton
        );
    }

    private MouseWheelEvent buildScrollEvent(double horizontal, double vertical) {
        return new MouseWheelEvent(
                fauxComponent,
                MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                lastModifiers,
                (int)Math.round(lastMouseX * browser_rect_.width),
                (int)Math.round(lastMouseY * browser_rect_.height),
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                (int)vertical,
                10
        );
    }

    private MouseEvent buildMoveEvent(double x, double y, boolean isDragging) {
        this.lastMouseX = x;
        this.lastMouseY = y;
        return new MouseEvent(
                fauxComponent,
                isDragging ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                this.lastModifiers,
                (int)Math.round(x * browser_rect_.width),
                (int)Math.round(y * browser_rect_.height),
                isDragging ? 0 : 1,
                false
        );
    }

    private KeyEvent buildKeyEvent(int key, int scancode, int action, int mods) {
        int swingType = 0;
        if(action == GLFW.GLFW_PRESS) {
            swingType = KeyEvent.KEY_PRESSED;
        } else if(action == GLFW.GLFW_RELEASE) {
            swingType = KeyEvent.KEY_RELEASED;
        } else if(action == GLFW.GLFW_REPEAT) {
            swingType = KeyEvent.KEY_PRESSED;
        }
        int swingModifiers = processKeyModifiers(mods, 0);
        int vkCode = GLFW2SwingKeyHandler.toVk(key);
        char c = GLFW2SwingKeyHandler.convertWeirdChars(vkCode);
        KeyEvent event = new KeyEvent(
                fauxComponent,
                swingType,
                System.currentTimeMillis(),
                swingModifiers,
                vkCode,
                c,
                KeyEvent.KEY_LOCATION_STANDARD
        );
        if(PandomiumOS.isWindows()) {
            try {
                setScancode.set(event, scancode);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return event;
    }

    private KeyEvent buildCharEvent(int codepoint, int mods) {
        int swingModifiers = processKeyModifiers(mods, 0);
        return new KeyEvent(
                fauxComponent,
                KeyEvent.KEY_TYPED,
                System.currentTimeMillis(),
                swingModifiers,
                KeyEvent.VK_UNDEFINED,
                (char) codepoint,
                KeyEvent.KEY_LOCATION_UNKNOWN
        );
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
        //ProjectInception.LOGGER.info("painting");
        GLContext context = this.canvas_.getContext();
        context.makeCurrent();
        this.renderer_.onPaint(this.canvas_.getGL().getGL2(), popup, rectangles, byteBuffer, width, height);

        if (rendererOut == null || rendererOut.capacity() < browser_rect_.width * browser_rect_.height * 4) {
            rendererOut = BufferUtils.createByteBuffer(width * height * 4);
        }
        context.getGL().glBindFramebuffer(GL_READ_FRAMEBUFFER, GL_FRONT_LEFT);
        context.getGL().glReadPixels(0, 0, browser_rect_.width, browser_rect_.height, GL_RGBA, GL_UNSIGNED_BYTE, rendererOut);

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
        context.release();
    }

    @Override
    public void setFocus(boolean b) {
        // For reasons beyond our understanding, JCEF
        // will call CefClient.onGetFocus in native
        // code, leading to a stack overflow
        // see https://discordapp.com/channels/507304429255393322/608088354042544139/754429396827242557
        Throwable stackTraceGetter = new Throwable();
        ArrayList<StackTraceElement> stackTrace = Lists.newArrayList(stackTraceGetter.getStackTrace());
        if(stackTrace.stream()
                .filter(ste -> ste.getClassName().equals(TaterwebzBrowser.class.getName()) && ste.getMethodName().equals("setFocus"))
                .count() < 2) {
            super.setFocus(b);
        }
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

    public ChronicleQueue getQueue() {
        return queue;
    }

    public void dispose() {
        try {
            canvas_.destroy();
            queue.close();
        } catch (Exception ignored) {}
    }

}