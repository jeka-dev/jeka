package org.jerkar.api.utils;

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

    /**
     * Returns the version of the running JDK.
     */
    public static String runningJavaVersion() {
        final String fullVersion = System.getProperty("java.version");
        final int index = fullVersion.lastIndexOf(".");
        final String candidate = fullVersion.substring(0, index);
        final String plainVersion = JkUtilsString.substringAfterLast(candidate, ".");
        final int version = Integer.parseInt(plainVersion);
        if (version >= 7) {
            return plainVersion;
        }
        return fullVersion.substring(0, index);
    }

}
