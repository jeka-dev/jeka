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

    JkBuildPlugins(JkBuild holder) {
        super();
        this.holder = holder;
    }

    public <T extends JkPlugin> T get(Class<T> pluginClass) {
        return getOrCreate(pluginClass, Collections.emptyMap());
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
        JkLog.startUnderlined("Method " + methodName + " of plugin " + plugin.getClass().getSimpleName());
        final Method method = pluginMethod(plugin.getClass(), methodName);
        JkUtilsReflect.invoke(plugin, method);
        JkLog.done();
    }

    <T extends JkPlugin> T getOrCreate(Class<T> pluginClass, Map<String, String> options) {
        final Optional<T> optPlugin = (Optional<T>) this.configuredPlugins.stream().filter(
                (item) -> item.getClass().equals(pluginClass)).findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        final T plugin = JkUtilsReflect.newInstance(pluginClass, JkBuild.class, this.holder);
        JkOptions.populateFields(plugin, options);
        configuredPlugins.add(plugin);
        JkLog.info("Build instance : " + this.holder + " will decorated with plugin " + plugin.name());
        return plugin;
    }

    private Method pluginMethod(Class pluginClass, String name) {
        try {
            return pluginClass.getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No method " + name + " found on plugin " + pluginClass);
        }
    }

    void loadCommandLinePlugins(boolean master) {
        Iterable<CommandLine.JkPluginSetup> pluginSetups = master ? CommandLine.INSTANCE.getMasterPluginSetups() :
                CommandLine.INSTANCE.getSubProjectPluginSetups();
        for (CommandLine.JkPluginSetup pluginSetup : pluginSetups){
            PluginDictionary.PluginDescription pluginDescription = PluginDictionary.loadByName(pluginSetup.pluginName);
            getOrCreate(pluginDescription.pluginClass(), pluginSetup.options);
        }
    }

}
