package org.jerkar.api.java;


import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathTreeSet;

import org.jerkar.api.file.JkPathMatcher;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.api.utils.JkUtilsSystem;
import org.jerkar.api.utils.JkUtilsZip;

/**
 * Wrapper around {@link URLClassLoader} offering convenient methods and fluent
 * interface to deal with <code>URLClassLoader</code>.
 *
 * @author Jerome Angibaud
 */
public final class JkClassLoader {

    private static final String CLASS_SUFFIX = ".class";

    private static final int CLASS_SUFFIX_LENGTH = CLASS_SUFFIX.length();

    private static final int JAVA_SUFFIX_LENGTH = ".java".length();

    private static Path urlCacheDir = JkLocator.jerkarUserHomeDir().resolve("cache/url-content");

    static {
        JkUtilsPath.createDirectories(urlCacheDir);
    }

    /**
     * A {@link PathMatcher} accepting only .class files.
     */
    public static final PathMatcher CLASS_FILE_FILTER = file -> (Files.isRegularFile(file)
            && file.getFileName().toString().endsWith(CLASS_SUFFIX));

    /**
     * Set the directory where are cached urls. For its internal use, Jerkar may
     * copy the the content of an URL to a file. This class manages the central
     * place where those URL are cached.
     */
    public static void urlCacheDir(Path dir) {
        JkUtilsPath.createDirectories(dir);
        urlCacheDir = dir;
    }

    /**
     * @see #urlCacheDir(Path)
     */
    public static Path urlCacheDir() {
        return urlCacheDir;
    }

    private final ClassLoader delegate;

    private JkClassLoader(ClassLoader delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns a {@link JkClassLoader} wrapping the current class loader.
     *
     * @see Class#getClassLoader()
     */
    public static JkClassLoader current() {
        if (! (JkClassLoader.class.getClassLoader() instanceof URLClassLoader)) {
            throw new RuntimeException("You seem running on JDK9+, Jerkar is currently compatible with JDK8 only. " +
                    "\nYou can build JDK9 project but Jerkar itself has to be run on JDK8.");
        }
        return new JkClassLoader((URLClassLoader) JkClassLoader.class.getClassLoader());
    }

    /**
     * Returns a {@link JkClassLoader} wrapping the system class loader.
     *
     * @see ClassLoader#getSystemClassLoader()
     */
    public static JkClassLoader system() {
        return new JkClassLoader((URLClassLoader) ClassLoader.getSystemClassLoader());
    }

    /**
     * Returns a {@link JkClassLoader} wrapping the class loader having loaded
     * the specified class.
     */
    public static JkClassLoader of(Class<?> clazz) {
        return new JkClassLoader((URLClassLoader) clazz.getClassLoader());
    }

    /**
     * Return the {@link URLClassLoader} wrapped by this object.
     */
    public ClassLoader classloader() {
        return delegate;
    }

    /**
     * Returns the class loader parent of this one.
     */
    public JkClassLoader parent() {
        return new JkClassLoader((URLClassLoader) this.delegate.getParent());
    }

    /**
     * @see #child(Iterable)
     */
    public JkClassLoader child(Path... entries) {
        return new JkClassLoader(new URLClassLoader(toUrl(Arrays.asList(entries)), this.delegate));
    }

    /**
     * Creates a <code>JkClassLoader</code>, child of this one and having the
     * specified entries.
     */
    public JkClassLoader child(Iterable<Path> entries) {
        return new JkClassLoader(new URLClassLoader(toUrl(entries), this.delegate));
    }

    /**
     * Creates a <code>JkClassLoader</code> loader having the same parent and
     * the same entries as this one plus the specified entries. URL entries that
     * are not file are transformed to file (created in temp folder).
     */
    public JkClassLoader sibling(URL... fileOrUrls) {
        return sibling(Arrays.asList(fileOrUrls));
    }

    /**
     * @see #sibling(Iterable)
     */
    public JkClassLoader sibling(Iterable<URL> fileOrUrls) {
        final List<Path> files = new LinkedList<>();
        for (final URL entry : fileOrUrls) {
            final URL url = entry;
            final String path = url.getFile();
            final File candidate = new File(path);
            if (JkUtilsString.isBlank(path)
                    || (!candidate.isFile() && !candidate.isDirectory())) {
                PrintStream printStream =
                        JkLog.Verbosity.VERBOSE == JkLog.verbosity() ? new PrintStream(JkLog.stream()) : null;
                final Path file = JkUtilsIO.copyUrlContentToCacheFile(url, printStream, urlCacheDir);
                files.add(file);
            } else {
                files.add(candidate.toPath());
            }
        }
        return parent().child(this.childClasspath().andMany(files));
    }

    /**
     * Same as {@link #sibling(URL...)} but more tolerant about the input. If
     * one of the specified entry is not valid, then it is simply ignored.
     */
    public JkClassLoader siblingWithOptional(URL... fileOrUrls) {
        final List<URL> objects = new LinkedList<>();
        for (final URL entry : fileOrUrls) {
            objects.add(entry);
        }
        return sibling(objects);
    }

    /**
     * Returns a sibling of this class loader that outputs every searched class.
     *
     * @see #sibling(Iterable)
     */
    public JkClassLoader printingSearchedClasses(Set<String> searchedClassContainer) {
        return new JkClassLoader(new TrackingClassLoader(searchedClassContainer, this
                .childClasspath().asArrayOfUrl(), (URLClassLoader) this.parent().delegate));
    }

    /**
     * Returns the classpath of this classloader without mentioning classpath of
     * the parent classloaders.
     */
    public JkClasspath childClasspath() {
        return JkClasspath.ofMany(JkUtilsSystem.classloaderEntries((URLClassLoader) this.delegate));
    }

    /**
     * Returns the complete classpath of this classloader.
     */
    public JkClasspath fullClasspath() {
        final JkClasspath classpath;
        if (this.delegate.getParent() != null) {
            classpath = this.parent().fullClasspath();
        } else {
            classpath = JkClasspath.of();
        }
        return classpath.andMany(childClasspath());
    }

    /**
     * Delegates the call to {@link ClassLoader#loadClass(String)} of this
     * wrapped <code>class loader</code>.<br/>
     * The specified class is supposed to be defined in this class loader,
     * otherwise an {@link IllegalArgumentException} is thrown.
     *
     * @see #loadIfExist(String) #isDefined(String)
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> load(String className) {
        try {
            return (Class<T>) delegate.loadClass(className);
        } catch (final ClassNotFoundException | NoClassDefFoundError e) {
            throw new IllegalArgumentException("Fail at loading class " + className + " on " + this, e);
        }
    }

    /**
     * Loads a class given its source relative path if exists. For example
     * <code>loadGivenSourcePath("mypack1/subpack/MyClass.java")
     * will load the class <code>mypack1.subpack.MyClass</code>. Returns
     * <code>null</code> if no such class exists.
     */
    public <T> Class<T> loadGivenClassSourcePathIfExist(String classSourcePath) {
        final String className = classSourcePath.replace('/', '.').replace('\\', '.')
                .substring(0, classSourcePath.length() - JAVA_SUFFIX_LENGTH);
        return loadIfExist(className);
    }

    /**
     * Loads a class given its source relative path. For example
     * <code>loadGivenSourcePath("mypack1/subpack/MyClass.java")
     * will load the class <code>mypack1.subpack.MyClass</code>.
     */
    public <T> Class<T> loadGivenClassSourcePath(String classSourcePath) {
        final String className = classSourcePath.replace('/', '.').replace('\\', '.')
                .substring(0, classSourcePath.length() - JAVA_SUFFIX_LENGTH);
        return load(className);
    }

    /**
     * Loads the class having the specified name or return <code>null</code> if
     * no such class is defined in this <code>classloader</code>.
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> loadIfExist(String className) {
        try {
            return (Class<T>) delegate.loadClass(className);
        } catch (final ClassNotFoundException e) { // NOSONAR
            return null;
        }
    }

    /**
     * Returns if the specified class is defined in this <code>classloader</code>.
     */
    public boolean isDefined(String className) {
        try {
            delegate.loadClass(className);
            return true;
        } catch (final ClassNotFoundException e) { // NOSONAR
            return false;
        }
    }

    /**
     * Loads the class having the specified full name or the specified simple
     * name. Returns <code>null</code> if no class matches. </br> For example :
     * loadFromNameOrSimpleName("MyClass", null) may returns
     * my.packAllArtifacts.MyClass class.
     *
     * @param name          The full name or the simple name of the class to load
     * @param superClassArg If not null, the search is narrowed to classes/interfaces
     *                      children of this class/interface.
     * @return The loaded class or <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public <T> Class<? extends T> loadFromNameOrSimpleName(final String name, Class<T> superClassArg) {
        final Class<T> superClass = this.load(superClassArg.getName());
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
     * Returns all classes of this <code>classloader</code> that are defined in
     * entries matching the specified fileFilter.</br> For example : if you want
     * to load all classes that are defined in folder and not accept jar file, you
     * have to provide a <code>FileFilter</code> which includes only
     * directories.
     *
     * @param entryFilter The classpath entry filter. Can be <code>null</code>.
     */
    public Set<Class<?>> loadClassesInEntries(PathMatcher entryFilter) {
        final List<Path> classfiles = new LinkedList<>();
        final Map<Path, Path> file2Entry = new HashMap<>();
        for (final Path file : childClasspath()) {
            if (entryFilter == null || entryFilter.matches(file) && Files.isDirectory(file)) {
                final List<Path> files = JkPathTree.of(file).andMatcher(CLASS_FILE_FILTER).files();
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
        final Set<Path> classFiles = this.fullClasspath().allPathMatching(patterns);
        for (final Path classFile : classFiles) {
            final String className = getAsClassName(classFile.toString());
            result.add(this.load(className));
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
     * @see JkClassLoader#loadClassesInEntries(PathMatcher)
     */
    public Set<Class<?>> loadClassesIn(JkPathTreeSet jkPathTreeSet) {
        final Set<Class<?>> result = new HashSet<>();
        for (final Path path : jkPathTreeSet.relativeFiles()) {
            if (path.toString().endsWith(".class")) {
                final String className = getAsClassName(path.toString());
                result.add(this.load(className));
            }
        }
        return result;
    }

    /**
     * Returns all classes of this <code>classloader</code> that are defined
     * inside the provided <code>JkPathTreeSet</code>.
     *
     * @see JkClassLoader#loadClassesInEntries(PathMatcher)
     */
    public Iterator<Class<?>> iterateClassesIn(JkPathTreeSet jkPathTreeSet) {
        final List<Path> fileNames = jkPathTreeSet.andMatcher(JkPathMatcher.accept("**/*.class"))
                .relativeFiles();
        return classIterator(fileNames.stream().map(path -> path.toString()).collect(Collectors.toList()));
    }

    /**
     * Returns all classes of this <code>classloader</code> that are defined
     * inside the specified directory.
     *
     * @see JkClassLoader#loadClassesInEntries(PathMatcher)
     */
    private Iterator<Class<?>> iterateClassesIn(Path dirOrJar) {
        final List<Path> paths;
        if (Files.isDirectory(dirOrJar)) {
            paths = JkPathTree.of(dirOrJar).accept("**.class").relativeFiles();
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
        return new Iterator<Class<?>>() {

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Class<?> next() {
                final String className = getAsClassName(it.next());
                return load(className);
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
        final JkClassLoader classLoader = JkClassLoader.system().child(classDirOrJar);
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
        builder.append(delegate.getClass().getName());
        if (delegate instanceof URLClassLoader) {
            for (final URL url : ((URLClassLoader) delegate).getURLs()) {
                builder.append("\n  ").append(url);
            }
        }
        if (delegate.getParent() != null) {
            builder.append("\n").append(this.parent());
        }
        return builder.toString();
    }



    /**
     * Add a new entry to this class loader. WARN : This method has side effect
     * on this class loader : use it with caution !
     */
    public void addEntry(Path entry) {
        final Method method = JkUtilsReflect.getDeclaredMethod(URLClassLoader.class, "addURL",
                URL.class);
        JkUtilsReflect.invoke(this.delegate, method, JkUtilsPath.toUrl(entry));
    }

    /**
     * Same as {@link #addEntry(Path)} but for several entries.
     */
    public void addEntries(Iterable<Path> entries) {
        for (final Path file : JkUtilsPath.disambiguate(entries)) {
            addEntry(file);
        }
    }

    /**
     * Invokes a static method on the specified class using the provided
     * arguments. <br/>
     * If the argument classes are the same on the current class loader and this
     * one then arguments are passed as is, otherwise arguments are serialized
     * in the current class loader and deserialized in this class loader in
     * order to be compliant with it. <br/>
     * The current thread context class loader is switched to this for the
     * method execution. <br/>
     * It is then turned back to the former one when the execution is done.
     */
    @SuppressWarnings("unchecked")
    public <T> T invokeStaticMethod(boolean serializeResult, String className, String methodName,
                                    Object... args) {
        if (args == null) {
            args = new Object[0];
        }
        final Class<?> clazz = this.load(className);
        final Object[] effectiveArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            effectiveArgs[i] = traverseClassLoader(args[i], this);
        }
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(delegate);
        initLogInClassloader();
        try {
            final Object returned = JkUtilsReflect.invokeStaticMethod(clazz, methodName,
                    effectiveArgs);
            final T result;
            if (serializeResult) {
                result = (T) traverseClassLoader(returned, JkClassLoader.current());
            } else {
                result = (T) returned;
            }
            return result;
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    /**
     * Invokes an instance method on the specified object using the specified
     * arguments. <br/>
     * If the argument classes are the same on the current class loader and this
     * one then arguments are passed as is, otherwise arguments are serialized
     * in the current class loader and deserialized accept this class loader of
     * order to be compliant with it. <br/>
     * The current thread context class loader is switched to this for the
     * method execution. <br/>
     * It is then turned back to the former one when the execution is done.
     */
    @SuppressWarnings("unchecked")
    public <T> T invokeInstanceMethod(boolean serializeResult, Object object, Method method,
                                      Object... args) {
        if (args == null) {
            args = new Object[0];
        }
        final Object[] effectiveArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            effectiveArgs[i] = traverseClassLoader(args[i], this);
        }
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(delegate);
        initLogInClassloader();
        try {

            final Object returned = JkUtilsReflect.invoke(object, method, effectiveArgs);
            final T result;
            if (serializeResult) {
                result = (T) traverseClassLoader(returned, JkClassLoader.current());
            } else {
                result = (T) returned;
            }
            return result;
        } catch (final IllegalArgumentException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    /**
     * Creates an instance of the class having the specified name in this class
     * loader.
     */
    @SuppressWarnings("unchecked")
    public <T> T newInstance(String className) {
        final Class<?> clazz = this.load(className);
        return (T) JkUtilsReflect.newInstance(clazz);
    }

    /**
     * Reloads all J2SE service providers. It can be necessary if adding
     * dynamically some service providers to the classpath.
     */
    public JkClassLoader loadAllServices() {
        final Set<Class<?>> serviceClasses = new HashSet<>();
        for (final Path file : this.fullClasspath()) {
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
                        final Class<?> serviceClass = this.loadIfExist(serviceName);
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
                    final Class<?> serviceClass = this.loadIfExist(candidate.getFileName().toString());
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
        return this;
    }

    private void initLogInClassloader() {
        JkLog.initializeInClassLoader(this.classloader());
    }

    private static Object traverseClassLoader(Object object, JkClassLoader to) {
        if (object == null) {
            return null;
        }
        final Class<?> clazz = object.getClass();
        final String className;
        if (clazz.isArray()) {
            className = object.getClass().getComponentType().getName();
        } else {
            className = object.getClass().getName();
        }

        final JkClassLoader from = JkClassLoader.of(object.getClass());
        final Class<?> toClass = to.load(className);
        final boolean container = Collection.class.isAssignableFrom(clazz)
                || Map.class.isAssignableFrom(clazz);
        if (from.delegate == null && !container) { // Class from JDK
            return object;
        }
        if (toClass.equals(object.getClass()) && !container) {
            return object;
        }

        return JkUtilsIO.cloneBySerialization(object, to.classloader());
    }

    // Class loader that keep all the find classes in a given set
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

    /**
     * Creates an instance from the specified class in this classloader and
     * callable from the current class loader. Arguments ans result are
     * serialized (if needed) so we keep compatibility between classes.
     */
    @SuppressWarnings("unchecked")
    public <T> T transClassloaderProxy(Class<T> interfaze, String className,
                                       String staticMethodFactory, Object... args) {
        final Object target = this.invokeStaticMethod(false, className, staticMethodFactory, args);
        return ((T) Proxy.newProxyInstance(JkClassLoader.current().delegate,
                new Class[]{interfaze}, new TransClassloaderInvokationHandler(target)));
    }

    private class TransClassloaderInvokationHandler implements InvocationHandler {

        TransClassloaderInvokationHandler(Object target) {
            super();
            this.target = target;
        }

        private final Object target;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final Method targetMethod = JkUtilsReflect.methodWithSameNameAndArgType(method,
                    target.getClass());
            return invokeInstanceMethod(true, target, targetMethod, args);

        }

    }

}
