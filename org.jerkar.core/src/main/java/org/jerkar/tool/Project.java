package org.jerkar.tool;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependencyResolver;
import org.jerkar.api.depmanagement.JkRepo;
import org.jerkar.api.depmanagement.JkRepos;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkPath;
import org.jerkar.api.java.JkClassLoader;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.system.JkLocator;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.CommandLine.MethodInvocation;

/**
 * Buildable project. This class has the responsability to compile the build
 * classes along to run them.<br/>
 * Build class are expected to lie in [project base dir]/build/spec.<br/>
 * Classes having simple name starting by '_' are not compiled.
 */
final class Project {

    private final File projectBaseDir;

    private JkDependencies buildDependencies;

    private JkRepos buildRepos;

    private List<File> subProjects = new LinkedList<File>();

    private JkPath buildPath;

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
		JkFileTree.of(resolver.buildSourceDir).include("**/*.java"));
	this.buildDependencies = this.buildDependencies.and(parser.dependencies());
	this.buildRepos = parser.importRepos().and(buildRepos);
	this.subProjects = parser.projects();
    }

    private void compile() {
	final LinkedHashSet<File> entries = new LinkedHashSet<File>();
	compile(new HashSet<File>(), entries);
	this.buildPath = JkPath.of(entries);
    }

    private void compile(Set<File> yetCompiledProjects, LinkedHashSet<File> path) {
	if (!this.resolver.hasBuildSource() || yetCompiledProjects.contains(this.projectBaseDir)) {
	    return;
	}
	yetCompiledProjects.add(this.projectBaseDir);
	preCompile();
	JkLog.startHeaded("Making build classes for project " + this.projectBaseDir.getName());
	final JkDependencyResolver buildClassDependencyResolver = getBuildDefDependencyResolver();
	final JkPath buildPath = buildClassDependencyResolver.get();
	path.addAll(buildPath.entries());
	path.addAll(compileDependentProjects(yetCompiledProjects, path).entries());
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
	this.buildDependencies = this.buildDependencies.andScopeless(init.commandLine().dependencies());
	compile();
	JkLog.nextLine();
	final JkClassLoader classLoader = JkClassLoader.current();
	classLoader.addEntries(this.buildPath);
	JkLog.info("Setting build execution classpath to : " + classLoader.childClasspath());
	final JkBuild build = resolver.resolve(init.buildClassHint());
	if (build == null) {
	    throw new JkException("Can't find or guess any build class for project hosted in " + this.projectBaseDir
		    + " .\nAre you sure this directory is a buildable project ?");
	}
	try {
	    this.launch(build, init);
	} catch (final RuntimeException e) {
	    JkLog.error("Project " + projectBaseDir.getAbsolutePath() + " failed");
	    throw e;
	}
    }

    private JkDependencies buildDefDependencies() {

	// If true, we assume Jerkar is produced by IDE (development mode)
	final boolean devMode = JkLocator.jerkarJarFile().isDirectory();

	return JkDependencies.builder().on(buildDependencies).onFiles(localBuildPath())
		.onFilesIf(devMode, JkClasspath.current()).onFilesIf(!devMode, jerkarLibs()).build();
    }

    private JkPath localBuildPath() {
	final List<File> extraLibs = new LinkedList<File>();
	final File localDeflibDir = new File(this.projectBaseDir, JkConstants.BUILD_LIB_DIR);
	if (localDeflibDir.exists()) {
	    extraLibs.addAll(JkFileTree.of(localDeflibDir).include("**/*.jar").files(false));
	}
	return JkPath.of(extraLibs).withoutDoubloons();
    }

    private static JkPath jerkarLibs() {
	final List<File> extraLibs = new LinkedList<File>();
	if (JkLocator.libExtDir().exists()) {
	    extraLibs.addAll(JkFileTree.of(JkLocator.libExtDir()).include("**/*.jar").files(false));
	}
	extraLibs.add(JkLocator.jerkarJarFile());
	return JkPath.of(extraLibs).withoutDoubloons();
    }

    private JkPath compileDependentProjects(Set<File> yetCompiledProjects, LinkedHashSet<File> pathEntries) {
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
	JkFileTree.of(this.resolver.buildSourceDir).exclude("**/*.java").copyTo(this.resolver.buildClassDir);
    }

    private void launch(JkBuild build, JkInit init) {
	build.setBuildDefDependencyResolver(getBuildDefDependencyResolver());
	final PluginDictionnary<JkBuildPlugin> dictionnary = init.initProject(build);
	final CommandLine commandLine = init.commandLine();

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
	final Map<String, String> displayedOptions = JkOptions.toDisplayedMap(OptionInjector.injectedFields(build));
	JkInit.logProps("Field values", displayedOptions);
	build.execute(toBuildMethods(invokes, dictionnary), null);
    }

    private static List<JkModelMethod> toBuildMethods(Iterable<MethodInvocation> invocations,
	    PluginDictionnary<JkBuildPlugin> dictionnary) {
	final List<JkModelMethod> jkModelMethods = new LinkedList<JkModelMethod>();
	for (final MethodInvocation methodInvokation : invocations) {
	    if (methodInvokation.isMethodPlugin()) {
		final Class<? extends JkBuildPlugin> clazz = dictionnary.loadByNameOrFail(methodInvokation.pluginName)
			.pluginClass();
		jkModelMethods.add(JkModelMethod.pluginMethod(clazz, methodInvokation.methodName));
	    } else {
		jkModelMethods.add(JkModelMethod.normal(methodInvokation.methodName));
	    }
	}
	return jkModelMethods;
    }

    private JkJavaCompiler baseBuildCompiler() {
	final JkFileTree buildSource = JkFileTree.of(resolver.buildSourceDir).include("**/*.java").exclude("**/_*");
	if (!resolver.buildClassDir.exists()) {
	    resolver.buildClassDir.mkdirs();
	}
	return JkJavaCompiler.ofOutput(resolver.buildClassDir).andSources(buildSource).failOnError(true);
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
		.andIfEmpty(JkBuildDependencySupport.reposOfOptions("download")).andIfEmpty(JkRepo.mavenCentral());
    }

}
