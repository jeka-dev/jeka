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
 *     <li>declared in {@link JkDefClasspath} annotation</li>
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

    private List<Path> rootsOfImportedCommandSets = new LinkedList<>();

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

    <T extends JkCommandSet> T getCommands(Class<T> baseClass, boolean initialised) {
        if (resolver.needCompile()) {
            this.compile();
        }
        return resolver.resolve(baseClass, initialised);
    }

    /**
     * Pre-compile and compile command classes (if needed) then execute methods mentioned in command line
     */
    void execute(CommandLine commandLine, String commandSetClassHint, JkLog.Verbosity verbosityToRestore) {
        defDependencies = defDependencies.andScopelessDependencies(commandLine.dependencies());
        final long start = System.nanoTime();
        JkLog.startTask("Compile def and initialise commandSet classes");
        JkCommandSet jkCommandSet = null;
        JkPathSequence path = JkPathSequence.of();
        if (!commandLine.dependencies().isEmpty()) {
            final JkPathSequence cmdPath = pathOf(commandLine.dependencies());
            path = path.andPrepend(cmdPath);
            JkLog.trace("Command line extra path : " + cmdPath);
        }
        preCompile();  // Need to pre-compile to get the declared def dependencies
        if (!JkUtilsString.isBlank(commandSetClassHint)) {  // First find a class in the existing classpath without compiling
            jkCommandSet = getCommandsInstance(commandSetClassHint, path);
        }
        if (jkCommandSet == null) {
            path = compile().and(path);
            jkCommandSet = getCommandsInstance(commandSetClassHint, path);
            if (jkCommandSet == null) {
                throw new JkException("Can't find or guess any command class for project hosted in " + this.projectBaseDir
                        + " .\nAre you sure this directory is a Jeka project ?");
            }
        }
        jkCommandSet.getImportedCommandSets().setImportedRunRoots(this.rootsOfImportedCommandSets);
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        JkLog.info("Jeka commands are ready to be executed.");
        JkLog.setVerbosity(verbosityToRestore);
        try {
            this.launch(jkCommandSet, commandLine);
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
        this.rootsOfImportedCommandSets = parser.projects();
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

    private JkCommandSet getCommandsInstance(String commandClassHint, JkPathSequence runtimePath) {
        final JkUrlClassLoader classLoader = JkUrlClassLoader.ofCurrent();
        classLoader.addEntries(runtimePath);
        JkLog.trace("Setting def execution classpath to : " + classLoader.getDirectClasspath());
        final JkCommandSet commands = resolver.resolve(commandClassHint);
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
        boolean compileImports = !this.rootsOfImportedCommandSets.isEmpty();
        if (compileImports) {
            JkLog.startTask("Compile command classes of dependent projects : "
                    + toRelativePaths(this.projectBaseDir, this.rootsOfImportedCommandSets));
        }
        for (final Path file : this.rootsOfImportedCommandSets) {
            final Engine engine = new Engine(file.toAbsolutePath().normalize());
            engine.compile(yetCompiledProjects, pathEntries);
            pathSequence = pathSequence.and(file);
        }
        if (compileImports) {
            JkLog.endTask();
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
        wrapCompile(() -> JkJavaCompiler.of().compile(javaCompileSpec));
        JkPathTree.of(this.resolver.defSourceDir)
                .andMatching(false, "**/*.java", "*.java", "**/*.kt", "*.kt")
                .copyTo(this.resolver.defClassDir, StandardCopyOption.REPLACE_EXISTING);
    }

    private void wrapCompile(Runnable runnable) {
        try {
            runnable.run();
        } catch (final JkException e) {
            JkLog.setVerbosity(JkLog.Verbosity.NORMAL);
            JkLog.info("Compilation of Jeka files failed. You can run jeka -CC=JkCommandSet to use default commands " +
                    " instead of the ones defined in 'def'.");
            throw e;
        }
    }

    private void launch(JkCommandSet jkCommandSet, CommandLine commandLine) {
        if (!commandLine.getSubProjectMethods().isEmpty()) {
            for (final JkCommandSet importedCommands : jkCommandSet.getImportedCommandSets().getAll()) {
                runProject(importedCommands, commandLine.getSubProjectMethods());
            }
            runProject(jkCommandSet, commandLine.getSubProjectMethods());
        }
        runProject(jkCommandSet, commandLine.getMasterMethods());
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

    private static void runProject(JkCommandSet jkCommandSet, List<CommandLine.MethodInvocation> invokes) {
        for (final CommandLine.MethodInvocation methodInvocation : invokes) {
            invokeMethodOnCommandSetOrPlugin(jkCommandSet, methodInvocation);
        }
    }

    private static void invokeMethodOnCommandSetOrPlugin(JkCommandSet commandSet,
                                                         CommandLine.MethodInvocation methodInvocation) {
        if (methodInvocation.pluginName != null) {
            final JkPlugin plugin = commandSet.getPlugins().get(methodInvocation.pluginName);
            invokeMethodOnCommandsOrPlugin(plugin, methodInvocation.methodName);
        } else {
            invokeMethodOnCommandsOrPlugin(commandSet, methodInvocation.methodName);
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
        return JkRepoSet.of(JkRepoConfigOptionLoader.defRepository(), JkRepo.ofLocal());
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
