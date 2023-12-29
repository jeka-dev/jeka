package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Container where are registered the KBeans.
 * There is one <code>JkRuntime</code> per project root.*<p>
 * In multi-project (aka multi-module project) there is one runtime per sub-project.
 */
public final class JkRuntime {

    // Experiment for invoking 'KBean#init()' method lately, once all KBean has been instantiated
    // Note : Calling all KBeans init() methods in a later stage then inside 'load' methods
    //        leads in difficult problems as the order the KBeans should be initialized.
    private static final boolean LATE_INIT = false;

    private static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();

    private static final Map<Path, JkRuntime> RUNTIMES = new LinkedHashMap<>();

    private final Path projectBaseDir;

    private JkDependencyResolver dependencyResolver;

    private JkPathSequence classpath;

    private JkPathSequence importedProjects = JkPathSequence.of();

    private List<EngineCommand> fieldInjections = Collections.emptyList();

    private final JkProperties properties;

    private final Map<Class<? extends KBean>, KBean> beans = new LinkedHashMap<>();

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

    /// TODO experimental late init
    static void initAll() {
        if (!LATE_INIT) {
            return;
        }
        List<JkRuntime> runtimes = new LinkedList<>(RUNTIMES.values());
        Collections.reverse(runtimes);
        runtimes.forEach(runtime -> {
            for (KBean kBean : runtime.getBeans()) {
                JkLog.info("Inject field values in %s", kBean);
                runtime.injectFieldValues(kBean);
            }
            List<KBean> reversedBeans = runtime.getBeans();
            Collections.reverse(reversedBeans);
            for (KBean kBean : reversedBeans) {
                JkLog.startTask("Invoke init() on %s", kBean);
                kBean.init();
                JkLog.endTask();
            }
        });
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
     * Instantiates the specified KBean into this runtime, if it is not already present. <p>
     * As KBeans are singleton within a runtime, this method has no effect if the bean is already loaded.
     * @param beanClass The class of the KBean to load.
     * @param consumer Give a chance to inject default values in the returned instance. It has only effect
     *                 when creating the singleton. If the singleton in cached, then the consumer
     *                 won't be applied.
     * @return This object for call chaining.
     */
    public <T extends KBean> T load(Class<T> beanClass, Consumer<T> consumer) {
        JkUtilsAssert.argument(beanClass != null, "KBean class cannot be null.");
        T result = (T) beans.get(beanClass);
        if (result == null) {
            String projectDisplayName = projectBaseDir.toString().isEmpty() ?
                    projectBaseDir.toAbsolutePath().getFileName().toString()
                    : projectBaseDir.toString();
            JkLog.startTask("Instantiate KBean %s in project '%s'", beanClass, projectDisplayName);
            Path previousProject = BASE_DIR_CONTEXT.get();
            BASE_DIR_CONTEXT.set(projectBaseDir);  // without this, projects nested with more than 1 level failed to get proper base dir
            result = this.instantiate(beanClass, consumer);
            BASE_DIR_CONTEXT.set(previousProject);
            JkLog.endTask();
        }
        return result;
    }

    /**
     * @see JkRuntime#load(Class, Consumer)
     */
    public <T extends KBean> T load(Class<T> beanClass) {
        return load(beanClass, bean -> {});
    }


    /**
     * Returns the KBean of the exact specified class, present in this runtime.
     */
    public <T extends KBean> Optional<T> find(Class<T> beanClass) {
        return (Optional<T>) Optional.ofNullable(beans.get(beanClass));
    }

    /**
     * Returns the first KBean being an instance of the specified class, present in this runtime.
     */
    public <T extends KBean> Optional<T> findInstanceOf(Class<T> beanClass) {
        return (Optional<T>) beans.values().stream()
                .filter(beanClass::isInstance)
                .findFirst();
    }

    /**
     * Returns the list of registered KBeans. A KBean is registered when it has been identified as the default KBean or
     * when {@link #load(Class)} is invoked.
     */
    public List<KBean> getBeans() {
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
        JkLog.trace("Initialize JkRuntime with \n" + JkUtilsIterable.toMultiLineString(commands, "  "));
        this.fieldInjections = commands.stream()
                .filter(engineCommand -> engineCommand.getAction() == EngineCommand.Action.PROPERTY_INJECT)
                .collect(Collectors.toList());

        // Instantiate & register beans
        JkLog.startTask("Register KBeans");
        commands.stream()
                //.filter(engineCommand -> engineCommand.getAction() != EngineCommand.Action.PROPERTY_INJECT)
                .map(EngineCommand::getBeanClass)
                .distinct()
                .forEach(this::load);
        JkLog.endTask();
    }

    void run(List<EngineCommand> commands) {
        for (EngineCommand engineCommand : commands) {
            KBean bean = load(engineCommand.getBeanClass());
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

    void putKBean(Class<? extends KBean> kbeanClass, KBean kbean) {
        beans.put(kbeanClass, kbean);
    }

    private <T extends KBean> T instantiate(Class<T> beanClass, Consumer<T> consumer) {
        if (Modifier.isAbstract(beanClass.getModifiers())) {
            throw new JkException("KBean class " + beanClass + " in " + this.projectBaseDir
                    + " is abstract and therefore cannot be instantiated. Please, use a concrete type to declare imported KBeans.");
        }
        T bean = JkUtilsReflect.newInstance(beanClass);
        consumer.accept(bean);

        // We must inject fields after instance creation cause in the KBean
        // constructor, fields of child classes are not yet initialized.
        if (!LATE_INIT) {
            injectFieldValues(bean);
            bean.init();
        }
        return bean;
    }

    // inject values in fields from command-line and properties.
    void injectFieldValues(KBean bean) {
        // Inject properties having name matching with a bean field
        String beanName = KBean.name(bean.getClass());
        Map<String, String> props = new HashMap<>();

        // accept 'kb#' prefix if the beanClass is declared with '-kb=' options
        if (bean.isMatchingName(Environment.standardOptions.kbeanName())) {
            props.putAll(properties.getAllStartingWith(Environment.KB_KEYWORD + "#", false));
        }
        props.putAll(properties.getAllStartingWith(beanName + "#", false));
        FieldInjector.inject(bean, props);

        // Inject properties on fields with @JkInjectProperty
        FieldInjector.injectAnnotatedProperties(bean, properties);

        // Inject from command line
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
    }

    static JkProperties constructProperties(Path baseDir) {
        JkProperties result = JkProperties.ofSysPropsThenEnv()
                    .withFallback(readProjectPropertiesRecursively(JkUtilsPath.relativizeFromWorkingDir(baseDir)));
        Path globalPropertiesFile = JkLocator.getGlobalPropertiesFile();
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

    // Reads the properties from the baseDir/jeka/local.properties
    // Takes also in account properties defined in parent project dirs if any.
    static JkProperties readProjectPropertiesRecursively(Path projectBaseDir) {
        Path baseDir = projectBaseDir.toAbsolutePath().normalize();
        Path projectPropertiesFile = baseDir.resolve(JkConstants.JEKA_DIR).resolve(JkConstants.PROPERTIES_FILE);
        JkProperties result = JkProperties.EMPTY;
        if (Files.exists(projectPropertiesFile)) {
            result = JkProperties.ofFile(JkUtilsPath.relativizeFromWorkingDir(projectPropertiesFile));
        }
        Path parentProject =baseDir.getParent();
        if (parentProject != null && Files.exists(parentProject.resolve(JkConstants.JEKA_DIR))
                & Files.isDirectory(parentProject.resolve(JkConstants.JEKA_DIR))) {
            result = result.withFallback(readProjectPropertiesRecursively(parentProject));
        }
        return result;
    }

}
