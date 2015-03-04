package org.jake;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jake.utils.JakeUtilsReflect;

/**
 * Set of plugins configured or activated in a {@link JakeBuild}.
 */
public final class JakeBuildPlugins {

	private final Map<Class<? extends JakeBuildPlugin>, JakeBuildPlugin> configuredPlugins = new LinkedHashMap<Class<? extends JakeBuildPlugin>, JakeBuildPlugin>();

	private final Map<Class<? extends JakeBuildPlugin>, JakeBuildPlugin> activatedPlugins = new LinkedHashMap<Class<? extends JakeBuildPlugin>, JakeBuildPlugin>();

	public void addhActivated(Class<? extends JakeBuildPlugin> exactPluginClass, Map<String, String> options) {
		final JakeBuildPlugin plugin;
		if (activatedPlugins.containsKey(exactPluginClass)) {
			plugin = activatedPlugins.get(exactPluginClass);
		} else if (configuredPlugins.containsKey(exactPluginClass)) {
			plugin = configuredPlugins.get(exactPluginClass);
		} else 	{
			plugin = JakeUtilsReflect.newInstance(exactPluginClass);
		}
		JakeOptions.populateFields(plugin, options);
		addActivated(plugin);
	}

	public void addActivated(JakeBuildPlugin plugin) {
		activatedPlugins.put(plugin.getClass(),  plugin);
	}

	public void addConfigured(JakeBuildPlugin plugin) {
		configuredPlugins.put(plugin.getClass(), plugin);
	}

	@SuppressWarnings({ "unchecked" })
	public <T extends JakeBuildPlugin> List<T> get(Class<T> clazz) {
		final List<T> result = new LinkedList<T>();
		for (final Map.Entry<Class<? extends JakeBuildPlugin>, JakeBuildPlugin> entry : this.activatedPlugins.entrySet()) {
			if (clazz.isAssignableFrom(entry.getKey())) {
				result.add((T) entry.getValue());
			}
		}
		return result;
	}


}
