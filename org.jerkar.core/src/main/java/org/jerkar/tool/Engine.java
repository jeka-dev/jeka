package org.jerkar.tool;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathMatcher;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompileSpec;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.system.JkException;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.*;
import org.jerkar.tool.CommandLine.MethodInvocation;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Engine having responsibility of compiling run classes, instantiate and run them.<br/>
 * Run class sources are expected to lie in [project base dir]/jerkar/def <br/>
 * Classes having simple name starting with '_' are ignored.
 *
 * Run classes can have dependencies on jars : <ul>
 *     <li>located in [base dir]/jerkar/boot directory</li>
 *     <li>declared in {@link JkImport} annotation</li>
 * </ul>
 *
 */
final class Engine {

    private final JkPathMatcher RUN_SOURCE_MATCHER = JkPathMatcher.accept("**.java").andReject("**/_*", "_*");

    private final Path projectBaseDir;

    private JkDependencySet runDependencies;

    private JkRepoSet runRepos;

    private List<Path> rootsOfImportedRuns = new LinkedList<>();

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

    <T extends JkRun> T getRun(Class<T> baseClass) {
        if (resolver.needCompile()) {
            this.compile();
        }
        return resolver.resolve(baseClass);
    }

    /**
     * Pre-compile and compile run classes (if needed) then execute methods mentioned in command line
     */
    void execute(CommandLine commandLine, String runClassHint, JkLog.Verbosity verbosityToRestore) {
        runDependencies = runDependencies.andUnscoped(commandLine.dependencies());
        long start = System.nanoTime();
        JkLog.startTask("Compile and initialise run classes");
        JkRun jkRun = null;
        JkPathSequence path = JkPathSequence.of();
        if (!commandLine.dependencies().isEmpty()) {
            final JkPathSequence cmdPath = pathOf(commandLine.dependencies());
            path = path.prependMany(cmdPath);
            JkLog.trace("Command line extra path : " + cmdPath);
        }
        if (!JkUtilsString.isBlank(runClassHint)) {  // First find a class in the existing classpath without compiling
            preCompile();  // Need to pre-compile to get the declared run dependencies
            jkRun = getRunInstance(runClassHint, path);
        }
        if (jkRun == null) {
            path = compile().appendMany(path);
            jkRun = getRunInstance(runClassHint, path);
        }
        if (jkRun == null) {
            throw new JkException("Can't find or guess any run class for project hosted in " + this.projectBaseDir
                    + " .\nAre you sure this directory is a Jerkar project ?");
        }
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        JkLog.info("Jerkar run is ready to start.");
        JkLog.setVerbosity(verbosityToRestore);
        try {
            this.launch(jkRun, commandLine);
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
        return JkDependencyResolver.of(this.runRepos).get(deps);
    }

    private void preCompile() {
        List<Path> sourceFiles = JkPathTree.of(resolver.runSourceDir).andMatcher(RUN_SOURCE_MATCHER).files();
        final SourceParser parser = SourceParser.of(this.projectBaseDir, sourceFiles);
        this.runDependencies = this.runDependencies.and(parser.dependencies());
        this.runRepos = parser.importRepos().and(runRepos);
        this.rootsOfImportedRuns = parser.projects();
    }

    // Compiles and returns the runtime classpath
    private JkPathSequence compile() {
        final LinkedHashSet<Path> entries = new LinkedHashSet<>();
        compile(new HashSet<>(), entries);
        return JkPathSequence.ofMany(entries).withoutDuplicates();
    }

    private void compile(Set<Path>  yetCompiledProjects, LinkedHashSet<Path>  path) {
        if (!this.resolver.hasDefSource() || yetCompiledProjects.contains(this.projectBaseDir)) {
            return;
        }
        yetCompiledProjects.add(this.projectBaseDir);
        preCompile(); // This enrich dependencies
        String msg = "Compiling run classes for project " + this.projectBaseDir.getFileName().toString();
        long start = System.nanoTime();
        JkLog.startTask(msg);
        final JkDependencyResolver runDependencyResolver = getRunDependencyResolver();
        final JkPathSequence runPath = runDependencyResolver.get(this.computeRunDependencies());
        path.addAll(runPath.entries());
        path.addAll(compileDependentProjects(yetCompiledProjects, path).entries());
        this.compileDef(JkPathSequence.ofMany(path));
        path.add(this.resolver.runClassDir);
        JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
    }

    private JkRun getRunInstance(String runClassHint, JkPathSequence runtimePath) {
        final JkClassLoader classLoader = JkClassLoader.current();
        classLoader.addEntries(runtimePath);
        JkLog.trace("Setting run execution classpath to : " + classLoader.childClasspath());
        final JkRun run = resolver.resolve(runClassHint);
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

        // If true, we assume Jerkar is provided by IDE (development mode)
        final boolean devMode = Files.isDirectory(JkLocator.jerkarJarPath());
        return JkDependencySet.of(runDependencies
                .andFiles(localRunPath())
                .andFiles(JkClasspath.current()).onlyIf(devMode)
                .andFiles(jerkarLibs()).onlyIf(!devMode)
                .withDefaultScope(JkScopeMapping.ALL_TO_DEFAULT));
    }

    private JkPathSequence localRunPath() {
        final List<Path>  extraLibs = new LinkedList<>();
        final Path localDefLibDir = this.projectBaseDir.resolve(JkConstants.BOOT_DIR);
        if (Files.exists(localDefLibDir)) {
            extraLibs.addAll(JkPathTree.of(localDefLibDir).andAccept("**.jar").files());
        }
        return JkPathSequence.ofMany(extraLibs).withoutDuplicates();
    }

    private JkPathSequence compileDependentProjects(Set<Path> yetCompiledProjects, LinkedHashSet<Path>  pathEntries) {
        JkPathSequence pathSequence = JkPathSequence.of();
        if (!this.rootsOfImportedRuns.isEmpty()) {
            JkLog.info("Compile run classes of dependent projects : "
                        + toRelativePaths(this.projectBaseDir, this.rootsOfImportedRuns));
        }
        for (final Path file : this.rootsOfImportedRuns) {
            final Engine engine = new Engine(file.toAbsolutePath().normalize());
            engine.compile(yetCompiledProjects, pathEntries);
            pathSequence = pathSequence.append(file);
        }
        return pathSequence;
    }

    private void compileDef(JkPathSequence runPath) {
        JkJavaCompileSpec compileSpec = defCompileSpec().setClasspath(runPath);
        JkJavaCompiler.of().compile(compileSpec);
        JkPathTree.of(this.resolver.runSourceDir).andReject("**/*.java").copyTo(this.resolver.runClassDir,
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void launch(JkRun jkRun, CommandLine commandLine) {
        if (!commandLine.getSubProjectMethods().isEmpty()) {
            for (final JkRun importedRun : jkRun.importedRuns().all()) {
                runProject(importedRun, commandLine.getSubProjectMethods());
            }
            runProject(jkRun, commandLine.getSubProjectMethods());
        }
        runProject(jkRun, commandLine.getMasterMethods());
    }

    private JkJavaCompileSpec defCompileSpec() {
        final JkPathTree defSource = JkPathTree.of(resolver.runSourceDir).andMatcher(RUN_SOURCE_MATCHER);
        JkUtilsPath.createDirectories(resolver.runClassDir);
        return new JkJavaCompileSpec().setOutputDir(resolver.runClassDir)
                .addSources(defSource.files());
    }

    private JkDependencyResolver getRunDependencyResolver() {
        if (this.computeRunDependencies().containsModules()) {
            return JkDependencyResolver.of(this.runRepos);
        }
        return JkDependencyResolver.of();
    }

    private static JkPathSequence jerkarLibs() {
        final List<Path>  extraLibs = new LinkedList<>();
        extraLibs.add(JkLocator.jerkarJarPath());
        return JkPathSequence.ofMany(extraLibs).withoutDuplicates();
    }

    private static void runProject(JkRun jkRun, List<MethodInvocation> invokes) {
        for (MethodInvocation methodInvocation : invokes) {
            invokeMethodOnRunClassOrPlugin(jkRun, methodInvocation);
        }
    }

    private static void invokeMethodOnRunClassOrPlugin(JkRun jkRun, MethodInvocation methodInvocation) {
        if (methodInvocation.pluginName != null) {
            final JkPlugin plugin = jkRun.plugins().get(methodInvocation.pluginName);
            invokeMethodOnRunOrPlugin(plugin, methodInvocation.methodName);
        } else {
            invokeMethodOnRunOrPlugin(jkRun, methodInvocation.methodName);
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

    private static JkRepoSet repos() {
        return JkRepoSet.of(JkRepoConfigOptionLoader.runRepository(), JkRepo.local());
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
