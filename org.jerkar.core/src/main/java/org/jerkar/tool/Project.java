package org.jerkar.tool;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependency;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.CommandLine.MethodInvocation;

/**
 * Buildable project. This class has the responsibility to compile the build
 * classes and to run them.<br/>
 * Build classes are expected to lie in [project base dir]/build/def<br/>
 * Classes having simple name starting by '_' are ignored.
 */
final class Project {

    private final JkPathFilter BUILD_SOURCE_FILTER = JkPathFilter.include("**/*.java").andExclude("**/_*");

    private final File projectBaseDir;

    private JkDependencies buildDependencies;

    private JkRepos buildRepos;

    private List<File> subProjects = new LinkedList<File>();

    private final BuildResolver resolver;

    /**
     * Constructs a project from its base directory and the download repository.
     * Download repository is used in case the build classes need some
     * dependencies in order to be compiled/run.
     */
    public Project(File baseDir) {
        super();
        this.projectBaseDir = JkUtilsFile.canonicalFile(baseDir);
        buildRepos = repos();
        this.buildDependencies = JkDependencies.of();
        this.resolver = new BuildResolver(baseDir);
    }

    private void preCompile() {
        final JavaSourceParser parser = JavaSourceParser.of(this.projectBaseDir,
                JkFileTree.of(resolver.buildSourceDir).andFilter(BUILD_SOURCE_FILTER));
        this.buildDependencies = this.buildDependencies.and(parser.dependencies());
        this.buildRepos = parser.importRepos().and(buildRepos);
        this.subProjects = parser.projects();
    }

    // Compiles and returns the runtime classpath
    private JkPath compile() {
        final LinkedHashSet<File> entries = new LinkedHashSet<File>();
        compile(new HashSet<File>(), entries);
        return JkPath.of(entries).withoutDoubloons();
    }

    private void compile(Set<File> yetCompiledProjects, LinkedHashSet<File> path) {
        if (!this.resolver.hasBuildSource() || yetCompiledProjects.contains(this.projectBaseDir)) {
            return;
        }
        yetCompiledProjects.add(this.projectBaseDir);
        preCompile(); // This enrich dependencies
        JkLog.startHeaded("Compiling build classes for project " + this.projectBaseDir.getName());
        JkLog.startln("Resolving compilation classpath");
        final JkDependencyResolver buildClassDependencyResolver = getBuildDefDependencyResolver();
        final JkPath buildPath = buildClassDependencyResolver.get();
        path.addAll(buildPath.entries());
        path.addAll(compileDependentProjects(yetCompiledProjects, path).entries());
        JkLog.done();
        this.compileBuild(JkPath.of(path));
        path.add(this.resolver.buildClassDir);
        JkLog.done();
    }

    public <T extends JkBuild> T getBuild(Class<T> baseClass) {
        if (resolver.needCompile()) {
            this.compile();
        }
        return resolver.resolve(baseClass);
    }

    public List<Class<?>> getBuildClasses() {
        if (resolver.needCompile()) {
            this.compile();
        }
        return resolver.resolveBuildClasses();
    }

    /**
     * Pre-compile and compile build classes (if needed) then execute the build
     * of this project.
     * 
     * @param buildClassNameHint
     *            The full or simple class name of the build class to execute.
     *            It can be <code>null</code> or empty.
     */
    public void execute(JkInit init) {
        this.buildDependencies = this.buildDependencies.andScopeless(init.commandLine()
                .dependencies());
        JkPath runtimeClasspath = compile();
        JkLog.startHeaded("Instantiating build class");
        if (!init.commandLine().dependencies().isEmpty()) {
            JkLog.startln("Grab dependencies specified in command line");
            final JkPath cmdPath = pathOf(init.commandLine().dependencies());
            runtimeClasspath = runtimeClasspath.andHead( cmdPath );
            JkLog.done("Command line extra path : " + cmdPath);
        }
        final BuildAndPluginDictionnary buidAndDict = getBuildInstance(init, runtimeClasspath);
        if (buidAndDict == null) {
            throw new JkException("Can't find or guess any build class for project hosted in "
                    + this.projectBaseDir
                    + " .\nAre you sure this directory is a buildable project ?");
        }
        JkLog.done();
        try {
            this.launch(buidAndDict.build, buidAndDict.dictionnary, init.commandLine());
        } catch (final RuntimeException e) {
            JkLog.error("Project " + projectBaseDir.getAbsolutePath() + " failed");
            throw e;
        }
    }

    private JkPath pathOf(List<? extends JkDependency> dependencies) {
        final JkDependencies deps = JkDependencies.of(dependencies);
        return JkDependencyResolver.managed(this.buildRepos, deps).get();
    }

    public JkBuild instantiate(JkInit init) {
        final JkPath runtimePath = compile();
        JkLog.nextLine();
        final BuildAndPluginDictionnary buildAndDict = getBuildInstance(init, runtimePath);
        if (buildAndDict == null) {
            return null;
        }
        return buildAndDict.build;
    }

    private BuildAndPluginDictionnary getBuildInstance(JkInit init, JkPath runtimePath) {
        final JkClassLoader classLoader = JkClassLoader.current();
        classLoader.addEntries(runtimePath);
        JkLog.info("Setting build execution classpath to : " + classLoader.childClasspath());
        final JkBuild build = resolver.resolve(init.buildClassHint());
        if (build == null) {
            return null;
        }
        try {
            build.setBuildDefDependencyResolver(getBuildDefDependencyResolver());
            final PluginDictionnary<JkBuildPlugin> dictionnary = init.initProject(build);
            final BuildAndPluginDictionnary result = new BuildAndPluginDictionnary();
            result.build = build;
            result.dictionnary = dictionnary;
            return result;
        } catch (final RuntimeException e) {
            JkLog.error("Project " + projectBaseDir.getAbsolutePath() + " failed");
            throw e;
        }
    }

    private static class BuildAndPluginDictionnary {
        JkBuild build;
        PluginDictionnary<JkBuildPlugin> dictionnary;
    }

    private JkDependencies buildDefDependencies() {

        // If true, we assume Jerkar is produced by IDE (development mode)
        final boolean devMode = JkLocator.jerkarJarFile().isDirectory();

        return JkDependencies.builder()
                .on(buildDependencies.withDefaultScopeMapping(JkScopeMapping.ALL_TO_DEFAULT))
                .onFiles(localBuildPath())
                .onFilesIf(devMode, JkClasspath.current())
                .onFilesIf(!devMode, jerkarLibs())
                .build();
    }

    private JkPath localBuildPath() {
        final List<File> extraLibs = new LinkedList<File>();
        final File localDeflibDir = new File(this.projectBaseDir, JkConstants.BUILD_BOOT);
        if (localDeflibDir.exists()) {
            extraLibs.addAll(JkFileTree.of(localDeflibDir).include("**/*.jar").files(false));
        }
        return JkPath.of(extraLibs).withoutDoubloons();
    }

    private static JkPath jerkarLibs() {
        final List<File> extraLibs = new LinkedList<File>();
        extraLibs.add(JkLocator.jerkarJarFile());
        return JkPath.of(extraLibs).withoutDoubloons();
    }

    private JkPath compileDependentProjects(Set<File> yetCompiledProjects,
            LinkedHashSet<File> pathEntries) {
        JkPath jkPath = JkPath.of();
        for (final File file : this.subProjects) {
            final Project project = new Project(file);
            project.compile(yetCompiledProjects, pathEntries);
            jkPath = jkPath.and(file);
        }
        return jkPath;
    }

    private void compileBuild(JkPath buildPath) {
        baseBuildCompiler().withClasspath(buildPath).compile();
        JkFileTree.of(this.resolver.buildSourceDir).exclude("**/*.java")
        .copyTo(this.resolver.buildClassDir);
    }

    private void launch(JkBuild build, PluginDictionnary<JkBuildPlugin> dictionnary, CommandLine commandLine) {


        // Now run projects
        if (!commandLine.getSubProjectMethods().isEmpty()) {
            for (final JkBuild subBuild : build.slaves().all()) {
                runProject(subBuild, commandLine.getSubProjectMethods(), dictionnary);
            }
        }
        runProject(build, commandLine.getMasterMethods(), dictionnary);
    }

    private static void runProject(JkBuild build, List<MethodInvocation> invokes,
            PluginDictionnary<JkBuildPlugin> dictionnary) {
        JkLog.infoHeaded("Executing build for project " + build.baseDir().root().getName());
        JkLog.info("Build class " + build.getClass().getName());
        JkLog.info("Activated plugins : " + build.plugins.getActives());
        final Map<String, String> displayedOptions = JkOptions.toDisplayedMap(OptionInjector
                .injectedFields(build));
        JkInit.logProps("Field values", displayedOptions);
        build.execute(toBuildMethods(invokes, dictionnary), null);
    }

    private static List<JkModelMethod> toBuildMethods(Iterable<MethodInvocation> invocations,
            PluginDictionnary<JkBuildPlugin> dictionnary) {
        final List<JkModelMethod> jkModelMethods = new LinkedList<JkModelMethod>();
        for (final MethodInvocation methodInvokation : invocations) {
            if (methodInvokation.isMethodPlugin()) {
                final Class<? extends JkBuildPlugin> clazz = dictionnary.loadByNameOrFail(
                        methodInvokation.pluginName).pluginClass();
                jkModelMethods.add(JkModelMethod.pluginMethod(clazz, methodInvokation.methodName));
            } else {
                jkModelMethods.add(JkModelMethod.normal(methodInvokation.methodName));
            }
        }
        return jkModelMethods;
    }

    private JkJavaCompiler baseBuildCompiler() {
        final JkFileTree buildSource = JkFileTree.of(resolver.buildSourceDir).andFilter(BUILD_SOURCE_FILTER);
        if (!resolver.buildClassDir.exists()) {
            resolver.buildClassDir.mkdirs();
        }
        return JkJavaCompiler.ofOutput(resolver.buildClassDir).andSources(buildSource)
                .failOnError(true);
    }

    private JkDependencyResolver getBuildDefDependencyResolver() {
        final JkDependencies deps = this.buildDefDependencies();
        if (deps.containsModules()) {
            return JkDependencyResolver.managed(this.buildRepos, deps);
        }
        return JkDependencyResolver.unmanaged(deps);
    }

    @Override
    public String toString() {
        return this.projectBaseDir.getName();
    }

    private static JkRepos repos() {
        return JkBuildDependencySupport.reposOfOptions("build")
                .andIfEmpty(JkBuildDependencySupport.reposOfOptions("download"))
                .andIfEmpty(JkRepo.mavenCentral());
    }

}
