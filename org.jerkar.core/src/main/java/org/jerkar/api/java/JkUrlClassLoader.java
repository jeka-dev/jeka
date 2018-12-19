package org.jerkar.api.java;


import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.*;

import java.io.File;
import java.io.PrintStream;
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

    private static final int JAVA_SUFFIX_LENGTH = ".java".length();

    private static Path urlCacheDir = JkLocator.getJerkarUserHomeDir().resolve("cache/url-content");

    static {
        JkUtilsPath.createDirectories(urlCacheDir);
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

    /**
     * Returns a {@link JkUrlClassLoader} wrapping the current thread context classloader.
     *
     * @see Class#getClassLoader()
     */
    public static JkUrlClassLoader ofCurrent() {
        return createUrlClassLoaderFrom(Thread.currentThread().getContextClassLoader());
    }

    /**
     * Returns a {@link JkUrlClassLoader} wrapping the ofSystem class loader.
     *
     * @see ClassLoader#getSystemClassLoader()
     */
    public static JkUrlClassLoader ofSystem() {
        return createUrlClassLoaderFrom(ClassLoader.getSystemClassLoader());
    }

    private static JkUrlClassLoader createUrlClassLoaderFrom(ClassLoader classLoader) {
        if (! (classLoader instanceof URLClassLoader)) {
            throw new IllegalStateException("The current or system classloader is not instance of URLClassLoader. It " +
                    "is probably due that you are currently running on JDK9.");
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
     * Get the directory where are cached urls. For its internal use, Jerkar may
     * copy the the content of an URL to a file. This class manages the central
     * place where those URL are cached.
     */
    public static Path getUrlCacheDir() {
        return urlCacheDir;
    }


    /**
     * Return the {@link URLClassLoader} wrapped by this object.
     */
    public ClassLoader getClassloader() {
        return delegate;
    }

    /**
     * Returns the class loader parent of this one.
     */
    public JkUrlClassLoader getParent() {
        return new JkUrlClassLoader((URLClassLoader) this.delegate.getParent());
    }

    /**
     * @see #getChild(Iterable)
     */
    private JkUrlClassLoader getChild(Path... entries) {
        return new JkUrlClassLoader(new URLClassLoader(toUrl(Arrays.asList(entries)), this.delegate));
    }

    /**
     * Creates a <code>JkClassLoader</code>, child of this one and having the specified entries.
     */
    public JkUrlClassLoader getChild(Iterable<Path> extraEntries) {
        Iterable<Path> paths = JkUtilsPath.disambiguate(extraEntries);
        return new JkUrlClassLoader(new URLClassLoader(toUrl(paths), this.delegate));
    }

    /**
     * Creates a <code>JkClassLoader</code> loader having the same parent and
     * the same entries as this one plus the specified entries. URL entries that
     * are not file are transformed to file (created in temp folder).
     */
    public JkUrlClassLoader getSibling(URL... fileOrUrls) {
        return getSibling(Arrays.asList(fileOrUrls));
    }

    /**
     * @see #getSibling(Iterable)
     */
    public JkUrlClassLoader getSibling(Iterable<URL> fileOrUrls) {
        final List<Path> files = new LinkedList<>();
        for (final URL entry : fileOrUrls) {
            final URL url = entry;
            final String path = url.getFile();
            final File candidate = new File(path);
            if (JkUtilsString.isBlank(path)
                    || (!candidate.isFile() && !candidate.isDirectory())) {
                PrintStream printStream =
                        JkLog.Verbosity.VERBOSE == JkLog.verbosity() ? new PrintStream(JkLog.getOutputStream()) : null;
                final Path file = JkUtilsIO.copyUrlContentToCacheFile(url, printStream, urlCacheDir);
                files.add(file);
            } else {
                files.add(candidate.toPath());
            }
        }
        return getParent().getChild(this.getDirectClasspath().and(files));
    }

    /**
     * Same as {@link #getSibling(URL...)} but more tolerant about the input. If
     * one of the specified entry is not valid, then it is simply ignored.
     */
    public JkUrlClassLoader getSiblingWithOptional(URL... fileOrUrls) {
        final List<URL> objects = new LinkedList<>();
        for (final URL entry : fileOrUrls) {
            objects.add(entry);
        }
        return getSibling(objects);
    }

    /**
     * Returns a getSibling of this class loader that outputs every searched class.
     *
     * @see #getSibling(Iterable)
     */
    public JkUrlClassLoader getSiblingPrintingSearchedClasses(Set<String> searchedClassContainer) {
        return new JkUrlClassLoader(new TrackingClassLoader(searchedClassContainer, this
                .getDirectClasspath().toArrayOfUrl(), this.getParent().delegate));
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
        if (this.delegate.getParent() != null) {
            classpath = this.getParent().getFullClasspath();
        } else {
            classpath = JkClasspath.of();
        }
        return classpath.and(getDirectClasspath());
    }



    public JkClassLoader toJkClassLoader() {
        return JkClassLoader.of(delegate);
    }



    /**
     * Returns all classes of this <code>classloader</code> that are defined in
     * entries matching the specified fileFilter.</br> For example : if you want
     * to load all classes that are defined in folder and not andAccept jar file, you
     * have to provide a <code>FileFilter</code> which includes only
     * directories.
     *
     * @param entryFilter The classpath entry filter. Can be <code>null</code>.
     */
    public Set<Class<?>> loadClassesInEntries(PathMatcher entryFilter) {
        final List<Path> classfiles = new LinkedList<>();
        final Map<Path, Path> file2Entry = new HashMap<>();
        for (final Path file : getDirectClasspath()) {
            if (entryFilter == null || entryFilter.matches(file) && Files.isDirectory(file)) {
                final List<Path> files = JkPathTree.of(file).andMatcher(CLASS_FILE_FILTER).getFiles();
                classfiles.addAll(files);
                JkUtilsIterable.putMultiEntry(file2Entry, files, file);
            }
        }
        final Set<Class<?>> result = new HashSet<>();
        for (final Path file : classfiles) {
            final Path entry = file2Entry.get(file);
            final String relativeName = entry.relativize(file).toString();
            final String className = getAsClassName(relativeName);
            Class<?> clazz;
            try {
                clazz = delegate.loadClass(className);
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException("Can't find the class " + className, e);
            }
            result.add(clazz);
        }
        return result;
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
     * @see JkUrlClassLoader#loadClassesInEntries(PathMatcher)
     */
    public Set<Class<?>> loadClassesIn(JkPathTreeSet jkPathTreeSet) {
        final Set<Class<?>> result = new HashSet<>();
        JkClassLoader jkClassLoader = JkClassLoader.of(delegate);
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
     *
     * @see JkUrlClassLoader#loadClassesInEntries(PathMatcher)
     */
    private Iterator<Class<?>> iterateClassesIn(Path dirOrJar) {
        final List<Path> paths;
        if (Files.isDirectory(dirOrJar)) {
            paths = JkPathTree.of(dirOrJar).andAccept("**.class").getRelativeFiles();
        } else {
            final List<ZipEntry> entries = JkUtilsZip.zipEntries(JkUtilsZip.zipFile(dirOrJar.toFile()));
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
     * Add a new entry to this class loader. WARN : This method has side effect
     * on this class loader : use it with caution !
     */
    public void addEntries(Path path1, Path path2, Path... others) {
        addEntries(JkUtilsIterable.listOf2orMore(path1, path2, others));
    }

    /**
     * Same as {@link #addEntries(Path, Path, Path...)}.
     * @param paths As {@link Path} class implements {@link Iterable<Path>} the argument can be a single {@link Path}
     * instance, if so it will be interpreted as a list containing a single element which is this argument.
     */
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
    public void loadAllServices() {
            final Set<Class<?>> serviceClasses = new HashSet<>();
        for (final Path file : this.getFullClasspath()) {
            if (Files.isRegularFile(file)) {
                JkLog.trace("Scanning " + file + " for META-INF/services.");
                final ZipFile zipFile = JkUtilsZip.zipFile(file.toFile());
                final ZipEntry serviceEntry = zipFile.getEntry("META-INF/services");
                if (serviceEntry == null) {
                    JkUtilsIO.closeOrFail(zipFile);
                    continue;
                }
                for (final ZipEntry entry : JkUtilsZip.zipEntries(zipFile)) {
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

    // Class loader that keep all the found classes in a given set
    private final class TrackingClassLoader extends URLClassLoader {

        private final Set<String> searchedClasses;

        public TrackingClassLoader(Set<String> searchedClasses, URL[] urls, ClassLoader parent) {
            super(urls, parent);
            this.searchedClasses = searchedClasses;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            searchedClasses.add(name);
            return super.findClass(name);
        }
    }

}
