package org.cef.browser;

import com.jogamp.common.nio.Buffers;
import com.mojang.blaze3d.systems.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class ProjectInceptionCefRenderer {

    private final boolean transparent;
    private final int[] textureId = new int[1];
    private int viewWidth = 0;
    private int viewHeight = 0;
    private float spinX = 0f;
    private float spinY = 0f;
    private Rectangle popupRect = new Rectangle(0, 0, 0, 0);
    private Rectangle originalPopupRect = new Rectangle(0, 0, 0, 0);
    private final boolean useDrawPixels = false;
    private static final FloatBuffer vertices;

    static {
        final float[] vertex_data = {// tu,   tv,     x,     y,    z
                0.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f};
        vertices = BufferUtils.createFloatBuffer(vertex_data.length);
        vertices.put(vertex_data);
    }

    protected ProjectInceptionCefRenderer(boolean transparent) {
        this.transparent = transparent;
    }

    protected boolean isTransparent() {
        return transparent;
    }

    public void initialize() {
        glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);

        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Necessary for non-power-of-2 textures to render correctly.
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        // Create the texture.
        glGenTextures(textureId);
        assert (textureId[0] != 0);

        glBindTexture(GL_TEXTURE_2D, textureId[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
    }

    protected void cleanup() {
        RenderCall cleanupAction = () -> {
            if (textureId[0] != 0) glDeleteTextures(textureId);
            viewWidth = viewHeight = 0;
        };
        if(!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(cleanupAction);
        } else {
            cleanupAction.execute();
        }
    }

    protected void onPopupSize(Rectangle rect) {
        if (rect.width <= 0 || rect.height <= 0) return;
        originalPopupRect = rect;
        popupRect = getPopupRectInWebView(originalPopupRect);
    }

    protected Rectangle getPopupRect() {
        return (Rectangle) popupRect.clone();
    }

    protected Rectangle getPopupRectInWebView(Rectangle originalRect) {
        Rectangle rc = originalRect;
        // if x or y are negative, move them to 0.
        if (rc.x < 0) rc.x = 0;
        if (rc.y < 0) rc.y = 0;
        // if popup goes outside the view, try to reposition origin
        if (rc.x + rc.width > viewWidth) rc.x = viewWidth - rc.width;
        if (rc.y + rc.height > viewHeight) rc.y = viewHeight - rc.height;
        // if x or y became negative, move them to 0 again.
        if (rc.x < 0) rc.x = 0;
        if (rc.y < 0) rc.y = 0;
        return rc;
    }

    protected void clearPopupRects() {
        popupRect.setBounds(0, 0, 0, 0);
        originalPopupRect.setBounds(0, 0, 0, 0);
    }

    protected void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        if (useDrawPixels) {
            glRasterPos2f(-1, 1);
            glPixelZoom(1, -1);
            glDrawPixels(width, height, GL_BGRA, GL_UNSIGNED_BYTE, buffer);
            return;
        }

        assert (textureId[0] != 0);
        glBindTexture(GL_TEXTURE_2D, textureId[0]);

        if (!popup) {
            int old_width = viewWidth;
            int old_height = viewHeight;

            viewWidth = width;
            viewHeight = height;

            glPixelStorei(GL_UNPACK_ROW_LENGTH, viewWidth);

            if (old_width != viewWidth || old_height != viewHeight) {
                // Update/resize the whole texture.
                glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
                glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, viewWidth, viewHeight, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
            } else {
                // Update just the dirty rectangles.
                for (int i = 0; i < dirtyRects.length; ++i) {
                    Rectangle rect = dirtyRects[i];
                    glPixelStorei(GL_UNPACK_SKIP_PIXELS, rect.x);
                    glPixelStorei(GL_UNPACK_SKIP_ROWS, rect.y);
                    glTexSubImage2D(GL_TEXTURE_2D, 0, rect.x, rect.y, rect.width, rect.height, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
                }
            }
        } else if (popup && popupRect.width > 0 && popupRect.height > 0) {
            int skipPixels = 0, x = popupRect.x;
            int skipRows = 0, y = popupRect.y;
            int w = width;
            int h = height;

            // Adjust the popup to fit inside the view.
            if (x < 0) {
                skipPixels = -x;
                x = 0;
            }
            if (y < 0) {
                skipRows = -y;
                y = 0;
            }
            if (x + w > viewWidth) w -= x + w - viewWidth;
            if (y + h > viewHeight) h -= y + h - viewHeight;

            // Update the popup rectangle.
            glPixelStorei(GL_UNPACK_ROW_LENGTH, width);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, skipPixels);
            glPixelStorei(GL_UNPACK_SKIP_ROWS, skipRows);
            glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, w, h, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
        }
    }

    protected void setSpin(float spinX, float spinY) {
        this.spinX = spinX;
        this.spinY = spinY;
    }

    protected void incrementSpin(float spinDX, float spinDY) {
        spinX -= spinDX;
        spinY -= spinDY;
    }

    public int getTextureId() {
        return textureId[0];
    }

}