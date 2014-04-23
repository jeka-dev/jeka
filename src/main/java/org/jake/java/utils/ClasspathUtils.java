package org.jake.java.utils;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.JakeBaseBuild;
import org.jake.java.JakeJarBuild;
import org.jake.utils.FileUtils;
import org.jake.utils.IterableUtils;

public final class ClasspathUtils {

	public static final String JAKE_HINT_CLASS = JakeBaseBuild.class.getName();

	public static final String LOMBOK_HINT_CLASS = "com.lombok.Lombok";

	public static final String JUNIT_HINT_CLASS = "org.junit.package-info";
	
	public static final FileFilter CLASS_FILE_FILTER = new FileFilter() {
		
		@Override
		public boolean accept(File file) {
			return (file.isFile() && file.getName().endsWith(".class"));
		}
	}; 

	public static List<File> getSearchPath(URLClassLoader urlClassLoader) {
		List<File> result = new LinkedList<File>();
		for (URL url : urlClassLoader.getURLs()) {
			try {
				File file = new File(url.toURI());
				result.add(file);
			} catch (URISyntaxException e) {
				// ignore as we want only files
			}

		}
		return result;
	}
	
	

	public static URLClassLoader getRunningJakeClassLoader() {
		return (URLClassLoader) JakeJarBuild.class.getClassLoader();
	}

	public static List<File> getSearchPathMinusEntriesContaingAnyOf(
			URLClassLoader urlClassLoader, String... classNames) {
		List<File> result = getSearchPath(urlClassLoader);
		for (String className : classNames) {
			result.removeAll(getEntriesContaingClass(result, className));
		}
		return result;
	}

	public static List<File> getDefaultEmbededDependencies(File projectDir) {
		URLClassLoader classLoader = getRunningJakeClassLoader();
		return getSearchPathMinusEntriesContaingAnyOf(classLoader,
				JAKE_HINT_CLASS, LOMBOK_HINT_CLASS, JUNIT_HINT_CLASS);
	}

	public static List<File> getClassEntryInsideProject(File projectDir) {
		List<File> entries = getSearchPath(getRunningJakeClassLoader());
		List<File> result = new LinkedList<File>();
		for (File file : entries) {
			if (file.isDirectory()
					&& (FileUtils.isAncestor(projectDir, file) || projectDir
							.equals(file))) {
				result.add(file);
			}
		}
		return result;
	}

	public static List<File> getEntriesContaingClass(Iterable<File> entries,
			String className) {
		List<File> result = new LinkedList<File>();
		for (File file : entries) {
			if (isEntryContainsClass(file, className)) {
				result.add(file);
			}
		}
		return result;
	}

	public static boolean isEntryContainsClass(File file, String className) {
		try {
			return isEntryContainsClass(file.toURI().toURL(), className);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isEntryContainsClass(URL url, String className) {
		String baseClassName = className.replace('.', '/') + ".class";
		URLClassLoader classLoader = new URLClassLoader(new URL[] { url });
		boolean result = (classLoader.findResource(baseClassName) != null);
		return result;
	}
	
	/**
	 * Transforms a class file name as <code>com/foo/Bar.class</code> to the corresponding class 
	 * name <code>com.foo.Bar</code>.
	 */
	public static String getAsClassName(String resourceName) {
		return resourceName.replace(File.separatorChar, '.').substring(0, resourceName.length()-6);
	}
	
	@SuppressWarnings("rawtypes")
	public static Class[] getAllTopLevelClassesAsArray(ClassLoader classLoader, File entryDirectory) {
		return IterableUtils.asArray(getAllTopLevelClasses(classLoader, entryDirectory), Class.class);
	}

	@SuppressWarnings("rawtypes")
	public static Set<Class> getAllTopLevelClasses(ClassLoader classLoader, File entryDirectory) {
		FileUtils.assertDir(entryDirectory);
		List<File> classfiles = FileUtils.filesOf(entryDirectory, CLASS_FILE_FILTER, false);
		Set<Class> result = new HashSet<Class>();
		for (File file : classfiles) {
			String relativeName = FileUtils.getRelativePath(entryDirectory, file);
			String className = getAsClassName(relativeName);
			Class<?> clazz;
			try {
				clazz = classLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("Can't find the class " + className, e);
			}
			result.add(clazz);
		}
		return result;
	}
}
