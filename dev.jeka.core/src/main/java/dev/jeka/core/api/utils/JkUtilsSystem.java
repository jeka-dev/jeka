package dev.jeka.core.api.utils;

import dev.jeka.core.api.java.JkClassLoader;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    public static final boolean IS_MACOS = isMacos();

    private static final Class UNSAFE_CLASS = JkClassLoader.ofCurrent().loadIfExist("sun.misc.Unsafe");

    private static boolean isWindows() {
        final String osName = System.getProperty("os.name");
        if (osName == null) {
            return false;
        }
        return osName.startsWith("Windows");
    }

    private static boolean isMacos() {
        final String osName = System.getProperty("os.name");
        if (osName == null) {
            return false;
        }
        return osName.startsWith("Mac OS X");
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
        if (UNSAFE_CLASS == null) {
            return;
        }
        // Try to use sun.misc.Unsafe class if present
        // https://stackoverflow.com/questions/46454995/how-to-hide-warning-illegal-reflective-access-in-java-9-without-jvm-argument
        try {
            Field theUnsafe = UNSAFE_CLASS.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            Method staticFieldOffsetMethod = UNSAFE_CLASS.getMethod("staticFieldOffset", Field.class);
            long staticFieldOffset = (long) staticFieldOffsetMethod.invoke(unsafe, logger);
            Method putObjectVolatileMethod = UNSAFE_CLASS.getMethod("putObjectVolatile", Object.class,
                    Long.class, Object.class);
            putObjectVolatileMethod.invoke(cls, staticFieldOffset, null);
        } catch (Exception e) {
            // ignore
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static void join(Thread thread) {
        try {
            thread.join();;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
