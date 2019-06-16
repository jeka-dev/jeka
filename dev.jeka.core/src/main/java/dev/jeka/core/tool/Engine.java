package dev.jeka.core.tool;

import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathMatcher;
import dev.jeka.core.api.file.JkPathSequence;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.java.JkClasspath;
import dev.jeka.core.api.java.JkJavaCompileSpec;
import dev.jeka.core.api.java.JkJavaCompiler;
import dev.jeka.core.api.java.JkUrlClassLoader;
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

    private final JkPathMatcher RUN_SOURCE_MATCHER = JkPathMatcher.of(true,"**.java").and(false, "**/_*", "_*");

    private final Path projectBaseDir;

    private JkDependencySet runDependencies;

    private JkRepoSet runRepos;

    private List<Path> rootOfImportedRuns = new LinkedList<>();

    private final RunResolver resolver;

    /**
     * Constructs an engine for the specified base directory.
     */
    Engine(Path baseDir) {
        super();
        JkUtilsAssert.isTrue(baseDir.isAbsolute(), baseDir + " is not absolute.");
        JkUtilsAssert.isTrue(Files.isDirectory(baseDir), baseDir + " is not directory.");
        this.projectBaseDir = baseDir.normalize();
        runRepos = repos();
        this.runDependencies = JkDependencySet.of();
        this.resolver = new RunResolver(baseDir);
    }

    <T extends JkCommands> T getRun(Class<T> baseClass) {
        if (resolver.needCompile()) {
            this.compile();
        }
        return resolver.resolve(baseClass);
    }

    /**
     * Pre-compile and compile command classes (if needed) then execute methods mentioned in command line
     */
    void execute(CommandLine commandLine, String runClassHint, JkLog.Verbosity verbosityToRestore) {
        runDependencies = runDependencies.andScopelessDependencies(commandLine.dependencies());
        long start = System.nanoTime();
        JkLog.startTask("Compile and initialise command classes");
        JkCommands jkCommands = null;
        JkPathSequence path = JkPathSequence.of();
        if (!commandLine.dependencies().isEmpty()) {
            final JkPathSequence cmdPath = pathOf(commandLine.dependencies());
            path = path.andPrepending(cmdPath);
            JkLog.trace("Command line extra path : " + cmdPath);
        }
        preCompile();  // Need to pre-compile to get the declared run dependencies
        if (!JkUtilsString.isBlank(runClassHint)) {  // First find a class in the existing classpath without compiling
            jkCommands = getRunInstance(runClassHint, path);
        }
        if (jkCommands == null) {
            path = compile().and(path);
            jkCommands = getRunInstance(runClassHint, path);
        }
        jkCommands.getImportedCommands().setImportedRunRoots(this.rootOfImportedRuns);
        if (jkCommands == null) {
            throw new JkException("Can't find or guess any command class for project hosted in " + this.projectBaseDir
                    + " .\nAre you sure this directory is a Jeka project ?");
        }
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
        for (JkDependency dependency : dependencies) {
            deps = deps.and(dependency);
        }
        return JkDependencyResolver.of(this.runRepos).resolve(deps).getFiles();
    }

    private void preCompile() {
        List<Path> sourceFiles = JkPathTree.of(resolver.runSourceDir).andMatcher(RUN_SOURCE_MATCHER).getFiles();
        final SourceParser parser = SourceParser.of(this.projectBaseDir, sourceFiles);
        this.runDependencies = this.runDependencies.and(parser.dependencies());
        this.runRepos = parser.importRepos().and(runRepos);
        this.rootOfImportedRuns = parser.projects();
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
        String msg = "Compiling command classes for project " + this.projectBaseDir.getFileName().toString();
        long start = System.nanoTime();
        JkLog.startTask(msg);
        final JkDependencyResolver runDependencyResolver = getRunDependencyResolver();
        JkResolveResult resolveResult = runDependencyResolver.resolve(this.computeRunDependencies());
        if (resolveResult.getErrorReport().hasErrors()) {
            JkLog.warn(resolveResult.getErrorReport().toString());
        }
        final JkPathSequence runPath = resolveResult.getFiles();
        path.addAll(runPath.getEntries());
        path.addAll(compileDependentProjects(yetCompiledProjects, path).getEntries());
        compileDef(JkPathSequence.of(path));
        path.add(this.resolver.runClassDir);
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
    }

    private JkCommands getRunInstance(String runClassHint, JkPathSequence runtimePath) {
        final JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent(); // Should be always a UrlClassloader
        classLoader.addEntries(runtimePath);
        JkLog.trace("Setting run execution classpath to : " + classLoader.getDirectClasspath());
        final JkCommands run = resolver.resolve(runClassHint);
        if (run == null) {
            return null;
        }
        try {
            run.setRunDependencyResolver(this.computeRunDependencies(), getRunDependencyResolver());
            return run;
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private JkDependencySet computeRunDependencies() {

        // If true, we assume Jeka is provided by IDE (development mode)
        final boolean devMode = Files.isDirectory(JkLocator.getJekaJarPath());
        return JkDependencySet.of(runDependencies
                .andFiles(localRunPath())
                .andFiles(JkClasspath.ofCurrentRuntime()).withoutLastIf(!devMode)
                .andFiles(jekaLibs()).withoutLastIf(devMode)
                .withDefaultScope(JkScopeMapping.ALL_TO_DEFAULT));
    }

    private JkPathSequence localRunPath() {
        final List<Path>  extraLibs = new LinkedList<>();
        final Path localDefLibDir = this.projectBaseDir.resolve(JkConstants.BOOT_DIR);
        if (Files.exists(localDefLibDir)) {
            extraLibs.addAll(JkPathTree.of(localDefLibDir).andMatching(true,"**.jar").getFiles());
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

    private void compileDef(JkPathSequence runPath) {
        JkJavaCompileSpec compileSpec = defCompileSpec().setClasspath(runPath);
        try {
            JkJavaCompiler.ofJdk().compile(compileSpec);
        } catch (JkException e) {
            JkLog.setVerbosity(JkLog.Verbosity.NORMAL);
            JkLog.info("Compilation of Jeka files failed. You can run jeka -CC=JkCommands to use default Jeka files" +
                    " instead of the ones located in this project.");
            throw e;
        }
        JkPathTree.of(this.resolver.runSourceDir).andMatching(false, "**/*.java")
                .copyTo(this.resolver.runClassDir,
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void launch(JkCommands jkCommands, CommandLine commandLine) {
        if (!commandLine.getSubProjectMethods().isEmpty()) {
            for (final JkCommands importedRun : jkCommands.getImportedCommands().getAll()) {
                runProject(importedRun, commandLine.getSubProjectMethods());
            }
            runProject(jkCommands, commandLine.getSubProjectMethods());
        }
        runProject(jkCommands, commandLine.getMasterMethods());
    }

    private JkJavaCompileSpec defCompileSpec() {
        final JkPathTree defSource = JkPathTree.of(resolver.runSourceDir).andMatcher(RUN_SOURCE_MATCHER);
        JkUtilsPath.createDirectories(resolver.runClassDir);
        return JkJavaCompileSpec.of().setOutputDir(resolver.runClassDir)
                .addSources(defSource.getFiles());
    }

    private JkDependencyResolver getRunDependencyResolver() {
        if (this.computeRunDependencies().hasModules()) {
            return JkDependencyResolver.of(this.runRepos);
        }
        return JkDependencyResolver.of();
    }

    private static JkPathSequence jekaLibs() {
        final List<Path>  extraLibs = new LinkedList<>();
        extraLibs.add(JkLocator.getJekaJarPath());
        return JkPathSequence.of(extraLibs).withoutDuplicates();
    }

    private static void runProject(JkCommands jkCommands, List<CommandLine.MethodInvocation> invokes) {
        for (CommandLine.MethodInvocation methodInvocation : invokes) {
            invokeMethodOnRunClassOrPlugin(jkCommands, methodInvocation);
        }
    }

    private static void invokeMethodOnRunClassOrPlugin(JkCommands jkCommands, CommandLine.MethodInvocation methodInvocation) {
        if (methodInvocation.pluginName != null) {
            final JkPlugin plugin = jkCommands.getPlugins().get(methodInvocation.pluginName);
            invokeMethodOnRunOrPlugin(plugin, methodInvocation.methodName);
        } else {
            invokeMethodOnRunOrPlugin(jkCommands, methodInvocation.methodName);
        }
    }

    /**
     * Invokes the specified method in this run.
     */
    private static void invokeMethodOnRunOrPlugin(Object run, String methodName) {
        final Method method;
        try {
            method = run.getClass().getMethod(methodName);
        } catch (final NoSuchMethodException e) {
            throw new JkException("No public zero-arg method '" + methodName + "' found in class '" + run.getClass());
        }
        if (Environment.standardOptions.logHeaders) {
            JkLog.info("Method : " + methodName + " on " + run.getClass().getName());
        }
        final long time = System.nanoTime();
        try {
            JkUtilsReflect.invoke(run, method);
            if (Environment.standardOptions.logHeaders) {
                JkLog.info("Method " + methodName + " succeeded in "
                        + JkUtilsTime.durationInMillis(time) + " milliseconds.");
            }
        } catch (final RuntimeException e) {
            JkLog.info("Method " + methodName + " failed in " + JkUtilsTime.durationInMillis(time)
                        + " milliseconds.");
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
