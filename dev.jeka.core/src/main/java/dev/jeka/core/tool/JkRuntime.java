package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public final class JkRuntime {

    private static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();

    private static final Map<Path, JkRuntime> RUNTIMES = new LinkedHashMap<>();

    private final Path projectBaseDir;

    private JkDependencyResolver dependencyResolver;

    private List<EngineCommand> fieldInjections = Collections.emptyList();

    private final Map<Class<? extends JkBean>, JkBean> beans = new LinkedHashMap<>();

    private JkRuntime(Path projectBaseDir) {
        this.projectBaseDir = projectBaseDir;
    }

    static JkRuntime get(Path projectBaseDir) {
        return RUNTIMES.computeIfAbsent(projectBaseDir, path -> new JkRuntime(path));
    }

    static JkRuntime getCurrentContextBaseDir() {
        return get(getBaseDirContext());
    }

    static void setBaseDirContext(Path baseDir) {
        JkUtilsAssert.argument(baseDir == null || Files.exists(baseDir),"Project " + baseDir + " not found.");
        BASE_DIR_CONTEXT.set(baseDir);
    }

    private static Path getBaseDirContext() {
        return Optional.ofNullable(BASE_DIR_CONTEXT.get()).orElseGet(() -> {
            setBaseDirContext(Paths.get("."));
            return BASE_DIR_CONTEXT.get();
        });
    }

    public Path getProjectBaseDir() {
        return projectBaseDir;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    /**
     * Returns the plugin instance of the specified class loaded in the holding JkClass instance. If it does not hold
     * a plugin of the specified class at call time, the plugin is loaded then returned.
     */
    public <T extends JkBean> T getBean(Class<T> jkBeanClass) {
        return (T) beans.computeIfAbsent(jkBeanClass, this::instantiate);
    }

    public <T extends JkBean> Optional<T> getBeanOptional(Class<T> jkBeanClass) {
        return (Optional<T>) Optional.ofNullable(beans.get(jkBeanClass));
    }

    /**
     * Returns the list of registered KBeans. A KBean is registered when it has been identified as the default KBean or
     * when {@link #getBean(Class)} is invoked.
     */
    public List<JkBean> getBeans() {
        return new LinkedList<>(beans.values());
    }

    void setDependencyResolver(JkDependencyResolver resolverArg) {
        dependencyResolver = resolverArg;
    }

    void init(List<EngineCommand> commands) {
        this.fieldInjections = commands.stream()
                .filter(engineCommand -> engineCommand.getAction() == EngineCommand.Action.PROPERTY_INJECT)
                .collect(Collectors.toList());

        // Instantiate & register beans
        JkLog.startTask("Register KBeans");
        commands.stream()
                .filter(engineCommand -> engineCommand.getAction() != EngineCommand.Action.PROPERTY_INJECT)
                .map(EngineCommand::getBeanClass)
                .distinct()
                .forEach(this::getBean);

        // Apply post-init
        List<JkRuntime> runtimes = new LinkedList<>(JkRuntime.RUNTIMES.values());
        Collections.reverse(runtimes);
        for (JkRuntime runtime : runtimes) {
            JkLog.startTask("Post-Initializing beans for project " + runtime);
            runtime.beans.values().forEach(this::postInit);
            JkLog.endTask();
        }

        JkLog.endTask();
    }

    private void init(JkBean bean) {
        try {
            JkLog.startTask("Init KBean " + bean);
            bean.init();
            JkLog.endTask();
        } catch (Exception e) {
            throw new JkException(e, "An exception has been raised while initializing KBean " + bean);
        }
    }

    private void postInit(JkBean bean) {
        try {
            JkLog.startTask("Post-Init KBean " + bean.getClass().getName());
            bean.postInit();
            JkLog.endTask();
        } catch (Exception e) {
            throw new JkException(e, "An exception has been raised while post-initializing KBean " + bean);
        }
    }

    void run(List<EngineCommand> commands) {
        for (EngineCommand engineCommand : commands) {
            JkBean bean = getBean(engineCommand.getBeanClass());
            if (engineCommand.getAction() == EngineCommand.Action.METHOD_INVOKE) {
                Method method;
                try {
                    method = bean.getClass().getMethod(engineCommand.getMember());
                } catch (NoSuchMethodException e) {
                    throw new JkException("No public no-args method '" + engineCommand.getMember() + "' found on KBean "
                            + bean.getClass());
                }
                JkUtilsReflect.invoke(bean, method);
            }
        }
    }

    private void postInitBeans() {
        List<JkBean> beans = getBeans();
        Collections.reverse(beans);
        for (JkBean bean : beans) {
            try {
                JkLog.startTask("PostInit KBean " + bean);
                bean.postInit();
                JkLog.endTask();
            } catch (Exception e) {
                throw JkUtilsThrowable.unchecked(e);
            }
        }
    }

    private JkBean instantiate(Class<? extends JkBean> beanClass) {
        if (Modifier.isAbstract(beanClass.getModifiers())) {
            throw new JkException("KBean class " + beanClass + " in " + this.projectBaseDir
                    + " is abstract and therefore cannot be instantiated. Please, use a concrete type to declare imported KBeans.");
        }
        JkBean bean = JkUtilsReflect.newInstance(beanClass);
        fieldInjections.stream()
                .filter(engineCommand -> engineCommand.getAction() == EngineCommand.Action.PROPERTY_INJECT)
                .filter(engineCommand -> engineCommand.getBeanClass().equals(bean.getClass()))
                .forEach(action -> {
                    Set<String> injectedProp =
                            FieldInjector.inject(bean, JkUtilsIterable.mapOf(action.getMember(), action.getValue()));
                    if (injectedProp.isEmpty()) {
                        throw new JkException("field %s does not exist in KBean %s", injectedProp, bean);
                    }
                });
        init(bean);
        return bean;
    }

    @Override
    public String toString() {
        return "JkRuntime{" +
                "projectBaseDir=" + projectBaseDir +
                ", beans=" + beans.keySet() +
                '}';
    }
}
