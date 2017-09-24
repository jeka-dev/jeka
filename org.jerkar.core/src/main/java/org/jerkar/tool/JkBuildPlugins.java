package org.jerkar.tool;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsReflect;

/**
 * Set of plugins configured or activated in a {@link JkBuild}.
 */
public final class JkBuildPlugins {

    private final JkBuild holder;

    private final List<JkBuildPlugin2<?>> registeredPlugins = new LinkedList<>();

    JkBuildPlugins(JkBuild holder) {
        super();
        this.holder = holder;
    }

    JkBuildPlugin2<? extends JkBuild> register(Class<? extends JkBuildPlugin2<?>> exactPluginClass, Map<String, String> options) {
        final JkBuildPlugin2<?> plugin = getOrCreate(exactPluginClass);
        JkOptions.populateFields(plugin, options);
        return plugin;
    }

    List<JkBuildPlugin2<?>> getRegistered() {
        return Collections.unmodifiableList(registeredPlugins);
    }

    public void activate(JkBuild build) {
        registeredPlugins.forEach((plugin) -> plugin.apply(holder));
    }


    void invoke(Class<? extends JkBuildPlugin2<?>> pluginClass, String methodName) {
        if (!JkUtilsReflect.isMethodPublicIn(pluginClass, methodName, JkBuild.class)) {
            throw new JkException("No zero-arg public method found in "
                    + pluginClass.getName());
        }
        final JkBuildPlugin2<?> plugin = this.getOrCreate(pluginClass);
        JkLog.startUnderlined("Method " + methodName + " to plugin " + plugin.getClass().getSimpleName());
        final Method method = JkUtilsReflect.getMethod(pluginClass, methodName, JkBuild.class);
        JkUtilsReflect.invoke(plugin, method, this.holder);
        JkLog.done();
    }

    private JkBuildPlugin2<?> getOrCreate(Class<? extends JkBuildPlugin2<?>> exactPluginClass) {
        final Optional<JkBuildPlugin2<?>> optPlugin = this.registeredPlugins.stream().filter(
                (item) -> item.getClass().equals(exactPluginClass)).findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        return JkUtilsReflect.newInstance(exactPluginClass);
    }



}
