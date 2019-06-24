package dev.jeka.core.api.java;

import dev.jeka.core.api.utils.*;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Wrapper around {@link URLClassLoader} offering convenient methods and fluent
 * interface to deal with <code>URLClassLoader</code>.
 *
 * @author Jerome Angibaud
 */
public final class JkUrlClassLoader {

    private static final String CLASS_SUFFIX = ".class";

    private static final int CLASS_SUFFIX_LENGTH = CLASS_SUFFIX.length();

    static final Path URL_CACHE_DIR = JkLocator.getJekaUserHomeDir().resolve("cache/url-content");

    static {
        JkUtilsPath.createDirectories(URL_CACHE_DIR);
    }

    /**
     * A {@link PathMatcher} accepting only .class files.
     */
    public static final PathMatcher CLASS_FILE_FILTER = file -> (Files.isRegularFile(file)
            && file.getFileName().toString().endsWith(CLASS_SUFFIX));

    private final URLClassLoader delegate;

    private JkUrlClassLoader(URLClassLoader delegate) {
        this.delegate = delegate;
    }

    public static JkUrlClassLoader of(URLClassLoader urlClassLoader) {
        return new JkUrlClassLoader(urlClassLoader);
    }

    public static JkUrlClassLoader of(Iterable<Path> paths) {
        return of(new URLClassLoader(toUrl(paths)));
    }

    public static JkUrlClassLoader of(Iterable<Path> paths, ClassLoader parent) {
        List<Path> cleanedPath = JkUtilsPath.disambiguate(paths);
        return of(new URLClassLoader(toUrl(cleanedPath), parent));
    }

    /**
     * Returns a {@link JkUrlClassLoader} wrapping the current thread context classloader.
     *
     * @see Class#getClassLoader()
     */
    public static JkUrlClassLoader ofCurrent() {
        return wrapUrlClassLoader(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Returns a {@link JkUrlClassLoader} wrapping the ofSystem class loader.
     *
     * @see ClassLoader#getSystemClassLoader()
     */
    public static JkUrlClassLoader ofSystem() {
        return wrapUrlClassLoader(ClassLoader.getSystemClassLoader());
    }

    private static JkUrlClassLoader wrapUrlClassLoader(ClassLoader classLoader) {
        if (! (classLoader instanceof URLClassLoader)) {
            throw new IllegalStateException("The current or system classloader is not instance of URLClassLoader but "
                    + classLoader.getClass() + ". It is probably due that you are currently running on JDK9.");
        }
        return of((URLClassLoader) classLoader);
    }

    /**
     * Returns a {@link JkUrlClassLoader} wrapping the class loader having loaded
     * the specified class.
     */
    public static JkUrlClassLoader ofLoaderOf(Class<?> clazz) {
        return new JkUrlClassLoader((URLClassLoader) clazz.getClassLoader());
    }

    /**
     * Get the directory where are cached urls. For its internal use, Jeka may
     * copy the the content of an URL to a file. This class manages the central
     * place where those URL are cached.
     */
    public static Path getUrlCacheDir() {
        return URL_CACHE_DIR;
    }

    /**
     * Return the {@link URLClassLoader} wrapped by this object.
     */
    public ClassLoader get() {
        return delegate;
    }

    /**
     * Returns the class loader parent of this one.
     */
    public JkClassLoader getParent() {
        return JkClassLoader.of(this.delegate.getParent());
    }

    /**
     * Creates a <code>JkClassLoader</code>, child of this one and having the specified entries.
     */
    public JkUrlClassLoader getChild(Iterable<Path> extraEntries) {
        Iterable<Path> paths = JkUtilsPath.disambiguate(extraEntries);
        return new JkUrlClassLoader(new URLClassLoader(toUrl(paths), this.delegate));
    }

    /**
     * Returns the classpath of this classloader without mentioning classpath of
     * the parent classloaders.
     */
    public JkClasspath getDirectClasspath() {
        return JkClasspath.of(JkUtilsSystem.classloaderEntries((URLClassLoader) this.delegate));
    }

    /**
     * Returns the complete classpath of this classloader.
     */
    public JkClasspath getFullClasspath() {
        final JkClasspath classpath;
        if (this.delegate.getParent() != null && this.getParent().get() instanceof  URLClassLoader) {
            classpath = JkUrlClassLoader.of((URLClassLoader) this.getParent().get()).getFullClasspath();

        } else {
            classpath = JkClasspath.of();
        }
        return classpath.and(getDirectClasspath());
    }

    public JkClassLoader toJkClassLoader() {
        return JkClassLoader.of(delegate);
    }

    /**
     * Loads all class having a relative path matching the supplied
     * glob patterns. For example, if you want to load all class
     * belonging to <code>my.packAllArtifacts</code> or its sub package, then you have to
     * supply a filter with an include pattern as
     * <code>my/packAllArtifacts/&#42;&#42;/&#42;.class</code>. Note that ending with
     * <code>.class</code> is important.
     */
    private Set<Class<?>> loadClasses(Iterable<String> patterns) {
        final Set<Class<?>> result = new HashSet<>();
        JkClassLoader jkClassLoader = JkClassLoader.of(delegate);
        final Set<Path> classFiles = this.getFullClasspath().getAllPathMatching(patterns);
        for (final Path classFile : classFiles) {
            final String className = getAsClassName(classFile.toString());
            result.add(jkClassLoader.load(className));
        }
        return result;
    }

    /**
     * Loads all class having a relative path matching the supplied ANT pattern.
     * For example, if you want to load all class belonging to
     * <code>my.packAllArtifacts</code> or its sub package, then you have to supply a the
     * following pattern <code>my/packAllArtifacts/&#42;&#42;/&#42;</code>.
     */
    public Set<Class<?>> loadClasses(String... globPatterns) {
        final List<String> patterns = new LinkedList<>();
        for (final String pattern : globPatterns) {
            patterns.add(pattern + ".class");
        }
        return loadClasses(patterns);
    }

    /**
     * Returns all classes of this <code>classloader</code> that are defined
     * inside the provided <code>JkPathTreeSet</code>.
     *
     */
    public Set<Class<?>> loadClassesIn(JkPathTreeSet jkPathTreeSet) {
        final Set<Class<?>> result = new HashSet<>();
        JkClassLoader jkClassLoader = this.toJkClassLoader();
        for (final Path path : jkPathTreeSet.getRelativeFiles()) {
            if (path.toString().endsWith(".class")) {
                final String className = getAsClassName(path.toString());
                result.add(jkClassLoader.load(className));
            }
        }
        return result;
    }

    /**
     * Loads the class having the specified full name or the specified simple
     * name. Returns <code>null</code> if no class matches. </br> For example :
     * loadFromNameOrSimpleName("MyClass", null) may returns
     * my.packAllArtifacts.MyClass class.
     *
     * @param name          The full name or the simple value of the class to load
     * @param superClassArg If not null, the search is narrowed to classes/interfaces
     *                      children of this class/interface.
     * @return The loaded class or <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public <T> Class<? extends T> loadFromNameOrSimpleName(final String name, Class<T> superClassArg) {
        final Class<T> superClass = this.toJkClassLoader().load(superClassArg.getName());
        try {
            if (superClass == null) {
                return (Class<? extends T>) delegate.loadClass(name);
            }
            final Class<?> type = delegate.loadClass(name);
            if (superClass.isAssignableFrom(type)) {
                return (Class<? extends T>) type;
            }
            return null;

        } catch (final ClassNotFoundException e) { // NOSONAR
            final Set<Class<?>> classes = loadClasses("**/" + name, name);
            for (final Class<?> clazz : classes) {
                if (clazz.getSimpleName().equals(name) && superClass == null
                        || superClass != null && superClass.isAssignableFrom(clazz)) {
                    return (Class<? extends T>) clazz;

                }
            }
        }
        return null;
    }

    /**
     * Returns all classes of this <code>classloader</code> that are defined
     * inside the specified directory.
     */
    private Iterator<Class<?>> iterateClassesIn(Path dirOrJar) {
        final List<Path> paths;
        if (Files.isDirectory(dirOrJar)) {
            paths = JkPathTree.of(dirOrJar).andMatching(true, "**.class").getRelativeFiles();
        } else {
            final List<ZipEntry> entries = JkUtilsZip.getZipEntries(JkUtilsZip.getZipFile(dirOrJar.toFile()));
            paths = new LinkedList<>();
            for (final ZipEntry entry : entries) {
                if (entry.getName().endsWith(".class")) {
                    paths.add(Paths.get(entry.getName()));
                }
            }
        }
        return classIterator(paths.stream().map(path -> path.toString()).collect(Collectors.toList()));
    }

    private Iterator<Class<?>> classIterator(final Iterable<String> fileNameIt) {
        final Iterator<String> it = fileNameIt.iterator();
        JkClassLoader jkClassLoader = JkClassLoader.of(delegate);
        return new Iterator<Class<?>>() {

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Class<?> next() {
                final String className = getAsClassName(it.next());
                return jkClassLoader.load(className);
            }

            @Override
            public void remove() {
                throw new IllegalStateException("Not supported");

            }
        };
    }

    private static URL[] toUrl(Iterable<Path> files) {
        final List<URL> urls = new ArrayList<>();
        for (final Path file : files) {
            try {
                urls.add(file.toUri().toURL());
            } catch (final MalformedURLException e) {
                throw new IllegalArgumentException(file + " is not convertible to URL");
            }
        }
        return urls.toArray(new URL[0]);
    }

    /**
     * Transforms a class file name as <code>com/foo/Bar.class</code> to the
     * corresponding class name <code>com.foo.Bar</code>.
     */
    private static String getAsClassName(String resourceName) {
        return resourceName.replace(File.separatorChar, '.').replace("/", ".")
                .substring(0, resourceName.length() - CLASS_SUFFIX_LENGTH);
    }

    /**
     * Returns the first class having a main method from the specified class
     * directory or Jar. Returns <code>null</code> if no such class found.
     */
    public static String findMainClass(Path classDirOrJar) {
        final JkUrlClassLoader classLoader = JkUrlClassLoader.ofSystem().getChild(classDirOrJar);
        final Iterator<Class<?>> it = classLoader.iterateClassesIn(classDirOrJar);
        while (it.hasNext()) {
            final Class<?> clazz = it.next();
            final Method mainMethod = JkUtilsReflect.getMethodOrNull(clazz, "main", String[].class);
            if (mainMethod != null) {
                final int modifiers = mainMethod.getModifiers();
                if (Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                    return clazz.getName();
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(delegate);
        if (delegate instanceof URLClassLoader) {
            for (final URL url : ((URLClassLoader) delegate).getURLs()) {
                builder.append("\n  ").append(url);
            }
        }
        if (delegate.getParent() != null) {
            builder.append("\n").append(this.getParent());
        }
        return builder.toString();
    }

    /**
     *
     * @param paths As {@link Path} class implements {@link Iterable<Path>} the argument can be a single {@link Path}
     * instance, if so it will be interpreted as a list containing a single element which is this argument.
     */
    @Deprecated
    public void addEntries(Iterable<Path> paths) {
        final Method method = JkUtilsReflect.getDeclaredMethod(URLClassLoader.class, "addURL",
                URL.class);
        for (final Path path : JkUtilsPath.disambiguate(paths)) {
            JkUtilsReflect.invoke(this.delegate, method, JkUtilsPath.toUrl(path));
        }
    }

    /**
     * Reloads all J2SE service providers. It can be necessary if adding
     * dynamically some service providers to the classpath.
     */
    // TODO move to JkClassloader
    public void loadAllServices() {
        final Set<Class<?>> serviceClasses = new HashSet<>();
        for (final Path file : this.getFullClasspath()) {
            if (Files.isRegularFile(file)) {
                JkLog.trace("Scanning " + file + " for META-INF/services.");
                final ZipFile zipFile = JkUtilsZip.getZipFile(file.toFile());
                final ZipEntry serviceEntry = zipFile.getEntry("META-INF/services");
                if (serviceEntry == null) {
                    JkUtilsIO.closeOrFail(zipFile);
                    continue;
                }
                for (final ZipEntry entry : JkUtilsZip.getZipEntries(zipFile)) {
                    if (entry.getName().startsWith("META-INF/services/")) {
                        final String serviceName = JkUtilsString.substringAfterLast(
                                entry.getName(), "/");
                        final Class<?> serviceClass = this.toJkClassLoader().loadIfExist(serviceName);
                        if (serviceClass != null) {
                            JkLog.trace("Found service providers for : " + serviceName);
                            serviceClasses.add(serviceClass);
                        }
                    }
                }
                JkUtilsIO.closeOrFail(zipFile);
            } else {
                final Path serviceDir = file.resolve("META-INF/services");
                if (!Files.exists(serviceDir) || !Files.isDirectory(serviceDir)) {
                    continue;
                }
                JkUtilsPath.walk(serviceDir, 1).forEach(candidate -> {
                    final Class<?> serviceClass = this.toJkClassLoader().loadIfExist(candidate.getFileName().toString());
                    if (serviceClass != null) {
                        serviceClasses.add(serviceClass);
                    }
                });

            }
        }
        for (final Class<?> serviceClass : serviceClasses) {
            JkLog.trace("Reload service providers for : " + serviceClass.getName());
            ServiceLoader.loadInstalled(serviceClass).reload();
        }
    }

}
