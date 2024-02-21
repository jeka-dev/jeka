package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.builtins.base.BaseKBean;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/*
 * Core of the execution engine. Responsible for :
 *     - Parse source to extract compilation directive and dependencies
 *     - Compile code on the fly
 *     - Resolve command line actions to executable tasks
 *     - Run the requested actions
 */
class Engine {

    private static final String[] PRIVATE_GLOB_PATTERN = new String[] { "**/_*", "_*"};

    private static final JkPathMatcher JAVA_JEKA_SRC_MATCHER = JkPathMatcher.of(true,"**.java")
            .and(false,PRIVATE_GLOB_PATTERN);

    private static final JkPathMatcher JAVA_CLASS_MATCHER = JkPathMatcher.of(true,"**.class")
            .and(false,PRIVATE_GLOB_PATTERN);

    private static final JkPathMatcher KOTLIN_JEKA_SRC_MATCHER = JkPathMatcher.of(true,"**.kt")
            .and(false, PRIVATE_GLOB_PATTERN);

    private static final JkPathMatcher JAVA_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.java")
            .and(false, "**/_*", "_*");

    private static final JkPathMatcher KOTLIN_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.kt")
            .and(false, "**/_*", "_*");

    static final JkPathMatcher JAVA_OR_KOTLIN_SOURCE_MATCHER = JAVA_DEF_SOURCE_MATCHER.or(KOTLIN_DEF_SOURCE_MATCHER);



    private static final String NO_JDK_MSG = String.format(
            "The running Java platform %s is not a valid JDK (no javac found).%n" +
            "Please provide a JDK by specifying 'jeka.java.version' in jeka.properties file.%n" +
            "Or set JAVA_HOME environment variable to a valid JDK.", System.getProperty("java.home"));

    private static final Map<Path, Engine> MAP = new HashMap<>();

    final Path baseDir;

    private final boolean skipJekaSrc;

    private final JkDependencyResolver dependencyResolver;

    private final JkDependencySet commandLineDependencies;

    private final LogSettings logSettings;

    private final BehaviorSettings behaviorSettings;

    // Computed values

    private final JkProperties properties;

    private final Path jekaSrcDir;

    private final Path jekaSrcClassDir;

    private ClasspathSetupResult classpathSetupResult;

    private KBeanResolution kbeanResolution;

    private List<EngineCommand> engineCommands;

    private JkRunbase runbase;

    private Engine(Path baseDir,
                   boolean skipJekaSrc,
                   JkRepoSet downloadRepos,
                   JkDependencySet commandLineDependencies,
                   LogSettings logSettings,
                   BehaviorSettings behaviorSettings) {

        this.baseDir = baseDir;
        this.skipJekaSrc = skipJekaSrc;
        this.commandLineDependencies = commandLineDependencies;
        this.logSettings = logSettings;
        this.behaviorSettings = behaviorSettings;
        this.properties = JkRunbase.constructProperties(baseDir);
        this.jekaSrcDir = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        this.jekaSrcClassDir = baseDir.resolve(JkConstants.JEKA_SRC_CLASSES_DIR);

        // Set dependency resolver
        this.dependencyResolver = JkDependencyResolver.of(downloadRepos)
                .setUseFileSystemCache(true)
                .setFileSystemCacheDir(baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve("jeka-src-deps"));

        // In lenient mode, we ignore dep resolution failure
        this.dependencyResolver.getDefaultParams().setFailOnDependencyResolutionError(
                !behaviorSettings.ignoreCompileFailure);
    }

     static Engine of(Path baseDir, boolean skipJekaSrc, JkRepoSet downloadRepos,
                      JkDependencySet commandLineDependencies, LogSettings logSettings,
                      BehaviorSettings behaviorSettings) {

        Path path = baseDir.toAbsolutePath().normalize();


        // ensure only 1 baseProcessor per base
        return MAP.computeIfAbsent(path,
                key -> new Engine(key, skipJekaSrc, downloadRepos, commandLineDependencies, logSettings, behaviorSettings));
    }

    private static String classNameFromClassFilePath(Path relativePath) {
        final String dotName = relativePath.toString().replace('/', '.');
        return JkUtilsString.substringBeforeLast(dotName, ".");
    }

    private static EngineCommand toEngineCommand(KBeanAction action,
                                          Map<String, Class<? extends KBean>> beanClasses) {
        Class<? extends KBean> beanClass = beanClasses.get(action.beanName);
        JkUtilsAssert.state(beanClass != null, "Can't resolve KBean class for action %s", action);
        return new EngineCommand(action.action, beanClass, action.member, action.value);
    }

    private static Optional<String> firstMatchingClassname(List<String> classNames, String candidate) {
         return classNames.stream()
                 .filter(className -> KBean.nameMatches(className, candidate))
                 .findFirst();
    }

    Engine withBaseDir(Path baseDir) {
        return of(baseDir, this.skipJekaSrc, this.dependencyResolver.getRepos(), this.commandLineDependencies,
                this.logSettings, this.behaviorSettings);
    }

    /*
     * Resolves dependencies and compiles and sources classes contained in jeka/def.
     * It returns a path sequence containing the resolved dependencies and result of compilation.
     */
     ClasspathSetupResult resolveClassPaths() {

        if (classpathSetupResult != null) {
            return classpathSetupResult;
        }

        JkLog.debugStartTask("Resolve classpath for jeka-src");

        if (behaviorSettings.cleanWork) {
            Path workDir = baseDir.resolve(JkConstants.JEKA_WORK_PATH);
            JkLog.debug("Clean .jaka-work directory  %s ", workDir.toAbsolutePath().normalize());
            JkPathTree.of(workDir).deleteContent();
        }

        // That is nice that this clean occurs here, because it will happen for sub-basedir as well.
        if (behaviorSettings.cleanOutput) {
            Path outputDir = baseDir.resolve(JkConstants.OUTPUT_PATH);
            JkPathTree.of(outputDir).deleteContent();
            JkLog.verbose("Clean %s dir", outputDir);
        }

        // Parse info from source code
        JkLog.debugStartTask("Scan jeka-src code for finding dependencies");
        final ParsedSourceInfo parsedSourceInfo = SourceParser.of(this.baseDir).parse();
        JkLog.debugEndTask();

        // Compute and get the classpath from sub-dirs
        List<Engine> subBaseDirs = parsedSourceInfo.importedBaseDirs.stream()
                .map(this::withBaseDir)
                .collect(Collectors.toList());

        JkPathSequence addedClasspathFromSubDirs = subBaseDirs.stream()
                .map(Engine::resolveClassPaths)
                .map(classpathSetupResult -> classpathSetupResult.runClasspath)
                .reduce(JkPathSequence.of(), JkPathSequence::and);

        // compute classpath to look in for finding KBeans
        String classpathProp = properties.get(JkConstants.CLASSPATH_INJECT_PROP, "");
        List<JkDependency> jekaPropsDeps = Arrays.stream(classpathProp.split(" "))
                .flatMap(desc -> Arrays.stream(desc.split(",")))     // handle ' ' or ',' separators
                .map(String::trim)
                .filter(desc -> ! desc.isEmpty())
                .map(desc -> JkDependency.of(baseDir, desc))
                .collect(Collectors.toList());
        JkDependencySet dependencies = commandLineDependencies
                .and(jekaPropsDeps)
                .and(parsedSourceInfo.getDependencies())
                .andVersionProvider(JkConstants.JEKA_VERSION_PROVIDER);

       JkPathSequence kbeanClasspath = JkPathSequence.of(dependencyResolver.resolveFiles(dependencies))
                         .and(JkLocator.getJekaJarPath());

         if (skipJekaSrc) {
             JkLog.debugEndTask();
             this.classpathSetupResult = compileLessClasspathResult(parsedSourceInfo, subBaseDirs, kbeanClasspath);
             return this.classpathSetupResult;
         }

        // compute classpath for compiling 'jeka-src'
        JkPathSequence compileClasspath = kbeanClasspath
                .and(addedClasspathFromSubDirs)
                .and(JkPathTree.of(baseDir.resolve(JkConstants.JEKA_BOOT_DIR)).getFiles());

        // Compile jeka-src
        CompileResult compileResult = compileJekaSrc(compileClasspath, parsedSourceInfo.compileOptions);

        // Add compile result to he run compile
        JkPathSequence runClasspath = compileClasspath.and(compileResult.extraClasspath);

        // If private dependencies has been defined, this means that jeka-src is supposed to host
        // an application that wants to control its classpath. Thus, we only put the dependency explicitly
        // exported.
        final JkPathSequence exportedClasspath;
        final JkDependencySet exportedDependencies;
        if (parsedSourceInfo.hasPrivateDependencies()) {
            exportedDependencies = parsedSourceInfo.getExportedDependencies()
                    .andVersionProvider(JkConstants.JEKA_VERSION_PROVIDER);
            exportedClasspath = JkPathSequence.of(
                    dependencyResolver.resolveFiles(parsedSourceInfo.getExportedDependencies()));
        } else {
            exportedDependencies = parsedSourceInfo.getDependencies();
            exportedClasspath = compileClasspath;
        }

        // Prepare result and return
        this.classpathSetupResult = new ClasspathSetupResult(compileResult.success,
                runClasspath, kbeanClasspath, exportedClasspath, exportedDependencies, subBaseDirs);
        JkLog.debugEndTask();
        return classpathSetupResult;
    }

    KBeanResolution resolveKBeans() {
        if (classpathSetupResult == null) {
            resolveClassPaths();
        }
        if (kbeanResolution != null) {
             return kbeanResolution;
        }

         // Find all KBean class available for this
        List<String> kbeanClassNames = findKBeanClassNames();

        // Filter to find kbeans defined in jeka-src
        List<String> localKbeanClassNames = JkPathTree.of(jekaSrcClassDir).streamBreathFirst()
                 .excludeDirectories()
                 .relativizeFromRoot()
                 .filter(path -> path.getFileName().toString().endsWith(".class"))
                 .map(Engine::classNameFromClassFilePath)
                 .filter(kbeanClassNames::contains)
                 .collect(Collectors.toList());

        DefaultAndInitKBean defaultAndInitKBean = defaultAndInitKbean(kbeanClassNames, localKbeanClassNames);
        kbeanResolution = new KBeanResolution(kbeanClassNames, localKbeanClassNames,
                 defaultAndInitKBean.initKbeanClassName, defaultAndInitKBean.defaultKBeanClassName);
        return kbeanResolution;
    }

    // TODO may merge KBeanAction and Engine commands
    List<EngineCommand> resolveEngineCommand(List<KBeanAction> kBeanActions) {
        if (engineCommands != null) {
            return engineCommands;
        }
        if (this.kbeanResolution == null) {
            resolveKBeans();
        }

        Map<String, Class<? extends KBean>> kbeanClasses = resolveKBeanClasses(kBeanActions);

        // Construct EngineCommands
        this.engineCommands = new LinkedList<>();

        // -- If there is an initBean it should be initialized first
        if (kbeanResolution.initKBeanClassname != null) {
            Class initBeanClass = JkClassLoader.ofCurrent().load(kbeanResolution.initKBeanClassname);
            engineCommands.add(new EngineCommand(EngineCommand.Action.BEAN_INIT, initBeanClass,
                    null, null));
        }

        // -- Add other actions
        kBeanActions.stream()
                .map(action -> toEngineCommand(action, kbeanClasses))
                .forEach(engineCommands::add);

        return engineCommands;
    }

    JkRunbase initRunbase() {
         if (runbase != null) {
             return runbase;
         }
         JkUtilsAssert.state(engineCommands != null, "Resolve engineCommand prior running");

         JkRunbase.setMasterBaseDir(baseDir); // master base dir is used to display relative path on console output
         runbase = JkRunbase.get(baseDir);
         runbase.setDependencyResolver(dependencyResolver);
         runbase.setClasspath(classpathSetupResult.runClasspath);
         runbase.setExportedClassPath(classpathSetupResult.exportedClasspath);
         runbase.setExportedDependencies(classpathSetupResult.exportedDependencies);
         runbase.assertValid(); // fail-fast. bugfix purpose

         // initialise runbase with resolved commands
         runbase.init(engineCommands);
         return runbase;
    }

    void run() {
         if (runbase == null) {
             initRunbase();
         }
         runbase.run(engineCommands);
    }

    DefaultAndInitKBean defaultAndInitKbean(List<String> kbeanClassNames, List<String> localKbeanClassNames) {
         return new DefaultAndInitKBean(kbeanClassNames, localKbeanClassNames);
    }

    JkRunbase getRunbase() {
         return runbase;
    }

    ClasspathSetupResult getClasspathSetupResult() {
        return classpathSetupResult;
    }

    KBeanResolution getKbeanResolution() {
        return kbeanResolution;
    }

    // non-private for testing purpose
    class DefaultAndInitKBean {

         final String initKbeanClassName;

         final String defaultKBeanClassName;

         private DefaultAndInitKBean(List<String> kbeanClassNames, List<String> localKbeanClassNames) {
             String defaultKBeanName = behaviorSettings.kbeanName
                     .orElse(properties.get(JkConstants.DEFAULT_KBEAN_PROP));
             defaultKBeanClassName = firstMatchingClassname(kbeanClassNames, defaultKBeanName)
                     .orElse(localKbeanClassNames.stream().findFirst().orElse(null));

             // The first KBean to be initialized
             if (localKbeanClassNames.contains(defaultKBeanClassName)) {
                 initKbeanClassName = defaultKBeanClassName;
             } else {
                 initKbeanClassName = localKbeanClassNames.stream().findFirst().orElse(defaultKBeanClassName);
             }
         }
    }

    private CompileResult compileJekaSrc(JkPathSequence classpath, List<String> compileOptions) {

         JkPathSequence extraClasspath = JkPathSequence.of();
         boolean hasKotlinSources = hasKotlinSource();

         // Avoid compilation if sources didn't change
         EngineCompilationUpdateTracker updateTracker = new EngineCompilationUpdateTracker(baseDir);
         if (!updateTracker.needRecompile(classpath)) {
             if (hasKotlinSources) {
                 JkPathSequence kotlinStdLibPath = updateTracker.readKotlinLibsFile();
                 extraClasspath = extraClasspath.and(kotlinStdLibPath);
             }
             extraClasspath = extraClasspath.and(jekaSrcClassDir);
             return new CompileResult(true, extraClasspath);
         }

        // Compile Kotlin code if any
        KotlinCompileResult kotlinCompileResult = new KotlinCompileResult(true, JkPathSequence.of());
        if (hasKotlinSource()) {
            kotlinCompileResult = compileWithKotlin(classpath, compileOptions);
            extraClasspath = extraClasspath.and(kotlinCompileResult.kotlinLibPath);
            updateTracker.updateKotlinLibs(kotlinCompileResult.kotlinLibPath);
        }

        // Compile  Java
        boolean javaCompileSuccess = compileJava(classpath, compileOptions);

        // Copy resources
        if (Files.isDirectory(jekaSrcDir)) {
            JkPathTree.of(jekaSrcDir)
                    .andMatching(false, "**/*.java", "*.java", "**/*.kt", "*.kt")
                    .copyTo(jekaSrcClassDir, StandardCopyOption.REPLACE_EXISTING);
        }

        // Prepare and return result
        updateTracker.updateCompileFlag();
        extraClasspath = extraClasspath.and(jekaSrcClassDir);
        boolean globalSuccess = javaCompileSuccess && kotlinCompileResult.success;
        if (!globalSuccess && !behaviorSettings.ignoreCompileFailure) {
            throw new JkException("Compilation of %s failed.", jekaSrcDir);
        }
        return new CompileResult(globalSuccess, extraClasspath);
    }

    private boolean hasKotlinSource() {
        return JkPathTree.of(jekaSrcDir).andMatcher(KOTLIN_JEKA_SRC_MATCHER)
                .count(1, false) > 0;
    }

    private KotlinCompileResult compileWithKotlin(JkPathSequence classpath, List<String> compileOptions) {
        String kotVer = properties.get(JkKotlinCompiler.KOTLIN_VERSION_OPTION);
        JkUtilsAssert.state(!JkUtilsString.isBlank(kotVer),
                "No jeka.kotlin.version property has been defined on base dir %s", baseDir);
        JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(dependencyResolver.getRepos(), kotVer)
                .setLogOutput(true)
                .addOption("-nowarn");
        compileOptions.forEach(option -> kotlinCompiler.addOption(option));
        JkPathSequence kotlinClasspath = classpath.and(kotlinCompiler.getStdJdk8Lib());
        final JkKotlinJvmCompileSpec kotlinCompileSpec = JkKotlinJvmCompileSpec.of()
                .setClasspath(kotlinClasspath)
                .setSources(JkPathTreeSet.ofRoots(jekaSrcDir))
                .setOutputDir(jekaSrcClassDir);
        if (JkLog.isVerbose()) {
            kotlinCompiler.addOption("-verbose");
        }

        // perform compilation in console spinner
        boolean success = kotlinCompiler.compile(kotlinCompileSpec);

        return new KotlinCompileResult(success, kotlinCompiler.getStdJdk8Lib());
    }

    private boolean compileJava(JkPathSequence classpath, List<String> compileOptions) {
        final JkPathTree jekaSource = JkPathTree.of(jekaSrcDir).andMatcher(JAVA_JEKA_SRC_MATCHER);
        JkJavaCompileSpec javaCompileSpec = JkJavaCompileSpec.of()
                .setClasspath(classpath.and(jekaSrcClassDir)) // for handling compiled kotlin code
                .setOutputDir(jekaSrcClassDir)
                .setSources(jekaSource.toSet())
                .addOptions(compileOptions);
        if (javaCompileSpec.getSources().containFiles() && ToolProvider.getSystemJavaCompiler() == null) {
            throw new JkException(NO_JDK_MSG);
        }
        return JkJavaCompilerToolChain.of().compile(javaCompileSpec);
    }

    private List<String> findKBeanClassNames() {

        JkInternalClasspathScanner scanner = JkInternalClasspathScanner.of();

        // Find classes in jeka-src-classes. As it is small scope, we don't need caching
        final List<String> jekaSrcKBeans;
        if (JkPathTree.of(jekaSrcClassDir).withMatcher(JAVA_CLASS_MATCHER).containFiles() && !skipJekaSrc) {
            ClassLoader srcClassloader = JkUrlClassLoader.of(jekaSrcClassDir).get();
            //AppendableUrlClassloader srcClassloader = createScanClassloader(jekaSrcClassDir);
            jekaSrcKBeans = scanner.findClassesExtending(srcClassloader, KBean.class, jekaSrcClassDir);
        } else {
            jekaSrcKBeans = Collections.emptyList();
        }

        // Find KBeans in dependencies. We need caching as there are potentially a lot of jars to scan
        JkPathSequence kbeanClasspath = classpathSetupResult.kbeanClasspath;

        // -- Look cached result in files
        Path classpathCache = baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve("jeka-kbean-classpath.txt");
        JkPathSequence cachedClasspath = JkPathSequence.readFromQuietly(classpathCache);
        Path kbeanCache = baseDir.resolve(JkConstants.JEKA_WORK_PATH).resolve("jeka-kbean-classes.txt");

        // -- If cache matches, returns cached resul
        // -- kbeanCache file may not exist is compilation is skipped
        if ( cachedClasspath.equals(kbeanClasspath) && Files.exists(kbeanCache)) {
             List<String> cachedKbeans = JkUtilsIterable.readStringsFrom(kbeanCache, "\n");
             if (!cachedKbeans.isEmpty()) {
                 List<String> result = new LinkedList<>(jekaSrcKBeans);
                 result.addAll(cachedKbeans);
                 return result;
             }
         }

        // -- Scan deps
        ClassLoader depClassloader = JkUrlClassLoader.of(classpathSetupResult.kbeanClasspath).get();

        // If we skip jeka-src, we need look in KBean classes in parent classloaders as we are
        // probably relying on IDE classloader.
        boolean ignoreParentClassloaders = !skipJekaSrc;
        List<String> depsKBeans = scanner.findClassesExtending(
                depClassloader,
                KBean.class,
                ignoreParentClassloaders,
                true,
                true);

        // -- Update cache
        kbeanClasspath.writeTo(classpathCache);
        JkUtilsIterable.writeStringsTo(kbeanCache, "\n", depsKBeans);

        // Prepare and return result
        List<String> result = new LinkedList<>(jekaSrcKBeans);
        result.addAll(depsKBeans);
        return result;
    }

    private Map<String, Class<? extends KBean>> resolveKBeanClasses(
            List<KBeanAction> kbeanActions) {

        // Resolve involved KBean classes
        final Map<String, Class<? extends KBean>> beanClasses = new HashMap<>();

        // -- find involved kbeans
        List<String> involvedKBeanNames = kbeanActions.stream()
                .map(kBeanAction -> kBeanAction.beanName)
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));

        for (String kbeanName : involvedKBeanNames) {
            String className = kbeanResolution.findKbeanClassName(kbeanName)
                    .orElseThrow(() -> new JkException("No KBean found with name : " + kbeanName));
            beanClasses.computeIfAbsent(kbeanName, key -> JkClassLoader.ofCurrent().load(className));
        }
        return beanClasses;

    }

    static class ClasspathSetupResult {

        final JkPathSequence runClasspath;
        final JkPathSequence kbeanClasspath;
        final JkPathSequence exportedClasspath;
        final List<Engine> subBaseDirs;
        final JkDependencySet exportedDependencies;
        final boolean compileResult;

        ClasspathSetupResult(boolean compileResult,
                                    JkPathSequence runClasspath,  // The full classpath to run Jeka upon
                                    JkPathSequence kbeanClasspath, // The classpath to find in KBeans
                                    JkPathSequence exportedClasspath,
                                    JkDependencySet exportedDependencies,
                                    List<Engine> subBaseDirs) {
            this.compileResult = compileResult;
            this.runClasspath = runClasspath;
            this.kbeanClasspath = kbeanClasspath;  // does not contain jeka-src-classes
            this.exportedClasspath = exportedClasspath;
            this.exportedDependencies = exportedDependencies;
            this.subBaseDirs = subBaseDirs;
        }

    }

    static class KBeanResolution {


        final List<String>  allKbeans;

        final List<String> localKBean;

        final String initKBeanClassname;

        final String defaultKbeanClassname;

        public KBeanResolution(List<String> allKbeans, List<String> localKBean,
                               String initKBeanClassname, String defaultKbeanClassname) {
            this.allKbeans = allKbeans;
            this.localKBean = localKBean;
            this.initKBeanClassname = initKBeanClassname;
            this.defaultKbeanClassname = defaultKbeanClassname;
        }

         Optional<String> findKbeanClassName(String kbeanName) {
             if (kbeanName == null) {
                 return Optional.of(Optional.ofNullable(defaultKbeanClassname).orElse(BaseKBean.class.getName()));
             }
            return this.allKbeans.stream()
                    .filter(className -> KBean.nameMatches(className, kbeanName))
                    .findFirst();
        }
    }

    // For test-purpose only
    void setKBeanResolution(KBeanResolution kbeanResolution) {
         this.kbeanResolution = kbeanResolution;
    }

    private static class KotlinCompileResult {

        final boolean success;

        final JkPathSequence kotlinLibPath;


        public KotlinCompileResult(boolean success, JkPathSequence kotlinLibPath) {
            this.success = success;
            this.kotlinLibPath = kotlinLibPath;
        }
    }

    private static class CompileResult {

        final boolean success;

        private final JkPathSequence extraClasspath;

        CompileResult(boolean success, JkPathSequence extraClasspath) {
            this.success = success;
            this.extraClasspath = extraClasspath;
        }
    }

    private ClasspathSetupResult compileLessClasspathResult(
            ParsedSourceInfo parsedSourceInfo,
            List<Engine> subBaseDirs,
            JkPathSequence classpath) {

        return new ClasspathSetupResult(
                true,
                classpath,
                classpath,
                JkPathSequence.of(
                        dependencyResolver.resolveFiles(parsedSourceInfo.getExportedDependencies())),
                parsedSourceInfo.getExportedDependencies()
                        .andVersionProvider(JkConstants.JEKA_VERSION_PROVIDER),
                subBaseDirs);
    }

}
