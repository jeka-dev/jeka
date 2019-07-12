package dev.jeka.core.api.system;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Provides location related to the running Jeka instance.
 *
 * @author Jerome Angibaud
 */
public final class JkLocator {

    private final static String JK_USER_HOM_ENV_NAME = "JEKA_USER_HOME";

    private final static String JK_REPOSITORY_CACHE_ENV_NAME = "JEKA_REPO";

    private static Path JEKA_JAR_FILE;

    /**
     * Returns the Jeka jar file currently used in the running process. Returns a folder if the classes
     * are not packaged in jar.
     */
    public static Path getJekaJarPath() {
        if (JEKA_JAR_FILE != null) {
            return JEKA_JAR_FILE;
        }
        URI uri;
        try {
            uri = JkLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Cannot find location of " + JkLocator.class);
        }
        final Path result = Paths.get(uri);
        JEKA_JAR_FILE = result;
        return result;
    }

    /**
     * Returns the directory where is installed the running Jeka instance.
     */
    public static Path getJekaHomeDir() {
        return getJekaJarPath().getParent();
    }

    /**
     * Returns the Jeka user directory.
     */
    public static Path getJekaUserHomeDir() {
        final Path result;
        final String env = System.getenv(JK_USER_HOM_ENV_NAME);
        if (!JkUtilsString.isBlank(env)) {
            result = Paths.get(env);
        } else {
            result = Paths.get(System.getProperty("user.home")).resolve(".jeka");
        }
        if (Files.exists(result) && Files.isRegularFile(result)) {
            JkUtilsPath.deleteFile(result);
        }
        JkUtilsPath.createDirectories(result);
        return result;
    }

    /**
     * Returns the location of the artifact repository cache.
     */
    public static Path getJekaRepositoryCache() {
        final String jekaCacheOption = System.getenv(JK_REPOSITORY_CACHE_ENV_NAME);
        final Path result;
        if (!JkUtilsString.isBlank(jekaCacheOption)) {
            result = Paths.get(jekaCacheOption);
        } else {
            result = getJekaUserHomeDir().resolve("cache/repo");
        }
        JkUtilsPath.createDirectories(result);
        return result;
    }


}
