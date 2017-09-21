package org.jerkar.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsReflect;

/**
 * Set of plugins configured or activated in a {@link JkBuild}.
 */
public final class JkBuildPlugins {

    private final JkBuild holder;

    private final Map<Class<? extends JkBuildPlugin>, JkBuildPlugin> configuredPlugins = new LinkedHashMap<>();

    private final Map<Class<? extends JkBuildPlugin>, JkBuildPlugin> activatedPlugins = new LinkedHashMap<>();

    JkBuildPlugins(JkBuild holder) {
        super();
        this.holder = holder;
    }

    /**
     * Add and activate the specified plugin for the holding build. Activate
     * means that the plugin will be executed whenever it redefine an extension
     * point.
     */
    public void activate(JkBuildPlugin... plugins) {
        for (final JkBuildPlugin plugin : plugins) {
            if (!accept(plugin)) {
                continue;
            }
            plugin.configure(holder);
            activatedPlugins.put(plugin.getClass(), plugin);
        }
    }

    /**
     * Add and configure the specified plugin for the holding build. Configure
     * means that the plugin will not be executed at extension point. But it is
     * on the specified instance that method may be invoked on.
     */
    public void configure(JkBuildPlugin plugin) {
        if (!accept(plugin)) {
            return;
        }
        plugin.configure(holder);
        configuredPlugins.put(plugin.getClass(), plugin);
    }

    JkBuildPlugin addActivated(Class<? extends JkBuildPlugin> exactPluginClass,
            Map<String, String> options) {
        final JkBuildPlugin plugin = getOrCreate(exactPluginClass);
        JkOptions.populateFields(plugin, options);
        activate(plugin);
        return plugin;
    }

    JkBuildPlugin addConfigured(Class<? extends JkBuildPlugin> exactPluginClass,
            Map<String, String> options) {
        final JkBuildPlugin plugin = getOrCreate(exactPluginClass);
        JkOptions.populateFields(plugin, options);
        configure(plugin);
        return plugin;
    }

    /**
     * Returns all the activated plugins for the holding plugin.
     */
    public List<JkBuildPlugin> getActives() {
        return new ArrayList<>(this.activatedPlugins.values());
    }

    List<JkBuildPlugin> getConfiguredPlugins() {
        return new ArrayList<>(this.configuredPlugins.values());
    }

    void invoke(Class<? extends JkBuildPlugin> exactPluginClass, String method) {
        if (!JkUtilsReflect.isMethodPublicIn(exactPluginClass, method)) {
            throw new JkException("No zero-arg public method found in "
                    + exactPluginClass.getName());
        }
        JkBuildPlugin buildPlugin = this.activatedPlugins.get(exactPluginClass);
        if (buildPlugin == null) {
            buildPlugin = this.configuredPlugins.get(exactPluginClass);
        }
        if (buildPlugin == null) {
            buildPlugin = JkUtilsReflect.newInstance(exactPluginClass);
            buildPlugin.configure(holder);
        }
        JkLog.startUnderlined("Method " + method + " to plugin "
                + buildPlugin.getClass().getSimpleName());
        JkUtilsReflect.invoke(buildPlugin, method);
        JkLog.done();
    }

    private boolean accept(JkBuildPlugin plugin) {
        return plugin.baseBuildClass().isAssignableFrom(holder.getClass());
    }

    private JkBuildPlugin getOrCreate(Class<? extends JkBuildPlugin> exactPluginClass) {
        final JkBuildPlugin plugin;
        if (activatedPlugins.containsKey(exactPluginClass)) {
            plugin = activatedPlugins.get(exactPluginClass);
        } else if (configuredPlugins.containsKey(exactPluginClass)) {
            plugin = configuredPlugins.get(exactPluginClass);
        } else {
            plugin = JkUtilsReflect.newInstance(exactPluginClass);
        }
        return plugin;
    }


    /**
     * Returns plugin bound to this holder build and extending the specified class.
     */
    @SuppressWarnings("unchecked")
    public <T extends JkBuildPlugin> T findInstanceOf(Class<T> pluginClass) {
        for (final JkBuildPlugin jkBuildPlugin : this.activatedPlugins.values()) {
            if (pluginClass.isAssignableFrom(jkBuildPlugin.getClass())) {
                return (T) jkBuildPlugin;
            }
        }
        for (final JkBuildPlugin jkBuildPlugin : this.configuredPlugins.values()) {
            if (pluginClass.isAssignableFrom(jkBuildPlugin.getClass())) {
                return ((T) jkBuildPlugin);
            }
        }
        return null;
    }

}
