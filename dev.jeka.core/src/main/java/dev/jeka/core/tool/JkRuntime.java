package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsThrowable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class JkRuntime {

    static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();

    private final static Map<Path, JkRuntime> RUNTIMES = new HashMap<>();

    private final Path projectBaseDir;

    private JkDependencyResolver dependencyResolver;

    private JkBeanRegistry beanRegistry = new JkBeanRegistry(this);

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
            setBaseDirContext(Paths.get(""));
            return BASE_DIR_CONTEXT.get();
        });
    }

    public Path getProjectBaseDir() {
        return projectBaseDir;
    }

    public JkBeanRegistry getBeanRegistry() {
        return beanRegistry;
    }

    public JkRepoSet getDownloadRepos() {
        return dependencyResolver.getRepos();
    }

    void setDependencyResolver(JkDependencyResolver resolverArg) {
        dependencyResolver = resolverArg;
    }

    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    void init(List<EngineCommand> commands) {

        Map<Class<? extends JkBean>, JkBean> fieldInjectedBeans = new LinkedHashMap<>();

        // inject field values
        Map<Class<? extends JkBean>, Map<String, String>> injectedValues = new HashMap<>();
        commands.stream()
                    .filter(engineCommand -> engineCommand.getAction() == EngineCommand.Action.PROPERTY_INJECT)
                    .forEach(engineCommand -> {
                        injectedValues.putIfAbsent(engineCommand.getBeanClass(), new HashMap<>());
                        injectedValues.get(engineCommand.getBeanClass()).put(engineCommand.getMember(),
                                engineCommand.getValue());
                    });
        for (Map.Entry<Class<? extends JkBean>, Map<String, String>> entry : injectedValues.entrySet()) {
            JkBean bean = fieldInjectedBeans.computeIfAbsent(entry.getKey(), this::instantiate);
            Set<String> usedProperties = FieldInjector.inject(bean, entry.getValue());
            Set<String> unusedProperties = new HashSet<>(entry.getValue().keySet());
            unusedProperties.removeAll(usedProperties);
            if (!unusedProperties.isEmpty()) {
                throw new JkException("fields %s do not exist in KBean %s", unusedProperties, entry.getKey());
            }
        }

        // register beans
        JkLog.startTask("Register KBeans");
        commands.stream()
                .map(EngineCommand::getBeanClass)
                .distinct()
                .map(beanClass -> fieldInjectedBeans.computeIfAbsent(beanClass, this::instantiate))
                .forEach(bean -> beanRegistry.register(bean));

        // postInit registered beans
        RUNTIMES.values().forEach(JkRuntime::postInitBeans);
        JkLog.endTask();
    }

    void run(List<EngineCommand> commands) {
        for (EngineCommand engineCommand : commands) {
            JkBean bean = beanRegistry.get(engineCommand.getBeanClass());
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
        List<JkBean> beans = getBeanRegistry().getAll();
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

    JkBean instantiate(Class<? extends JkBean> beanClass) {
        if (Modifier.isAbstract(beanClass.getModifiers())) {
            throw new JkException("KBean class " + beanClass + " in " + this.projectBaseDir
                    + " is abstract and therefore cannot be instantiated. Please, use a concrete type to declare imported KBeans.");
        }
        try {
            return JkUtilsReflect.newInstance(beanClass);
        } catch (RuntimeException e) {
            throw new JkException(e, "Error while calling constructor of %s in project %s", beanClass, projectBaseDir);
        }
    }

}
