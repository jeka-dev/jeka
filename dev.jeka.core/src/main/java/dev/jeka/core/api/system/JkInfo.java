package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsString;

import java.io.File;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
            final Class<?> clazz = JkInfo.class;
            final String className = clazz.getSimpleName() + ".class";
            final String classPath = clazz.getResource(className).toString();
            if (!classPath.startsWith("jar")) {
                // Class not from JAR
                final String relativePath = clazz.getName().replace('.', File.separatorChar)
                        + ".class";
                final String classFolder = classPath.substring(0,
                        classPath.length() - relativePath.length() - 1);
                final String manifestPath = classFolder + "/META-INF/MANIFEST.MF";
                version = readVersionFrom(manifestPath);
            } else {
                final String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1)
                        + "/META-INF/MANIFEST.MF";
                version = readVersionFrom(manifestPath);
            }
        }
        return version;
    }

    private static String readVersionFrom(String manifestPath) {
        Manifest manifest = null;
        try {
            manifest = new Manifest(new URL(manifestPath).openStream());
            final Attributes attrs = manifest.getMainAttributes();
            return attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
