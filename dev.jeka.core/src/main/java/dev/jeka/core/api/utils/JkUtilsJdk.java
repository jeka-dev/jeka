package dev.jeka.core.api.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Convenient methods to deal with running JDK.
 */
public final class JkUtilsJdk {

    private JkUtilsJdk() {
        // Can not instantiate
    }

    /**
     * Returns the tool library file of the running JDK.
     */
    public static Path toolsJar() {
        final String jdkLocation = System.getProperty("java.home");
        final Path javaHome = Paths.get(jdkLocation);
        return javaHome.resolve("../lib/tools.jar").normalize().toAbsolutePath();
    }

    public static Path javaHome() {
        final String jdkLocation = System.getProperty("java.home");
        return Paths.get(jdkLocation);
    }

    public static int runningMajorVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) { version = version.substring(0, dot); }
        }
        return Integer.parseInt(version);
    }

}
