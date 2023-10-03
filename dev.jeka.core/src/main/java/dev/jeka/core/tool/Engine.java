package dev.jeka.core.tool;

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
import dev.jeka.core.api.java.JkJavaCompiler;
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
 * Engine having responsibility of compile def classes, instantiate KBeans and run.<br/>
 * Sources are expected to lie in [project base dir]/jeka/def <br/>
 * Def classes having simple name starting with '_' are ignored.
 *
 * Def classes can have dependencies on jars : <ul>
 *     <li>located in [base dir]/jeka/boot directory</li>
 *     <li>declared in {@link JkInjectClasspath} annotation</li>
 *     <li>declared in command-line, using '@'</li>
 * </ul>
 */
final class Engine {

    private static final JkPathMatcher JAVA_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.java")
            .and(false, "**/_*", "_*");

    private static final JkPathMatcher KOTLIN_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.kt")
           .and(false, "**/_*", "_*");

    static final JkPathMatcher JAVA_OR_KOTLIN_SOURCE_MATCHER = JAVA_DEF_SOURCE_MATCHER.or(KOTLIN_DEF_SOURCE_MATCHER);

    private final Path projectBaseDir;

    private final JkDependencyResolver dependencyResolver;

    private final EngineBeanClassResolver beanClassesResolver;

    Engine(Path baseDir) {
        super();
        this.projectBaseDir = baseDir;
        this.beanClassesResolver = new EngineBeanClassResolver(baseDir);
        JkRepoSet repos = JkRepoProperties.of(JkRuntime.constructProperties(baseDir)).getDownloadRepos();
        this.dependencyResolver = JkDependencyResolver.of(repos);
        this.dependencyResolver
                .getDefaultParams()
                    .setFailOnDependencyResolutionError(true);
    }

    /**
     * Execute the specified command line in Jeka engine.
     */
    void execute(CommandLine commandLine) {
        JkLog.startTask("Compile def and initialise KBeans");
        JkDependencySet commandLineDependencies = JkDependencySet.of(commandLine.getDefDependencies());
        JkLog.trace("Inject classpath from command line : " + commandLineDependencies);
        final JkPathSequence computedClasspath;
        final CompilationResult result;
        boolean hasJekaDir = Files.exists(projectBaseDir.resolve(JkConstants.JEKA_DIR))
                || commandLine.involvedBeanNames().contains("scaffold");
        if (hasJekaDir) {
            boolean failOnError = !Environment.standardOptions.ignoreCompileFail && !commandLine.isHelp();
            result = resolveAndCompile(new HashMap<>(), true,
                    failOnError);
            computedClasspath = result.classpath;
                    // the command deps has been included in cache for classpath resolution
                    //.andPrepend(dependencyResolver.resolve(commandLineDependencies).getFiles());
            AppendableUrlClassloader.addEntriesOnContextClassLoader(computedClasspath);
            beanClassesResolver.setClasspath(computedClasspath, result.classpathChanged);
        } else {
            computedClasspath = dependencyResolver.resolve(commandLineDependencies).getFiles();
            result = null;
        }
        if (isHelpCmd()) {
            help();
            return;
        }
        JkLog.startTask("Setting up runtime");
        JkRuntime runtime = JkRuntime.get(projectBaseDir);
        runtime.setClasspath(computedClasspath);
        List<EngineCommand> resolvedCommands = beanClassesResolver.resolve(commandLine,
                Environment.standardOptions.kBeanName());
        JkLog.startTask("Init runtime");
        runtime.init(resolvedCommands);
        JkLog.endTask();
        JkLog.endTask();
        JkLog.endTask();
        JkLog.info("KBeans are ready to run.");
        stopBusyIndicator();
        if (result != null && !result.compileFailedProjects.getEntries().isEmpty()) {
            JkLog.warn("Def compilation failed on projects " + result.compileFailedProjects.getEntries()
                    .stream().map(path -> "'" + projectName(path) + "'").collect(Collectors.toList()));
            JkLog.warn("As -dci option is on, the failure will be ignored.");
        }
        if (Environment.standardOptions.logRuntimeInformation) {
            System.out.println("Classloader : " + JkClassLoader.ofCurrent());
            System.out.println();
        }
        if (!hasJekaDir) {
            JkLog.warn("You are not running Jeka inside a Jeka project.");
        }
        if (!commandLine.hasMethodInvokations() && !Environment.standardOptions.logRuntimeInformation) {
            JkLog.warn("This command contains no actions. Execute 'jeka help' to know about available actions.");
        }
        runtime.run(resolvedCommands);
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
                        && beanClassesResolver.getSourceFiles().isEmpty()
                        && Environment.standardOptions.kBeanName() == null;

    }

    private CompilationContext preCompile() {
        final List<Path> sourceFiles = beanClassesResolver.getSourceFiles();
        JkLog.traceStartTask("Parse source code of " + sourceFiles);
        final EngineSourceParser parser = EngineSourceParser.of(this.projectBaseDir, sourceFiles);
        EngineClasspathCache engineClasspathCache = new EngineClasspathCache(this.projectBaseDir, dependencyResolver);
        JkDependencySet parsedDependencies = parser.dependencies().and(Environment.commandLine.getDefDependencies());
        EngineClasspathCache.Result cacheResult = engineClasspathCache.resolvedClasspath(parsedDependencies);
        JkLog.traceEndTask();
        return new CompilationContext(
                jekaClasspath().and(cacheResult.resolvedClasspath),
                new LinkedList<>(parser.projects()),
                parser.compileOptions(),
                cacheResult.changed
        );
    }

    /*
     * Resolves dependencies and compiles and sources classes contained in jeka/def.
     * It returns a path sequence containing the resolved dependencies and result of compilation.
     */
    private CompilationResult resolveAndCompile(Map<Path, JkPathSequence> yetCompiledProjects, boolean compileSources,
                                             boolean failOnCompileError) {
        if (yetCompiledProjects.containsKey(this.projectBaseDir)) {
            JkLog.trace("Project '%s' already compiled. Skip", this.projectBaseDir);
            return new CompilationResult(JkPathSequence.of(), JkPathSequence.of(),
                    yetCompiledProjects.get(this.projectBaseDir), false);
        }
        yetCompiledProjects.put(this.projectBaseDir, JkPathSequence.of());
        if (Environment.standardOptions.workClean()) {
            Path workDir = projectBaseDir.resolve(JkConstants.WORK_PATH);
            JkLog.info("Clean .work directory " + workDir.toAbsolutePath().normalize());
            JkPathTree.of(workDir).deleteContent();
        }
        String msg = "Scanning sources and compiling def classes for project '"
                + this.projectBaseDir.getFileName() + "'";
        JkLog.startTask(msg);
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
        EngineCompilationUpdateTracker compilationTracker = new EngineCompilationUpdateTracker(projectBaseDir);
        boolean outdated = compilationTracker.isOutdated();
        if (!beanClassesResolver.hasDefSource() && beanClassesResolver.hasClassesInWorkDir()) {
            JkPathTree.of(beanClassesResolver.defClassDir).deleteContent();
        }
        if (compileSources && this.beanClassesResolver.hasDefSource()) {
            boolean missingBinayFiles = compilationTracker.isMissingBinaryFiles();
            if (missingBinayFiles) {
                JkLog.trace("Some binary files seem missing.");
            }
            if (outdated || missingBinayFiles) {
                JkLog.trace("Compile classpath : " + classpath);
                SingleCompileResult result = compileDef(classpath, compilationContext.compileOptions, failOnCompileError);
                if (!result.success) {
                    failedProjects.add(projectBaseDir);
                    compilationTracker.deleteCompileFlag();
                } else {
                    classpath = classpath.and(result.extraClasspath);
                    compilationTracker.updateCompileFlag();
                }
            } else {
                // We need to add kotlin libs in order to invoke local KBean compiled with kotlin
                if (hasKotlinSource()) {
                    JkLog.traceStartTask("Preparing for Kotlin");
                    JkProperties props = JkRuntime.constructProperties(projectBaseDir);
                    String kotVer = props.get(JkKotlinCompiler.KOTLIN_VERSION_OPTION);
                    JkUtilsAssert.state(!JkUtilsString.isBlank(kotVer), "No jeka.kotlin.version property has been defined.");
                    JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofJvm(dependencyResolver.getRepos(), kotVer);
                    AppendableUrlClassloader.addEntriesOnContextClassLoader(kotlinCompiler.getStdLib());
                    JkLog.traceEndTask();
                }
                JkLog.trace("Last def classes are up-to-date : No need to compile.");
            }
        } else if (outdated) {
            compilationTracker.updateCompileFlag();
        }
        JkLog.endTask();
        JkPathSequence resultClasspath = classpath.andPrepend(beanClassesResolver.defClassDir);
        yetCompiledProjects.put(this.projectBaseDir, resultClasspath);
        CompilationResult compilationResult = new CompilationResult(
                JkPathSequence.of(compilationContext.importedProjectDirs),
                JkPathSequence.of(failedProjects).withoutDuplicates(),
                resultClasspath,
                compilationContext.classpathChanged || importedProjectClasspathChanged);
        JkRuntime runtime = JkRuntime.get(projectBaseDir);
        runtime.setDependencyResolver(dependencyResolver);
        runtime.setImportedProjects(compilationResult.importedProjects);
        runtime.setClasspath(compilationResult.classpath);
        return compilationResult;
    }

    private SingleCompileResult compileDef(JkPathSequence defClasspath, List<String> compileOptions,
                                           boolean failOnCompileError) {
        JkPathTree.of(beanClassesResolver.defClassDir).deleteContent();
        JkPathSequence extraClasspath = JkPathSequence.of();
        JkProperties props = JkRuntime.constructProperties(projectBaseDir);
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
                final JkKotlinJvmCompileSpec kotlinCompileSpec = defKotlinCompileSpec(kotlinClasspath);
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
        final JkJavaCompileSpec javaCompileSpec = defJavaCompileSpec(defClasspath, compileOptions);
        if (javaCompileSpec.getSources().containFiles() && ToolProvider.getSystemJavaCompiler() == null) {
            throw new JkException("The running Java platform (" +  System.getProperty("java.home") +
                    ") does not provide compiler (javac). Please provide a JDK java platform by pointing JAVA_HOME" +
                    " or JEKA_JDK environment variable to a JDK directory.");
        }
        boolean success = wrapCompile(() -> JkJavaCompiler.of().compile(javaCompileSpec), failOnCompileError);
        if (!success) {
            return new SingleCompileResult(false, JkPathSequence.of());
        }
        JkPathTree.of(this.beanClassesResolver.defSourceDir)
                .andMatching(false, "**/*.java", "*.java", "**/*.kt", "*.kt")
                .copyTo(this.beanClassesResolver.defClassDir, StandardCopyOption.REPLACE_EXISTING);
        return new SingleCompileResult(true, extraClasspath);
    }

    private JkPathSequence jekaClasspath() {

        // If true, we assume Jeka is provided in IDE classpath (development mode)
        final boolean devMode = Files.isDirectory(JkLocator.getJekaJarPath());
        JkPathSequence result = JkPathSequence.of(bootLibs());
        if (devMode) {
            result = result.and(JkClasspath.ofCurrentRuntime());
        } else {
            result = result.and(JkLocator.getJekaJarPath());
        }
        JkLog.trace("Use Jeka " + result + " for compilation.");
        return result.withoutDuplicates();
    }

    private JkPathSequence bootLibs() {
        final List<Path>  extraLibs = new LinkedList<>();
        final Path bootDir = this.projectBaseDir.resolve(JkConstants.BOOT_DIR);
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
        return JkPathTree.of(beanClassesResolver.defSourceDir).andMatcher(KOTLIN_DEF_SOURCE_MATCHER)
                .count(1, false) > 0;
    }

    private JkJavaCompileSpec defJavaCompileSpec(JkPathSequence classpath, List<String> options) {
        final JkPathTree defSource = JkPathTree.of(beanClassesResolver.defSourceDir).andMatcher(JAVA_DEF_SOURCE_MATCHER);
        JkUtilsPath.createDirectories(beanClassesResolver.defClassDir);
        return JkJavaCompileSpec.of()
                .setClasspath(classpath.and(beanClassesResolver.defClassDir))
                .setOutputDir(beanClassesResolver.defClassDir)
                .setSources(defSource.toSet())
                .addOptions(options);
    }

    private JkKotlinJvmCompileSpec defKotlinCompileSpec(JkPathSequence defClasspath) {
        JkUtilsPath.createDirectories(beanClassesResolver.defClassDir);
        return JkKotlinJvmCompileSpec.of()
                .setClasspath(defClasspath)
                .setSources(JkPathTreeSet.ofRoots(beanClassesResolver.defSourceDir))
                .setOutputDir(JkUtilsPath.relativizeFromWorkingDir(beanClassesResolver.defClassDir));
    }

    @Override
    public String toString() {
        return this.projectBaseDir.getFileName().toString();
    }

    private static class CompilationContext {

        private final JkPathSequence classpath;

        private final List<Path> importedProjectDirs;

        private final List<String> compileOptions;

        private final boolean classpathChanged;

        CompilationContext(JkPathSequence classpath, List<Path> importedProjectDirs,
                           List<String> compileOptions, boolean classpathChanged) {
            this.classpath = classpath;
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
        List<Class<? extends JkBean>> localBeanClasses = beanClassesResolver.defBeanClasses();
        List<Class> globalBeanClasses = beanClassesResolver.globalBeanClassNames().stream()
                .map(className -> JkClassLoader.ofCurrent().loadIfExist(className))
                .filter(Objects::nonNull)   // due to cache, some classNames may not be in classpath
                .filter(beanClass -> !localBeanClasses.contains(beanClass))
                .collect(Collectors.toList());
        HelpDisplayer.help(localBeanClasses, globalBeanClasses, false, this.projectBaseDir);
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

}
