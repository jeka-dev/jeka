package org.jerkar.api.system;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsSystem;
import org.jerkar.api.utils.JkUtilsThrowable;

/**
 * Provides location related to the running Jerkar instance.
 *
 * @author Jerome Angibaud
 */
public final class JkLocator {

    private final static String JK_USER_HOM_ENV_NAME = "JERKAR_USER_HOME";

    private final static String JK_REPOSITORY_CACHE_ENV_NAME = "JERKAR_REPO";

    private static Path JERKAR_JAR_FILE;

    /**
     * Returns the Jerkar jar file currently used in the running process. It can be a folder in the classes
     * are not packaged in jar.
     */
    public static Path jerkarJarPath() {
        if (JERKAR_JAR_FILE != null) {
            return JERKAR_JAR_FILE;
        }
        for (final Path file : JkUtilsSystem.classloaderEntries((URLClassLoader) JkLocator.class.getClassLoader())) {
            final URL url = JkUtilsPath.toUrl(file);

            try ( URLClassLoader classLoader =  new URLClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader().getParent())) {
                classLoader.loadClass(JkLocator.class.getName());
                JERKAR_JAR_FILE = file;
                return file;
            } catch (final ClassNotFoundException e) {
                // Class just not there
            } catch (final IOException e) {
                throw JkUtilsThrowable.unchecked(e);
            }
        }
        throw new IllegalStateException("Main not found in classpath");
    }

    /**
     * Returns the directory where is installed the running Jerkar instance.
     */
    public static Path jerkarHomeDir() {
        return jerkarJarPath().getParent();
    }


    /**
     * Returns the Jerkar user directory.
     */
    public static Path jerkarUserHomeDir() {
        final Path result;
        final String env = System.getenv(JK_USER_HOM_ENV_NAME);
        if (!JkUtilsString.isBlank(env)) {
            result = Paths.get(env);
        } else {
            result = Paths.get(System.getProperty("user.home")).resolve(".jerkar");
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
    public static Path jerkarRepositoryCache() {
        final String jerkarCacheOption = System.getenv(JK_REPOSITORY_CACHE_ENV_NAME);
        final Path result;
        if (!JkUtilsString.isBlank(jerkarCacheOption)) {
            result = Paths.get(jerkarCacheOption);
        } else {
            result = jerkarUserHomeDir().resolve("cache/repo");
        }
        JkUtilsPath.createDirectories(result);
        return result;
    }


}
