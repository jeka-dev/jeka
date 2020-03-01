package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkUrlClassLoader;
import dev.jeka.core.api.kotlin.JkKotlinJvmCompileSpec;
import dev.jeka.core.api.kotlin.JkKotlinCompiler;
import dev.jeka.core.api.system.JkException;
import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.*;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Engine having responsibility of compiling command classes, instantiate and run them.<br/>
 * Command class sources are expected to lie in [project base dir]/jeka/def <br/>
 * Classes having simple name starting with '_' are ignored.
 *
 * Command classes can have dependencies on jars : <ul>
 *     <li>located in [base dir]/jeka/boot directory</li>
 *     <li>declared in {@link JkImport} annotation</li>
 * </ul>
 */
final class Engine {

    private final JkPathMatcher JAVA_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.java")
            .and(false, "**/_*", "_*");

    private final JkPathMatcher KOTLIN_DEF_SOURCE_MATCHER = JkPathMatcher.of(true,"**.kt")
           .and(false, "**/_*", "_*");

    private final Path projectBaseDir;

    private JkDependencySet defDependencies;

    private JkRepoSet defRepos;

    private List<Path> rootOfImportedRuns = new LinkedList<>();

    private List<String> compileOptions = new LinkedList<>();

    private final CommandResolver resolver;

    /**
     * Constructs an engine for the specified base directory.
     */
    Engine(Path baseDir) {
        super();
        JkUtilsAssert.isTrue(baseDir.isAbsolute(), baseDir + " is not absolute.");
        JkUtilsAssert.isTrue(Files.isDirectory(baseDir), baseDir + " is not directory.");
        this.projectBaseDir = baseDir.normalize();
        defRepos = repos();
        this.defDependencies = JkDependencySet.of();
        this.resolver = new CommandResolver(baseDir);
    }

    <T extends JkCommands> T getCommands(Class<T> baseClass, boolean initialised) {
        if (resolver.needCompile()) {
            this.compile();
        }
        return resolver.resolve(baseClass, initialised);
    }

    /**
     * Pre-compile and compile command classes (if needed) then execute methods mentioned in command line
     */
    void execute(CommandLine commandLine, String runClassHint, JkLog.Verbosity verbosityToRestore) {
        defDependencies = defDependencies.andScopelessDependencies(commandLine.dependencies());
        final long start = System.nanoTime();
        JkLog.startTask("Compile def classes and initialise command classes");
        JkCommands jkCommands = null;
        JkPathSequence path = JkPathSequence.of();
        if (!commandLine.dependencies().isEmpty()) {
            final JkPathSequence cmdPath = pathOf(commandLine.dependencies());
            path = path.andPrepending(cmdPath);
            JkLog.trace("Command line extra path : " + cmdPath);
        }
        preCompile();  // Need to pre-compile to get the declared run dependencies
        if (!JkUtilsString.isBlank(runClassHint)) {  // First find a class in the existing classpath without compiling
            jkCommands = getCommandsInstance(runClassHint, path);
        }
        if (jkCommands == null) {
            path = compile().and(path);
            jkCommands = getCommandsInstance(runClassHint, path);
            if (jkCommands == null) {
                throw new JkException("Can't find or guess any command class for project hosted in " + this.projectBaseDir
                        + " .\nAre you sure this directory is a Jeka project ?");
            }
        }
        jkCommands.getImportedCommands().setImportedRunRoots(this.rootOfImportedRuns);
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        JkLog.info("Jeka commands are ready to be executed.");
        JkLog.setVerbosity(verbosityToRestore);
        try {
            this.launch(jkCommands, commandLine);
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private JkPathSequence pathOf(List<? extends JkDependency> dependencies) {
        JkDependencySet deps = JkDependencySet.of();
        for (final JkDependency dependency : dependencies) {
            deps = deps.and(dependency);
        }
        return JkDependencyResolver.of(this.defRepos).resolve(deps).getFiles();
    }

    private void preCompile() {
        final List<Path> sourceFiles = JkPathTree.of(resolver.defSourceDir)
                .andMatcher(JAVA_DEF_SOURCE_MATCHER.or(KOTLIN_DEF_SOURCE_MATCHER)).getFiles();
        final SourceParser parser = SourceParser.of(this.projectBaseDir, sourceFiles);
        this.defDependencies = this.defDependencies.and(parser.dependencies());
        this.defRepos = parser.importRepos().and(defRepos);
        this.rootOfImportedRuns = parser.projects();
        this.compileOptions = parser.compileOptions();
    }

    // Compiles and returns the runtime classpath
    private JkPathSequence compile() {
        final LinkedHashSet<Path> entries = new LinkedHashSet<>();
        compile(new HashSet<>(), entries);
        return JkPathSequence.of(entries).withoutDuplicates();
    }

    private void compile(Set<Path>  yetCompiledProjects, LinkedHashSet<Path>  path) {
        if (!this.resolver.hasDefSource() || yetCompiledProjects.contains(this.projectBaseDir)) {
            return;
        }
        yetCompiledProjects.add(this.projectBaseDir);
        preCompile(); // This enrich dependencies
        final String msg = "Compiling def classes for project " + this.projectBaseDir.getFileName().toString();
        final long start = System.nanoTime();
        JkLog.startTask(msg);
        final JkDependencyResolver runDependencyResolver = getRunDependencyResolver();
        final JkResolveResult resolveResult = runDependencyResolver.resolve(this.computeDefDependencies());
        if (resolveResult.getErrorReport().hasErrors()) {
            JkLog.warn(resolveResult.getErrorReport().toString());
        }
        final JkPathSequence runPath = resolveResult.getFiles();
        path.addAll(runPath.getEntries());
        path.addAll(compileDependentProjects(yetCompiledProjects, path).getEntries());
        compileDef(JkPathSequence.of(path));
        path.add(this.resolver.defClassDir);
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
    }

    private JkCommands getCommandsInstance(String commandClassHint, JkPathSequence runtimePath) {
        final JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent();
        classLoader.addEntries(runtimePath);
        JkLog.trace("Setting run execution classpath to : " + classLoader.getDirectClasspath());
        final JkCommands commands = resolver.resolve(commandClassHint);
        if (commands == null) {
            return null;
        }
        try {
            commands.setDefDependencyResolver(this.computeDefDependencies(), getRunDependencyResolver());
            return commands;
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private JkDependencySet computeDefDependencies() {

        // If true, we assume Jeka is provided by IDE (development mode)
        final boolean devMode = Files.isDirectory(JkLocator.getJekaJarPath());
        JkScopeMapping scope = JkScope.of("*").mapTo("default(*)");
        return JkDependencySet.of(defDependencies
                .andFiles(bootLibs())
                .andFiles(JkClasspath.ofCurrentRuntime()).minusLastIf(!devMode)
                .andFile(JkLocator.getJekaJarPath()).minusLastIf(devMode)
                .withDefaultScope(scope));
    }

    private JkPathSequence bootLibs() {
        final List<Path>  extraLibs = new LinkedList<>();
        final Path bootDir = this.projectBaseDir.resolve(JkConstants.BOOT_DIR);
        if (Files.exists(bootDir)) {
            extraLibs.addAll(JkPathTree.of(bootDir).andMatching(true,"**.jar").getFiles());
        }
        return JkPathSequence.of(extraLibs).withoutDuplicates();
    }

    private JkPathSequence compileDependentProjects(Set<Path> yetCompiledProjects, LinkedHashSet<Path>  pathEntries) {
        JkPathSequence pathSequence = JkPathSequence.of();
        if (!this.rootOfImportedRuns.isEmpty()) {
            JkLog.info("Compile command classes of dependent projects : "
                    + toRelativePaths(this.projectBaseDir, this.rootOfImportedRuns));
        }
        for (final Path file : this.rootOfImportedRuns) {
            final Engine engine = new Engine(file.toAbsolutePath().normalize());
            engine.compile(yetCompiledProjects, pathEntries);
            pathSequence = pathSequence.and(file);
        }
        return pathSequence;
    }

    private void compileDef(JkPathSequence defClasspath) {
        JkPathTree.of(resolver.defClassDir).deleteContent();
        if (hasKotlin()) {
            final JkKotlinJvmCompileSpec kotlinCompileSpec = defKotlinCompileSpec(defClasspath);
            JkKotlinCompiler kotlinCompiler = JkKotlinCompiler.ofKotlinHome();
            wrapCompile(() -> kotlinCompiler.compile(kotlinCompileSpec));
            JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent();
            classLoader.addEntries(kotlinCompiler.getStdLib());
        }
        final JkJavaCompileSpec javaCompileSpec = defJavaCompileSpec(defClasspath);
        wrapCompile(() -> JkJavaCompiler.ofJdk().compile(javaCompileSpec));
        JkPathTree.of(this.resolver.defSourceDir).andMatching(false, "**/*.java", "**/*.kt")
        .copyTo(this.resolver.defClassDir,
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void wrapCompile(Runnable runnable) {
        try {
            runnable.run();
        } catch (final JkException e) {
            JkLog.setVerbosity(JkLog.Verbosity.NORMAL);
            JkLog.info("Compilation of Jeka files failed. You can run jeka -CC=JkCommands to use default commands " +
                    " instead of the ones defined in 'def'.");
            throw e;
        }
    }

    private void launch(JkCommands jkCommands, CommandLine commandLine) {
        if (!commandLine.getSubProjectMethods().isEmpty()) {
            for (final JkCommands importedCommands : jkCommands.getImportedCommands().getAll()) {
                runProject(importedCommands, commandLine.getSubProjectMethods());
            }
            runProject(jkCommands, commandLine.getSubProjectMethods());
        }
        runProject(jkCommands, commandLine.getMasterMethods());
    }

    private boolean hasKotlin() {
        return JkPathTree.of(resolver.defSourceDir).andMatcher(KOTLIN_DEF_SOURCE_MATCHER)
                .count(1, false) > 0;
    }

    private JkJavaCompileSpec defJavaCompileSpec(JkPathSequence classpath) {
        final JkPathTree defSource = JkPathTree.of(resolver.defSourceDir).andMatcher(JAVA_DEF_SOURCE_MATCHER);
        JkUtilsPath.createDirectories(resolver.defClassDir);
        return JkJavaCompileSpec.of()
                .setClasspath(classpath.and(resolver.defClassDir))
                .setOutputDir(resolver.defClassDir)
                .addSources(defSource.getFiles())
                .addOptions(this.compileOptions);
    }

    private JkKotlinJvmCompileSpec defKotlinCompileSpec(JkPathSequence defClasspath) {
        JkUtilsPath.createDirectories(resolver.defClassDir);
        return JkKotlinJvmCompileSpec.of()
                .setClasspath(defClasspath)
                .addSources(resolver.defSourceDir)
                .setOutputDir(resolver.defClassDir);
    }

    private JkDependencyResolver getRunDependencyResolver() {
        if (this.computeDefDependencies().hasModules()) {
            return JkDependencyResolver.of(this.defRepos);
        }
        return JkDependencyResolver.of();
    }

    private static void runProject(JkCommands jkCommands, List<CommandLine.MethodInvocation> invokes) {
        for (final CommandLine.MethodInvocation methodInvocation : invokes) {
            invokeMethodCommandsOrPlugin(jkCommands, methodInvocation);
        }
    }

    private static void invokeMethodCommandsOrPlugin(JkCommands jkCommands, CommandLine.MethodInvocation methodInvocation) {
        if (methodInvocation.pluginName != null) {
            final JkPlugin plugin = jkCommands.getPlugins().get(methodInvocation.pluginName);
            invokeMethodOnCommandsOrPlugin(plugin, methodInvocation.methodName);
        } else {
            invokeMethodOnCommandsOrPlugin(jkCommands, methodInvocation.methodName);
        }
    }

    /**
     * Invokes the specified method in this run.
     */
    private static void invokeMethodOnCommandsOrPlugin(Object run, String methodName) {
        final Method method;
        try {
            method = run.getClass().getMethod(methodName);
        } catch (final NoSuchMethodException e) {
            throw new JkException("No public zero-arg method '" + methodName + "' found in class '" + run.getClass());
        }
        String fullMethodName = run.getClass().getName() + "#" + methodName;
        if (Environment.standardOptions.logHeaders) {
            JkLog.startTask("\nExecuting method : " + fullMethodName);
        }
        try {
            JkUtilsReflect.invoke(run, method);
            if (Environment.standardOptions.logHeaders) {
                JkLog.endTask("Method " + fullMethodName + " succeeded in %d milliseconds.");
            }
        } catch (final RuntimeException e) {
            if (Environment.standardOptions.logHeaders) {
                JkLog.endTask("Method " + fullMethodName + " failed in %d milliseconds.");
            }
            throw e;
        }
    }

    static JkRepoSet repos() {
        return JkRepoSet.of(JkRepoConfigOptionLoader.runRepository(), JkRepo.ofLocal());
    }

    private static List<String> toRelativePaths(Path from, List<Path>  files) {
        final List<String> result = new LinkedList<>();
        for (final Path file : files) {
            final String relPath = from.relativize(file).toString();
            result.add(relPath);
        }
        return result;
    }

    @Override
    public String toString() {
        return this.projectBaseDir.getFileName().toString();
    }

}
