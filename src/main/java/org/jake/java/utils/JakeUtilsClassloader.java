package org.jake.java.utils;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.file.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsIterable;

/**
 * Convenient methods to deal with <code>ClassLoader</codes>.
 */
public class JakeUtilsClassloader {

	/**
	 * A {@link FileFilter} accepting only .class files.
	 */
	public static final FileFilter CLASS_FILE_FILTER = new FileFilter() {

		@Override
		public boolean accept(File file) {
			return (file.isFile() && file.getName().endsWith(".class"));
		}
	};

	/**
	 * Returns the current classLoader as a <code>UrlClassLoader</code>. Fails if the current classloader is
	 * not an <code>UrlClassLoader</code>.
	 */
	public static URLClassLoader current() {
		return (URLClassLoader) JakeUtilsClassloader.class.getClassLoader();
	}

	public static URLClassLoader createFrom(Iterable<File> entries) {
		final List<URL> urls = new LinkedList<URL>();
		for(final File file : entries) {
			try {
				urls.add(file.toURI().toURL());
			} catch (final MalformedURLException e) {
				throw new IllegalStateException(e);
			}
		}
		return new URLClassLoader(urls.toArray(new URL[0]));
	}

	public static URLClassLoader createFrom(Iterable<File> entries, ClassLoader parent) {
		final List<URL> urls = new LinkedList<URL>();
		for(final File file : entries) {
			try {
				urls.add(file.toURI().toURL());
			} catch (final MalformedURLException e) {
				throw new IllegalStateException(e);
			}
		}
		return new URLClassLoader(urls.toArray(new URL[0]), parent);
	}

	/**
	 * Returns the files that constitute the provided <code>UrlClassLoader</code>.
	 * Urls not being file or folder are ignored.
	 */
	public static List<File> getUrlsAsFiles(URLClassLoader urlClassLoader) {
		final List<File> result = new LinkedList<File>();
		for (final URL url : urlClassLoader.getURLs()) {
			try {
				final File file = new File(url.toURI());
				result.add(file);
			} catch (final URISyntaxException e) {
				// ignore as we want only files
			}

		}
		return result;
	}

	/**
	 * Given a <code>UrlClassLoader</code>, returns urls that stands for directory under the base
	 * directory passed as parameter.
	 */
	public static List<File> getFolderClassEntriesUnder(File baseDir, URLClassLoader classLoader) {
		final List<File> entries = JakeUtilsClassloader.getUrlsAsFiles(classLoader);
		final List<File> result = new LinkedList<File>();
		for (final File file : entries) {
			if (file.isDirectory()
					&& (JakeUtilsFile.isAncestor(baseDir, file) || baseDir
							.equals(file))) {
				result.add(file);
			}
		}
		return result;
	}

	/**
	 * Returns if the given className is declared under the directory passed as parameter.
	 * 
	 * @param classDir A directory containing java code. This stands for the folder containing the
	 * root package (e.g com, org,...) and not the folder directly containing the .class file.
	 * @param className The class name we are looking for.
	 * 
	 */
	public static boolean isEntryContainsClass(File classDir, String className) {
		try {
			return isEntryContainsClass(classDir.toURI().toURL(), className);
		} catch (final MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns if the given className is declared under the url passed as parameter.
	 * @see #isEntryContainsClass(File, String)
	 */
	public static boolean isEntryContainsClass(URL url, String className) {
		final String baseClassName = className.replace('.', '/') + ".class";
		final URLClassLoader classLoader = new URLClassLoader(new URL[] { url });
		final boolean result = (classLoader.findResource(baseClassName) != null);
		return result;
	}

	/**
	 * Transforms a class file name as <code>com/foo/Bar.class</code> to the corresponding class
	 * name <code>com.foo.Bar</code>.
	 */
	public static String getAsClassName(String resourceName) {
		return resourceName.replace(File.separatorChar, '.').substring(0, resourceName.length()-6);
	}

	/**
	 * Returns all top level classes of a given entry of a <code>URLClassLoader</code>.
	 * 
	 * @param classLoader The <code>UrlClassLoader</code> we retrieve the class from.
	 * @param entryDirectory A directory which is an entry of the provided <code>ClassLoader</code>.
	 */
	@SuppressWarnings("rawtypes")
	public static Set<Class<?>> getAllTopLevelClasses(URLClassLoader classLoader, FileFilter entryFilter) {
		final List<File> classfiles = new LinkedList<File>();
		final Map<File, File> file2Entry = new HashMap<File, File>();
		for (final File file : getUrlsAsFiles(classLoader)) {
			if (entryFilter.accept(file)) {
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
				clazz = classLoader.loadClass(className);
			} catch (final ClassNotFoundException e) {
				throw new IllegalStateException("Can't find the class " + className, e);
			}
			result.add(clazz);
		}
		return result;
	}

	public static Class<?> loadClass(ClassLoader classLoader, String className) {
		try {
			return classLoader.loadClass(className);
		} catch (final ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String toString(ClassLoader classLoader) {
		final StringBuilder builder = new StringBuilder();
		builder.append(classLoader.getClass().getName());
		if (classLoader instanceof URLClassLoader) {
			final URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
			for (final URL url : urlClassLoader.getURLs()) {
				builder.append("\n  " + url);
			}
		}
		if (classLoader.getParent() != null) {
			builder.append("\n").append(toString(classLoader.getParent()));
		}
		return builder.toString();

	}

	public static URLClassLoader plus(URLClassLoader base, URL[] extra) {
		final List<URL> urls = new LinkedList<URL>(Arrays.asList(base.getURLs()));
		for (final URL url : extra) {
			urls.add(url);
		}
		final URL[] newUrls = urls.toArray(new URL[0]);
		return new URLClassLoader(newUrls, base.getParent());
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<? extends T> loadFromSimpleName(URLClassLoader classLoader, String name, Class<T> superClass) {
		try {
			if (superClass == null) {
				return (Class<? extends T>) classLoader.loadClass(name);
			}
			final Class<?> type = classLoader.loadClass(name);
			if (superClass.isAssignableFrom(type)) {
				return (Class<? extends T>) type;
			}
			return null;

		} catch (final ClassNotFoundException e) {
			final Set<Class<?>> classes = getAllTopLevelClasses(classLoader, JakeUtilsFile.acceptAll());
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


}
