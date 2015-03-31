package org.jake;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jake.utils.JakeUtilsReflect;

/**
 * Set of plugins configured or activated in a {@link JakeBuild}.
 */
public final class JakeBuildPlugins {

	private final JakeBuild holder;

	private final Map<Class<? extends JakeBuildPlugin>, JakeBuildPlugin> configuredPlugins = new LinkedHashMap<Class<? extends JakeBuildPlugin>, JakeBuildPlugin>();

	private final Map<Class<? extends JakeBuildPlugin>, JakeBuildPlugin> activatedPlugins = new LinkedHashMap<Class<? extends JakeBuildPlugin>, JakeBuildPlugin>();

	JakeBuildPlugins(JakeBuild holder) {
		super();
		this.holder = holder;
	}

	/**
	 * Add and activate the specified plugin for the holding build.
	 * Activate means that the plugin will be executed whenever it redefine an extension point.
	 */
	public void addActivated(JakeBuildPlugin plugin) {
		if (!accept(plugin)) {
			return;
		}
		plugin.configure(holder);
		activatedPlugins.put(plugin.getClass(),  plugin);
	}

	/**
	 * Add and configure the specified plugin for the holding build.
	 * Configure means that the plugin will not be executed at extension point. But it
	 * is on the specified instance that method may be invoked on.
	 */
	public void addConfigured(JakeBuildPlugin plugin) {
		if (!accept(plugin)) {
			return;
		}
		plugin.configure(holder);
		configuredPlugins.put(plugin.getClass(), plugin);
	}


	JakeBuildPlugin addActivated(Class<? extends JakeBuildPlugin> exactPluginClass, Map<String, String> options) {
		final JakeBuildPlugin plugin = getOrCreate(exactPluginClass);
		JakeOptions.populateFields(plugin, options);
		addActivated(plugin);
		return plugin;
	}

	JakeBuildPlugin addConfigured(Class<? extends JakeBuildPlugin> exactPluginClass, Map<String, String> options) {
		final JakeBuildPlugin plugin = getOrCreate(exactPluginClass);
		JakeOptions.populateFields(plugin, options);
		addConfigured(plugin);
		return plugin;
	}

	/**
	 * Returns all the activated plugins for the holding plugin.
	 */
	public  List<JakeBuildPlugin> getActives() {
		return new ArrayList<JakeBuildPlugin>(this.activatedPlugins.values());
	}

	List<JakeBuildPlugin> getConfiguredPlugins() {
		return new ArrayList<JakeBuildPlugin>(this.configuredPlugins.values());
	}

	void invoke(Class<? extends JakeBuildPlugin> exactPluginClass, String method) {
		if (!JakeUtilsReflect.isMethodPublicIn(exactPluginClass, method)) {
			throw new JakeException("No zero-arg public method found in " + exactPluginClass.getName() );
		}
		JakeBuildPlugin buildPlugin = this.activatedPlugins.get(exactPluginClass);
		if (buildPlugin == null) {
			buildPlugin = this.configuredPlugins.get(exactPluginClass);
		}
		if (buildPlugin == null) {
			buildPlugin = JakeUtilsReflect.newInstance(exactPluginClass);
			buildPlugin.configure(holder);
		}
		JakeUtilsReflect.invoke(buildPlugin, method);
	}

	private boolean accept(JakeBuildPlugin plugin) {
		return plugin.baseBuildClass().isAssignableFrom(holder.getClass());
	}

	private JakeBuildPlugin getOrCreate(Class<? extends JakeBuildPlugin> exactPluginClass) {
		final JakeBuildPlugin plugin;
		if (activatedPlugins.containsKey(exactPluginClass)) {
			plugin = activatedPlugins.get(exactPluginClass);
		} else if (configuredPlugins.containsKey(exactPluginClass)) {
			plugin = configuredPlugins.get(exactPluginClass);
		} else 	{
			plugin = JakeUtilsReflect.newInstance(exactPluginClass);
		}
		return plugin;
	}

	@SuppressWarnings("unchecked")
	public <T extends JakeBuildPlugin> T findInstanceOf(Class<T> pluginClass) {
		for (final JakeBuildPlugin jakeBuildPlugin : this.activatedPlugins.values()) {
			if (pluginClass.isAssignableFrom(jakeBuildPlugin.getClass())) {
				return (T) jakeBuildPlugin;
			}
		}
		for (final JakeBuildPlugin jakeBuildPlugin : this.configuredPlugins.values()) {
			if (pluginClass.isAssignableFrom(jakeBuildPlugin.getClass())) {
				return ((T) jakeBuildPlugin);
			}
		}
		return null;
	}


}
