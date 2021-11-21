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

    static JkRuntime of(Path projectBaseDir) {
        return RUNTIMES.computeIfAbsent(projectBaseDir, path -> new JkRuntime(path));
    }

    static JkRuntime ofContextBaseDir() {
        return of(getBaseDirContext());
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

        // inject field values
        Map<Class<? extends JkBean>, JkBean> fieldInjectedBeans = new LinkedHashMap<>();
        for (EngineCommand engineCommand : commands) {
            if (engineCommand.getAction() == EngineCommand.Action.PROPERTY_INJECT) {
                Class<? extends JkBean> beanClass = engineCommand.getBeanClass();
                JkBean bean = fieldInjectedBeans.computeIfAbsent(beanClass, this::instantiate);
                Field field;
                try {
                    field = bean.getClass().getField(engineCommand.getMember());
                } catch (NoSuchFieldException e) {
                    throw new JkException("No public field '" + engineCommand.getMember() + "' found on KBean class "
                            + bean.getClass());
                }
                JkUtilsReflect.setFieldValue(bean, field, engineCommand.getValue());
            }
        }

        // register beans
        commands.stream()
                .map(EngineCommand::getBeanClass)
                .distinct()
                .map(beanClass -> fieldInjectedBeans.computeIfAbsent(beanClass, this::instantiate))
                .forEach(bean -> beanRegistry.register(bean));

        // postInit registered beans
        RUNTIMES.values().forEach(JkRuntime::postInitBeans);
    }

    void run(List<EngineCommand> commands) {
        for (EngineCommand engineCommand : commands) {
            JkBean bean = beanRegistry.get(engineCommand.getBeanClass());
            if (engineCommand.getAction() == EngineCommand.Action.METHOD_INVOKE) {
                Method method;
                try {
                    method = bean.getClass().getMethod(engineCommand.getMember());
                } catch (NoSuchMethodException e) {
                    throw new JkException("No public no-args method '" + engineCommand.getMember() + "' found on KBean class "
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
                bean.postInit();
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
