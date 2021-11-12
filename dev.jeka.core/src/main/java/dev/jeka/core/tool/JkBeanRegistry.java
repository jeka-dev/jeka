package dev.jeka.core.tool;

import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Set of plugin instances loaded in a {@link JkClass}.
 */
public final class JkBeanRegistry {

    private final JkClass holder;

    private final List<JkBean> registeredJkBean = new LinkedList<>();

    private final List<JkBeanOptions> jkBeanOptionsList;

    JkBeanRegistry(JkClass holder, List<JkBeanOptions> jkBeanOptionsList) {
        super();
        this.holder = holder;
        this.jkBeanOptionsList = Collections.unmodifiableList(new ArrayList<>(jkBeanOptionsList));
    }

    /**
     * Returns the plugin instance of the specified class loaded in the holding JkClass instance. If it does not hold
     * a plugin of the specified class at call time, the plugin is loaded then returned.
     */
    public <T extends JkBean> T get(Class<T> jkBeanClass) {
        return getOrCreate(jkBeanClass);
    }

    public <T extends JkBean> Optional<T> getOptional(Class<T> jkBeanClass) {
        return (Optional<T>) registeredJkBean.stream()
                .filter(jkPlugin -> jkPlugin.getClass().equals(jkBeanClass))
                .findFirst();
    }

    /**
     * Returns the plugin instance of the specified name loaded in the holding JkClass instance. If it does not hold
     * a plugin of the specified name at call time, the plugin is loaded then returned.<br/>
     * Caution : this method may be significantly slower than {@link #get(Class)} as it may involve classpath scanning.
     */
    public JkBean get(String jkBeanName) {
        final Optional<JkBean> optPlugin = registeredJkBean.stream()
                .filter(plugin -> plugin.shortName().equals(jkBeanName))
                .findFirst();
        if (optPlugin.isPresent()) {
            return optPlugin.get();
        }
        final KBeanDictionary.KBeanDescription KBeanDescription = KBeanDictionary.loadByName(jkBeanName);
        if (KBeanDescription == null) {
            return null;
        }
        return get(KBeanDescription.pluginClass());
    }

    /**
     * Returns <code>true</code> if the specified plugin class has been loaded in the holding JkClass instance.
     */
    public boolean hasLoaded(Class<? extends JkBean> pluginClass) {
        return registeredJkBean.stream()
                .anyMatch(plugin -> plugin.getClass().equals(pluginClass));
    }

    /**
     * Returns a list of all loaded plugins in the holding JkClass instance.
     */
    public List<JkBean> getRegisteredJkBean() {
        return Collections.unmodifiableList(registeredJkBean);
    }

    /**
     * Returns the list of loaded plugin instance of the specified class/interface.
     */
    public <T> List<T> getLoadedPluginInstanceOf(Class<T> clazz) {
        return registeredJkBean.stream()
                .filter(clazz::isInstance)
                .map(plugin -> (T) plugin)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends JkBean> T getOrCreate(Class<T> pluginClass) {
        final Optional<T> optPlugin = (Optional<T>) this.registeredJkBean.stream()
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
            throw new RuntimeException("Error while instantiating KBean class " + pluginClass, t);
        }
        injectOptions(plugin);
        try {
            plugin.beforeSetup();
        } catch (Exception e) {
            throw JkUtilsThrowable.unchecked(e, "Error while initializing KBean " + plugin);
        }
        registeredJkBean.add(plugin);
        return plugin;
    }

    void injectOptions(JkBean jkBean) {
        FieldInjector.injectEnv(jkBean);
        Set<String> unusedKeys = JkOptions.populateFields(jkBean, JkBeanOptions.options(jkBean.shortName(),
                this.jkBeanOptionsList));
        unusedKeys.forEach(key -> JkLog.warn("Option '" + jkBean.shortName() + "#" + key
                + "' from command line does not match any field of class " + jkBean.getClass().getName()));
    }

    void loadCommandLinePlugins() {
        final Iterable<JkBeanOptions> pluginOptionsList = Environment.commandLine.getPluginOptions();
        for (final JkBeanOptions jkBeanOptions : pluginOptionsList){
            final KBeanDictionary.KBeanDescription KBeanDescription = KBeanDictionary.loadByName(jkBeanOptions.pluginName);
            if (KBeanDescription == null) {
                throw new JkException("No plugin found with name '" + jkBeanOptions.pluginName + "'.");
            }
            getOrCreate(KBeanDescription.pluginClass());
        }
    }

}
