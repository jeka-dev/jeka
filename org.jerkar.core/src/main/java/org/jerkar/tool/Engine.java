package org.jerkar.tool;

import org.jerkar.api.depmanagement.*;
import org.jerkar.api.file.JkPathMatcher;
import org.jerkar.api.file.JkPathSequence;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompileSpec;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsAssert;
import org.jerkar.api.utils.JkUtilsPath;
import org.jerkar.api.utils.JkUtilsReflect;
import org.jerkar.api.utils.JkUtilsTime;
import org.jerkar.tool.CommandLine.MethodInvocation;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Engine having responsibility of compiling build classes, instantiate and run them.<br/>
 * Build class sources are expected to lie in [project base dir]/build/def <br/>
 * Classes having simple name starting with '_' are ignored.
 *
 * Build classes can have dependencies on jars : <ul>
 *     <li>located in [base dir]/build/boot directory</li>
 *     <li>declared in {@link JkImport} annotation</li>
 * </ul>
 *
 */
final class Engine {

    private final JkPathMatcher BUILD_SOURCE_MATCHER = JkPathMatcher.accept("**.java").andRefuse("**/_*", "_*");

    private final Path projectBaseDir;

    private JkDependencies buildDependencies;

    private JkRepos buildRepos;

    private List<Path>  rootsOfImportedBuilds = new LinkedList<>();

    private final BuildResolver resolver;

    /**
     * Constructs an engine for the specified base directory.
     */
    Engine(Path baseDir) {
        super();
        JkUtilsAssert.isTrue(baseDir.isAbsolute(), baseDir + " is not absolute.");
        JkUtilsAssert.isTrue(Files.isDirectory(baseDir), baseDir + " is not directory.");
        this.projectBaseDir = baseDir.normalize();
        buildRepos = repos();
        this.buildDependencies = JkDependencies.of();
        this.resolver = new BuildResolver(baseDir);
    }

    <T extends JkBuild> T getBuild(Class<T> baseClass) {
        if (resolver.needCompile()) {
            this.compile();
        }
        return resolver.resolve(baseClass);
    }

    /**
     * Pre-compile and compile build classes (if needed) then execute the build
     * of this project.
     */
    void execute(CommandLine commandLine, String buildClassHint) {
        this.buildDependencies = this.buildDependencies.andScopeless(commandLine.dependencies());
        final AtomicReference<JkBuild> build = new AtomicReference<>();
        long start = System.nanoTime();
        if (!Environment.standardOptions.logNoHeaders) {
            JkLog.startTask("Compile and initialise build classes");
        }
        JkPathSequence runtimeClasspath = compile();
        if (!commandLine.dependencies().isEmpty()) {
            final JkPathSequence cmdPath = pathOf(commandLine.dependencies());
            runtimeClasspath = runtimeClasspath.andManyFirst(cmdPath);
            JkLog.trace("Command line extra path : " + cmdPath);
        }
        build.set(getBuildInstance(buildClassHint, runtimeClasspath));
        if (build == null) {
            throw new JkException("Can't find or guess any build class for project hosted in " + this.projectBaseDir
                    + " .\nAre you sure this directory is a buildable project ?");
        }
        if (!Environment.standardOptions.logNoHeaders) {
            JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
            JkLog.info("Build is ready to start.");
        }
        try {
            this.launch(build.get(), commandLine);
        } catch (final RuntimeException e) {
            if (!Environment.standardOptions.logNoHeaders) {
                JkLog.error("Engine " + projectBaseDir + " failed");
            }
            throw e;
        }
    }

    private JkPathSequence pathOf(List<? extends JkDependency> dependencies) {
        final JkDependencies deps = JkDependencies.of(dependencies);
        return JkDependencyResolver.of(this.buildRepos).get(deps);
    }

    private void preCompile() {
        List<Path> sourceFiles = JkPathTree.of(resolver.buildSourceDir).andMatcher(BUILD_SOURCE_MATCHER).files();
        final SourceParser parser = SourceParser.of(this.projectBaseDir, sourceFiles);
        this.buildDependencies = this.buildDependencies.and(parser.dependencies());
        this.buildRepos = parser.importRepos().and(buildRepos);
        this.rootsOfImportedBuilds = parser.projects();
    }

    // Compiles and returns the runtime classpath
    private JkPathSequence compile() {
        final LinkedHashSet<Path> entries = new LinkedHashSet<>();
        compile(new HashSet<>(), entries);
        return JkPathSequence.ofMany(entries).withoutDuplicates();
    }

    private void compile(Set<Path>  yetCompiledProjects, LinkedHashSet<Path>  path) {
        if (!this.resolver.hasBuildSource() || yetCompiledProjects.contains(this.projectBaseDir)) {
            return;
        }
        yetCompiledProjects.add(this.projectBaseDir);
        preCompile(); // This enrich dependencies
        String msg = "Compiling build classes for project " + this.projectBaseDir.getFileName().toString();
        long start = System.nanoTime();
        if (!Environment.standardOptions.logNoHeaders) {
            JkLog.startTask(msg);
        }
            final JkDependencyResolver buildClassDependencyResolver = getBuildDefDependencyResolver();
            final JkPathSequence buildPath = buildClassDependencyResolver.get(this.buildDefDependencies());
            path.addAll(buildPath.entries());
            path.addAll(compileDependentProjects(yetCompiledProjects, path).entries());
            this.compileBuild(JkPathSequence.ofMany(path));
            path.add(this.resolver.buildClassDir);
        if (!Environment.standardOptions.logNoHeaders) {
            JkLog.endTask("Done in " + JkUtilsTime.durationInMillis(start) + " milliseconds.");
        }
    }

    private JkBuild getBuildInstance(String buildClassHint, JkPathSequence runtimePath) {
        final JkClassLoader classLoader = JkClassLoader.current();
        classLoader.addEntries(runtimePath);
        JkLog.trace("Setting build execution classpath to : " + classLoader.childClasspath());
        final JkBuild build = resolver.resolve(buildClassHint);
        if (build == null) {
            return null;
        }
        try {
            build.setBuildDefDependencyResolver(this.buildDefDependencies(), getBuildDefDependencyResolver());
            return build;
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private JkDependencies buildDefDependencies() {

        // If true, we assume Jerkar is provided by IDE (development mode)
        final boolean devMode = Files.isDirectory(JkLocator.jerkarJarPath());

        return JkDependencies.builder().on(buildDependencies
                .withDefaultScope(JkScopeMapping.ALL_TO_DEFAULT))
                .onFiles(localBuildPath())
                .onFilesIf(devMode, JkClasspath.current())
                .onFilesIf(!devMode, jerkarLibs())
                .build();
    }

    private JkPathSequence localBuildPath() {
        final List<Path>  extraLibs = new LinkedList<>();
        final Path localDefLibDir = this.projectBaseDir.resolve(JkConstants.BUILD_BOOT);
        if (Files.exists(localDefLibDir)) {
            extraLibs.addAll(JkPathTree.of(localDefLibDir).accept("**.jar").files());
        }
        return JkPathSequence.ofMany(extraLibs).withoutDuplicates();
    }

    private JkPathSequence compileDependentProjects(Set<Path> yetCompiledProjects, LinkedHashSet<Path>  pathEntries) {
        JkPathSequence pathSequence = JkPathSequence.of();
        if (!this.rootsOfImportedBuilds.isEmpty()) {
            if (!Environment.standardOptions.logNoHeaders) {
                JkLog.info("Compile build classes of dependent projects : "
                        + toRelativePaths(this.projectBaseDir, this.rootsOfImportedBuilds));
            }
        }
        for (final Path file : this.rootsOfImportedBuilds) {
            final Engine engine = new Engine(file.toAbsolutePath().normalize());
            engine.compile(yetCompiledProjects, pathEntries);
            pathSequence = pathSequence.and(file);
        }
        return pathSequence;
    }

    private void compileBuild(JkPathSequence buildPath) {
        JkJavaCompileSpec compileSpec = buildCompileSpec().setClasspath(buildPath);
        JkJavaCompiler.base().compile(compileSpec);
        JkPathTree.of(this.resolver.buildSourceDir).refuse("**/*.java").copyTo(this.resolver.buildClassDir,
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void launch(JkBuild build, CommandLine commandLine) {
        if (!commandLine.getSubProjectMethods().isEmpty()) {
            for (final JkBuild subBuild : build.importedBuilds().all()) {
                runProject(subBuild, commandLine.getSubProjectMethods());
            }
            runProject(build, commandLine.getSubProjectMethods());
        }
        runProject(build, commandLine.getMasterMethods());
    }

    private JkJavaCompileSpec buildCompileSpec() {
        final JkPathTree buildSource = JkPathTree.of(resolver.buildSourceDir).andMatcher(BUILD_SOURCE_MATCHER);
        JkUtilsPath.createDirectories(resolver.buildClassDir);
        return new JkJavaCompileSpec().setOutputDir(resolver.buildClassDir)
                .addSources(buildSource.files());
    }

    private JkDependencyResolver getBuildDefDependencyResolver() {
        if (this.buildDefDependencies().containsModules()) {
            return JkDependencyResolver.of(this.buildRepos);
        }
        return JkDependencyResolver.of();
    }

    private static JkPathSequence jerkarLibs() {
        final List<Path>  extraLibs = new LinkedList<>();
        extraLibs.add(JkLocator.jerkarJarPath());
        return JkPathSequence.ofMany(extraLibs).withoutDuplicates();
    }

    private static void runProject(JkBuild build, List<MethodInvocation> invokes) {
        for (MethodInvocation methodInvocation : invokes) {
            invokeMethodOnBuildClassOrPlugin(build, methodInvocation);
        }
    }

    private static void invokeMethodOnBuildClassOrPlugin(JkBuild build, MethodInvocation methodInvocation) {
        if (methodInvocation.pluginName != null) {
            final JkPlugin plugin = build.plugins().get(methodInvocation.pluginName);
            build.plugins().invoke(plugin, methodInvocation.methodName);
        } else {
            invokeMethodOnBuildClass(build, methodInvocation.methodName);
        }
    }

    /**
     * Invokes the specified method in this build.
     */
    private static void invokeMethodOnBuildClass(JkBuild build, String methodName) {
        final Method method;
        try {
            method = build.getClass().getMethod(methodName);
        } catch (final NoSuchMethodException e) {
            throw new JkException("No zero-arg method '" + methodName + "' found in class '" + build.getClass());
        }
        if (!Environment.standardOptions.logNoHeaders) {
            JkLog.info("\nMethod : " + methodName + " on " + build);
        }
        final long time = System.nanoTime();
        try {
            JkUtilsReflect.invoke(build, method);
            if (!Environment.standardOptions.logNoHeaders) {
                JkLog.info("Method " + methodName + " succeeded in "
                        + JkUtilsTime.durationInSeconds(time) + " seconds.");
            }
        } catch (final RuntimeException e) {
            if (!Environment.standardOptions.logNoHeaders) {
                JkLog.info("Method " + methodName + " failed in " + JkUtilsTime.durationInSeconds(time)
                        + " seconds.");
            }
            throw e;
        }
    }

    private static JkRepos repos() {
        return JkRepo
                .firstNonNull(
                        JkRepoOptions.repoFromOptions("build"),
                        JkRepoOptions.repoFromOptions("download"),
                        JkRepo.mavenCentral())
                .and(JkPublishRepo.local().repo());
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
