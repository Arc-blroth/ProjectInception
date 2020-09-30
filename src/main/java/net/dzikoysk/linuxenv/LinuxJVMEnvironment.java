package net.dzikoysk.linuxenv;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class LinuxJVMEnvironment {
    
    static {
        System.out.println("here");
        System.setProperty("jna.boot.library.name", "jnidispatch");
        System.setProperty("jna.nosys", "true");
    }

    public interface LibC extends Library {
        int setenv(String name, String value, int overwrite);
        int unsetenv(String name);
        String getenv(String name);
        
        public static LibC INSTANCE = Native.loadLibrary("c", LibC.class);
    }

    public int unsetJVMEnvironmentVariable(String name) {
        return LibC.INSTANCE.unsetenv(name);
    }

    public int setJVMEnvironmentVariable(String name, String value, int overwrite) {
        return LibC.INSTANCE.setenv(name, value, overwrite);
    }

    public String getJVMEnvironmentVariable(String name) {
        return LibC.INSTANCE.getenv(name);
    }

}
