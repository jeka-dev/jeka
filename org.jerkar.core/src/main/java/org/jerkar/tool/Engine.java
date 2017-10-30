package org.jerkar.tool;

import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependency;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkPublishRepo;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.file.JkPathMatcher;
import org.jerkar.api.file.JkPathSequence;
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

/**
 * Engine having responsibility ofMany compiling build classes, instantiate/configure build instances
 * and run them.<br/>
 * Build classes are expected to lie in [project base dir]/build/def <br/>
 * Classes having simple name starting with '_' are ignored.
 */
final class Engine {

    //private final JkPathFilter BUILD_SOURCE_FILTER = JkPathFilter.include("**/*.java").andExclude("**/_*");

    private final JkPathMatcher BUILD_SOURCE_MATCHER = JkPathMatcher.accept("**.java").andRefuse("**/_*", "_*");

    private final Path projectBaseDir;

    private JkDependencies buildDependencies;

    private JkRepos buildRepos;

    private List<Path>  rootsOfImportedBuilds = new LinkedList<>();

    private final BuildResolver resolver;

    /**
     * Constructs an engine for specified base directory .
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
        JkLog.startln("Compiling build classes for project " + this.projectBaseDir.getFileName().toString());
        JkLog.startln("Resolving compilation classpath");
        final JkDependencyResolver buildClassDependencyResolver = getBuildDefDependencyResolver();
        final JkPathSequence buildPath = buildClassDependencyResolver.get(this.buildDefDependencies());
        path.addAll(buildPath.entries());
        path.addAll(compileDependentProjects(yetCompiledProjects, path).entries());
        JkLog.done();
        this.compileBuild(JkPathSequence.ofMany(path));
        path.add(this.resolver.buildClassDir);
        JkLog.done();
    }

    <T extends JkBuild> T getBuild(Class<T> baseClass) {
        if (resolver.needCompile()) {
            this.compile();
        }
        return resolver.resolve(baseClass);
    }

    /**
     * Pre-compile and compile build classes (if needed) then execute the build
     * ofMany this project.
     */
    void execute(JkInit init) {
        this.buildDependencies = this.buildDependencies.andScopeless(init.commandLine().dependencies());
        JkLog.startHeaded("Compiling and instantiating build class");
        JkPathSequence runtimeClasspath = compile();
        if (!init.commandLine().dependencies().isEmpty()) {
            JkLog.startln("Grab dependencies specified in command line");
            final JkPathSequence cmdPath = pathOf(init.commandLine().dependencies());
            runtimeClasspath = runtimeClasspath.andManyFirst(cmdPath);
            if (JkLog.verbose()) {
                JkLog.done("Command line extra path : " + cmdPath);
            } else {
                JkLog.done();
            }
        }
        JkLog.info("Instantiating and configuring build class");
        final BuildAndPluginDictionnary buildAndDict = getBuildInstance(init, runtimeClasspath);
        if (buildAndDict == null) {
            throw new JkException("Can't find or guess any build class for project hosted in " + this.projectBaseDir
                    + " .\nAre you sure this directory is a buildable project ?");
        }
        JkLog.done();
        try {
            this.launch(buildAndDict.build, buildAndDict.dictionnary, init.commandLine());
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private JkPathSequence pathOf(List<? extends JkDependency> dependencies) {
        final JkDependencies deps = JkDependencies.of(dependencies);
        return JkDependencyResolver.of(this.buildRepos).get(deps);
    }

    JkBuild instantiate(JkInit init) {
        final JkPathSequence runtimePath = compile();
        JkLog.nextLine();
        final BuildAndPluginDictionnary buildAndDict = getBuildInstance(init, runtimePath);
        if (buildAndDict == null) {
            return null;
        }
        return buildAndDict.build;
    }

    private BuildAndPluginDictionnary getBuildInstance(JkInit init, JkPathSequence runtimePath) {
        final JkClassLoader classLoader = JkClassLoader.current();
        classLoader.addEntries(runtimePath);
        JkLog.trace("Setting build execution classpath to : " + classLoader.childClasspath());
        final JkBuild build = resolver.resolve(init.buildClassHint());
        if (build == null) {
            return null;
        }
        try {
            build.setBuildDefDependencyResolver(this.buildDefDependencies(), getBuildDefDependencyResolver());
            final PluginDictionnary dictionnary = init.initProject(build);
            final BuildAndPluginDictionnary result = new BuildAndPluginDictionnary();
            result.build = build;
            result.dictionnary = dictionnary;
            return result;
        } catch (final RuntimeException e) {
            JkLog.error("Engine " + projectBaseDir + " failed");
            throw e;
        }
    }

    private static class BuildAndPluginDictionnary {
        JkBuild build;
        PluginDictionnary dictionnary;
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
        final Path localDeflibDir = this.projectBaseDir.resolve(JkConstants.BUILD_BOOT);
        if (Files.exists(localDeflibDir)) {
            extraLibs.addAll(JkPathTree.of(localDeflibDir).accept("**.jar").files());
        }
        return JkPathSequence.ofMany(extraLibs).withoutDuplicates();
    }

    private static JkPathSequence jerkarLibs() {
        final List<Path>  extraLibs = new LinkedList<>();
        extraLibs.add(JkLocator.jerkarJarPath());
        return JkPathSequence.ofMany(extraLibs).withoutDuplicates();
    }

    private JkPathSequence compileDependentProjects(Set<Path> yetCompiledProjects, LinkedHashSet<Path>  pathEntries) {
        JkPathSequence pathSequence = JkPathSequence.of();
        if (!this.rootsOfImportedBuilds.isEmpty()) {
            JkLog.info("Compile build classes ofMany dependent projects : "
                    + toRelativePaths(this.projectBaseDir, this.rootsOfImportedBuilds));
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

    private void launch(JkBuild build, PluginDictionnary dictionnary, CommandLine commandLine) {

        // Now run projects
        if (!commandLine.getSubProjectMethods().isEmpty()) {
            for (final JkBuild subBuild : build.importedBuilds().all()) {
                runProject(subBuild, commandLine.getSubProjectMethods(), dictionnary);
            }
        }
        runProject(build, commandLine.getMasterMethods(), dictionnary);
    }

    private static void runProject(JkBuild build, List<MethodInvocation> invokes,
            PluginDictionnary dictionnary) {
        JkLog.infoHeaded("Executing build for project " + build.baseTree().root().getFileName().toString());
        JkLog.info("Build class : " + build.getClass().getName());
        JkLog.info("Base dir : " + build.baseDir());
        final Map<String, String> displayedOptions = JkOptions.toDisplayedMap(OptionInjector.injectedFields(build));
        if (JkLog.verbose()) {
            JkInit.logProps("Field values", displayedOptions);
        }
        execute(build, toBuildMethods(invokes, dictionnary), null);
    }

    /**
     * Executes the specified methods given the fromDir as working directory.
     */
    private static void execute(JkBuild build, Iterable<BuildMethod> methods, Path fromDir) {
        for (final BuildMethod method : methods) {
            invoke(build, method, fromDir);
        }
    }

    private static void invoke(JkBuild build, BuildMethod modelMethod, Path fromDir) {
        if (modelMethod.isMethodPlugin()) {
            final JkPlugin plugin = build.plugins().get(modelMethod.pluginClass());
            build.plugins().invoke(plugin, modelMethod.name());
        } else {
            invoke(build, modelMethod.name(), fromDir);
        }
    }

    /**
     * Invokes the specified method in this build.
     */
    private static void invoke(JkBuild build, String methodName, Path fromDir) {
        final Method method;
        try {
            method = build.getClass().getMethod(methodName);
        } catch (final NoSuchMethodException e) {
            JkLog.warn("No zero-arg method '" + methodName + "' found in class '" + build.getClass()
            + "'. Skip.");
            JkLog.warnStream().flush();
            return;
        }
        final String context;
        if (fromDir != null) {
            final String path = fromDir.relativize(build.baseTree().root()).toString().replace(
                    FileSystems.getDefault().getSeparator(), "/");
            context = " to project " + path + ", class " + build.getClass().getName();
        } else {
            context = "";
        }
        JkLog.infoUnderlined("Method : " + methodName + context);
        final long time = System.nanoTime();
        try {
            JkUtilsReflect.invoke(build, method);
            JkLog.info("Method " + methodName + " success in "
                    + JkUtilsTime.durationInSeconds(time) + " seconds.");
        } catch (final RuntimeException e) {
            JkLog.info("Method " + methodName + " failed in " + JkUtilsTime.durationInSeconds(time)
            + " seconds.");
            throw e;
        }
    }

    private static List<BuildMethod> toBuildMethods(Iterable<MethodInvocation> invocations,
            PluginDictionnary dictionnary) {
        final List<BuildMethod> buildMethods = new LinkedList<>();
        for (final MethodInvocation methodInvokation : invocations) {
            if (methodInvokation.isMethodPlugin()) {
                final Class<? extends JkPlugin> clazz = dictionnary.loadByNameOrFail(methodInvokation.pluginName)
                        .pluginClass();
                buildMethods.add(BuildMethod.pluginMethod(clazz, methodInvokation.methodName));
            } else {
                buildMethods.add(BuildMethod.normal(methodInvokation.methodName));
            }
        }
        return buildMethods;
    }

    private JkJavaCompileSpec buildCompileSpec() {
        final JkPathTree buildSource = JkPathTree.of(resolver.buildSourceDir).andMatcher(BUILD_SOURCE_MATCHER);
        JkUtilsPath.createDirectories(resolver.buildClassDir);
        return new JkJavaCompileSpec().setOutputDir(resolver.buildClassDir)
                .addSources(buildSource.files());
    }

    private JkDependencyResolver getBuildDefDependencyResolver() {
        final JkDependencies deps = this.buildDefDependencies();
        if (deps.containsModules()) {
            return JkDependencyResolver.of(this.buildRepos);
        }
        return JkDependencyResolver.of();
    }

    @Override
    public String toString() {
        return this.projectBaseDir.getFileName().toString();
    }

    private static JkRepos repos() {
        return JkRepo
                .firstNonNull(JkRepoOptions.repoFromOptions("build"),
                        JkRepoOptions.repoFromOptions("download"), JkRepo.mavenCentral())
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

}
