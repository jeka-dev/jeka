package dev.jeka.core.api.system;

import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides location related to the running Jeka instance.
 *
 * @author Jerome Angibaud
 */
public final class JkLocator {

    private final static String JK_USER_HOME_ENV_NAME = "JEKA_USER_HOME";

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
        Path result;
        try {
            URI uri = JkLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            result = Paths.get(uri);
        } catch (final Exception e) {
            try {
                URI uri = JkLocator.class.getResource('/' + JkLocator.class.getName()
                        .replace('.', '/') + ".class").toURI();
                String uriString = JkUtilsString.substringAfterFirst(uri.toString(), "jar:");
                uriString = JkUtilsString.substringBeforeFirst(uriString, "!");
                result = Paths.get(URI.create(uriString)).getParent().getParent().getParent().getParent().getParent();
            } catch (URISyntaxException ex) {
                throw new IllegalStateException("Cannot find location of jeka jar", ex);
            }
        }
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
        final String env = System.getenv(JK_USER_HOME_ENV_NAME);
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
