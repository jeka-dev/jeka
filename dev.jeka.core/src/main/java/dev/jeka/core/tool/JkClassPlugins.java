package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Set of plugin instances loaded in a {@link JkClass}.
 */
public final class JkClassPlugins {

    private final JkClass holder;

    private final List<JkPlugin> loadedPlugins = new LinkedList<>();

    private final List<PluginOptions> pluginOptionsList;

    JkClassPlugins(JkClass holder, List<PluginOptions> pluginOptionsList) {
        super();
        this.holder = holder;
        this.pluginOptionsList = Collections.unmodifiableList(new ArrayList<>(pluginOptionsList));
    }

    /**
     * Returns the plugin instance of the specified class loaded in the holding JkClass instance. If it does not hold
     * a plugin of the specified class at call time, the plugin is loaded then returned.
     */
    public <T extends JkPlugin> T get(Class<T> pluginClass) {
        return getOrCreate(pluginClass);
    }

    public <T extends JkPlugin> Optional<T> getOptional(Class<T> pluginClass) {
        return (Optional<T>) loadedPlugins.stream()
                .filter(jkPlugin -> jkPlugin.getClass().equals(pluginClass))
                .findFirst();
    }

    /**
     * Returns the plugin instance of the specified name loaded in the holding JkClass instance. If it does not hold
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
     * Returns <code>true</code> if the specified plugin class has been loaded in the holding JkClass instance.
     */
    public boolean hasLoaded(Class<? extends JkPlugin> pluginClass) {
        return loadedPlugins.stream()
                .anyMatch(plugin -> plugin.getClass().equals(pluginClass));
    }

    /**
     * Returns a list of all loaded plugins in the holding JkClass instance.
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
            PluginCompatibilityBreakChecker.checkCompatibility(pluginClass, this.holder.getDefDependencyResolver());
            plugin = JkUtilsReflect.newInstance(pluginClass, JkClass.class, this.holder);
        } catch (Throwable t) {  // Catch LinkageError
            if (t instanceof LinkageError) {
                throw new RuntimeException("Plugin class " + pluginClass
                        + " seems not compatible with this Jeka version as this plugin reference an unknown class " +
                        "from Jeka", t);
            }
            throw new RuntimeException("Error while instantiating plugin class " + pluginClass, t);
        }
        injectOptions(plugin);
        try {
            plugin.beforeSetup();
        } catch (Exception e) {
            throw JkUtilsThrowable.unchecked(e, "Error while initializing plugin " + plugin);
        }
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
