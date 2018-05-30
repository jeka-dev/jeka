package org.jerkar.tool;

import java.lang.reflect.Method;
import java.util.HashMap;
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

    private final List<JkPluginOld> configuredPlugins = new LinkedList<>();

    JkBuildPlugins(JkBuild holder) {
        super();
        this.holder = holder;
    }

    public void configure(JkPluginOld plugin) {
        configuredPlugins.add(plugin);
    }

    public <T extends JkPluginOld> T get(Class<T> pluginClass) {
        return getOrCreate(pluginClass, new HashMap<String, String>());
    }

    void invoke(JkPluginOld plugin, String methodName) {
        JkLog.startUnderlined("Method " + methodName + " to plugin " + plugin.getClass().getSimpleName());
        final Method method = pluginMethod(plugin.getClass(), methodName, holder.getClass());
        JkUtilsReflect.invoke(plugin, method, this.holder);
        JkLog.done();
    }

    <T extends JkPluginOld> T getOrCreate(Class<T> pluginClass, Map<String, String> options) {
        final Optional<T> optPlugin = (Optional<T>) this.configuredPlugins.stream().filter(
                (item) -> item.getClass().equals(pluginClass)).findFirst();
        final T plugin = optPlugin.orElse(JkUtilsReflect.newInstance(pluginClass));
        JkOptions.populateFields(plugin, options);
        return plugin;
    }

    private Method pluginMethod(Class pluginClass, String name, Class buildClass) {
        Class<JkBuild> buildClassParamType = null;
        for (Method method : JkUtilsReflect.methodsHavingName(pluginClass, name)) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            Class paramType = method.getParameterTypes()[0];
            if (paramType.isAssignableFrom(buildClass)) {
                return method;
            }
            if (JkBuild.class.isAssignableFrom(paramType)) {
                buildClassParamType = paramType;
                continue;
            }
            return method;
        }
        if (buildClassParamType == null) {
            throw new IllegalStateException("No method " + name + "(JkBuild) found on " + buildClass);
        }
        throw new IllegalStateException("Method " + name + " from " + buildClass.getName() + " is not callable from a build scrpit of class or extending "
                + buildClass + " but a script of class or extending extending " + buildClassParamType);
    }

}
