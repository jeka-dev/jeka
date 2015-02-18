package org.jake;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

/**
 * Jake offers a very simple, yet powerful, plugin mechanism.<br/>
 * The plugins discovery is achieved by scanning the classpath.
 * Plugin classes are supposed to be named with a given suffix, according its template class (the abstract class they are inheriting from).
 * This way class scanning keep to be efficient and does not required to specify any base package.
 * 
 * For example, a plugin class extending <code>or.jake.java.build.JakeJavaBuildPlugin</code> template is supposed to be named 'my.package.XxxxxJakeJavaBuildPlugin.class'.
 * Xxxxx will be its short name, while my.package.XxxxxJakeJavaBuildPlugin will be its full name. It is also supposed to
 * inherit for <code>or.jake.java.build.JakeJavaBuildPlugin</code>.
 * 
 * Instances of <code>JakePlugins</code>, define available plugins in the classpath extending a given template class.
 * 
 * @author Jerome Angibaud
 */
public final class JakePlugins<T>  {

	private static final Map<Class<?>, Set<Class<?>>> CACHE = new HashMap<Class<?>, Set<Class<?>>>();

	/**
	 * Creates a {@link JakePlugins} for the specified extension points. That means, this instance
	 * will refer to all plugin extending the specified extension point in the
	 * @param templateClass
	 * @return
	 */
	public static <T> JakePlugins<T> of(Class<T> templateClass) {
		final JakePlugins<T> result = new JakePlugins<T>(templateClass);
		if (CACHE.containsKey(templateClass)) {
			final Set<Class<?>> pluginClasses = CACHE.get(templateClass);
			result.plugins = toPluginSet(templateClass, pluginClasses);
		}
		return result;
	}

	/**
	 * Returns all <code>JakePlugins</code> instances declared as field in the specified instance.
	 * It includes fields declared in the specified instance class and the ones declared in its super classes.
	 */
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

	private final Class<T> templateClass;



	private JakePlugins(Class<T> extendingClass) {
		super();
		this.templateClass = extendingClass;
	}

	/**
	 * Returns all the plugins present in classpath for this template class.
	 */
	public Set<JakePlugin<T>> getAll() {
		if (plugins == null) {
			synchronized (this) {
				final Set<JakePlugin<T>> result = loadAllPlugins(templateClass);
				this.plugins = Collections.unmodifiableSet(result);
			}
		}
		return this.plugins;
	}

	/**
	 * Returns the plugin having a full name equals to the specified name.
	 * If not found, returns the plugin having a short name equals to the specified name. Note that the short name is
	 * capitalized for you so using "myPluging" or "MyPlugin" is equal.
	 * If not found, returns <code>null</code>.
	 */
	public JakePlugin<T> loadByName(String name) {
		if (!name.contains(".")) {
			final JakePlugin<T> result = loadPluginHavingShortName(templateClass, JakeUtilsString.capitalize(name));
			if (result != null) {
				return result;
			}
		}
		return loadPluginsHavingLongName(templateClass, name);
	}

	/**
	 * Returns all the plugins having one of the specified names (either it be its long name or its short name).
	 * 
	 * @see JakePlugins#loadByName(String)
	 */
	public Map<String, JakePlugin<T>> loadAllByNames(Iterable<String> names) {
		final Map<String, JakePlugin<T>> result = new HashMap<String, JakePlugins.JakePlugin<T>>();
		for (final String name : names) {
			final JakePlugin<T> plugin = loadByName(name);
			if (plugin != null) {
				result.put(name, plugin);
			}
		}
		return result;
	}

	public List<T> pluginInstances(Iterable<String> pluginNames) {
		final List<T> result = new LinkedList<T>();
		for (final JakePlugin<T> jakePlugin : this.loadAllByNames(pluginNames).values()) {
			result.add(jakePlugin.instance());
		}
		return result;
	}


	@Override
	public String toString() {
		if (this.plugins == null) {
			return "Not loaded (template class = " + this.templateClass + ")";
		}
		return this.plugins.toString();
	}

	private static <T> Set<JakePlugin<T>> loadAllPlugins(Class<T> templateClass) {
		final String nameSuffix = templateClass.getSimpleName();
		return loadPlugins(templateClass, "**/*" + nameSuffix);
	}

	private static <T> JakePlugin<T> loadPluginHavingShortName(Class<T> templateClass, String shortName) {
		final String nameSuffix = templateClass.getSimpleName();
		final Set<JakePlugin<T>> set = loadPlugins(templateClass, "**/" + shortName + nameSuffix);
		if (set.size() > 1) {
			throw new JakeException("Several plugin have the same short name : '" + shortName + "'. Please disambiguate with using plugin long name (full class name)."
					+ " Following plugins have same shortName : " + set);
		}
		if (set.isEmpty()) {
			return null;
		}
		return set.iterator().next();
	}

	private static <T> JakePlugin<T> loadPluginsHavingLongName(Class<T> templateClass, String longName) {
		final Class<? extends T> pluginClass = JakeClassLoader.current().loadIfExist(longName);
		if (pluginClass == null) {
			return null;
		}
		return new JakePlugin<T>(templateClass, pluginClass);
	}

	private static <T> Set<JakePlugin<T>> loadPlugins(Class<T> templateClass, String pattern) {
		final Set<Class<?>> matchingClasses = JakeClassLoader.current().loadClasses(pattern);
		final Set<Class<?>> result = new HashSet<Class<?>>();
		for (final Class<?> candidate : matchingClasses) {
			if (templateClass.isAssignableFrom(candidate)
					&& !Modifier.isAbstract(candidate.getModifiers())
					&& !candidate.equals(templateClass)) {
				result.add(candidate);
			}
		}
		return toPluginSet(templateClass, result);
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
				synchronized (this) {
					instance = JakeUtilsReflect.newInstance(clazz);
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

	}

	public static class JakePluginSetup {

		public static JakePluginSetup of(String name, Map<String, String> options) {
			return new JakePluginSetup(name, new HashMap<String, String>(options));
		}

		@SuppressWarnings("unchecked")
		public static JakePluginSetup of(String name) {
			return new JakePluginSetup(name, Collections.EMPTY_MAP);
		}

		public final String pluginName;

		public final Map<String, String> options;

		private JakePluginSetup(String pluginName, Map<String, String> options) {
			super();
			this.pluginName = pluginName;
			this.options = Collections.unmodifiableMap(options);
		}

		public JakePluginSetup with(String key, String value) {
			final Map<String, String> map = new HashMap<String, String>(options);
			map.put(key, value);
			return new JakePluginSetup(pluginName, map);
		}

	}


}
