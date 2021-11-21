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

    private final Map<Class<? extends JkBean>, JkBean> beans = new LinkedHashMap<>();

    private final JkRuntime holder;

    JkBeanRegistry(JkRuntime runtime) {
        this.holder = runtime;
    }

    void register(JkBean bean) {
        beans.put(bean.getClass(), bean);
        try {
            bean.init();
        } catch (Exception e) {
            throw new JkException(e, "An exception has been raised while initializing KBean " + bean);
        }
    }

    /**
     * Returns the plugin instance of the specified class loaded in the holding JkClass instance. If it does not hold
     * a plugin of the specified class at call time, the plugin is loaded then returned.
     */
    public <T extends JkBean> T get(Class<T> jkBeanClass) {
        return getOptional(jkBeanClass)
                .orElseGet(() -> {
                    JkBean bean = holder.instantiate(jkBeanClass);
                    register(bean);
                    return (T) bean;
                });
    }

    public <T extends JkBean> Optional<T> getOptional(Class<T> jkBeanClass) {
        return (Optional<T>) Optional.ofNullable(beans.get(jkBeanClass));
    }

    /**
     * Returns a list of all loaded plugins in the holding JkClass instance.
     */
    public List<JkBean> getAll() {
        return new LinkedList<>(beans.values());
    }




}
