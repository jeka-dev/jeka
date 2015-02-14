package org.jake;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.JakePlugins.JakePlugin.JakePluginConfigurer;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

/**
 * Jake offers a very simple, yet powerful, plugin mechanism.<br/>
 * The plugins discovery is achieved by scanning the classpath.
 * Plugin classes are supposed to be named with a given suffix, according its type.
 * This way class scanning keep to be efficient and does not required to specify any base package.
 * 
 * For example, a plugin class of JakeJavaBuildPlugin type are supposed to be named 'my.package.XxxxxJakeJavaBuildPlugin.class'.
 * Xxxxx will be its short name, while my.package.XxxxxJakeJavaBuildPlugin will be its full name.
 * 
 * @author Jerome Angibaud
 */
public final class JakePlugins<T> implements Iterable<T> {

	private static final Map<Class<?>, Set<Class<?>>> CACHE = new HashMap<Class<?>, Set<Class<?>>>();

	public static <T> JakePlugins<T> of(Class<T> extendingClass) {
		final JakePlugins<T> result = new JakePlugins<T>(extendingClass);
		if (CACHE.containsKey(extendingClass)) {
			final Set<Class<?>> pluginClasses = CACHE.get(extendingClass);
			result.plugins = toPluginSet(extendingClass, pluginClasses);
		}
		return result;
	}

	public static List<JakePlugins<?>> declaredAsField(Object hostingInstance) {
		final List<Field> fields = JakeUtilsReflect.getAllDeclaredField(hostingInstance.getClass(), true);
		final List<JakePlugins<?>> result = new LinkedList<JakePlugins<?>>();
		for (final Field field : fields) {
			if (field.getType().equals(JakePlugins.class)) {
				final JakePlugins<?> plugins = JakeUtilsReflect.getFieldValue(hostingInstance, field);
				result.add(plugins);
			}
		}
		return result;
	}

	private Set<JakePlugin<T>> plugins;

	private final Class<T> extendingClass;

	private JakePluginConfigurer<T> configurer;

	private JakePlugins(Class<T> extendingClass) {
		super();
		this.extendingClass = extendingClass;
	}

	public Set<JakePlugin<T>> plugins() {
		if (plugins == null) {
			synchronized (this) {
				final Set<JakePlugin<T>> result = loadAllPlugins(extendingClass);
				if (configurer != null) {
					for (final JakePlugin<T> jakePlugin : result) {
						jakePlugin.configure(configurer);
					}
				}
				this.plugins = Collections.unmodifiableSet(result);
			}
		}
		return this.plugins;
	}

	/**
	 * Returns the plugin having a full name equals to the specified name.
	 * If not found, returns the plugin having a short name equals to the specified name.
	 * If not found, returns <code>null</code>.
	 */
	public JakePlugin<T> byName(String name) {
		for (final JakePlugin<T> jakePlugin : this.plugins()) {
			if (name.equals(jakePlugin.fullName)) {
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

	@Override
	public Iterator<T> iterator() {
		return toPluginInstances().iterator();
	}

	public void configureAll(JakePluginConfigurer<T> configurer) {
		this.configurer = configurer;
		synchronized (this.plugins) {
			this.plugins = null;
		}
	}

	private List<T> toPluginInstances() {
		final List<T> result = new LinkedList<T>();
		for (final JakePlugin<T> jakePlugin : this.plugins()) {
			result.add(jakePlugin.instance());
		}
		return result;
	}

	@Override
	public String toString() {
		if (this.plugins == null) {
			return "Not loaded (extending class = " + this.extendingClass + ")";
		}
		return this.plugins.toString();
	}

	private static <T> Set<JakePlugin<T>> loadAllPlugins(Class<T> extendingClass) {
		final String nameSuffix = extendingClass.getSimpleName();
		final Set<Class<?>> matchingClasses = JakeClassLoader.current().loadClasses("**/*"+nameSuffix);
		final Set<Class<?>> result = new HashSet<Class<?>>();
		for (final Class<?> candidate : matchingClasses) {
			if (extendingClass.isAssignableFrom(candidate)
					&& !Modifier.isAbstract(candidate.getModifiers())
					&& !candidate.equals(extendingClass)) {
				result.add(candidate);
			}
		}
		return toPluginSet(extendingClass, result);
	}

	@SuppressWarnings("unchecked")
	private static <T> Set<JakePlugin<T>> toPluginSet(Class<T> extendingClass, Iterable<Class<?>> classes) {
		final Set<JakePlugin<T>> result = new HashSet<JakePlugins.JakePlugin<T>>();
		for (final Class<?> clazz : classes) {
			result.add(new JakePlugin<T>(extendingClass, (Class<? extends T>) clazz));
		}
		return result;
	}


	public static class JakePlugin<T> {

		private static String shortName(Class<?> extendingClass, Class<?> clazz) {
			return JakeUtilsString.substringBeforeLast(clazz.getSimpleName(), extendingClass.getSimpleName());
		}

		private static String longName(Class<?> extendingClass, Class<?> clazz) {
			return JakeUtilsString.substringBeforeLast(clazz.getName(), extendingClass.getSimpleName());
		}

		private final String shortName;

		private final String fullName;

		private final Class<? extends T> extendingClass;

		private final Class<? extends T> clazz;

		private T instance;

		private JakePluginConfigurer<T> configurer;


		public void configure(JakePluginConfigurer<T> configurer) {
			this.configurer = configurer;
			synchronized (this.instance) {
				instance = null;
			}
		}

		public JakePlugin(Class<T> extendingClass, Class<? extends T> clazz) {
			super();
			this.extendingClass = extendingClass;
			this.shortName = shortName(extendingClass, clazz);
			this.fullName = longName(extendingClass, clazz);
			this.clazz = clazz;
		}

		public Class<? extends T> pluginClass() {
			return this.clazz;
		}

		public T instance() {
			if (instance == null) {
				synchronized (this.instance) {
					T result = JakeUtilsReflect.newInstance(clazz);
					if (configurer != null) {
						result = this.configurer.configure(result);
					}
					instance = result;
				}
			}
			return instance;
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

		@Override
		public String toString() {
			return "name=" + this.shortName + ", fullName=" + this.fullName+ ", inheriting from " + this.extendingClass.getName();
		}

		public static interface JakePluginConfigurer<T> {

			T configure(T plugin);

		}

	}


}
