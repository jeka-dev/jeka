package org.jerkar.api.utils;

import java.io.File;
import java.io.IOException;

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
    public static File toolsJar() {
        final String jdkLocation = System.getProperty("java.home");
        final File javaHome = new File(jdkLocation);
        try {
            return new File(javaHome, "../lib/tools.jar").getCanonicalFile();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the version of the running JDK.
     */
    public static String runningJavaVersion() {
        final String fullVersion = System.getProperty("java.version");
        final int index = fullVersion.lastIndexOf(".");
        return fullVersion.substring(0, index);
    }

}
