package org.jake;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;

/**
 * Wrapper around {@link URLClassLoader} offering convenient methods and fluent interface to deal
 * with <code>URLClassLoader</code>.
 * 
 * @author Jerome Angibaud
 */
public final class JakeClassLoader {

	private static final String CLASS_SUFFIX = ".class";

	private static final int CLASS_SUFFIX_LENGTH = CLASS_SUFFIX.length();

	private static final int JAVA_SUFFIX_LENGTH = ".java".length();

	/**
	 * A {@link FileFilter} accepting only .class files.
	 */
	public static final FileFilter CLASS_FILE_FILTER = new FileFilter() {

		@Override
		public boolean accept(File file) {
			return (file.isFile() && file.getName().endsWith(CLASS_SUFFIX));
		}
	};

	private final URLClassLoader delegate;

	private JakeClassLoader(URLClassLoader delegate) {
		this.delegate = delegate;
	}

	/**
	 * Returns a {@link JakeClassLoader} wrapping the current class loader.
	 * @see Class#getClassLoader()
	 */
	public static JakeClassLoader current() {
		return new JakeClassLoader((URLClassLoader) JakeClassLoader.class.getClassLoader());
	}

	/**
	 * Returns a {@link JakeClassLoader} wrapping the system class loader.
	 * @see ClassLoader#getSystemClassLoader()
	 */
	public static JakeClassLoader system() {
		return new JakeClassLoader((URLClassLoader) ClassLoader.getSystemClassLoader());
	}

	/**
	 * Returns a {@link JakeClassLoader} wrapping the class loader having loaded the specified class.
	 */
	public static JakeClassLoader of(Class<?> clazz) {
		return new JakeClassLoader((URLClassLoader) clazz.getClassLoader());
	}

	/**
	 * Return the {@link URLClassLoader} wrapped by this object.
	 */
	public URLClassLoader classloader() {
		return delegate;
	}

	/**
	 * Returns the class loader parent of this one.
	 */
	public JakeClassLoader parent() {
		return new JakeClassLoader((URLClassLoader) this.delegate.getParent());
	}

	/**
	 * @see #createChild(Iterable).
	 */
	public JakeClassLoader createChild(File...urls) {
		return new JakeClassLoader(new URLClassLoader(toUrl(Arrays.asList(urls)), this.delegate));
	}

	/**
	 * Creates a class loader, child of this one and having the specified entries.
	 */
	public JakeClassLoader createChild(Iterable<File> urls) {
		return new JakeClassLoader(new URLClassLoader(toUrl(urls), this.delegate));
	}

	/**
	 * Creates a class loader having the same parent and the same entries as this one plus the specified entries.
	 */
	public JakeClassLoader and(Iterable<File> files) {
		return parent().createChild(this.childClasspath().and(files));
	}

	/**
	 * @see #and(Iterable).
	 */
	public JakeClassLoader and(File...files) {
		return and(Arrays.asList(files));
	}

	/**
	 * Returns the classpath of this classloader without mentioning classpath of the parent classloaders.
	 */
	public JakeClasspath childClasspath() {
		final List<File> result = new ArrayList<File>(this.delegate.getURLs().length);
		for (final URL url : this.delegate.getURLs()) {
			result.add(new File(url.getFile().replaceAll("%20", " ")));
		}
		return JakeClasspath.of(result);
	}

	/**
	 * Returns the complete classpath of this classloader.
	 */
	public JakeClasspath fullClasspath() {
		final JakeClasspath classpath;
		if (this.delegate.getParent() != null) {
			classpath = this.parent().fullClasspath();
		} else {
			classpath = JakeClasspath.of();
		}
		final List<File> result = new ArrayList<File>(this.delegate.getURLs().length);
		for (final URL url : this.delegate.getURLs()) {
			result.add(new File(url.getFile().replaceAll("%20", " ")));
		}
		return classpath.and(result);
	}


	/**
	 * Delegates the call to {@link ClassLoader#loadClass(String)} of this wrapped <code>class loader</code>.<br/>
	 * The specified class is supposed to be defined in this class loader, otherwise an
	 * {@link IllegalArgumentException} is thrown.
	 * 
	 * @see #loadIfExist(String) #isDefined(String)
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> Class<T> load(String className) {
		try {
			return (Class<T>) delegate.loadClass(className);
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException("Class " + className + " not found on " + this, e);
		}
	}

	/**
	 * Loads a class given its source relative path. For example <code>loadGivenSourcePath("mypack1/subpack/MyClass.java")
	 * will load the class <code>mypack1.subpack.MyClass</code>.
	 */
	public <T extends Object> Class<T> loadGivenClassSourcePath(String classSourcePath) {
		final String className = classSourcePath.replace('/', '.').replace('\\', '.').substring(0, classSourcePath.length()-JAVA_SUFFIX_LENGTH);
		return load(className);
	}

	/**
	 * Loads the class having the specified name or return <code>null</code> if no such class is defined in this <code>class loader</code>.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Object> Class<T> loadIfExist(String className) {
		try {
			return (Class<T>) delegate.loadClass(className);
		} catch (final ClassNotFoundException e) { // NOSONAR
			return null;
		}
	}

	/**
	 * Returns if the specified class is defined in this <code>class loader</code>.
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
	 * Loads the class having the specified full name or the specified simple name.
	 * Returns <code>null</code> if no class matches. </br>
	 * For example : loadFromNameOrSimpleName("MyClass", null) may return the class my.pack.MyClass.
	 * 
	 * @param name The full name or the simple name of the class to load
	 * @param superClassArg If not null, the search is narrowed to classes/interfaces children of this class/interface.
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

		} catch (final ClassNotFoundException e) {  //NOSONAR
			final Set<Class<?>> classes = loadClasses("**/"+name);
			for (final Class<?> clazz : classes) {
				if (clazz.getSimpleName().equals(name) && superClass == null || superClass.isAssignableFrom(clazz)) {
					return (Class<? extends T>) clazz;

				}
			}
		}
		return null;
	}

	/**
	 * Returns all classes of this <code>classloader</code> that are defined in entries matching
	 * the specified fileFilter.</br>
	 * For example : if you want to load all classes that are defined in folder and not in jar file,
	 * you have to provide a <code>FileFilter</code> which includes only directories.
	 * 
	 * @param entryFilter The classpath entry filter. Can be <code>null</code>.
	 */
	public Set<Class<? extends Object>> loadClassesInEntries(FileFilter entryFilter) {
		final List<File> classfiles = new LinkedList<File>();
		final Map<File, File> file2Entry = new HashMap<File, File>();
		for (final File file : childClasspath()) {
			if (entryFilter == null || entryFilter.accept(file) && file.isDirectory()) {
				final List<File> files = JakeUtilsFile.filesOf(file, CLASS_FILE_FILTER, false);
				classfiles.addAll(files);
				JakeUtilsIterable.putMultiEntry(file2Entry, files, file);
			}
		}
		final Set<Class<?>> result = new HashSet<Class<?>>();
		for (final File file : classfiles) {
			final File entry = file2Entry.get(file);
			final String relativeName = JakeUtilsFile.getRelativePath(entry, file);
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
	 * Loads all class having a relative path matching the supplied {@link JakeFileFilter}.
	 * For example, if you want to load all class belonging to <code>my.pack</code> or its sub package,
	 * then you have to supply a filter with an include pattern as <code>my/pack/&#42;&#42;/&#42;.class</code>.
	 * Note that ending with <code>.class</code> is important.
	 */
	public Set<Class<?>> loadClasses(JakeFileFilter classFileFilter) {
		final Set<Class<?>> result = new HashSet<Class<?>>();
		final Set<String> classFiles = this.fullClasspath().allItemsMatching(classFileFilter);
		for (final String classFile : classFiles) {
			final String className = getAsClassName(classFile);
			result.add(this.load(className));
		}
		return result;
	}

	/**
	 * Loads all class having a relative path matching the supplied ANT pattern.
	 * For example, if you want to load all class belonging to <code>my.pack</code> or its sub package,
	 * then you have to supply a the following pattern <code>my/pack/&#42;&#42;/&#42;</code>.
	 * 
	 * @see JakeClassLoader#loadClasses(JakeFileFilter)
	 */
	public Set<Class<?>> loadClasses(String includingPattern) {
		return loadClasses(JakeFileFilter.include(includingPattern+ ".class"));
	}

	/**
	 * Returns all classes of this <code>classloader</code> that are defined inside the provided <code>JakeDirSet</code>.
	 * 
	 * @see JakeClassLoader#loadClassesInEntries(FileFilter)
	 */
	public Set<Class<? extends Object>> loadClassesIn(JakeDirSet jakeDirSet) {
		final Set<Class<?>> result = new HashSet<Class<?>>();
		for (final String path : jakeDirSet.relativePathes()) {
			if (path.endsWith(".class")) {
				final String className = getAsClassName(path);
				result.add(this.load(className));
			}
		}
		return result;
	}


	private static URL[] toUrl(Iterable<File> files) {
		final List<URL> urls = new ArrayList<URL>();
		for (final File file : files) {
			try {
				urls.add(file.toURI().toURL());
			} catch (final MalformedURLException e) {
				throw new IllegalArgumentException(file.getPath() + " is not convertible to URL");
			}
		}
		return urls.toArray(new URL[0]);
	}

	/**
	 * Transforms a class file name as <code>com/foo/Bar.class</code> to the corresponding class
	 * name <code>com.foo.Bar</code>.
	 */
	private static String getAsClassName(String resourceName) {
		return resourceName.replace(File.separatorChar, '.').replace("/", ".").substring(0, resourceName.length()-CLASS_SUFFIX_LENGTH);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(delegate.getClass().getName());
		if (delegate instanceof URLClassLoader) {
			for (final URL url : delegate.getURLs()) {
				builder.append("\n  " + url);
			}
		}
		if (delegate.getParent() != null) {
			builder.append("\n").append(this.parent());
		}
		return builder.toString();
	}

	/**
	 * Add a new entry to this class loader.
	 * WARN : This method has side effect on this class loader : use it with caution !
	 */
	public void addEntry(File entry) {
		final Method method = JakeUtilsReflect.getDeclaredMethod(URLClassLoader.class, "addURL", URL.class);
		JakeUtilsReflect.invoke(this.delegate, method, JakeUtilsFile.toUrl(entry));
	}

	/**
	 * Same as {@link #addEntry(File)} but for several entries.
	 */
	public void addEntries(Iterable<File> entries) {
		for (final File file : entries) {
			addEntry(file);
		}
	}

	/**
	 * Invokes a static method on the specified class using the provided arguments. <br/>
	 * If the argument classes are the same on the current class loader and this one then arguments are passed as is,
	 * otherwise arguments are serialize in the current class loader and  deserialized
	 * in this class loader in order to be compliant with it.
	 */
	@SuppressWarnings("unchecked")
	public <T> T invokeStaticMethod(String className, String methodName, Object... args) {
		final Class<?> clazz = this.load(className);
		final Object[] effectiveArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			effectiveArgs[i] = traverseClassLoader(args[i], this);
		}
		offsetJakeLog();
		final Object returned = JakeUtilsReflect.invokeStaticMethod(clazz, methodName, effectiveArgs);
		return (T) traverseClassLoader(returned, JakeClassLoader.current());
	}

	/**
	 * Creates an instance of the class having the specified name in this class loader.
	 */
	@SuppressWarnings("unchecked")
	public <T> T newInstance(String className) {
		final Class<?> clazz = this.load(className);
		return (T) JakeUtilsReflect.newInstance(clazz);
	}

	private void offsetJakeLog() {
		if (this.isDefined(JakeLog.class.getName())) {
			final int offset = JakeLog.offset();
			final Class<?> toClass = this.load(JakeLog.class.getName());
			JakeUtilsReflect.invokeStaticMethod(toClass, "shift", offset);
		}
	}

	private static Object traverseClassLoader(Object object, JakeClassLoader to) {
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

		final JakeClassLoader from = JakeClassLoader.of(object.getClass());
		final Class<?> toClass = to.load(className);
		if (from.delegate == null && toClass.getClassLoader() == null) {
			return object;
		}
		if ( from.load(className).equals(toClass)) {
			return object;
		}
		return JakeUtilsIO.cloneBySerialization(object, to.classloader());
	}

}
