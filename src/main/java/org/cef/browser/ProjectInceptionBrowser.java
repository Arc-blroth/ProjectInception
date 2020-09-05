package org.cef.browser;

import org.cef.CefClient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class ProjectInceptionBrowser extends CefBrowserOsr {

    private JFrame frame;

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
        this.frame = new JFrame("");
        this.frame.add(this.canvas_);
        this.frame.setSize(width, height);
        this.frame.setVisible(true);
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

}