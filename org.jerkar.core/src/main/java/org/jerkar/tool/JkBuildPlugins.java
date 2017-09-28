package org.jerkar.tool;

import java.lang.reflect.Method;
import java.util.*;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsReflect;

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

    public void configure(JkPlugin plugin) {
        configuredPlugins.add(plugin);
    }

    public <T extends JkPlugin> T get(Class<T> pluginClass) {
        return getOrCreate(pluginClass, new HashMap<String, String>());
    }

    void invoke(JkPlugin plugin, String methodName) {
        JkLog.startUnderlined("Method " + methodName + " to plugin " + plugin.getClass().getSimpleName());
        final Method method = JkUtilsReflect.getMethod(plugin.getClass(), methodName, JkBuild.class);
        JkUtilsReflect.invoke(plugin, method, this.holder);
        JkLog.done();
    }

    <T extends JkPlugin> T getOrCreate(Class<T> pluginClass, Map<String, String> options) {
        final Optional<T> optPlugin = (Optional<T>) this.configuredPlugins.stream().filter(
                (item) -> item.getClass().equals(pluginClass)).findFirst();
        final T plugin = optPlugin.orElse(JkUtilsReflect.newInstance(pluginClass));
        JkOptions.populateFields(plugin, options);
        return plugin;
    }

}
