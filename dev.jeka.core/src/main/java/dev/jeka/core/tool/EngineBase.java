package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

class EngineBase {

    private static final String[] PRIVATE_GLOB_PATTERN = new String[] { "**/_*", "_*"};

    private static final JkPathMatcher JAVA_JEKA_SRC_MATCHER = JkPathMatcher.of(true,"**.java")
            .and(false,PRIVATE_GLOB_PATTERN);

    private static final JkPathMatcher KOTLIN_JEKA_SRC_MATCHER = JkPathMatcher.of(true,"**.kt")
            .and(false, PRIVATE_GLOB_PATTERN);

    private static final String NO_JDK_MSG = String.format(
            "The running Java platform %s is not a valid JDK (no javac found).%n" +
            "Please provide a JDK by specifying 'jeka.java.version' in jeka.properties file.%n" +
            "Or set JAVA_HOME environment variable to a valid JDK.", System.getProperty("java.home"));

    private static final Map<Path, EngineBase> MAP = new HashMap<>();

    private final Path baseDir;

    private final JkDependencyResolver dependencyResolver;

    private final EnvLogSettings logSettings;

    private final EnvBehaviorSettings behaviorSettings;

    // Computed values

    private final JkProperties properties;

    private final Path jekaSrcDir;

    private final Path jekaSrcClassDir;

    private ClasspathSetupResult classpathSetupResult;

    private KBeanResolution kbeanResolution;

    private List<EngineCommand> engineCommands;

    private EngineBase(Path baseDir, JkDependencyResolver dependencyResolver, EnvLogSettings logSettings, EnvBehaviorSettings behaviorSettings) {
        this.baseDir = baseDir;
        this.dependencyResolver = dependencyResolver;
        this.logSettings = logSettings;
        this.behaviorSettings = behaviorSettings;
        this.properties = JkRunbase.constructProperties(baseDir);
        this.jekaSrcDir = baseDir.resolve(JkConstants.JEKA_SRC_DIR);
        this.jekaSrcClassDir = baseDir.resolve(JkConstants.JEKA_SRC_CLASSES_DIR);
    }

     static EngineBase of(Path baseDir, JkDependencyResolver dependencyResolver, EnvLogSettings logSettings,
                          EnvBehaviorSettings behaviorSettings) {
        Path path = baseDir.toAbsolutePath().normalize();

        // ensure only 1 baseProcessor per base
        return MAP.computeIfAbsent(path,
                key -> new EngineBase(key, dependencyResolver, logSettings, behaviorSettings));
    }

    EngineBase withBaseDir(Path baseDir) {
        return of(baseDir, this.dependencyResolver, this.logSettings, this.behaviorSettings);
    }

    /*
     * Resolves dependencies and compiles and sources classes contained in jeka/def.
     * It returns a path sequence containing the resolved dependencies and result of compilation.
     */
     ClasspathSetupResult resolveClasspaths() {

        if (classpathSetupResult != null) {
            return classpathSetupResult;
        }

        if (behaviorSettings.cleanWork) {
            Path workDir = baseDir.resolve(JkConstants.JEKA_WORK_PATH);
            JkLog.info("Clean .jaka-work directory  %s ", workDir.toAbsolutePath().normalize());
            JkPathTree.of(workDir).deleteContent();
        }
        if (Environment.behavior.cleanOutput) {
            Path outputDir = baseDir.resolve(JkConstants.OUTPUT_PATH);
            JkLog.info("Clean jeka-output directory %s", outputDir.toAbsolutePath().normalize());
            JkPathTree.of(outputDir).deleteContent();
        }

        // Parse info from source code
        final ParsedSourceInfo parsedSourceInfo = SourceParser.of(this.baseDir).parse();

        // Compute and get the classpath from sub-dirs
        List<EngineBase> subBaseDirs = parsedSourceInfo.dependencyProjects.stream()
                .map(this::withBaseDir)
                .collect(Collectors.toList());
        JkPathSequence addedClasspathFromSubDirs = subBaseDirs.stream()
                .map(EngineBase::resolveClasspaths)
                .map(classpathSetupResult -> classpathSetupResult.runClasspath)
                .reduce(JkPathSequence.of(), JkPathSequence::and);

        // compute classpath to look in for finding KBeans
        String classpathProp = properties.get(JkConstants.CLASSPATH_INJECT_PROP, "");
        List<JkDependency> jekaPropsDepas = Arrays.stream(classpathProp.split("\n"))
                .map(desc -> ParsedCmdLine.toDependency(baseDir, desc))
                .collect(Collectors.toList());
        JkDependencySet dependencies = JkDependencySet.of(jekaPropsDepas).and(parsedSourceInfo.getDependencies());
        JkPathSequence kbeanClasspath = dependencyResolver.resolve(dependencies).getFiles();

        // compute classpath for compiling 'jeka-src'
        JkPathSequence compileClasspath = kbeanClasspath.and(addedClasspathFromSubDirs);

        // Compile jeka-src
        CompileResult compileResult = compileJekaSrc(compileClasspath, parsedSourceInfo.compileOptions);

        // Prepare result and return
        JkPathSequence runClasspath = compileClasspath.and(compileResult.extraClasspath);
        this.classpathSetupResult = new ClasspathSetupResult(compileResult.success,
                runClasspath, kbeanClasspath, subBaseDirs);
        return classpathSetupResult;
    }

    KBeanResolution resolveKBeans() {
         if (classpathSetupResult == null) {
             resolveClasspaths();
         }
        if (kbeanResolution == null) {
            return kbeanResolution;
        }

         // Find all KBean class available for this
         List<String> kbeanClassNames = findKBeanClassNames();

         // Filter to find local beans
         List<String> localKbeanClassNames = JkPathTree.of(jekaSrcClassDir).stream()
                 .filter(Files::isRegularFile)
                 .filter(path -> path.getFileName().endsWith(".class"))
                 .map(EngineBase::classNameFromClassFilePath)
                 .filter(kbeanClassNames::contains)
                 .collect(Collectors.toList());

         // The KBean we can invoke methods on without being specifically invoked
         String defaultKBean = properties.get(JkConstants.DEFAULT_KBEAN_PROP,
                 localKbeanClassNames.stream().findFirst().orElse(null));

         // The first KBean to be initialized
         String initKbean = localKbeanClassNames.stream().findFirst().orElse(defaultKBean);

         kbeanResolution = new KBeanResolution(kbeanClassNames, localKbeanClassNames,initKbean, defaultKBean);
         return kbeanResolution;
    }

    List<EngineCommand> resolveCommandEngine(List<KBeanAction> kBeanActionsFromCmdLine) {
        if (engineCommands != null) {
            return engineCommands;
        }
        if (this.kbeanResolution == null) {
            resolveKBeans();
        }

        // We need to run in the classpath computed in previous steps
        AppendableUrlClassloader.addEntriesOnContextClassLoader(this.classpathSetupResult.runClasspath);

        // -- Gather KbeanActions from props and command line (command line should override props)
        List<KBeanAction> effectiveKeanActions = new LinkedList<>(getKBeanActionFromProperties());
        effectiveKeanActions.addAll(kBeanActionsFromCmdLine);

        Map<String, Class<? extends KBean>> kbeanClasses = resolveKBeanClasses(effectiveKeanActions);

        // Construct EngineCommands
        this.engineCommands = new LinkedList<>();

        // -- If there is an initBean it should be initialized first
        if (kbeanResolution.initKBean != null) {
            Class initBeanClass = JkClassLoader.ofCurrent().load(kbeanResolution.initKBean);
            engineCommands.add(new EngineCommand(EngineCommand.Action.BEAN_INSTANTIATION, initBeanClass,
                    null, null));
        }

        // -- Add other actions
        effectiveKeanActions.stream()
                .map(action -> toEngineCommand(action, kbeanClasses))
                .forEach(engineCommands::add);

        return engineCommands;
    }

    void run() {
         JkUtilsAssert.state(engineCommands != null, "Resolve engineCommand prior running");

         JkRunbase runbase = JkRunbase.get(baseDir);
         runbase.setDependencyResolver(dependencyResolver);
         runbase.setClasspath(classpathSetupResult.runClasspath);
         runbase.assertValid(); // fail-fast. bugfix purpose

         // initialise runbase with resolved commands
         runbase.init(engineCommands);
    }


    static class ClasspathSetupResult {

        public ClasspathSetupResult(boolean compileResult,
                                    JkPathSequence runClasspath,  // The full classpath to run Jeka upon
                                    JkPathSequence kbeanClasspath, // The classpath to find in KBeans
                                    List<EngineBase> subBaseDirs) {
            this.compileResult = compileResult;
            this.runClasspath = runClasspath;
            this.kbeanClasspath = kbeanClasspath;
            this.subBaseDirs = subBaseDirs;
        }

        final JkPathSequence runClasspath;

        final JkPathSequence kbeanClasspath;

        final List<EngineBase> subBaseDirs;

        final boolean compileResult;

    }

    static class KBeanResolution {
        public KBeanResolution(List<String> allKbeans, List<String> localKBean, String initKBean, String defaultKbean) {
            this.allKbeans = allKbeans;
            this.localKBean = localKBean;
            this.initKBean = initKBean;
            this.defaultKbean = defaultKbean;
        }

        final List<String>  allKbeans;

         final List<String> localKBean;

         final String initKBean;

         final String defaultKbean;

         Optional<String> findKbeanClassName(String kbeanName) {
            return this.allKbeans.stream()
                    .filter(className -> KBean.nameMatches(className, kbeanName))
                    .findFirst();
        }
    }

    private CompileResult compileJekaSrc(JkPathSequence classpath, List<String> compileOptions) {

        JkPathTree.of(jekaSrcDir).deleteContent();
        JkPathSequence extraClasspath = JkPathSequence.of();

        // Compile Kotlin code if any
        KotlinCompileResult kotlinCompileResult = new KotlinCompileResult(true, JkPathSequence.of());
        if (hasKotlinSource()) {
            kotlinCompileResult = compileWithKotlin(classpath, compileOptions);
            extraClasspath = extraClasspath.and(kotlinCompileResult.kotlinLibPath);

            // TODO comment Why needed
            AppendableUrlClassloader.addEntriesOnContextClassLoader(kotlinCompileResult.kotlinLibPath);
        }

        // Compile  Java
        boolean javaCompileSuccess = compileJava(classpath, compileOptions);

        // Copy resources
        JkPathTree.of(jekaSrcDir)
                .andMatching(false, "**/*.java", "*.java", "**/*.kt", "*.kt")
                .copyTo(jekaSrcClassDir, StandardCopyOption.REPLACE_EXISTING);

        // Prepare and return result
        extraClasspath = extraClasspath.and(jekaSrcClassDir);
        boolean globalSuccess = javaCompileSuccess && kotlinCompileResult.success;
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

    private List<String> findKBeanClassNames() {
         URLClassLoader classLoader = JkUrlClassLoader.of(classpathSetupResult.kbeanClasspath,
                EngineBase.class.getClassLoader()).get();
         return JkInternalClasspathScanner.of().findClassesInheritingOrAnnotatesWith(
                classLoader, KBean.class, path -> true, path -> true, true, false);
    }

    private static String classNameFromClassFilePath(Path relativePath) {
        final String dotName = relativePath.toString().replace('/', '.');
        return JkUtilsString.substringBeforeLast(dotName, ".");
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

    private static EngineCommand toEngineCommand(KBeanAction action,
                                          Map<String, Class<? extends KBean>> beanClasses) {
        Class<? extends KBean> beanClass = beanClasses.get(action.beanName);
        JkUtilsAssert.state(beanClass != null, "Can't resolve KBean class for action %s", action);
        return new EngineCommand(action.action, beanClass, action.member, action.value);
    }

    private List<KBeanAction> getKBeanActionFromProperties() {
        Map<String, String> props = properties.getAllStartingWith("", true);
        return props.entrySet().stream()
                .filter(entry -> entry.getKey().contains(ParsedCmdLine.KBEAN_SYMBOL))
                .map(entry -> {

                    // 'someBean#=' should be interpreted as 'someBean#'
                    if (entry.getKey().endsWith(ParsedCmdLine.KBEAN_SYMBOL)) {
                        return entry.getKey() + ParsedCmdLine.KBEAN_SYMBOL;
                    }
                    return entry.getKey() + "=" + entry.getValue();
                })
                .map(KBeanAction::new)
                .collect(Collectors.toList());
    }

}
