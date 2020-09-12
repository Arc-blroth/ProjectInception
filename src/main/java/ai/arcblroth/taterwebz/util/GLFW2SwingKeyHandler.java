package ai.arcblroth.taterwebz.util;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.awt.event.KeyEvent.*;
import static org.lwjgl.glfw.GLFW.*;

// This class is so cursed
public class GLFW2SwingKeyHandler {

    private static final SortedMap<Integer, Integer> GLFW_TO_VK_MAP;

    static {
        TreeMap<Integer, Integer> temp = new TreeMap<>();
        temp.put(GLFW_KEY_UNKNOWN      , VK_UNDEFINED);
        temp.put(GLFW_KEY_SPACE        , VK_SPACE);
        temp.put(GLFW_KEY_APOSTROPHE   , VK_QUOTE);
        temp.put(GLFW_KEY_COMMA        , VK_COMMA);
        temp.put(GLFW_KEY_MINUS        , VK_MINUS);
        temp.put(GLFW_KEY_PERIOD       , VK_PERIOD);
        temp.put(GLFW_KEY_SLASH        , VK_SLASH);
        temp.put(GLFW_KEY_0            , VK_0);
        temp.put(GLFW_KEY_1            , VK_1);
        temp.put(GLFW_KEY_2            , VK_2);
        temp.put(GLFW_KEY_3            , VK_3);
        temp.put(GLFW_KEY_4            , VK_4);
        temp.put(GLFW_KEY_5            , VK_5);
        temp.put(GLFW_KEY_6            , VK_6);
        temp.put(GLFW_KEY_7            , VK_7);
        temp.put(GLFW_KEY_8            , VK_8);
        temp.put(GLFW_KEY_9            , VK_9);
        temp.put(GLFW_KEY_SEMICOLON    , VK_SEMICOLON);
        temp.put(GLFW_KEY_EQUAL        , VK_EQUALS);
        temp.put(GLFW_KEY_A            , VK_A);
        temp.put(GLFW_KEY_B            , VK_B);
        temp.put(GLFW_KEY_C            , VK_C);
        temp.put(GLFW_KEY_D            , VK_D);
        temp.put(GLFW_KEY_E            , VK_E);
        temp.put(GLFW_KEY_F            , VK_F);
        temp.put(GLFW_KEY_G            , VK_G);
        temp.put(GLFW_KEY_H            , VK_H);
        temp.put(GLFW_KEY_I            , VK_I);
        temp.put(GLFW_KEY_J            , VK_J);
        temp.put(GLFW_KEY_K            , VK_K);
        temp.put(GLFW_KEY_L            , VK_L);
        temp.put(GLFW_KEY_M            , VK_M);
        temp.put(GLFW_KEY_N            , VK_N);
        temp.put(GLFW_KEY_O            , VK_O);
        temp.put(GLFW_KEY_P            , VK_P);
        temp.put(GLFW_KEY_Q            , VK_Q);
        temp.put(GLFW_KEY_R            , VK_R);
        temp.put(GLFW_KEY_S            , VK_S);
        temp.put(GLFW_KEY_T            , VK_T);
        temp.put(GLFW_KEY_U            , VK_U);
        temp.put(GLFW_KEY_V            , VK_V);
        temp.put(GLFW_KEY_W            , VK_W);
        temp.put(GLFW_KEY_X            , VK_X);
        temp.put(GLFW_KEY_Y            , VK_Y);
        temp.put(GLFW_KEY_Z            , VK_Z);
        temp.put(GLFW_KEY_LEFT_BRACKET , VK_OPEN_BRACKET);
        temp.put(GLFW_KEY_BACKSLASH    , VK_BACK_SLASH);
        temp.put(GLFW_KEY_RIGHT_BRACKET, VK_CLOSE_BRACKET);
        temp.put(GLFW_KEY_GRAVE_ACCENT , VK_DEAD_GRAVE);
        temp.put(GLFW_KEY_ESCAPE       , VK_ESCAPE);
        temp.put(GLFW_KEY_ENTER        , VK_ENTER);
        temp.put(GLFW_KEY_TAB          , VK_TAB);
        temp.put(GLFW_KEY_BACKSPACE    , VK_BACK_SPACE);
        temp.put(GLFW_KEY_INSERT       , VK_INSERT);
        temp.put(GLFW_KEY_DELETE       , VK_DELETE);
        temp.put(GLFW_KEY_RIGHT        , VK_RIGHT);
        temp.put(GLFW_KEY_LEFT         , VK_LEFT);
        temp.put(GLFW_KEY_DOWN         , VK_DOWN);
        temp.put(GLFW_KEY_UP           , VK_UP);
        temp.put(GLFW_KEY_PAGE_UP      , VK_PAGE_UP);
        temp.put(GLFW_KEY_PAGE_DOWN    , VK_PAGE_DOWN);
        temp.put(GLFW_KEY_HOME         , VK_HOME);
        temp.put(GLFW_KEY_END          , VK_END);
        temp.put(GLFW_KEY_CAPS_LOCK    , VK_CAPS_LOCK);
        temp.put(GLFW_KEY_SCROLL_LOCK  , VK_SCROLL_LOCK);
        temp.put(GLFW_KEY_NUM_LOCK     , VK_NUM_LOCK);
        temp.put(GLFW_KEY_PRINT_SCREEN , VK_PRINTSCREEN);
        temp.put(GLFW_KEY_PAUSE        , VK_PAUSE);
        temp.put(GLFW_KEY_F1           , VK_F1);
        temp.put(GLFW_KEY_F2           , VK_F2);
        temp.put(GLFW_KEY_F3           , VK_F3);
        temp.put(GLFW_KEY_F4           , VK_F4);
        temp.put(GLFW_KEY_F5           , VK_F5);
        temp.put(GLFW_KEY_F6           , VK_F6);
        temp.put(GLFW_KEY_F7           , VK_F7);
        temp.put(GLFW_KEY_F8           , VK_F8);
        temp.put(GLFW_KEY_F9           , VK_F9);
        temp.put(GLFW_KEY_F10          , VK_F10);
        temp.put(GLFW_KEY_F11          , VK_F11);
        temp.put(GLFW_KEY_F12          , VK_F12);
        temp.put(GLFW_KEY_F13          , VK_F13);
        temp.put(GLFW_KEY_F14          , VK_F14);
        temp.put(GLFW_KEY_F15          , VK_F15);
        temp.put(GLFW_KEY_F16          , VK_F16);
        temp.put(GLFW_KEY_F17          , VK_F17);
        temp.put(GLFW_KEY_F18          , VK_F18);
        temp.put(GLFW_KEY_F19          , VK_F19);
        temp.put(GLFW_KEY_F20          , VK_F20);
        temp.put(GLFW_KEY_F21          , VK_F21);
        temp.put(GLFW_KEY_F22          , VK_F22);
        temp.put(GLFW_KEY_F23          , VK_F23);
        temp.put(GLFW_KEY_F24          , VK_F24);
        temp.put(GLFW_KEY_F25          , VK_UNDEFINED); //sadness
        temp.put(GLFW_KEY_KP_0         , VK_NUMPAD0);
        temp.put(GLFW_KEY_KP_1         , VK_NUMPAD1);
        temp.put(GLFW_KEY_KP_2         , VK_NUMPAD2);
        temp.put(GLFW_KEY_KP_3         , VK_NUMPAD3);
        temp.put(GLFW_KEY_KP_4         , VK_NUMPAD4);
        temp.put(GLFW_KEY_KP_5         , VK_NUMPAD5);
        temp.put(GLFW_KEY_KP_6         , VK_NUMPAD6);
        temp.put(GLFW_KEY_KP_7         , VK_NUMPAD7);
        temp.put(GLFW_KEY_KP_8         , VK_NUMPAD8);
        temp.put(GLFW_KEY_KP_9         , VK_NUMPAD9);
        temp.put(GLFW_KEY_KP_DECIMAL   , VK_DECIMAL);
        temp.put(GLFW_KEY_KP_DIVIDE    , VK_DIVIDE);
        temp.put(GLFW_KEY_KP_MULTIPLY  , VK_MULTIPLY);
        temp.put(GLFW_KEY_KP_SUBTRACT  , VK_SUBTRACT);
        temp.put(GLFW_KEY_KP_ADD       , VK_ADD);
        temp.put(GLFW_KEY_KP_ENTER     , VK_ENTER);
        temp.put(GLFW_KEY_KP_EQUAL     , VK_EQUALS);
        temp.put(GLFW_KEY_LEFT_SHIFT   , VK_SHIFT);
        temp.put(GLFW_KEY_LEFT_CONTROL , VK_CONTROL);
        temp.put(GLFW_KEY_LEFT_ALT     , VK_ALT);
        temp.put(GLFW_KEY_LEFT_SUPER   , VK_META);
        temp.put(GLFW_KEY_RIGHT_SHIFT  , VK_SHIFT);
        temp.put(GLFW_KEY_RIGHT_CONTROL, VK_CONTROL);
        temp.put(GLFW_KEY_RIGHT_ALT    , VK_ALT);
        temp.put(GLFW_KEY_RIGHT_SUPER  , VK_META);
        temp.put(GLFW_KEY_MENU         , VK_CONTEXT_MENU);
        GLFW_TO_VK_MAP = Collections.unmodifiableSortedMap(temp);
    }

    public static int toVk(int code) {
        return GLFW_TO_VK_MAP.getOrDefault(code, VK_UNDEFINED);
    }

    public static char convertWeirdChars(int vkCode) {
        switch (vkCode) {
            case VK_BACK_SPACE: return '\b';
            case VK_ESCAPE    : return '\0';
            case VK_ENTER     : return '\n';
            case VK_TAB       : return '\t';
            default           : return CHAR_UNDEFINED;
        }
    }

}
