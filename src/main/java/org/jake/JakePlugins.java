package org.jake;

import java.lang.reflect.Modifier;
import java.util.Arrays;
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
 * This way class scanning keep to be efficient without declaring any base package to scan.
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

	public static List<Object> instantiatePlugins(Iterable<Class<Object>> templateClasses,
			Iterable<JakePluginSetup> setups) {
		final List<Object> result = new LinkedList<Object>();
		final Set<String> names = JakePluginSetup.names(setups);
		for (final Class<Object> templateClass : templateClasses) {
			final JakePlugins<Object> plugins = JakePlugins.of(templateClass);
			final Map<String, JakePluginDescription<Object>> pluginDescriptions = plugins.loadAllByNames(names);
			for (final String name : pluginDescriptions.keySet()) {
				final JakePluginDescription<Object> desc = pluginDescriptions.get(name);
				final Object plugin = JakeUtilsReflect.newInstance(desc.pluginClass());
				final JakePluginSetup setup = JakePluginSetup.findOrFail(name, setups);
				JakeOptions.populateFields(plugin, setup.options);
				result.add(plugin);
			}
		}
		return result;
	}

	/**
	 * Creates a {@link JakePlugins} for the specified extension points. That means, this instance
	 * will refer to all plugin extending the specified extension point in the
	 * @param templateClass
	 * @return
	 */
	static <T> JakePlugins<T> of(Class<T> templateClass) {
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
	public static List<JakePluginDescription<?>> declaredAsField(JakeBuild hostingInstance) {
		final List<JakePluginDescription<?>> result = new LinkedList<JakePluginDescription<?>>();
		final List<Class<Object>> templateClasses = hostingInstance.pluginTemplateClasses();
		for(final Class<Object> clazz : templateClasses) {
			final JakePlugins<Object> plugins = JakePlugins.of(clazz);
			result.addAll(plugins.getAll());
		}
		return result;
	}


	private Set<JakePluginDescription<T>> plugins;

	private final Class<T> templateClass;



	private JakePlugins(Class<T> extendingClass) {
		super();
		this.templateClass = extendingClass;
	}

	/**
	 * Returns all the plugins present in classpath for this template class.
	 */
	public Set<JakePluginDescription<T>> getAll() {
		if (plugins == null) {
			synchronized (this) {
				final Set<JakePluginDescription<T>> result = loadAllPlugins(templateClass);
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
	private JakePluginDescription<T> loadByName(String name) {
		if (!name.contains(".")) {
			final JakePluginDescription<T> result = loadPluginHavingShortName(templateClass, JakeUtilsString.capitalize(name));
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
	private Map<String, JakePluginDescription<T>> loadAllByNames(Iterable<String> names) {
		final Map<String, JakePluginDescription<T>> result = new HashMap<String, JakePlugins.JakePluginDescription<T>>();
		for (final String name : names) {
			final JakePluginDescription<T> plugin = loadByName(name);
			if (plugin != null) {
				result.put(name, plugin);
			}
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

	private static <T> Set<JakePluginDescription<T>> loadAllPlugins(Class<T> templateClass) {
		final String nameSuffix = templateClass.getSimpleName();
		return loadPlugins(templateClass, "**/*" + nameSuffix);
	}

	private static <T> JakePluginDescription<T> loadPluginHavingShortName(Class<T> templateClass, String shortName) {
		final String nameSuffix = templateClass.getSimpleName();
		final Set<JakePluginDescription<T>> set = loadPlugins(templateClass, "**/" + shortName + nameSuffix);
		if (set.size() > 1) {
			throw new JakeException("Several plugin have the same short name : '" + shortName + "'. Please disambiguate with using plugin long name (full class name)."
					+ " Following plugins have same shortName : " + set);
		}
		if (set.isEmpty()) {
			return null;
		}
		return set.iterator().next();
	}

	private static <T> JakePluginDescription<T> loadPluginsHavingLongName(Class<T> templateClass, String longName) {
		final Class<? extends T> pluginClass = JakeClassLoader.current().loadIfExist(longName);
		if (pluginClass == null) {
			return null;
		}
		return new JakePluginDescription<T>(templateClass, pluginClass);
	}

	private static <T> Set<JakePluginDescription<T>> loadPlugins(Class<T> templateClass, String pattern) {
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
	private static <T> Set<JakePluginDescription<T>> toPluginSet(Class<T> extendingClass, Iterable<Class<?>> classes) {
		final Set<JakePluginDescription<T>> result = new HashSet<JakePlugins.JakePluginDescription<T>>();
		for (final Class<?> clazz : classes) {
			result.add(new JakePluginDescription<T>(extendingClass, (Class<? extends T>) clazz));
		}
		return result;
	}


	public static class JakePluginDescription<T> {

		private static String shortName(Class<?> extendingClass, Class<?> clazz) {
			return JakeUtilsString.substringBeforeLast(clazz.getSimpleName(), extendingClass.getSimpleName());
		}

		private static String longName(Class<?> extendingClass, Class<?> clazz) {
			return clazz.getName();
		}

		private final String shortName;

		private final String fullName;

		private final Class<T> templateClass;

		private final Class<? extends T> clazz;


		public JakePluginDescription(Class<T> templateClass, Class<? extends T> clazz) {
			super();
			this.templateClass = templateClass;
			this.shortName = shortName(templateClass, clazz);
			this.fullName = longName(templateClass, clazz);
			this.clazz = clazz;
		}


		public String shortName() {
			return this.shortName;
		}

		public String longName() {
			return this.longName();
		}

		public Class<T> templateClass() {
			return templateClass;
		}

		public Class<? extends T> pluginClass() {
			return clazz;
		}

		public List<String> explanation() {
			if (this.clazz.getAnnotation(JakeDoc.class) == null) {
				return Collections.emptyList();
			}
			return Arrays.asList(this.clazz.getAnnotation(JakeDoc.class).value());
		}

		@Override
		public String toString() {
			return "name=" + this.shortName + "(" + this.fullName+ ")";
		}

	}

	public static class JakePluginSetup {

		public static Set<String> names(Iterable<JakePluginSetup> setups) {
			final Set<String> result = new HashSet<String>();
			for (final JakePluginSetup setup : setups) {
				result.add(setup.pluginName);
			}
			return result;
		}

		public static JakePluginSetup findOrFail(String name, Iterable<JakePluginSetup> setups) {
			for (final JakePluginSetup setup : setups) {
				if (name.equals(setup.pluginName)) {
					return setup;
				}
			}
			throw new IllegalArgumentException("No setup found with name " + name +" found in " + setups);
		}

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
