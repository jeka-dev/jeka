package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Container where is registered the KBeans for a given project.
 * In multi-project builds, there is one <code>JkRuntime</code> per project.
 */
public final class JkRuntime {

    private static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();   //NOSONAR

    private static final Map<Path, JkRuntime> RUNTIMES = new LinkedHashMap<>();

    private final Path projectBaseDir;

    private JkDependencyResolver dependencyResolver;

    private JkPathSequence classpath;

    private JkPathSequence importedProjects;

    private List<EngineCommand> fieldInjections = Collections.emptyList();

    private JkProperties properties;

    private final Map<Class<? extends JkBean>, JkBean> beans = new LinkedHashMap<>();

    private JkRuntime(Path projectBaseDir) {
        this.projectBaseDir = projectBaseDir;
        this.properties = constructProperties(projectBaseDir);
    }

    public static JkRuntime get(Path projectBaseDir) {
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

    Path getProjectBaseDir() {
        return projectBaseDir;
    }

    /**
     * Returns the dependency resolver used to fetch 3rd party build dependencies.
     */
    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    /**
     * Returns the classpath used to compile def classes.
     */
    public JkPathSequence getClasspath() {
        return classpath;
    }

    /**
     * Returns root path of imported projects.
     */
    public JkPathSequence getImportedProjects() {
        return importedProjects;
    }

    /**
     * Returns the plugin instance of the specified class loaded in the holding JkClass instance. If it does not hold
     * a plugin of the specified class at call time, the plugin is loaded then returned.
     */
    public <T extends JkBean> T getBean(Class<T> jkBeanClass) {
        JkUtilsAssert.argument(jkBeanClass != null, "KBean class cannot be null.");
        JkBean result = beans.get(jkBeanClass);
        if (result == null) {
            JkLog.startTask("Instantiate KBean " + jkBeanClass + " in project '" + projectBaseDir + "'");
            Path previousProject = BASE_DIR_CONTEXT.get();
            BASE_DIR_CONTEXT.set(projectBaseDir);  // without this, projects nested with more than 1 level failed to get proper base dir
            result = this.instantiate(jkBeanClass);
            BASE_DIR_CONTEXT.set(previousProject);
            JkLog.endTask();
            beans.put(jkBeanClass, result);
        }
        return (T) result;
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

    public JkProperties getProperties() {
        return this.properties;
    }

    void setDependencyResolver(JkDependencyResolver resolverArg) {
        dependencyResolver = resolverArg;
    }

    void setClasspath(JkPathSequence pathSequence) {
        this.classpath = pathSequence;
    }

    void setImportedProjects(JkPathSequence importedProjects) {
        this.importedProjects = importedProjects;
    }

    void init(List<EngineCommand> commands) {
        JkLog.trace("Initialize JkRuntime with " + commands);
        this.fieldInjections = commands.stream()
                .filter(engineCommand -> engineCommand.getAction() == EngineCommand.Action.PROPERTY_INJECT)
                .collect(Collectors.toList());

        // Instantiate & register beans
        JkLog.startTask("Register KBeans");
        List<? extends JkBean> beans = commands.stream()
                //.filter(engineCommand -> engineCommand.getAction() != EngineCommand.Action.PROPERTY_INJECT)
                .map(EngineCommand::getBeanClass)
                .distinct()
                .map(this::getBean)
                .collect(Collectors.toList());
        JkLog.endTask();
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

    private JkBean instantiate(Class<? extends JkBean> beanClass) {
        if (Modifier.isAbstract(beanClass.getModifiers())) {
            throw new JkException("KBean class " + beanClass + " in " + this.projectBaseDir
                    + " is abstract and therefore cannot be instantiated. Please, use a concrete type to declare imported KBeans.");
        }
        JkBean bean = JkUtilsReflect.newInstance(beanClass);

        // Inject properties having name matching with a bean field
        String beanName = JkBean.name(beanClass);
        Map<String, String> props = properties.getAllStartingWith(beanName + "#", false);
        FieldInjector.inject(bean, props);

        // Inject properties on fields with @JkInjectProperty
        FieldInjector.injectAnnotatedProperties(bean, properties);

        // Inject from command name
        fieldInjections.stream()
                .filter(engineCommand -> engineCommand.getAction() == EngineCommand.Action.PROPERTY_INJECT)
                .filter(engineCommand -> engineCommand.getBeanClass().equals(bean.getClass()))
                .forEach(action -> {
                    Set<String> usedProperties =
                            FieldInjector.inject(bean, JkUtilsIterable.mapOf(action.getMember(), action.getValue()));
                    if (usedProperties.isEmpty()) {
                        throw new JkException("Field %s do not exist in KBean %s", action.getMember(), bean.getClass().getName());
                    }
                });

        return bean;
    }

    static JkProperties constructProperties(Path baseDir) {
        JkProperties result = JkProperties.ofSystemProperties()
                .withFallback(JkProperties.ofEnvironmentVariables()
                    .withFallback(readProjectPropertiesRecursively(baseDir)));
        Path globalPropertiesFile = JkLocator.getJekaUserHomeDir().resolve(JkConstants.GLOBAL_PROPERTIES);
        if (Files.exists(globalPropertiesFile)) {
            result = result.withFallback(JkProperties.ofFile(globalPropertiesFile));
        }
        return result;
    }

    @Override
    public String toString() {
        return "JkRuntime{" +
                "projectBaseDir=" + projectBaseDir +
                ", beans=" + beans.keySet() +
                '}';
    }

    // Reads the properties from the baseDir/jeka/project.properties
    // Takes also in account properties defined in parent project dirs if any.
    static JkProperties readProjectPropertiesRecursively(Path projectBaseDir) {
        Path baseDir = projectBaseDir.toAbsolutePath().normalize();
        Path projectPropertiesFile = baseDir.resolve(JkConstants.JEKA_DIR).resolve(JkConstants.PROPERTIES_FILE);
        JkProperties result = JkProperties.EMPTY;
        if (Files.exists(projectPropertiesFile)) {
            result = JkProperties.ofFile(projectPropertiesFile);
        }
        Path parentProject =baseDir.getParent();
        if (parentProject != null && Files.exists(parentProject.resolve(JkConstants.JEKA_DIR))
                & Files.isDirectory(parentProject.resolve(JkConstants.JEKA_DIR))) {
            result = result.withFallback(readProjectPropertiesRecursively(parentProject));

        }
        return result;
    }

}
