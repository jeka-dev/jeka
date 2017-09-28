package org.jerkar.api.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to deal with the underlying system.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsSystem {

    private JkUtilsSystem() {
    }

    /**
     * Flag valuing <code>true</code> if the running underlying system is
     * Windows.
     */
    public static final boolean IS_WINDOWS = isWindows();

    private static boolean isWindows() {
        final String osName = System.getProperty("os.name");
        if (osName == null) {
            return false;
        }
        return osName.startsWith("Windows");
    }

    /**
     * Returns the classpath of this classloader without mentioning classpath of
     * the parent classloaders.
     */
    public static List<Path> classloaderEntries(URLClassLoader classLoader) {
        final List<Path> result = new ArrayList<>();
        for (final URL url : classLoader.getURLs()) {
            String pathName = null;
            try {
                pathName = url.toURI().getPath().replaceAll("%20", " ").trim();
            } catch (URISyntaxException e) {
                throw JkUtilsThrowable.unchecked(e);
            }
            String fileName = new File(pathName).getAbsolutePath();  // Paths.get() fails at interpreting /c:/local.....
            if (fileName.endsWith("*")) {
                String parent = JkUtilsString.substringBeforeLast(fileName, "/*");
                JkUtilsPath.listDirectChildren(Paths.get(parent)).stream()
                        .filter(item -> item.toString().toLowerCase().endsWith(".jar"))
                        .forEach(item -> result.add(item));
                continue;
            }

            result.add(Paths.get(fileName));
        }
        return result;
    }

    /**
     * Adds an action to be executed when he JVM shuts down.
     */
    public static void addOnExitAction(Runnable runnable) {
        Runtime.getRuntime().addShutdownHook(new Thread(runnable));
    }

}
