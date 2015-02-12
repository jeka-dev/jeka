package org.jake;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jake.utils.JakeUtilsString;

/**
 * Jake offers a very simple, yet powerfull, Plugin mechanism.<br/>
 * The plugins discovery is achieved by scanning the classpath.
 * Plugin classes are supposed to be named with a given suffix, according its type.
 * This way class scanning keep to be efficient and does not required to specify any base package.
 * 
 * For example, a plugin class of JakeJavaBuildPlugin type are supposed to be named 'my.package.XxxxxJakeJavaBuildPlugin.class'.
 * Xxxxx will be its short name, while my.package.Xxxxx will be its long name.
 * 
 * @author Jerome Angibaud
 */
public final class JakePlugins<T> {

	private static final Map<Class<?>, JakePlugins<?>> CACHE = new HashMap<Class<?>, JakePlugins<?>>();

	@SuppressWarnings("unchecked")
	public static <T> JakePlugins<T> of(Class<T> extendingClass) {
		if (CACHE.containsKey(extendingClass)) {
			return (JakePlugins<T>) CACHE.get(extendingClass);
		}
		final JakePlugins<T> jakePlugins = new JakePlugins<T>(extendingClass);
		CACHE.put(extendingClass, jakePlugins);
		return jakePlugins;
	}

	@SuppressWarnings("unchecked")
	private static <T> Set<JakePlugin<T>> from(Class<T> extendingClass) {
		final String nameSuffix = extendingClass.getSimpleName();
		final FileFilter fileFilter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(nameSuffix + ".class");
			}
		};
		final Set<Class<?>> matchingClasses = JakeClassLoader.current().loadClasses(fileFilter);
		final Set<Class<? extends T>> result = new HashSet<Class<? extends T>>();
		for (final Class<?> candidate : matchingClasses) {
			if (extendingClass.isAssignableFrom(candidate)) {
				result.add((Class<? extends T>) candidate);
			}
		}
		return from(nameSuffix, result);
	}

	private static <T> Set<JakePlugin<T>> from(String suffix, Iterable<Class<? extends T>> classes) {
		final Set<JakePlugin<T>> result = new HashSet<JakePlugins.JakePlugin<T>>();
		for (final Class<? extends T> clazz : classes) {
			result.add(new JakePlugin<T>(suffix, clazz));
		}
		return result;
	}

	private Set<JakePlugin<T>> plugins;

	private final Class<T> extendingClass;

	private JakePlugins(Class<T> extendingClass) {
		super();
		this.extendingClass = extendingClass;
	}

	private Set<JakePlugin<T>> plugins() {
		if (plugins == null) {
			plugins = from(extendingClass);
		}
		return plugins;
	}


	/**
	 * Returns the plugin having a long name equals to the specified name.
	 * If not found, returns the plugin having a short name equals to the specified name.
	 * If not found, returns <code>null</code>.
	 */
	public JakePlugin<T> byName(String name) {
		for (final JakePlugin<T> jakePlugin : this.plugins()) {
			if (name.equals(jakePlugin.longName)) {
				return jakePlugin;
			}
		}
		for (final JakePlugin<T> jakePlugin : this.plugins()) {
			if (name.equals(jakePlugin.shortName)) {
				return jakePlugin;
			}
		}
		return null;
	}


	public static class JakePlugin<T> {

		private static String shortName(String suffix, Class<?> clazz) {
			return JakeUtilsString.substringBeforeLast(clazz.getSimpleName(), suffix);
		}

		private static String longName(String suffix, Class<?> clazz) {
			return JakeUtilsString.substringBeforeLast(clazz.getName(), suffix);
		}

		private final String shortName;

		private final String longName;

		private final Class<? extends T> clazz;

		public JakePlugin(String suffix, Class<? extends T> clazz) {
			this(shortName(suffix, clazz), longName(suffix, clazz), clazz);
		}

		public JakePlugin(String shortName, String longName, Class<? extends T> clazz) {
			super();
			this.shortName = shortName;
			this.longName = longName;
			this.clazz = clazz;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final JakePlugin<T> other = (JakePlugin<T>) obj;
			if (clazz == null) {
				if (other.clazz != null) {
					return false;
				}
			} else if (!clazz.equals(other.clazz)) {
				return false;
			}
			return true;
		}



	}



}
