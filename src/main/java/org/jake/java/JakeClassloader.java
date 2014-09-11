package org.jake.java;

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

import org.jake.file.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIO;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;

public class JakeClassloader {

	/**
	 * A {@link FileFilter} accepting only .class files.
	 */
	public static final FileFilter CLASS_FILE_FILTER = new FileFilter() {

		@Override
		public boolean accept(File file) {
			return (file.isFile() && file.getName().endsWith(".class"));
		}
	};

	private final URLClassLoader delegate;

	private JakeClassloader(URLClassLoader delegate) {
		this.delegate = delegate;
	}

	public static JakeClassloader current() {
		return new JakeClassloader((URLClassLoader) JakeClassloader.class.getClassLoader());
	}

	public static JakeClassloader system() {
		return new JakeClassloader((URLClassLoader) ClassLoader.getSystemClassLoader());
	}

	public static JakeClassloader of(Class<?> clazz) {
		return new JakeClassloader((URLClassLoader) clazz.getClassLoader());
	}

	public URLClassLoader classloader() {
		return delegate;
	}

	public JakeClassloader parent() {
		return new JakeClassloader((URLClassLoader) this.delegate.getParent());
	}

	public JakeClassloader createChild(File...urls) {
		return new JakeClassloader(new URLClassLoader(toUrl(Arrays.asList(urls)), this.delegate));
	}


	public JakeClassloader createChild(Iterable<File> urls) {
		return new JakeClassloader(new URLClassLoader(toUrl(urls), this.delegate));
	}

	public JakeClassloader and(Iterable<File> files) {
		return parent().createChild(this.getChildClasspath().and(files));
	}

	public JakeClassloader and(File...files) {
		return and(Arrays.asList(files));
	}

	/**
	 * Returns the classpath of this classloader without mentioning classpath of the parent classloaders.
	 */
	public JakeClasspath getChildClasspath() {
		final List<File> result = new ArrayList<File>(this.delegate.getURLs().length);
		for (final URL url : this.delegate.getURLs()) {
			result.add(new File(url.getFile().replaceAll("%20", " ")));
		}
		return JakeClasspath.of(result);
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> Class<T> load(String className) {
		try {
			return (Class<T>) delegate.loadClass(className);
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException("Class " + className + " not found on " + this, e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> Class<T> loadIfExist(String className) {
		try {
			return (Class<T>) delegate.loadClass(className);
		} catch (final ClassNotFoundException e) {
			return null;
		}
	}

	public boolean isDefined(String className) {
		try {
			delegate.loadClass(className);
			return true;
		} catch (final ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Load the class having the specified full name or the specified simple name
	 * if the specified name is not a full name. Returns <code>null</code> if no class matches. </br>
	 * For example : loadFromNameOrSimpleName("MyClass", null) may return the class my.pack.MyClass.
	 * 
	 * @param name The full name or the simple name of the class to load
	 * @param superClass If not null, the search is narrowed to classes/interfaces children of this class/interface.
	 * @return The loaded class or <code>null</code>.
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> loadFromNameOrSimpleName(String name, Class<T> superClass) {
		try {
			if (superClass == null) {
				return (Class<? extends T>) delegate.loadClass(name);
			}
			final Class<?> type = delegate.loadClass(name);
			if (superClass.isAssignableFrom(type)) {
				return (Class<? extends T>) type;
			}
			return null;

		} catch (final ClassNotFoundException e) {

			// TODO Not optimized. Should look only at class files having same name as the one provided.
			final Set<Class<?>> classes = getAllTopLevelClasses(null);
			for (final Class<?> clazz : classes) {
				if (clazz.getSimpleName().equals(name)) {
					if (superClass == null || superClass.isAssignableFrom(clazz)) {
						return (Class<? extends T>) clazz;
					}
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
	public Set<Class<?>> getAllTopLevelClasses(FileFilter entryFilter) {
		final List<File> classfiles = new LinkedList<File>();
		final Map<File, File> file2Entry = new HashMap<File, File>();
		for (final File file : getChildClasspath()) {
			if (entryFilter == null || entryFilter.accept(file)) {
				if (file.isDirectory()) {
					final List<File> files = JakeUtilsFile.filesOf(file, CLASS_FILE_FILTER, false);
					classfiles.addAll(files);
					JakeUtilsIterable.putMultiEntry(file2Entry, files, file);
				}
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


	private static URL[] toUrl(Iterable<File> files) {
		final ArrayList<URL> urls = new ArrayList<URL>();
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
		return resourceName.replace(File.separatorChar, '.').substring(0, resourceName.length()-6);
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
	 * This allow to access to protected method {@link URLClassLoader#addUrl}. Use it with caution !
	 */
	public static final void addUrl(URLClassLoader classLoader, File file) {
		final Method method = JakeUtilsReflect.getDeclaredMethod(URLClassLoader.class, "addURL", URL.class);
		JakeUtilsReflect.invoke(classLoader, method, JakeUtilsFile.toUrl(file));
	}


	@SuppressWarnings("unchecked")
	public <T> T invokeStaticMethod(String className, String methodName, Object... args) {
		final Class<?> clazz = this.load(className);
		final Object[] effectiveArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			effectiveArgs[i] = traverseClassLoader(args[i], this);
		}
		final Object returned = JakeUtilsReflect.invokeStaticMethod(clazz, methodName, effectiveArgs);
		final T result = (T) traverseClassLoader(returned, JakeClassloader.current());
		return result;

	}

	public <T> T invokeInstanceMethod(Object object, String methodName, Object... args) {

		final Object[] effectiveArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			effectiveArgs[i] = traverseClassLoader(args[i], this);
		}
		final Object returned = JakeUtilsReflect.invokeInstanceMethod(object, methodName, effectiveArgs);
		@SuppressWarnings("unchecked")
		final T result = (T) traverseClassLoader(returned, JakeClassloader.current());
		return result;

	}

	public Object newInstanceOf(String className) {
		return JakeUtilsReflect.newInstance(this.load(className));
	}

	private static Object traverseClassLoader(Object object, JakeClassloader to) {
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

		final JakeClassloader from = JakeClassloader.of(object.getClass());
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
