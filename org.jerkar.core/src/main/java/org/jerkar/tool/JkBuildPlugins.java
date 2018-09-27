package org.jerkar.tool;

import org.jerkar.api.system.JkException;
import org.jerkar.api.utils.JkUtilsReflect;

import java.util.*;

/**
 * Set of plugins configured or activated in a {@link JkBuild}.
 */
public final class JkBuildPlugins {

    private final JkBuild holder;

    private final List<JkPlugin> loadedPlugins = new LinkedList<>();

    private final List<PluginOptions> pluginOptionsList;

    JkBuildPlugins(JkBuild holder,  List<PluginOptions> pluginOptionsList) {
        super();
        this.holder = holder;
        this.pluginOptionsList = Collections.unmodifiableList(new ArrayList<>(pluginOptionsList));
    }

    /**
     * Returns the plugin instance of the specified class loaded in the holding JkBuild instance. If it does not hold
     * a plugin of the specified class at call time, the plugin is loaded then returned.
     */
    public <T extends JkPlugin> T get(Class<T> pluginClass) {
        return getOrCreate(pluginClass);
    }

    /**
     * Returns the plugin instance of the specified name loaded in the holding JkBuild instance. If it does not hold
     * a plugin of the specified name at call time, the plugin is loaded then returned.<br/>
     * Caution : this method may be significantly slower than {@link #get(Class)} as it may involve classpath scanning.
     */
    public JkPlugin get(String pluginName) {
        Optional<JkPlugin> optPlugin = loadedPlugins.stream().filter(plugin -> plugin.name().equals(pluginName)).findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        PluginDictionary.PluginDescription pluginDescription = PluginDictionary.loadByName(pluginName);
        if (pluginDescription == null) {
            return null;
        }
        return get(pluginDescription.pluginClass());
    }

    /**
     * Returns <code>true</code> if the specified plugin class has been loaded in the holding JkBuild instance.
     */
    public boolean hasLoaded(Class<? extends JkPlugin> pluginClass) {
        for (JkPlugin plugin : loadedPlugins) {
            if (plugin.getClass().equals(pluginClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list of all loaded plugins in the holding JkBuild instance.
     */
    public List<JkPlugin> all() {
        return Collections.unmodifiableList(loadedPlugins);
    }

    private <T extends JkPlugin> T getOrCreate(Class<T> pluginClass) {
        final Optional<T> optPlugin = (Optional<T>) this.loadedPlugins.stream().filter(
                (item) -> item.getClass().equals(pluginClass)).findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        final T plugin = JkUtilsReflect.newInstance(pluginClass, JkBuild.class, this.holder);
        injectOptions(plugin);
        loadedPlugins.add(plugin);
        return plugin;
    }

    void injectOptions(JkPlugin plugin) {
        JkOptions.populateFields(plugin, PluginOptions.options(plugin.name(), this.pluginOptionsList));
    }

    void loadCommandLinePlugins() {
        Iterable<PluginOptions> pluginOptionsList = Environment.commandLine.getPluginOptions();
        for (PluginOptions pluginOptions : pluginOptionsList){
            PluginDictionary.PluginDescription pluginDescription = PluginDictionary.loadByName(pluginOptions.pluginName);
            if (pluginDescription == null) {
                throw new JkException("No plugin found with name '" + pluginOptions.pluginName + "'.");
            }
            getOrCreate(pluginDescription.pluginClass());
        }
    }

}
