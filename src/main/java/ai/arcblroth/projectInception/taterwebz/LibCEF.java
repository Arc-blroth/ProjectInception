package ai.arcblroth.projectInception.taterwebz;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.File;

public interface LibCEF extends Library {

    LibCEF[] INSTANCE_HOLDER = new LibCEF[1];

    static void init(File libraryPath) {
        System.setProperty("jna.library.path", System.getProperty("jna.library.path") + File.pathSeparator + libraryPath.getAbsolutePath());
        if(INSTANCE_HOLDER[0] == null) {
            INSTANCE_HOLDER[0] = Native.loadLibrary("libcef", LibCEF.class);
        }
    }

    static LibCEF getInstance() {
        return INSTANCE_HOLDER[0];
    }

}