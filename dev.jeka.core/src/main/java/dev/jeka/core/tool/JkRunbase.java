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
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.project.JkBuildable;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

import java.lang.reflect.Field;
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

    // Experiment for invoking 'KBean#init()' method lately, once all KBean has been instantiated
    // Note : Calling all KBeans init() methods in a later stage then inside 'load' methods
    //        leads in difficult problems as the order the KBeans should be initialized.
    ///private static final boolean LATE_INIT = false;

    private static final String PROP_KBEAN_PREFIX = "@";

    private static final ThreadLocal<Path> BASE_DIR_CONTEXT = new ThreadLocal<>();

    private static final Map<Path, JkRunbase> SUB_RUNTIMES = new LinkedHashMap<>();

    private static Path masterBaseDir;

    private static Engine.KBeanResolution kbeanResolution;

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

    private List<Class<? extends KBean>> kbeanInitDeclaredInProps = new LinkedList<>();

    private final KBeanAction.Container effectiveActions = new KBeanAction.Container();

    private final JkProperties properties;

    private final Map<Class<? extends KBean>, KBean> beans = new LinkedHashMap<>();

    private JkRunbase(Path baseDir) {
        this.baseDir = baseDir;
        this.properties = constructProperties(baseDir);
    }

    /**
     * Returns the JkRunbase instance associated with the specified project base directory.
     */
    public static JkRunbase get(Path baseDir) {
        return SUB_RUNTIMES.computeIfAbsent(baseDir, path -> new JkRunbase(path));
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
     * Instantiates the specified KBean into this runbase, if it is not already present. <p>
     * As KBeans are singleton within a runbase, this method has no effect if the bean is already loaded.
     * @param beanClass The class of the KBean to load.
     * @return This object for call chaining.
     */
    public <T extends KBean> T load(Class<T> beanClass) {
        JkUtilsAssert.argument(beanClass != null, "KBean class cannot be null.");
        T result = (T) beans.get(beanClass);
        if (result == null) {
            String relBaseDir = relBaseDir().toString();
            String subBaseLabel = relBaseDir.isEmpty() ? "" : "[" + relBaseDir + "]";
            JkLog.debugStartTask("Instantiate KBean %s %s", beanClass.getName(), subBaseLabel);
            Path previousBaseDir = BASE_DIR_CONTEXT.get();
            BASE_DIR_CONTEXT.set(baseDir);  // without this, projects nested with more than 1 level failed to get proper base dir
            result = this.instantiateKBean(beanClass);
            BASE_DIR_CONTEXT.set(previousBaseDir);
            JkLog.debugEndTask();
        }
        return result;
    }

    /**
     * Returns the KBean of the exact specified class, present in this runbase.
     */
    public <T extends KBean> Optional<T> find(Class<T> beanClass) {
        if (cmdLineActions.findInvolvedKBeanClasses().contains(beanClass) ||
                this.kbeanInitDeclaredInProps.contains(beanClass)) {
            return Optional.of(load(beanClass));
        }
        return (Optional<T>) Optional.ofNullable(beans.get(beanClass));
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
     * Finds either a ProjectKBean or a BaseKBean present in this runbase.
     * The result is returned as a JkBuildable abstraction or <code>null</code>
     * if no such structure is discovered in this runbase.
     */
    public JkBuildable findBuildable() {
        Optional<ProjectKBean> optionalProjectKBean = this.find(ProjectKBean.class);
        if (optionalProjectKBean.isPresent()) {
            return optionalProjectKBean.get().project.asBuildable();
        }
        if (this.find(BaseKBean.class).isPresent()) {
            return this.find(BaseKBean.class).get().asBuildable();
        }
        if (Files.isDirectory(this.getBaseDir().resolve("src"))) {
            return this.load(ProjectKBean.class).project.asBuildable();
        }
        return null;
    }

    /**
     * @see #findBuildable()
     */
    public JkBuildable getBuildable() {
        return Optional.ofNullable(findBuildable()).orElseThrow(
                () -> new JkException("Cannot find project or base KBean in this runbase " + this.getBaseDir())
        );
    }



    void setDependencyResolver(JkDependencyResolver resolverArg) {
        dependencyResolver = resolverArg;
    }

    void setClasspath(JkPathSequence pathSequence) {
        this.classpath = pathSequence;
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

    KBeanAction.Container getEffectiveActions() {
        return effectiveActions;
    }

    void init(KBeanAction.Container cmdLineActionContainer) {
        if (JkLog.isDebug()) {
            JkLog.debug("Initialize JkRunbase with \n" + cmdLineActionContainer.toColumnText());
        }
        this.cmdLineActions = cmdLineActionContainer;

        JkLog.debugStartTask("Register KBeans");

        // KBeans init from cmdline
        List<Class<? extends KBean>> kbeansToInit = cmdLineActionContainer.toList().stream()
                .map(kbeanAction -> kbeanAction.beanClass)
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));

        this.kbeanInitDeclaredInProps = kbeansToInitFromProps();

        // KBeans init from props
        kbeansToInit.addAll(this.kbeanInitDeclaredInProps);

        kbeansToInit.stream().distinct().forEach(this::load);  // register kbeans

        JkLog.debugEndTask();

        // Once KBeans has been initialised, #postInit is invoked on each,
        // so they can act upon final settings of other KBeans.
        beans.values().forEach(KBean::postInit);
    }

    void assertValid() {
        JkUtilsAssert.state(dependencyResolver != null, "Dependency resolver can't be null.");
    }

    void run(KBeanAction.Container actionContainer) {
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

    static JkRunbase getCurrentContextBaseDir() {
        return get(getBaseDirContext());
    }

    static void setBaseDirContext(Path baseDir) {
        JkUtilsAssert.argument(baseDir == null || Files.exists(baseDir),"Base dir " + baseDir + " not found.");
        BASE_DIR_CONTEXT.set(baseDir);
    }

    static void setMasterBaseDir(Path baseDir) {
        masterBaseDir = baseDir;
    }

    static void setKBeanResolution(Engine.KBeanResolution kbeanResolution) {
        JkRunbase.kbeanResolution = kbeanResolution;
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

        this.effectiveActions.add(KBeanAction.ofInit(beanClass));

        T bean = JkUtilsReflect.newInstance(beanClass);

        // This way KBeans are registered in the order they have been requested for instantiation,
        // and not the order they have finished to be instantiated.
        this.beans.put(beanClass, bean);

        // We must inject fields after instance creation cause in the KBean
        // constructor, fields of child classes are not yet initialized.
        this.effectiveActions.addAll(injectDefaultsFromProps(bean));
        this.effectiveActions.addAll(injectValuesFromCmdLine(bean));

        bean.init();
        return bean;
    }

    private static Path getBaseDirContext() {
        return Optional.ofNullable(BASE_DIR_CONTEXT.get()).orElseGet(() -> {
            setBaseDirContext(Paths.get(""));
            return BASE_DIR_CONTEXT.get();
        });
    }

    private Path relBaseDir() {
        if (masterBaseDir != null && baseDir.isAbsolute()) {
            return masterBaseDir.relativize(baseDir);
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
        KBeanDescription desc = KBeanDescription.of(kbeanClass, true);

        CommandLine commandLine = new CommandLine(PicocliCommands.fromKBeanDesc(desc));
        commandLine.setDefaultValueProvider(optionSpec -> getDefaultFromProps(optionSpec, desc));
        commandLine.parseArgs();
        CommandLine.Model.CommandSpec commandSpec = commandLine.getCommandSpec();

        for (KBeanDescription.BeanField beanField : desc.beanFields) {
            CommandLine.Model.OptionSpec optionSpec = commandSpec.findOption(beanField.name);
            Object value = optionSpec.getValue();
            if (!Objects.equals(beanField.defaultValue, value)) {
                setValue(kbean, beanField.name, value);
                result.add(KBeanAction.ofSetValue(kbeanClass, beanField.name, value, "properties"));
            }
        }
        return result;
    }

    private String getDefaultFromProps(CommandLine.Model.ArgSpec argSpec, KBeanDescription desc) {
        CommandLine.Model.OptionSpec optionSpec = (CommandLine.Model.OptionSpec) argSpec;
        KBeanDescription.BeanField beanField = desc.beanFields.stream()
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
        if (desc.kbeanClass.getName().equals(kbeanResolution.defaultKbeanClassname)) {
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
