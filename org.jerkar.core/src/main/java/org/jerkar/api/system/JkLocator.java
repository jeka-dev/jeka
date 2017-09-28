package org.jerkar.api.system;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jerkar.api.utils.*;

/**
 * Provides location related to the running Jerkar instance.
 *
 * @author Jerome Angibaud
 */
public final class JkLocator {

    private final static String JK_USER_HOM_ENV_NAME = "JERKAR_USER_HOME";

    private final static String JK_REPOSITORY_CACHE_ENV_NAME = "JERKAR_REPO";

    private static File JERKAR_JAR_FILE;

    /**
     * Returns the Jerkar jar file currently used in the running process.
     */
    public static File jerkarJarFile() {
        if (JERKAR_JAR_FILE != null) {
            return JERKAR_JAR_FILE;
        }
        for (final File file : JkUtilsSystem.classloaderEntries((URLClassLoader) JkLocator.class
                .getClassLoader())) {
            final URL url = JkUtilsFile.toUrl(file);

            try ( URLClassLoader classLoader =  new URLClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader().getParent())) {
                classLoader.loadClass(JkLocator.class.getName());
                JERKAR_JAR_FILE = file;
                return file;
            } catch (final ClassNotFoundException e) {
                // Class just not there
            } catch (IOException e) {
                throw JkUtilsThrowable.unchecked(e);
            }
        }
        throw new IllegalStateException("Main not found in classpath");
    }

    /**
     * Returns the Jerkar jar file currently used in the running process.
     */
    public static Path jerkarJarPath() {
        return jerkarJarFile().toPath();
    }

    /**
     * Returns the directory where is installed the running Jerkar instance.
     */
    public static File jerkarHome() {
        return jerkarJarFile().getParentFile();
    }

    /**
     * Returns the directory where is installed the running Jerkar instance.
     */
    public static Path jerkarHomePath() {
        return jerkarJarFile().getAbsoluteFile().toPath().getParent();
    }

    /**
     * Returns the temporary directory used by Jerkar for its internal use.
     */
    public static File jerkarTempDir() {
        final File result = new File(jerkarUserHome(), "temp");
        if (!result.exists()) {
            result.mkdirs();
        }
        return result;
    }

    /**
     * Returns the Jerkar user directory.
     */
    public static File jerkarUserHome() {
        final File result;
        final String env = System.getenv(JK_USER_HOM_ENV_NAME);
        if (!JkUtilsString.isBlank(env)) {
            result = new File(env);
        } else {
            result = new File(JkUtilsFile.userHome(), ".jerkar");
        }
        if (!result.exists()) {
            JkLog.info("Create Jerkar user directory : " + result.getPath());
            result.mkdirs();
        }
        return result;
    }

    /**
     * Returns the Jerkar user directory.
     */
    public static Path jerkarUserHomePath() {
        final Path result;
        final String env = System.getenv(JK_USER_HOM_ENV_NAME);
        if (!JkUtilsString.isBlank(env)) {
            result = Paths.get(env);
        } else {
            result = JkUtilsFile.userHomePath().resolve(".jerkar");
        }
        if (!Files.exists(result)) {
            JkLog.info("Create Jerkar user directory : " + result);
            JkUtilsPath.createDirectories(result);
        }
        return result;
    }

    /**
     * Returns the location of the artifact repository cache.
     */
    public static File jerkarRepositoryCache() {
        final String jerkarCacheOption = System.getenv(JK_REPOSITORY_CACHE_ENV_NAME);
        final File result;
        if (!JkUtilsString.isBlank(jerkarCacheOption)) {
            result = new File(jerkarCacheOption);
        } else {
            result = new File(jerkarUserHome(), "cache/repo");
        }
        result.mkdirs();
        return result;
    }


}
