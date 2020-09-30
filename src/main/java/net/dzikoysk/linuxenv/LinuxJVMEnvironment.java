package net.dzikoysk.linuxenv;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class LinuxJVMEnvironment {

    private interface LibC extends Library {
        int setenv(String name, String value, int overwrite);
        int unsetenv(String name);
        String getenv(String name);
    }

    private static LibC LIBC = Native.loadLibrary("c", LibC.class);

    public int unsetJVMEnvironmentVariable(String name) {
        return LIBC.unsetenv(name);
    }

    public int setJVMEnvironmentVariable(String name, String value, int overwrite) {
        return LIBC.setenv(name, value, overwrite);
    }

    public String getJVMEnvironmentVariable(String name) {
        return LIBC.getenv(name);
    }

}
