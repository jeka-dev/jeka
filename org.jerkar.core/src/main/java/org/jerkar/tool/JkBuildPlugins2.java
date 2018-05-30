package org.jerkar.tool;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.tool.builtins.java.JkPluginJava;
import sun.net.www.content.text.plain;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Set of plugins configured or activated in a {@link JkBuild}.
 */
public final class JkBuildPlugins2 {

    private final JkBuild holder;

    private final List<JkPlugin2> configuredPlugins = new LinkedList<>();

    JkBuildPlugins2(JkBuild holder) {
        super();
        this.holder = holder;
    }

    public <T extends JkPlugin2> T get(Class<T> pluginClass) {
        return getOrCreate(pluginClass, Collections.emptyMap());
    }

    public boolean has(Class<? extends JkPlugin2> pluginClass) {
        for (JkPlugin2 plugin : configuredPlugins) {
            if (plugin.getClass().equals(pluginClass)) {
                return true;
            }
        }
        return false;
    }

    void invoke(JkPlugin2 plugin, String methodName) {
        JkLog.startUnderlined("Method " + methodName + " to plugin " + plugin.getClass().getSimpleName());
        final Method method = pluginMethod(plugin.getClass(), methodName);
        JkUtilsReflect.invoke(plugin, method, this.holder);
        JkLog.done();
    }

    <T extends JkPlugin2> T getOrCreate(Class<T> pluginClass, Map<String, String> options) {
        final Optional<T> optPlugin = (Optional<T>) this.configuredPlugins.stream().filter(
                (item) -> item.getClass().equals(pluginClass)).findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        final T plugin = JkUtilsReflect.newInstance(pluginClass, JkBuild.class, this.holder);
        JkOptions.populateFields(plugin, options);
        plugin.postConfigure();
        configuredPlugins.add(plugin);
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
