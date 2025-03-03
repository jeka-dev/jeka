/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.function.JkRunnables;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Execution context associated with a base directory.
 * <p>
 * Each <i>runBase</i> has :
 * <ul>
 *     <li>A base directory from which JeKa resolves file paths. This base directory might contains a <i>jeka-src</i> subdirectory and/or a <i>jeka.properties</i> file at its root.</li>
 *     <li>A KBean registry for holding KBeans involved in the run context.
 *     There can be only one KBean instance per class within a runBase.</li>
 *     <li>A set of properties defined in [baseDir]/jeka.properties file</li>
 *     <li>A set of imported runBase</li>
 * </ul>
 * Typically, there is one runbase per project to build, sharing the same base dir.
 */
public final class JkRunbase {

    // Trick for passing the runbase to the KBean default constructor
    static final ThreadLocal<JkRunbase> CURRENT = new ThreadLocal<>();

    private static JkRunbase MASTER;

    private static final Map<Path, JkRunbase> SUB_RUN_BASES = new LinkedHashMap<>();

    private static final String PROP_KBEAN_PREFIX = "@";

    private KBeanResolution kbeanResolution;

    private final Path baseDir; // Relative Path

    private JkDependencyResolver dependencyResolver;

    private JkPathSequence classpath;

    private JkPathSequence exportedClasspath;

    private JkDependencySet exportedDependencies;

    private JkDependencySet fullDependencies;

    private final JkPathSequence importedBaseDirs = JkPathSequence.of();

    // Note: An empty container has to be present at instantiation time for sub-runBases, as
    // they are not initialized with any KbeanActions.
    private KBeanAction.Container cmdLineActions = new KBeanAction.Container();

    // TODO remove when load() will be prevented at initialisation time
    private List<Class<? extends KBean>> kbeanInitDeclaredInProps = new LinkedList<>();

    private final KBeanAction.Container effectiveActions = new KBeanAction.Container();

    private final JkProperties properties;

    private final JkRunnables cleanActions = JkRunnables.of().setLogTasks(JkLog.isDebug());

    // We use class name as key because using `Class` objects as key may lead
    // in duplicate initialization in some circumstances where several class loader
    // are present (this has happened when using "jeka aKbean: --doc)
    private final Map<String, KBean> beans = new LinkedHashMap<>();

    private PreInitializer preInitializer = PreInitializer.of(Collections.emptyList());

    private final PostInitializer postInitializer = PostInitializer.of();

    private boolean initialized;

    private JkRunbase(Path baseDir) {
        this.baseDir = baseDir;
        this.properties = constructProperties(baseDir);
    }

    static JkRunbase createMaster(Path baseDir) {
        MASTER = new JkRunbase(baseDir);
        return MASTER;
    }

    /**
     * Returns the JkRunbase instance associated with the specified project base directory.
     */
    public static JkRunbase get(Path baseDir) {
        if (MASTER.baseDir.equals(baseDir)) {
            return MASTER;
        }
        // Map#computeIsAbsent may throw ConcurrentModificationException
        JkRunbase result = SUB_RUN_BASES.get(baseDir);
        if (result == null) {
            JkLog.startTask("Initializing Runbase " + baseDir);
            Engine engine = Engines.get(baseDir);
            engine.resolveKBeans();
            result = engine.initRunbase(new KBeanAction.Container());
            JkLog.endTask();
            //JkRunbase result = new JkRunbase(path);
            //result.classpath = MASTER.classpath;  // todo need to narrow
            //result.kbeanResolution = MASTER.kbeanResolution.toSubRunbase(baseDir);
            SUB_RUN_BASES.put(baseDir, result);
        }
        return result;
    }

    static JkRunbase getMaster() {
        return MASTER;
    }

    /**
     * Returns the dependency resolver used to fetch 3rd party build dependencies.
     */
    public JkDependencyResolver getDependencyResolver() {
        return dependencyResolver;
    }

    /**
     * Returns the classpath used to compile jeka-src.
     */
    public JkPathSequence getClasspath() {
        return classpath;
    }

    /**
     * Returns the exported classpath used by the JkRunbase instance.
     * <p>
     * The exported classpath is the classpath minus 'private' dependencies. Private dependencies
     * are declared using <code>@JkDep</code> in a class that is in package with root
     * folder name stating with <code>_</code>.
     */
    public JkPathSequence getExportedClasspath() {
        return this.exportedClasspath;
    }

    /**
     * Returns the exported dependencies of the JkRunbase instance.
     * <p>
     * The exported dependencies is the dependencies minus 'private' dependencies. Private dependencies
     * are declared using <code>@JkDep</code> in a class that is in package with root
     * folder name stating with <code>_</code>.
     */
    public JkDependencySet getExportedDependencies() {
        return this.exportedDependencies;
    }

    /**
     * Returns the complete dependencies of the JkRunbase instance. This values to exported + private dependencies.
     */
    public JkDependencySet getFullDependencies() {
        return this.fullDependencies;
    }

    /**
     * Returns root path of imported projects.
     */
    public JkPathSequence getImportBaseDirs() {
        return importedBaseDirs;
    }

    /**
     * Returns the KBean of the exact specified class, present in this runbase.
     */
    public <T extends KBean> Optional<T> find(Class<T> beanClass) {

        // TODO remove when find() will be prevented at init time
        if (cmdLineActions.findInvolvedKBeanClasses().contains(beanClass) ||
                this.kbeanInitDeclaredInProps.contains(beanClass)) {
            return Optional.of(load(beanClass));
        }
        return (Optional<T>) Optional.ofNullable(beans.get(beanClass.getName()));
    }

    /**
     * Instantiates the specified KBean into the current runbase, if it is not already present. <p>
     * Since KBeans are singletons within a runbase, calling this method has no effect if the bean is already loaded.
     *
     * @param beanClass The class of the KBean to load.
     *
     * @return This object for call chaining.
     */
    public <T extends KBean> T load(Class<T> beanClass) {
        if (!initialized) {
            JkLog.warn("Loading bean %s from Runbase %s during initialization.", beanClass.getName(), baseDir);
            JkLog.warn("This action will be disallowed in future releases.");
            JkLog.warn("Please, update your code to use @JkPostInit methods for KBean configuration.");
            JkLog.warn("Kbeans under initialization was: "
                    + beans.keySet().stream().map(KBean::name).collect(Collectors.joining(", ")));
            JkLog.warn("Run with --debug option to see stacktrace at the moment of initialization.");
            if (JkLog.isDebug()) {
                JkUtilsThrowable.printStackTrace(JkLog.getOutPrintStream(),
                        Thread.currentThread().getStackTrace(), 100);
            }
        }
        T result = (T) beans.get(beanClass.getName());
        if (result != null) {
            return result;
        }
        result = loadInternal(beanClass);
        this.postInitializer.addPostInitializerCandidate(result); // the postInit methods won't apply to already initialized beans
        this.postInitializer.apply(result);
        return result;
    }

    /**
     * Returns the list of registered KBeans. A KBean is registered when it has been identified as the default KBean or
     * when {@link #load(Class)} is invoked.
     */
    public List<KBean> getBeans() {
        return new LinkedList<>(beans.values());
    }

    /**
     * Returns the JkProperties object associated with the current instance of JkRunBase.
     * JkProperties is a class that holds key-value pairs of properties relevant to the execution of Jeka build tasks.
     */
    public JkProperties getProperties() {
        return this.properties;
    }

    public Path getBaseDir() {
        return baseDir;
    }

    /**
     * Get either a ProjectKBean or a BaseKBean present in this runbase.
     * The result is returned as a JkBuildable abstraction.<p>
     * This methods never returns <code>null</code>, if no such KBean is present in the runbase
     * a ProjectKBean is created if the presence of *src* dir is detected, otherwise it returns
     * a BaseKBean instance.
     */
    public JkBuildable getBuildable() {
        Class<? extends KBean> clazz = getBuildableKBeanClass();
        JkBuildable.Supplier buidableSupplier = (JkBuildable.Supplier) load(clazz);
        return buidableSupplier.asBuildable();
    }

    /**
     *
     * @return
     */
    public Class<? extends KBean> getBuildableKBeanClass() {
        if (isKBeanClassPresent(ProjectKBean.class)) {
            return ProjectKBean.class;
        }
        if (isKBeanClassPresent(BaseKBean.class)) {
            return BaseKBean.class;
        }
        if (Files.isDirectory(this.getBaseDir().resolve("src"))) {
            return ProjectKBean.class;
        }
        return BaseKBean.class;
    }

    /**
     * Register extra file-system clean action to run when --clean option is specified.
     * This is used for KBeans as nodeJS that needs to clean an extra folder that does not belong to jeka-output.
     *
     * Important: This method should be called inside the a KBean #init() method in order it
     *            can be taken in account by the execution engine.
     *
     * @param name
     * @param runnable
     */
    public void registerCLeanAction(String name, Runnable runnable) {
        cleanActions.append(name, runnable);
    }

    void setDependencyResolver(JkDependencyResolver resolverArg) {
        dependencyResolver = resolverArg;
    }

    void setClasspath(JkPathSequence pathSequence) {
        this.classpath = pathSequence;
    }

    void setKbeanResolution(KBeanResolution kbeanResolution) {
        this.kbeanResolution = kbeanResolution;
    }

    void setExportedClassPath(JkPathSequence exportedClassPath) {
        this.exportedClasspath = exportedClassPath;
    }

    void setExportedDependencies(JkDependencySet exportedDependencies) {
        this.exportedDependencies = exportedDependencies;
    }

    void setFullDependencies(JkDependencySet fullDependencies) {
        this.fullDependencies = fullDependencies;
    }

    void init(KBeanAction.Container cmdLineActionContainer) {

        // Add initKBean
        Class<? extends KBean> localKBeanClass = kbeanResolution.findImplicitKBeanClass().orElse(null);
        KBeanAction.Container actions = cmdLineActionContainer.withInitBean(localKBeanClass);

        if (JkLog.isDebug()) {
            JkLog.debug("Initialize JkRunbase with \n" + actions.toColumnText());
            JkLog.debug("Local KBean class name: " + kbeanResolution.implicitKBeanClassName);
            JkLog.debug("Local KBean class: " + localKBeanClass);
            JkLog.debug("All KBean classes: " + kbeanResolution.allKbeanClassNames);
            JkLog.debug("All local KBean classes: " + kbeanResolution.localKBeanClassNames);
        }

        // Needed for find() and inject values at instantiation time
        this.cmdLineActions = actions;

        JkLog.debugStartTask("Register KBeans");

        // Discover classes to initialize
        List<Class<? extends KBean>> initialClasses = actions.findInvolvedKBeanClasses();
        this.kbeanInitDeclaredInProps = kbeansToInitFromProps();  // todo remove when find() prevented at init time
        initialClasses.addAll(this.kbeanInitDeclaredInProps);
        initialClasses = initialClasses.stream().distinct().collect(Collectors.toList());
        InitClassesResolver initClassesResolver = InitClassesResolver.of(this, initialClasses);
        List<Class<? extends KBean>> classesToInit = initClassesResolver.getClassesToInitialize();

        if (LogSettings.INSTANCE.inspect) {
            String classNames = classesToInit.stream()
                    .map(KBean::name)
                    .collect(Collectors.joining(", "));
            JkLog.info("KBeans to initialize: ");
            JkLog.info("    " + classNames);
        }

        // Register pre-initializers from classes to initialize
        // KBeans will be pre-initialized at instantiation time
        this.preInitializer = PreInitializer.of(classesToInit);

        // Initialize KBeans then register their post-initializers
        for (Class<? extends KBean> beanClass : classesToInit) {
            KBean kbean = loadInternal(beanClass);
            this.postInitializer.addPostInitializerCandidate(kbean);
        }

        // Log KBean Pre-initialization
        List<KBean> preInitializedKBeans = preInitializer.getPreInitializedKbeans();
        if (LogSettings.INSTANCE.inspect && !preInitializedKBeans.isEmpty()) {
            JkLog.info("KBeans pre-initialisation:");
            final Jk2ColumnsText preInitializeText = Jk2ColumnsText.of(18, 120);
            for (KBean kbean : preInitializedKBeans) {
                preInitializer.getInitializerNamesFor(kbean.getClass()).forEach(name -> {
                    String kbeanName = KBean.name(kbean.getClass());
                    preInitializeText.add("    " + kbeanName, name);
                });
            }
            JkLog.info(preInitializeText.toString());
        }

        // Log KBean initialization
        if (LogSettings.INSTANCE.inspect) {
            JkLog.info("KBeans Initialization    :");
            JkLog.info(this.effectiveActions.toColumnText()
                    .setSeparator(" | ")
                    .setMarginLeft("   | ")
                    .toString());
        }

        // Post-initialize KBeans
        Jk2ColumnsText postInitializeText = null;
        if (LogSettings.INSTANCE.inspect) {
            JkLog.info("KBeans post-initialisation:");
            postInitializeText = Jk2ColumnsText.of(18, 120);
        }
        for (Class<? extends KBean> beanClass : initClassesResolver.getClassesToInitialize()) {
            KBean kbean = beans.get(beanClass.getName());
            List<String> initializerNames = postInitializer.apply(kbean);
            if (LogSettings.INSTANCE.inspect) {
                for (String initializerName : initializerNames) {
                    postInitializeText.add("    "  + KBean.name(beanClass), initializerName);
                }
            }
        }
        if (LogSettings.INSTANCE.inspect) {
            JkLog.info(postInitializeText.toString());
        }
        initialized = true;

        JkLog.debugEndTask();
    }



    void assertValid() {
        JkUtilsAssert.state(dependencyResolver != null, "Dependency resolver can't be null.");
    }

    void run(KBeanAction.Container actionContainer) {

        if (cleanActions.getSize() > 0 && BehaviorSettings.INSTANCE.cleanOutput) {
            JkLog.verbose("Run extra-clean actions");
            cleanActions.run();
        }

        for (KBeanAction kBeanAction : actionContainer.findInvokes()) {
            KBean bean = load(kBeanAction.beanClass);
            JkUtilsReflect.invoke(bean, kBeanAction.method());
        }
    }

    // inject values in fields from command-line and properties.
    List<KBeanAction> injectValuesFromCmdLine(KBean bean) {
        List<KBeanAction> actions = this.cmdLineActions.findSetValues(bean.getClass());
        actions.forEach(action -> setValue(bean, action.member, action.value));
        return actions;
    }

    boolean isInitialized() {
        return initialized;
    }

    KBean getBean(Class<?> kbeanClass) {
        return beans.get(kbeanClass.getName());
    }

    KBeanInitStore getInitStore() {
        return new KBeanInitStore(
                kbeanResolution.defaultKbeanClassName,
                kbeanResolution.implicitKBeanClassName,
                new LinkedList<>(this.beans.keySet())
        );
    }

    static JkProperties constructProperties(Path baseDir) {
        JkProperties result = JkProperties.ofSysPropsThenEnv()
                    .withFallback(readBasePropertiesRecursively(JkUtilsPath.relativizeFromWorkingDir(baseDir)));
        Path globalPropertiesFile = JkLocator.getGlobalPropertiesFile();
        if (Files.exists(globalPropertiesFile)) {
            result = result.withFallback(JkProperties.ofFile(globalPropertiesFile));
        }
        return result;
    }

    /*
     * Reads the properties from the baseDir/jeka.properties and its ancestors.
     *
     * Takes also in account properties defined in parent project dirs if any.
     * this doesn't take in account System and global props.
     */
    static JkProperties readBasePropertiesRecursively(Path baseDir) {
        baseDir = baseDir.toAbsolutePath().normalize();
        JkProperties result = readBaseProperties(baseDir);
        Path parentDir = baseDir.getParent();

        // Stop if parent dir has no jeka.properties file
        if (parentDir != null && Files.exists(parentDir.resolve(JkConstants.PROPERTIES_FILE))) {
            result = result.withFallback(readBasePropertiesRecursively(parentDir));
        }
        return result;
    }

    // Reads the properties from the baseDir/jeka.properties
    static JkProperties readBaseProperties(Path baseDir) {
        Path jekaPropertiesFile = baseDir.resolve(JkConstants.PROPERTIES_FILE);
        if (Files.exists(jekaPropertiesFile)) {
            return JkProperties.ofFile(jekaPropertiesFile);
        }
        return JkProperties.EMPTY;
    }

    @Override
    public String toString() {
        return String.format("JkRunbase{ baseDir=%s, beans=%s }", relBaseDir(), beans.keySet());
    }

    private <T extends KBean> T instantiateKBean(Class<T> beanClass) {

        // Record the instantiation to allow visual tracking of initialization activity.
        this.effectiveActions.add(KBeanAction.ofInit(beanClass));

        CURRENT.set(this);
        T bean = JkUtilsReflect.newInstance(beanClass);
        CURRENT.remove();

        // This way KBeans are registered in the order they have been requested for instantiation,
        // and not the order they have finished to be instantiated.
        this.beans.put(beanClass.getName(), bean);

        // Inject values annotated with raw @JkInject
        Injects.injectLocalKBeans(bean);

        // Apply the defaultProvider defined in method annotated with @JkDefaultProvider
        this.preInitializer.accept(bean);

        // We must inject fields after instance creation cause in the KBean
        // constructor, fields of child classes are not yet initialized.
        this.effectiveActions.addAll(injectDefaultsFromProps(bean));
        this.effectiveActions.addAll(injectValuesFromCmdLine(bean));

        try {
            bean.init();
        } catch (RuntimeException | IllegalAccessError e) {
            if (!BehaviorSettings.INSTANCE.forceMode) {
                throw e;
            }
            JkLog.warn("Can't instantiate bean %s due to %s.", beanClass.getName(), e.getMessage());
        }

        return bean;
    }

    private <T extends KBean> T loadInternal(Class<T> beanClass) {
        JkUtilsAssert.argument(beanClass != null, "KBean class cannot be null.");
        T result = (T) beans.get(beanClass.getName());
        if (result == null) {
            String relBaseDir = relBaseDir().toString();
            String subBaseLabel = relBaseDir.isEmpty() ? "" : "[" + relBaseDir + "]";
            JkLog.debugStartTask("Instantiate KBean %s %s", beanClass.getName(), subBaseLabel);
            result = this.instantiateKBean(beanClass);
            JkLog.debugEndTask();
        }
        return result;
    }

    private boolean isKBeanClassPresent(Class<? extends KBean> beanClass) {
        if (cmdLineActions.findInvolvedKBeanClasses().contains(beanClass) ||
                this.kbeanInitDeclaredInProps.contains(beanClass)) {
            return true;
        }
        return beans.values().stream()
                .map(Object::getClass)
                .anyMatch(ProjectKBean.class::equals);
    }

    private Path relBaseDir() {
        if (MASTER.baseDir != null && baseDir.isAbsolute()) {
            return MASTER.baseDir.relativize(baseDir);
        }
        return baseDir;
    }

    private static void setValue(Object target, String propName, Object value) {
        if (propName.contains(".")) {
            String first = JkUtilsString.substringBeforeFirst(propName, ".");
            String remaining = JkUtilsString.substringAfterFirst(propName, ".");
            Object child = JkUtilsReflect.getFieldValue(target, first);
            if (child == null) {
                String msg = String.format(
                        "Compound property '%s' on class '%s' should not value 'null'" +
                        " right after been instantiate.%n. Please instantiate this property in %s constructor",
                        first, target.getClass().getName(), target.getClass().getSimpleName());
                throw new JkException(msg);
            }
            setValue(child, remaining, value);
            return;
        }
        Field field = JkUtilsReflect.getField(target.getClass(), propName);
        JkUtilsAssert.state(field != null, "Null field found for class %s and field %s",
                target.getClass().getName(), propName);
        JkUtilsReflect.setFieldValue(target, field, value);
    }

    /*
     * Note: sys props cannot be resolved at command-line parsing time,
     * cause beans may be loaded at init or exec time.
     */
    private List<KBeanAction> injectDefaultsFromProps(KBean kbean) {

        Class<? extends KBean> kbeanClass = kbean.getClass();
        List<KBeanAction> result = new LinkedList<>();
        JkBeanDescription desc = JkBeanDescription.of(kbeanClass);

        CommandLine commandLine = new CommandLine(PicocliCommands.fromKBeanDesc(desc));
        commandLine.setDefaultValueProvider(optionSpec -> getDefaultFromProps(optionSpec, desc));
        commandLine.parseArgs();
        CommandLine.Model.CommandSpec commandSpec = commandLine.getCommandSpec();

        for (JkBeanDescription.BeanField beanField : desc.beanFields) {
            CommandLine.Model.OptionSpec optionSpec = commandSpec.findOption(beanField.name);
            Object value = optionSpec.getValue();
            if (value != null) {  // cannot set "null" on non-primitive
                setValue(kbean, beanField.name, value);
            }
            result.add(KBeanAction.ofSetValue(kbeanClass, beanField.name, value, "properties"));

        }
        return result;
    }

    private String getDefaultFromProps(CommandLine.Model.ArgSpec argSpec, JkBeanDescription desc) {
        CommandLine.Model.OptionSpec optionSpec = (CommandLine.Model.OptionSpec) argSpec;
        JkBeanDescription.BeanField beanField = desc.beanFields.stream()
                .filter(beanField1 -> beanField1.name.equals(optionSpec.longestName()))
                .findFirst().orElseThrow(
                        () -> new IllegalStateException("Cannot find field " + optionSpec.longestName()
                                + " in bean " + desc.kbeanClass.getName()));

        // from explicit ENV VAR or sys props defined with @JkInjectProperty
        if (beanField.injectedPropertyName != null) {
            if (properties.get(beanField.injectedPropertyName) != null) {
                return System.getenv(beanField.injectedPropertyName);
            }
        }

        // from property names formatted as '@kbeanName.field.name='
        List<String> acceptedNames = new LinkedList<>(KBean.acceptedNames(desc.kbeanClass));
        if (desc.kbeanClass.getName().equals(kbeanResolution.defaultKbeanClassName)) {
            acceptedNames.add("");
        }
        for (String acceptedName : acceptedNames) {
            String candidateProp = propNameForField(acceptedName, beanField.name);
            String value = this.properties.get(candidateProp);
            if (value != null) {
                return value;
            }
        }

        // from bean value instantiation*
        return null;
    }

    private List<Class<? extends KBean>> kbeansToInitFromProps() {
        List<String> kbeanClassNames = properties.getAllStartingWith(PROP_KBEAN_PREFIX, false).keySet().stream()
                .filter(key -> !key.contains("."))
                .map(key -> kbeanResolution.findKbeanClassName(key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        List<Class<? extends KBean>> actions = new LinkedList<>();
        for (String className : kbeanClassNames) {
            Class<? extends KBean> kbeanClass = JkClassLoader.ofCurrent().load(className);
            actions.add(kbeanClass);
        }
        return actions;
    }

    private static String propNameForField(String kbeanName, String fieldName) {
        return PROP_KBEAN_PREFIX + kbeanName + "." + fieldName;
    }

}
