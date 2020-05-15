package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Set of plugin instances loaded in a {@link JkCommandSet}.
 */
public final class JkCommandSetPlugins {

    private final JkCommandSet holder;

    private final List<JkPlugin> loadedPlugins = new LinkedList<>();

    private final List<PluginOptions> pluginOptionsList;

    JkCommandSetPlugins(JkCommandSet holder, List<PluginOptions> pluginOptionsList) {
        super();
        this.holder = holder;
        this.pluginOptionsList = Collections.unmodifiableList(new ArrayList<>(pluginOptionsList));
    }

    /**
     * Returns the plugin instance of the specified class loaded in the holding JkCommandSet instance. If it does not hold
     * a plugin of the specified class at call time, the plugin is loaded then returned.
     */
    public <T extends JkPlugin> T get(Class<T> pluginClass) {
        return getOrCreate(pluginClass);
    }

    /**
     * Returns the plugin instance of the specified name loaded in the holding JkCommandSet instance. If it does not hold
     * a plugin of the specified name at call time, the plugin is loaded then returned.<br/>
     * Caution : this method may be significantly slower than {@link #get(Class)} as it may involve classpath scanning.
     */
    public JkPlugin get(String pluginName) {
        final Optional<JkPlugin> optPlugin = loadedPlugins.stream()
                .filter(plugin -> plugin.name().equals(pluginName))
                .findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        final PluginDictionary.PluginDescription pluginDescription = PluginDictionary.loadByName(pluginName);
        if (pluginDescription == null) {
            return null;
        }
        return get(pluginDescription.pluginClass());
    }

    /**
     * Returns <code>true</code> if the specified plugin class has been loaded in the holding JkCommandSet instance.
     */
    public boolean hasLoaded(Class<? extends JkPlugin> pluginClass) {
        return loadedPlugins.stream()
                .anyMatch(plugin -> plugin.getClass().equals(pluginClass));
    }

    /**
     * Returns a list of all loaded plugins in the holding JkCommandSet instance.
     */
    public List<JkPlugin> getLoadedPlugins() {
        return Collections.unmodifiableList(loadedPlugins);
    }

    /**
     * Returns the list of loaded plugin instance of the specified class/interface.
     */
    public <T> List<T> getLoadedPluginInstanceOf(Class<T> clazz) {
        return loadedPlugins.stream()
                .filter(clazz::isInstance)
                .map(plugin -> (T) plugin)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends JkPlugin> T getOrCreate(Class<T> pluginClass) {
        final Optional<T> optPlugin = (Optional<T>) this.loadedPlugins.stream()
                .filter(item -> item.getClass().equals(pluginClass))
                .findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        final T plugin;
        try {
            plugin = JkUtilsReflect.newInstance(pluginClass, JkCommandSet.class, this.holder);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error while instantiating plugin " + pluginClass);
        }
        injectOptions(plugin);
        plugin.init();
        loadedPlugins.add(plugin);
        return plugin;
    }

    void injectOptions(JkPlugin plugin) {
        FieldInjector.injectEnv(plugin);
        Set<String> unusedKeys = JkOptions.populateFields(plugin, PluginOptions.options(plugin.name(),
                this.pluginOptionsList));
        unusedKeys.forEach(key -> JkLog.warn("Option '" + plugin.name() + "#" + key
                + "' from command line does not match any field of class " + plugin.getClass().getName()));
    }

    void loadCommandLinePlugins() {
        final Iterable<PluginOptions> pluginOptionsList = Environment.commandLine.getPluginOptions();
        for (final PluginOptions pluginOptions : pluginOptionsList){
            final PluginDictionary.PluginDescription pluginDescription = PluginDictionary.loadByName(pluginOptions.pluginName);
            if (pluginDescription == null) {
                throw new JkException("No plugin found with name '" + pluginOptions.pluginName + "'.");
            }
            getOrCreate(pluginDescription.pluginClass());
        }
    }

}
