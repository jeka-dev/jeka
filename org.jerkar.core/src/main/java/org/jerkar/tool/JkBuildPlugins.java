package org.jerkar.tool;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsReflect;

import java.lang.reflect.Method;
import java.util.*;

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

    public boolean has(Class<? extends JkPlugin> pluginClass) {
        for (JkPlugin plugin : configuredPlugins) {
            if (plugin.getClass().equals(pluginClass)) {
                return true;
            }
        }
        return false;
    }

    void invoke(JkPlugin plugin, String methodName) {
        JkLog.startUnderlined("Method " + methodName + " to plugin " + plugin.getClass().getSimpleName());
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
        plugin.decorate();
        configuredPlugins.add(plugin);
        JkLog.info("Build instance " + this.holder + " decorated with plugin " + plugin);
        return plugin;
    }

    private Method pluginMethod(Class pluginClass, String name) {
        try {
            return pluginClass.getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No method " + name + "(JkBuild) found on plugin " + pluginClass);
        }
    }

}
