package org.jerkar.tool;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsReflect;

import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/**
 * Set of plugins configured or activated in a {@link JkBuild}.
 */
public final class JkBuildPlugins {

    private final JkBuild holder;

    private final List<JkPlugin> configuredPlugins = new LinkedList<>();

    private final List<PluginOptions> pluginOptionsList;

    JkBuildPlugins(JkBuild holder,  List<PluginOptions> pluginOptionsList) {
        super();
        this.holder = holder;
        this.pluginOptionsList = Collections.unmodifiableList(new ArrayList<>(pluginOptionsList));
    }

    public <T extends JkPlugin> T get(Class<T> pluginClass) {
        return getOrCreate(pluginClass);
    }

    public JkPlugin get(String pluginName) {
        Optional<JkPlugin> optPlugin = configuredPlugins.stream().filter(plugin -> plugin.name().equals(pluginName)).findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        PluginDictionary.PluginDescription pluginDescription = PluginDictionary.loadByName(pluginName);
        if (pluginDescription == null) {
            return null;
        }
        return get(pluginDescription.pluginClass());
    }

    public boolean has(Class<? extends JkPlugin> pluginClass) {
        for (JkPlugin plugin : configuredPlugins) {
            if (plugin.getClass().equals(pluginClass)) {
                return true;
            }
        }
        return false;
    }

    public List<JkPlugin> all() {
        return Collections.unmodifiableList(configuredPlugins);
    }

    void invoke(JkPlugin plugin, String methodName) {
        Runnable task = () -> JkUtilsReflect.invoke(plugin, pluginMethod(plugin.getClass(), methodName));
        String msg = "Method " + methodName + " of plugin " + plugin.getClass().getSimpleName();
        JkLog.execute( msg, task);
    }

    private <T extends JkPlugin> T getOrCreate(Class<T> pluginClass) {
        final Optional<T> optPlugin = (Optional<T>) this.configuredPlugins.stream().filter(
                (item) -> item.getClass().equals(pluginClass)).findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        final T plugin = JkUtilsReflect.newInstance(pluginClass, JkBuild.class, this.holder);
        JkOptions.populateFields(plugin, PluginOptions.options(plugin.name(), this.pluginOptionsList));
        configuredPlugins.add(plugin);
        return plugin;
    }

    private Method pluginMethod(Class pluginClass, String name) {
        try {
            return pluginClass.getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No method " + name + " found on plugin " + pluginClass);
        }
    }

    void loadCommandLinePlugins() {
        Iterable<PluginOptions> pluginSetups = Environment.commandLine.getPluginOptions();
        for (PluginOptions pluginSetup : pluginSetups){
            PluginDictionary.PluginDescription pluginDescription = PluginDictionary.loadByName(pluginSetup.pluginName);
            getOrCreate(pluginDescription.pluginClass());
        }
    }

}
