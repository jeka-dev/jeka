package dev.jeka.core.api.utils;

import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to deal with the underlying ofSystem.
 *
 * @author Jerome Angibaud
 */
public final class JkUtilsSystem {

    private JkUtilsSystem() {
    }

    /**
     * Flag valuing <code>true</code> if the running underlying ofSystem is
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
            String pathName;
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
     * On Jdk 9+, a warning is emitted while attempting to access private fields by reflection. This hack aims at
     * removing this warning.
     */
    public static void disableUnsafeWarning() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            unsafe.putObjectVolatile(cls, unsafe.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
    }

}
