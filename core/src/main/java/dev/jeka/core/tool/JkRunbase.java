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
import dev.jeka.core.api.system.JkAnsi;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.text.Jk2ColumnsText;
import dev.jeka.core.api.utils.*;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    static final String PROP_KBEAN_PREFIX = "@";

    static final String PROP_KBEAN_OFF = "off";

    private static JkRunbase MASTER;

    private final Path baseDir; // Relative Path

    private KBeanResolution kbeanResolution;

    private JkDependencyResolver dependencyResolver;

    private JkPathSequence classpath;

    private JkPathSequence exportedClasspath;

    private JkDependencySet exportedDependencies;

    private JkDependencySet fullDependencies;

    // Contains the runbase children
    private RunbaseGraph runbaseGraph;

    // Note: An empty container has to be present at instantiation time for sub-runBases, as
    // they are not initialized with any KbeanActions.
    private KBeanAction.Container cmdLineActions = new KBeanAction.Container();

    // TODO remove when load() will be prevented at initialisation time
    private List<Class<? extends KBean>> kbeanInitDeclaredInProps = new LinkedList<>();

    // Actions effectively run during initialization (for logging purpose)
    private final KBeanAction.Container effectiveActions = new KBeanAction.Container();

    private final JkProperties properties;

    private final JkRunnables cleanActions = JkRunnables.of().setLogTasks(JkLog.isDebug());

    // We use class name as a key because using `Class` objects as a key may lead
    // in duplicate initialization in some circumstances where several classloader
    // are present (this has happened when using "jeka aKbean: --doc").
    private final Map<String, KBean> beans = new LinkedHashMap<>();

    private PreInitializer preInitializer = PreInitializer.of(Collections.emptyList());

    private final PostInitializer postInitializer = PostInitializer.of();

    private boolean initialized;

    private boolean cleanActionsExecuted;

    JkRunbase(Path baseDir) {
        this.baseDir = baseDir;
        this.properties = PropertiesHandler.constructRunbaseProperties(baseDir);
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
     * Finds and returns a child JkRunbase instance with the specified name from the master runbase.
     *
     * @param name the name (the relative path from this runbase) of the runbase to find.
     * @return the child JkRunbase instance with the specified name, or null if no such child runbase exists
     */
    public JkRunbase findRunbase(String name) {
        Path masterRelativePath = this.relBaseDir().resolve(name).normalize();
        return MASTER.getChildRunbase(masterRelativePath.toString());
    }

    /**
     * Retrieves a list of injected runbases associated with the current object's relative base directory.
     *
     * @return a list of injected {@link JkRunbase} objects derived from the runbase graph based on the
     *         relative path of the master directory.
     */
    public List<JkRunbase> getInjectedRunbases() {
        Path masterRelativePath = this.relBaseDir();
        return MASTER.runbaseGraph.getInjectedRunbases(masterRelativePath.toString());
    }

    /**
     * Retrieves the list of child {@code JkRunbase} instances associated with the current runbase.
     * This method should only be called on the parent runbase; otherwise, an exception will be thrown.
     *
     * @return a list of {@code JkRunbase} objects representing the child runbases of the current runbase
     * @throws IllegalStateException if the method is invoked on a non-parent runbase
     */
    public List<JkRunbase> getChildRunbases() {
        JkUtilsAssert.state(MASTER.getBaseDir().equals(this.getBaseDir()),
                "loadChildren() can only be called on the parent runbase.");
        List<JkRunbase> result = runbaseGraph.getChildren();
        JkLog.debug("Child runbases of %s are: %s", this.baseDir, result.stream().map(JkRunbase::getBaseDir).collect(Collectors.toList()));
        return result;
    }

    /**
     * Finds and returns a child {@code JkRunbase} instance with the specified name.
     * This method can only be invoked on the parent runbase. Otherwise, an exception will be thrown.
     *
     * @param name the name of the child runbase to retrieve
     * @return the child {@code JkRunbase} instance with the specified name,
     *         or {@code null} if no such child exists
     * @throws IllegalStateException if invoked on a non-parent runbase
     */
    public JkRunbase getChildRunbase(String name) {
        JkUtilsAssert.state(MASTER.getBaseDir().equals(this.getBaseDir()),
                "loadChildren() can only be called on the parent runbase.");
        return runbaseGraph.getRunbase(name);
    }

    /**
     * Loads KBeans of the specified type from all child runbases of the current runbase.
     * This method must be invoked on the parent runbase; otherwise, an exception is thrown.
     *
     * @param <T>       the type of KBean to load, which must extend the {@code KBean} class
     * @param beanClass the {@code Class} object representing the type of KBean to load
     * @return a list of KBeans of the specified type found in the child runbases
     * @throws IllegalStateException if the method is invoked on a non-parent runbase
     */
    public <T extends KBean> List<T> loadChildren(Class<T> beanClass) {
        JkUtilsAssert.state(MASTER.getBaseDir().equals(this.getBaseDir()),
                "loadChildren() can only be called on the parent runbase.");
        return getChildRunbases().stream().map(runBase -> runBase.load(beanClass)).collect(Collectors.toList());
    }

    /**
     * Finds and retrieves all child KBeans of the specified type from the current runbase's child runbases.
     * This method should only be called on the parent runbase; otherwise, it will throw an exception.
     *
     * @param <T>       the type of KBean being searched for, which must extend the {@code KBean} class
     * @param beanClass the {@code Class} object representing the type of the KBean to find
     * @return a list of KBeans of the specified type found in the child runbases
     */
    public <T extends KBean> List<T> findChildren(Class<T> beanClass) {
        JkUtilsAssert.state(MASTER.getBaseDir().equals(this.getBaseDir()),
                "findChildren() can only be called on the parent runbase.");
        return getChildRunbases().stream()
                .map(runBase -> runBase.find(beanClass))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
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
     * Determines the class of the buildable KBean to use within the current run base.
     * The method evaluates available KBean classes and directory structures in order
     * to decide the appropriate KBean type.
     *
     * @return A class object representing a subclass of {@code KBean}. The possible
     * return values are {@code ProjectKBean.class} or {@code BaseKBean.class},
     * depending on the presence of the classes or a specific directory structure.
     * Never returns <code>null</code>, but BaseKBean in last resort.
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
     * <p>
     * Important: This method should be called inside the a KBean #init() method in order it
     * can be taken in account by the execution engine.
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

    void init(KBeanAction.Container cmdLineActionContainer, boolean master) {

        // Add default KBean
        Class<? extends KBean> defaultKBeanClass = kbeanResolution.findDefaultBeanClass();

        if (master) {
            runbaseGraph = RunbaseGraph.of(defaultKBeanClass, this);
        }

        if (LogSettings.INSTANCE.inspect) {
            if (master) {
                JkLog.startTask("init-" + JkAnsi.of().fg(JkAnsi.Color.MAGENTA).a("main").reset() + "-base");
            } else {
                JkLog.startTask("init-child-runbase " +
                        JkAnsi.of().fg(JkAnsi.Color.MAGENTA).a(toRelPathName()).reset());
            }
        }

        // Add default kbean
        KBeanAction.Container actions = cmdLineActionContainer;
        actions = actions.withKBeanInitialization(defaultKBeanClass);

        // Remove Kbean explicitly disabled
        List<String> kbeansToExclude = kbeansToExclude();
        actions = actions.withoutAnyOfKBeanClasses(kbeansToExclude);

        if (JkLog.isDebug()) {
            JkLog.debug("Initialize Runbase with \n" + actions.toColumnText());
            JkLog.debug("Default KBean class name: " + kbeanResolution.defaultKbeanClassName);
            JkLog.debug("All KBean classes: \n  " + String.join("\n  ", kbeanResolution.allKbeanClassNames));
            JkLog.debug("All local KBean classes: " + String.join("\n  ", kbeanResolution.localKBeanClassNames));
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

        // Register pre-initializers from classes to initialize
        // KBeans will be pre-initialized at instantiation time
        this.preInitializer = PreInitializer.of(classesToInit);

        // Initialize KBeans then register their post-initializers
        for (Class<? extends KBean> beanClass : classesToInit) {
            KBean kbean = loadInternal(beanClass);
            this.postInitializer.addPostInitializerCandidate(kbean);
        }

        if (LogSettings.INSTANCE.inspect) {
            String classNames = classesToInit.stream()
                    .map(KBean::name)
                    .collect(Collectors.joining(", "));
            JkLog.info("KBeans to initialize: ");
            if (!classesToInit.isEmpty()) {
                JkLog.info("    " + classNames);
            }
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
            JkLog.info("KBeans Initialization:");
            if (!preInitializedKBeans.isEmpty()) {
                JkLog.info(this.effectiveActions.toColumnText()
                        .setSeparator(" | ")
                        .setMarginLeft("   | ")
                        .toString());
            }
        }
        initialized = true;

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
                    postInitializeText.add("    " + KBean.name(beanClass), initializerName);
                }
            }
        }
        if (LogSettings.INSTANCE.inspect && !postInitializeText.toString().trim().isEmpty()) {
            JkLog.info(postInitializeText.toString());
        }
        JkLog.debugEndTask();
        if (LogSettings.INSTANCE.inspect) {
            JkLog.endTask();
        }
    }

    void assertValid() {
        JkUtilsAssert.state(dependencyResolver != null, "Dependency resolver can't be null.");
    }

    void run(KBeanAction.Container actionContainer, boolean master) {

        // Guard if '-cb=" is activated
        String childBaseFilter = BehaviorSettings.INSTANCE.childBase;


        List<KBeanAction.Container> splitContainers = actionContainer.splitByKBean();
        final boolean needSplit;
        if (master) {
            if (runbaseGraph.declaresChildren()) {
                List<JkRunbase> childRunbases = new LinkedList<>(runbaseGraph.getChildren());

                if (childBaseFilter == null) {
                    String msg = JkAnsi.of().fg(JkAnsi.Color.BLUE).a("Run child bases in sequence:").reset().toString();
                    JkLog.info("%s %n    %s", msg, childRunbases.stream()
                            .map(JkRunbase::toRelPathName).collect(Collectors.joining("\n    ")));
                    JkLog.info("    *parent base*");
                } else {
                    List<String> allRelPaths = childRunbases.stream()
                            .map(JkRunbase::toRelPathName)
                            .collect(Collectors.toList());
                    if (!childBaseFilter.equals(".") && !allRelPaths.contains(childBaseFilter)) {
                        throw new JkException("No child base found at location %s", childBaseFilter);
                    }
                }
                needSplit = splitContainers.size() > 1;
            } else {
                needSplit = false;
            }
        } else {
            needSplit = splitContainers.size() > 1;
        }

        if (needSplit) {
            actionContainer.splitByKBean().forEach(splitContainer -> {
                JkLog.info(JkAnsi.of().fg(JkAnsi.Color.BLUE).a("Running KBean methods: ").reset().toString()
                        + splitContainer.toCmdLineRun());
                runUnit(splitContainer, master);
            });
        } else {
            runUnit(actionContainer, master);
        }
    }

    private void runUnit(KBeanAction.Container actionContainer, boolean master) {

        // Guard if '-cb=" is activated
        String childBaseFilter = BehaviorSettings.INSTANCE.childBase;

        KBeanAction.Container runActions = actionContainer;

        if (master && runbaseGraph.declaresChildren()) {  // We are in a parent runbase

            // Run child runbases
            // -- We remove from child actions, parent local KBeans
            KBeanAction.Container childActions = runActions
                    .withoutAnyOfKBeanClasses(kbeanResolution.localKBeanClassNames);
            List<JkRunbase> childRunbases = new LinkedList<>(runbaseGraph.getChildren());

            // filter if -cb= option
            if (childBaseFilter != null) {
                for (ListIterator<JkRunbase> it = childRunbases.listIterator(); it.hasNext(); ) {
                    JkRunbase childBase = it.next();
                    String relPath = childBase.toRelPathName();
                    if (!childBaseFilter.equals(relPath)) {
                        JkLog.verbose("Child base filtered on %s, skip child base %s", childBaseFilter, relPath);
                        it.remove();
                    }
                }
            }

            childRunbases.forEach(runbase -> {
                String relPath = runbase.toRelPathName();
                KBeanAction.Container runChildActions =
                        childActions.withoutAnyOfKBeanClasses(runbase.kbeansToExclude());
                if (!runChildActions.toList().isEmpty()) {
                    String msgPrefix = "run-child-base " + JkAnsi.of().fg(JkAnsi.Color.MAGENTA).a(relPath).reset();
                    JkLog.startTask(msgPrefix + " " + runChildActions.toCmdLineRun());
                    runbase.run(runChildActions, false);
                    JkLog.endTask();
                } else if (JkLog.isVerbose()) {
                    String msgPrefix = "run-child-base " + JkAnsi.of().fg(JkAnsi.Color.MAGENTA).a(relPath);
                    JkLog.verbose(msgPrefix + " -skipped-");
                }
            });

            // Guard on -cb= option
            if (childBaseFilter != null && !childBaseFilter.equals(".")) {
                JkLog.verbose("Parent base filtered on %s, skip main base", childBaseFilter);
                return;
            }

            // For parent runbase, we only keep the actions for KBeans that has been initialized
            List<String> initializedClasses = this.beans.values().stream()
                    .map(Object::getClass)
                    .map(Class::getName)
                    .collect(Collectors.toList());
            runActions = runActions.withOnlyKBeanClasses(initializedClasses);
            if (runActions.toList().isEmpty()) {
                JkLog.verbose("run-parent-base: -skipped-");
                return;
            }
            JkLog.startTask("run-" + JkAnsi.of().fg(JkAnsi.Color.MAGENTA).a("parent").reset()
                    + "-base " + runActions.toCmdLineRun());
        }

        if (cleanActions.getSize() > 0 && BehaviorSettings.INSTANCE.cleanOutput && !cleanActionsExecuted) {
            JkLog.verbose("Run extra-clean actions");
            cleanActions.run();
            cleanActionsExecuted = true;
        }

        for (KBeanAction kBeanAction : runActions.findInvokes()) {
            KBean bean = load(kBeanAction.beanClass);
            JkUtilsReflect.invoke(bean, kBeanAction.method());
        }
        if (master && runbaseGraph.declaresChildren()) {
            JkLog.endTask();
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
                new LinkedList<>(this.beans.keySet())
        );
    }

    @Override
    public String toString() {
        return String.format("JkRunbase{ baseDir=%s, beans=%s }", relBaseDir(), beans.keySet());
    }

    public String toRelPathName() {
        return MASTER.getBaseDir().toAbsolutePath().normalize().relativize(baseDir.toAbsolutePath()).toString();
    }

    /**
     * Checks whether the force mode is enabled in the behavior settings.
     * Force mode allows ignoring jeka-src compilation and dependency resolution failures.
     *
     * @return true if force mode is enabled, false otherwise.
     */
    public boolean isForceMode() {
        return BehaviorSettings.INSTANCE.forceMode;
    }

    static void setMaster(JkRunbase runbase) {
        MASTER = runbase;
    }

    private <T extends KBean> T instantiateKBean(Class<T> beanClass) {

        // Record the instantiation to allow visual tracking of initialization activity.
        this.effectiveActions.add(KBeanAction.ofInitialization(beanClass));

        CURRENT.set(this);
        T bean = JkUtilsReflect.newInstance(beanClass);
        CURRENT.remove();

        // This way KBeans are registered in the order they have been requested for instantiation,
        // and not the order they have finished to be instantiated.
        this.beans.put(beanClass.getName(), bean);

        // Inject values annotated with raw @JkInject
        Injects.injectAnnotatedFields(bean);

        // Apply the defaultProvider defined in method annotated with @JkDefaultProvider
        this.preInitializer.accept(bean);

        // We must inject fields after instance creation cause in the KBean
        // constructor, fields of child classes are not yet initialized.
        this.effectiveActions.addAll(injectValuesFromProps(bean));
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
        Path result = Paths.get("").toAbsolutePath().relativize(baseDir);
        if (result.getFileName().toString().isEmpty()) {
            result = Paths.get(".");
        }
        return result;
    }

     private static void setValue(Object target, String propName, Object value) {
        BeanUtils.setValue(target, propName, value);
     }

    /*
     * Note: sys props cannot be resolved at command-line parsing time,
     * cause beans may be loaded at init or exec time.
     */
    private List<KBeanAction> injectValuesFromProps(KBean kbean) {

        Class<? extends KBean> kbeanClass = kbean.getClass();
        List<KBeanAction> result = new LinkedList<>();
        JkBeanDescription desc = JkBeanDescription.of(kbeanClass);

        List<String> propNames = BeanUtils.extractKBeanPropertyNamesFromProps(kbeanClass, this.properties);
        List<JkBeanDescription.BeanField> beanFields = BeanUtils.enhanceWithMultiValues(desc.beanFields, propNames);
        CommandLine commandLine = new CommandLine(PicocliCommands.fromKBeanDesc(desc, propNames));
        commandLine.setDefaultValueProvider(optionSpec -> getDefaultFromProps(optionSpec, kbeanClass, beanFields));
        commandLine.parseArgs();
        CommandLine.Model.CommandSpec commandSpec = commandLine.getCommandSpec();

        for (JkBeanDescription.BeanField beanField : beanFields) {
            CommandLine.Model.OptionSpec optionSpec = commandSpec.findOption(beanField.name);
            Object value = optionSpec.getValue();
            if (value != null) {  // cannot set "null" on non-primitive
                setValue(kbean, beanField.name, value);
            }
            result.add(KBeanAction.ofSetValue(kbeanClass, beanField.name, value, "properties"));

        }
        return result;
    }

    private String getDefaultFromProps(CommandLine.Model.ArgSpec argSpec,
                                       Class<? extends KBean> kbeanClass,
                                       List<JkBeanDescription.BeanField> beanFields) {
        CommandLine.Model.OptionSpec optionSpec = (CommandLine.Model.OptionSpec) argSpec;

        JkBeanDescription.BeanField beanField = beanFields.stream()
                .filter(beanField1 -> beanField1.name.equals(optionSpec.longestName()))
                .findFirst().orElseThrow(
                        () -> new IllegalStateException("Cannot find field " + optionSpec.longestName()
                                + " in bean " + kbeanClass.getName()));

        // from explicit ENV VAR or sys props defined with @JkInjectProperty
        if (beanField.injectedPropertyName != null) {
            if (properties.get(beanField.injectedPropertyName) != null) {
                return System.getenv(beanField.injectedPropertyName);
            }
        }

        // from property names formatted as '@kbeanName.field.name='
        List<String> acceptedNames = new LinkedList<>(KBean.acceptedNames(kbeanClass));
        if (kbeanClass.getName().equals(kbeanResolution.defaultKbeanClassName)) {
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
                .filter(key -> {
                    String propName = PROP_KBEAN_PREFIX + key;
                    String value = JkUtilsString.nullToEmpty(properties.get(propName)).trim();
                    return !PROP_KBEAN_OFF.equals(value);
                })
                .map(key -> kbeanResolution.findKbeanClassName(key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        List<Class<? extends KBean>> actions = new LinkedList<>();
        for (String className : kbeanClassNames) {
            try {
                Class<? extends KBean> kbeanClass =
                        (Class<? extends KBean>) JkClassLoader.ofCurrent().get().loadClass(className);
                actions.add(kbeanClass);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Cannot load KBean class " + className + " from base '"
                        + this.toRelPathName() + "'", e);
            }
        }
        return actions;
    }

    private List<String> kbeansToExclude() {
        return properties.getAllStartingWith(PROP_KBEAN_PREFIX, false).keySet().stream()
                .filter(key -> !key.contains("."))
                .filter(key -> {
                    String propName = PROP_KBEAN_PREFIX + key;
                    String value = JkUtilsString.nullToEmpty(properties.get(propName)).trim();
                    return PROP_KBEAN_OFF.equals(value);
                })
                .map(key -> kbeanResolution.findKbeanClassName(key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static String propNameForField(String kbeanName, String fieldName) {
        return PROP_KBEAN_PREFIX + kbeanName + "." + fieldName;
    }

}
