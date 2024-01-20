package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.JkDependency;
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.depmanagement.JkRepoProperties;
import dev.jeka.core.api.depmanagement.JkRepoSet;
import dev.jeka.core.api.depmanagement.resolution.JkDependencyResolver;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompilerToolChain;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.system.*;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Engine is responsible to compile jeka-src ,instantiate KBeans and run them.<br/>
 * Sources are expected to lie in [project base dir]/jeka-src <br/>
 * jeka-src classes having simple name starting with '_' are ignored.
 *
 * jeka-src classes can have dependencies on jars : <ul>
 *     <li>located in [base dir]/jeka-boot directory</li>
 *     <li>Dependencies declared in {@link JkInjectClasspath} annotation</li>
 *     <li>declared in command-line, using '@'</li>
 * </ul>
 */
final class Engine {

    private static final String[] PRIVATE_GLOB_PATTERN = new String[] { "**/_*", "_*"};

    static final JkPathMatcher JAVA_JEKA_SRC_MATCHER = JkPathMatcher.of(true,"**.java")
            .and(false,PRIVATE_GLOB_PATTERN);

    private static final JkPathMatcher KOTLIN_JEKA_SRC_MATCHER = JkPathMatcher.of(true,"**.kt")
           .and(false, PRIVATE_GLOB_PATTERN);

    static final JkPathMatcher JAVA_OR_KOTLIN_SOURCE_MATCHER = JAVA_JEKA_SRC_MATCHER.or(KOTLIN_JEKA_SRC_MATCHER);

    private final Path baseDir;

    private final JkDependencyResolver dependencyResolver;

    private final EngineKBeanClassResolver beanClassesResolver;

    Engine(Path baseDir) {
        super();
        this.baseDir = baseDir;
        this.beanClassesResolver = new EngineKBeanClassResolver(baseDir);
        JkRepoSet repos = JkRepoProperties.of(JkRunbase.constructProperties(baseDir)).getDownloadRepos();
        this.dependencyResolver = JkDependencyResolver.of(repos);
        this.dependencyResolver
                .getDefaultParams()
                    .setFailOnDependencyResolutionError(true);
    }

    /**
     * Execute the specified command line in Jeka engine.
     */
    void execute(CommandLine commandLine) {
        JkLog.startTask("Compile jeka-src and initialize KBeans");
        JkDependencySet commandLineDependencies = JkDependencySet.of(commandLine.getJekaSrcDependencies());
        JkLog.trace("Dependencies injected in classpath from command line : " + commandLineDependencies);
        JkDependencySet dependenciesFromJekaProps = dependenciesFromJekaProps();
        JkLog.trace("IDependencies injected in classpath from jeka.properties : " + dependenciesFromJekaProps);
        final JkPathSequence computedClasspath;
        final CompilationResult result;
        boolean hasJekaSrc = Files.exists(baseDir.resolve(JkConstants.JEKA_SRC_DIR))
                || commandLine.involvedBeanNames().contains("scaffold");
        if (hasJekaSrc) {
            boolean failOnError = !Environment.standardOptions.ignoreCompileFail && !commandLine.isHelp();
            result = resolveAndCompile(new HashMap<>(), true, failOnError);
            computedClasspath = result.classpath;
                    // the command deps has been included in cache for classpath resolution
                    //.andPrepend(dependencyResolver.resolve(commandLineDependencies).getFiles());
            AppendableUrlClassloader.addEntriesOnContextClassLoader(computedClasspath);
            beanClassesResolver.setClasspath(computedClasspath, result.classpathChanged);
        } else {
            computedClasspath = dependencyResolver.resolve(commandLineDependencies.and(dependenciesFromJekaProps))
                    .getFiles();
            result = null;
        }
        if (isHelpCmd()) {
            help();
            return;
        }
        JkLog.startTask("Setting up runbase");
        JkRunbase runbase = JkRunbase.get(baseDir);
        runbase.setClasspath(computedClasspath);

        // If jeka-src compilation failed, we ignore the defaultBeanClass cause it may be absent
        // from the classpath.
        boolean ignoreDefaultBeanNotFound = result == null ||
                (!result.compileFailedProjects.getEntries().isEmpty() &&
                Environment.standardOptions.ignoreCompileFail);

        List<EngineCommand> resolvedCommands = beanClassesResolver.resolve(commandLine,
                Environment.standardOptions.kbeanName(), ignoreDefaultBeanNotFound);
        JkLog.startTask("Init runbase");
        runbase.init(resolvedCommands);
        JkLog.endTask();
        JkLog.endTask();
        JkLog.endTask();
        JkLog.info("KBeans are ready to run.");
        stopBusyIndicator();
        if (result != null && !result.compileFailedProjects.getEntries().isEmpty()) {
            JkLog.warn("Jeka-src compilation failed on base dirs " + result.compileFailedProjects.getEntries()
                    .stream().map(path -> "'" + projectName(path) + "'").collect(Collectors.toList()));
            JkLog.warn("As -dci option is on, the failure will be ignored.");
        }
        if (Environment.standardOptions.logRuntimeInformation) {
            System.out.println("Classloader : " + JkClassLoader.ofCurrent());
            System.out.println();
        }
        if (!hasJekaSrc) {
            JkLog.trace("You are not running Jeka inside a Jeka project.");
        }
        if (!commandLine.hasMethodInvokations() && !Environment.standardOptions.logRuntimeInformation) {
            JkLog.warn("This command contains no actions. Execute 'jeka help' to know about available actions.");
        }
        runbase.run(resolvedCommands);
    }
    
    private static String projectName(Path path) {
        if (JkUtilsString.isBlank(path.getFileName().toString())) {
            return Paths.get("").toAbsolutePath().getFileName().toString();
        }
        return path.toString();
    }

    private boolean isHelpCmd() {
        if (Environment.isPureHelpCmd()) {
            return true;
        }
        return "help".equals(Environment.originalCmdLineAsString())
                        && beanClassesResolver.getSourceTree().getFiles().isEmpty()
                        && Environment.standardOptions.kbeanName() == null;
    }

    private CompilationContext preCompile() {

        JkPathTree sourceTree = beanClassesResolver.getSourceTree();
        JkLog.traceStartTask("Parsing source code of " + sourceTree);
        EngineClasspathCache engineClasspathCache = new EngineClasspathCache(this.baseDir, dependencyResolver);
        final ParsedSourceInfo parsedSourceInfo =
                SourceParser.of(this.baseDir, sourceTree).parse();
        JkLog.traceEndTask();

        JkDependencySet defDependencies = JkDependencySet.of()
                .and(Environment.commandLine.getJekaSrcDependencies())
                .and(parsedSourceInfo.getDependencies())
                .and(dependenciesFromJekaProps());
        JkDependencySet exportedDependencies = defDependencies;
        boolean hasPrivateDependencies = parsedSourceInfo.hasPrivateDependencies();
        if (hasPrivateDependencies) {
            exportedDependencies = JkDependencySet.of()
                    .and(Environment.commandLine.getJekaSrcDependencies())
                    .and(parsedSourceInfo.getExportedDependencies())
                    .and(dependenciesFromJekaProps());
        }

        EngineClasspathCache.Result cacheResult = engineClasspathCache.resolvedClasspath(
                defDependencies, exportedDependencies, !hasPrivateDependencies);

        return new CompilationContext(
                jekaClasspath().and(cacheResult.classpath),
                cacheResult.exportedClasspath,
                exportedDependencies,
                new LinkedList<>(parsedSourceInfo.dependencyProjects),
                parsedSourceInfo.compileOptions,
                cacheResult.changed
        );
    }

    /*
     * Resolves dependencies and compiles and sources classes contained in jeka/def.
     * It returns a path sequence containing the resolved dependencies and result of compilation.
     */
    private CompilationResult resolveAndCompile(Map<Path, JkPathSequence> yetCompiledProjects, boolean compileSources,
                                             boolean failOnCompileError) {
        if (yetCompiledProjects.containsKey(this.baseDir)) {
            JkLog.trace("Project '%s' already compiled. Skip", this.baseDir);
            return new CompilationResult(JkPathSequence.of(), JkPathSequence.of(),
                    yetCompiledProjects.get(this.baseDir), false);
        }
        yetCompiledProjects.put(this.baseDir, JkPathSequence.of());
        if (Environment.standardOptions.workClean()) {
            Path workDir = baseDir.resolve(JkConstants.JEKA_WORK_PATH);
            JkLog.info("Clean .work directory " + workDir.toAbsolutePath().normalize());
            JkPathTree.of(workDir).deleteContent();
        }

        JkLog.startTask("Scanning and compiling jeka-src for base dir %s", baseDir.toAbsolutePath());
        CompilationContext compilationContext = preCompile();
        List<Path> importedProjectClasspath = new LinkedList<>();
        List<Path> failedProjects = new LinkedList<>();
        boolean importedProjectClasspathChanged = false;
        for (Path importedProjectDir : compilationContext.importedProjectDirs) {
            Engine importedProjectEngine = new Engine(importedProjectDir);
            CompilationResult compilationResult = importedProjectEngine.resolveAndCompile(yetCompiledProjects,
                    compileSources, failOnCompileError);
            importedProjectClasspath.addAll(compilationResult.classpath.getEntries());
            failedProjects.addAll(compilationResult.compileFailedProjects.getEntries());
            importedProjectClasspathChanged = importedProjectClasspathChanged || compilationResult.classpathChanged;
        }
        JkPathSequence classpath = compilationContext.classpath.and(importedProjectClasspath).withoutDuplicates();
        EngineCompilationUpdateTracker compilationTracker = new EngineCompilationUpdateTracker(baseDir);
        boolean outdated = compilationTracker.isOutdated();
        if (!beanClassesResolver.hasDefSource() && beanClassesResolver.hasClassesInWorkDir()) {
            JkPathTree.of(beanClassesResolver.jekaSrcClassDir).deleteContent();
        }
        if (compileSources && this.beanClassesResolver.hasDefSource()) {
            boolean missingBinaryFiles = compilationTracker.isMissingBinaryFiles();
            if (missingBinaryFiles) {
                JkLog.trace("Some binary files seem missing.");
            }
            if (outdated || missingBinaryFiles) {
                SingleCompileResult result = compileDef(classpath, compilationContext.compileOptions, failOnCompileError);
                if (!result.success) {
                    failedProjects.add(baseDir);
                    compilationTracker.deleteCompileFlag();
                } else {
                    classpath = classpath.and(result.extraClasspath);
                    compilationTracker.updateCompileFlag();
                }
            } else {
                // We need to add kotlin libs in order to invoke local KBean compiled with kotlin
                if (hasKotlinSource()) {
                    JkLog.traceStartTask("Preparing for Kotlin");
                    JkProperties props = JkRunbase.constructProperties(baseDir);
                    String kotVer = props.get(JkKotlinCompiler.KOTLIN_VERSION_OPTION);
                    JkUtilsAssert.state(!JkUtilsString.isBlank(kotVer), "No jeka.kotlin.version property has been defined.");
                    JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(dependencyResolver.getRepos(), kotVer);
                    AppendableUrlClassloader.addEntriesOnContextClassLoader(kotlinCompiler.getStdLib());
                    JkLog.traceEndTask();
                }
                JkLog.trace("Last jeka-src classes are up-to-date : No need to compile.");
            }
        } else if (outdated) {
            compilationTracker.updateCompileFlag();
        }
        JkLog.endTask();
        JkPathSequence resultClasspath = classpath.andPrepend(beanClassesResolver.jekaSrcClassDir);
        yetCompiledProjects.put(this.baseDir, resultClasspath);
        CompilationResult compilationResult = new CompilationResult(
                JkPathSequence.of(compilationContext.importedProjectDirs),
                JkPathSequence.of(failedProjects).withoutDuplicates(),
                resultClasspath,
                compilationContext.classpathChanged || importedProjectClasspathChanged);
        JkRunbase runbase = JkRunbase.get(baseDir);
        runbase.setDependencyResolver(dependencyResolver);
        runbase.setImportedRunbaseDirs(compilationResult.importedProjects);
        runbase.setClasspath(compilationResult.classpath);
        runbase.setExportedClassPath(compilationContext.exportedClasspath);
        runbase.setExportedDependencies(compilationContext.exportedDependencies);
        return compilationResult;
    }

    private SingleCompileResult compileDef(JkPathSequence defClasspath, List<String> compileOptions,
                                           boolean failOnCompileError) {
        JkPathTree.of(beanClassesResolver.jekaSrcClassDir).deleteContent();
        JkPathSequence extraClasspath = JkPathSequence.of();
        JkProperties props = JkRunbase.constructProperties(baseDir);
        String kotVer = props.get(JkKotlinCompiler.KOTLIN_VERSION_OPTION);
        if (hasKotlinSource()) {
            JkUtilsAssert.state(!JkUtilsString.isBlank(kotVer), "No jeka.kotlin.version property has been defined.");
            JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(dependencyResolver.getRepos(), kotVer)
                    .setLogOutput(true)
                    .setFailOnError(failOnCompileError)
                    .addOption("-nowarn");
            boolean success = wrapCompile(() -> {
                compileOptions.forEach(option -> kotlinCompiler.addOption(option));
                JkPathSequence kotlinClasspath = defClasspath.and(kotlinCompiler.getStdJdk8Lib());
                final JkKotlinJvmCompileSpec kotlinCompileSpec = jekaSrcKotlinCompileSpec(kotlinClasspath);
                if (JkLog.isVerbose()) {
                    kotlinCompiler.addOption("-verbose");
                }
                return kotlinCompiler.compile(kotlinCompileSpec);
                    }, failOnCompileError);
            if (!failOnCompileError && !success) {
                return new SingleCompileResult(false, JkPathSequence.of());
            }
            extraClasspath = extraClasspath.and(kotlinCompiler.getStdLib());
            AppendableUrlClassloader.addEntriesOnContextClassLoader(kotlinCompiler.getStdLib());
        }
        final JkJavaCompileSpec javaCompileSpec = jekaSrcJavaCompileSpec(defClasspath, compileOptions);
        if (javaCompileSpec.getSources().containFiles() && ToolProvider.getSystemJavaCompiler() == null) {
            throw new JkException("The running Java platform (" +  System.getProperty("java.home") +
                    ") does not provide compiler (javac). Please provide a JDK java platform by pointing JAVA_HOME" +
                    " or JEKA_JDK environment variable to a JDK directory.");
        }
        boolean success = wrapCompile(() -> JkJavaCompilerToolChain.of().compile(javaCompileSpec), failOnCompileError);
        if (!success) {
            return new SingleCompileResult(false, JkPathSequence.of());
        }
        JkPathTree.of(this.beanClassesResolver.jekaSourceDir)
                .andMatching(false, "**/*.java", "*.java", "**/*.kt", "*.kt")
                .copyTo(this.beanClassesResolver.jekaSrcClassDir, StandardCopyOption.REPLACE_EXISTING);
        return new SingleCompileResult(true, extraClasspath);
    }

    private JkPathSequence jekaClasspath() {

        // If true, we assume Jeka is provided in IDE classpath (development mode)
        final boolean devMode = Files.isDirectory(JkLocator.getJekaJarPath());
        JkPathSequence result = JkPathSequence.of(bootLibs()).normalized();
        if (devMode) {
            result = result.and(JkClasspath.ofCurrentRuntime());
        } else {
            result = result.and(JkLocator.getJekaJarPath());
        }
        JkLog.trace("Use Jeka " + result.normalized() + " for compilation.");
        return result.withoutDuplicates();
    }

    private JkPathSequence bootLibs() {
        final List<Path>  extraLibs = new LinkedList<>();
        final Path bootDir = this.baseDir.resolve(JkConstants.JEKA_BOOT_DIR);
        if (Files.exists(bootDir)) {
            extraLibs.addAll(JkPathTree.of(bootDir).andMatching(true,"**.jar").getFiles());
        }
        return JkPathSequence.of(extraLibs);
    }

    private boolean wrapCompile(Supplier<Boolean> compileTask, boolean failOnError) {
        boolean success = compileTask.get();
        if (!success && failOnError) {
            throw new JkException("Compilation of Jeka files failed. " +
                    "\nRun with '-dci' option to ignore compilation failure. " +
                    "\nRun with '-lv' option to display compilation failure details.");
        }
        return success;
    }

    private boolean hasKotlinSource() {
        return JkPathTree.of(beanClassesResolver.jekaSourceDir).andMatcher(KOTLIN_JEKA_SRC_MATCHER)
                .count(1, false) > 0;
    }

    private JkJavaCompileSpec jekaSrcJavaCompileSpec(JkPathSequence classpath, List<String> options) {
        final JkPathTree jekaSource = JkPathTree.of(beanClassesResolver.jekaSourceDir).andMatcher(JAVA_JEKA_SRC_MATCHER);
        JkUtilsPath.createDirectories(beanClassesResolver.jekaSrcClassDir);
        return JkJavaCompileSpec.of()
                .setClasspath(classpath.and(beanClassesResolver.jekaSrcClassDir))
                .setOutputDir(beanClassesResolver.jekaSrcClassDir)
                .setSources(jekaSource.toSet())
                .addOptions(options);
    }

    private JkKotlinJvmCompileSpec jekaSrcKotlinCompileSpec(JkPathSequence jekaSrcClasspath) {
        JkUtilsPath.createDirectories(beanClassesResolver.jekaSrcClassDir);
        return JkKotlinJvmCompileSpec.of()
                .setClasspath(jekaSrcClasspath)
                .setSources(JkPathTreeSet.ofRoots(beanClassesResolver.jekaSourceDir))
                .setOutputDir(JkUtilsPath.relativizeFromWorkingDir(beanClassesResolver.jekaSrcClassDir));
    }

    @Override
    public String toString() {
        return this.baseDir.getFileName().toString();
    }

    private static class CompilationContext {

        private final JkPathSequence classpath;

        // classpath used for consumption by other project or to package jeka-src as an application
        private final JkPathSequence exportedClasspath;

        private final JkDependencySet exportedDependencies;

        private final List<Path> importedProjectDirs;

        private final List<String> compileOptions;

        private final boolean classpathChanged;

        CompilationContext(
                JkPathSequence classpath,
                JkPathSequence exportedClasspath,
                JkDependencySet exportedDependencies,
                List<Path> importedProjectDirs,
                List<String> compileOptions,
                boolean classpathChanged) {

            this.classpath = classpath;
            this.exportedClasspath = exportedClasspath;
            this.exportedDependencies = exportedDependencies;
            this.importedProjectDirs = Collections.unmodifiableList(importedProjectDirs);
            this.compileOptions = Collections.unmodifiableList(compileOptions);
            this.classpathChanged = classpathChanged;
        }
    }

    private static void stopBusyIndicator() {
        if (JkMemoryBufferLogDecorator.isActive()) {
            JkBusyIndicator.stop();
            JkMemoryBufferLogDecorator.inactivateOnJkLog();
        }
    }

    private void help() {
        JkLog.endTask();
        stopBusyIndicator();
        List<Class<? extends KBean>> localBeanClasses = beanClassesResolver.jekaSrcBeanClasses();
        List<Class> globalBeanClasses = beanClassesResolver.globalBeanClassNames().stream()
                .map(className -> JkClassLoader.ofCurrent().loadIfExist(className))
                .filter(Objects::nonNull)   // due to cache, some classNames may not be in classpath
                .filter(beanClass -> !localBeanClasses.contains(beanClass))
                .collect(Collectors.toList());
        HelpDisplayer.help(localBeanClasses, globalBeanClasses, false, this.baseDir);
    }

    private static class CompilationResult {

        final JkPathSequence compileFailedProjects;

        final JkPathSequence classpath;

        // Direct imported projects
        final JkPathSequence importedProjects;

        final boolean classpathChanged;

        CompilationResult(JkPathSequence importedProjects, JkPathSequence compileFailedProjects,
                          JkPathSequence resultClasspath, boolean classpathChanged) {
            this.importedProjects = importedProjects;
            this.compileFailedProjects = compileFailedProjects;
            this.classpath = resultClasspath;
            this.classpathChanged = classpathChanged;
        }
    }

    private static class SingleCompileResult {

        final boolean success;

        final JkPathSequence extraClasspath;

        SingleCompileResult(boolean success, JkPathSequence extraClasspath) {
            this.success = success;
            this.extraClasspath = extraClasspath;
        }

    }

    private JkDependencySet dependenciesFromJekaProps() {
        String depsString = localProperties().get(JkConstants.CLASSPATH_INJECT_PROP);
        if (depsString == null) {
            return JkDependencySet.of();
        }
        List<JkDependency> dependencies = new LinkedList<>();
        for (String depString : depsString.split(" ")) {
            if (depString.trim().isEmpty()) {
                continue;
            }
            dependencies.add(CommandLine.toDependency(baseDir, depString.trim()));
        }
        return JkDependencySet.of(dependencies);
    }

    private JkProperties localProperties() {
        return JkRunbase.localProperties(baseDir);
    }

    private String projectBaseDirName() {
        String rawDirName = this.baseDir.getFileName().toString();
        if (rawDirName.isEmpty()) {
            return this.baseDir.toAbsolutePath().getFileName().toString();
        }
        return rawDirName;
    }



}
