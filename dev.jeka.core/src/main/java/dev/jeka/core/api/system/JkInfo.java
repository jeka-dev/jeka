package dev.jeka.core.api.system;

import dev.jeka.core.api.java.JkManifest;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Provides information about Jeka running instance.
 */
public final class JkInfo {

    private static String version;

    public static final String JEKA_MODULE_ID = "dev.jeka:jeka-core";

    /**
     * Returns the current Jeka version.
     */
    public static String getJekaVersion() {
        if (JkUtilsString.isBlank(version)) {
            version = JkManifest.of().setManifestFromClass(JkInfo.class).getMainAttribute(JkManifest.IMPLEMENTATION_VERSION);
        }
        return version;
    }

}
