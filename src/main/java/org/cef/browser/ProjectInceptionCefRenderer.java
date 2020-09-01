package org.cef.browser;

import com.mojang.blaze3d.systems.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;

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

    protected ProjectInceptionCefRenderer(boolean transparent) {
        this.transparent = transparent;
    }

    protected boolean isTransparent() {
        return transparent;
    }

    protected void initialize() {
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

    public void render() {
        if (useDrawPixels || viewWidth == 0 || viewHeight == 0) return;

        final float[] vertex_data = {// tu,   tv,     x,     y,    z
                0.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 1.0f, -1.0f, 0.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 1.0f, 0.0f};
        FloatBuffer vertices = FloatBuffer.wrap(vertex_data);

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Match GL units to screen coordinates.
        glViewport(0, 0, viewWidth, viewHeight);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        // Draw the background gradient.
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glBegin(GL_QUADS);
        glColor4f(1.0f, 0.0f, 0.0f, 1.0f); // red
        glVertex2f(-1.0f, -1.0f);
        glVertex2f(1.0f, -1.0f);
        glColor4f(0.0f, 0.0f, 1.0f, 1.0f); // blue
        glVertex2f(1.0f, 1.0f);
        glVertex2f(-1.0f, 1.0f);
        glEnd();
        glPopAttrib();

        // Rotate the view based on the mouse spin.
        if (spinX != 0) glRotatef(-spinX, 1.0f, 0.0f, 0.0f);
        if (spinY != 0) glRotatef(-spinY, 0.0f, 1.0f, 0.0f);

        if (transparent) {
            // Alpha blending style. Texture values have premultiplied alpha.
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

            // Enable alpha blending.
            glEnable(GL_BLEND);
        }

        // Enable 2D textures.
        glEnable(GL_TEXTURE_2D);

        // Draw the facets with the texture.
        assert (textureId[0] != 0);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glBindTexture(GL_TEXTURE_2D, textureId[0]);
        glInterleavedArrays(GL_T2F_V3F, 0, vertices);
        glDrawArrays(GL_QUADS, 0, 4);

        // Disable 2D textures.
        glDisable(GL_TEXTURE_2D);

        if (transparent) {
            // Disable alpha blending.
            glDisable(GL_BLEND);
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
        initialize();

        if (useDrawPixels) {
            glRasterPos2f(-1, 1);
            glPixelZoom(1, -1);
            glDrawPixels(width, height, GL_BGRA, GL_UNSIGNED_BYTE, buffer);
            return;
        }

        if (transparent) {
            // Enable alpha blending.
            glEnable(GL_BLEND);
        }

        // Enable 2D textures.
        glEnable(GL_TEXTURE_2D);

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

        // Disable 2D textures.
        glDisable(GL_TEXTURE_2D);

        if (transparent) {
            // Disable alpha blending.
            glDisable(GL_BLEND);
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
    
}