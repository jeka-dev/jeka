package org.jake;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.CommandLine.JakePluginSetup;
import org.jake.CommandLine.MethodInvocation;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.ivy.JakeIvy;
import org.jake.utils.JakeUtilsFile;

/**
 * Buildable project. This class has the responsability to compile the build classes along to run them.<br/>
 * Build class are expected to lie in [project base dir]/build/spec.<br/>
 * Classes having simple name starting by '_' are not compiled.
 */
class Project {

	static final JakeScope JAKE_SCOPE = JakeScope.of("jake");

	private final File projectBaseDir;

	private JakeDependencies buildDependencies;

	private JakeRepos buildRepos;

	private List<File> subProjects = new LinkedList<File>();

	private JakePath buildPath;

	private final JakeBuildResolver resolver;


	/**
	 * Constructs a project from its base directory and the download repo.
	 * Download repo is used in case the build classes need some dependencies
	 * in order to be compiled/run.
	 */
	public Project(File baseDir) {
		super();
		this.projectBaseDir = JakeUtilsFile.canonicalFile(baseDir);
		buildRepos = repos();
		this.buildDependencies = JakeDependencies.on();
		this.resolver = new JakeBuildResolver(baseDir);
	}

	private void preCompile() {
		final JavaSourceParser parser = JavaSourceParser.of(this.projectBaseDir, JakeDir.of(resolver.buildSourceDir).include("**/*.java"));
		this.buildDependencies = parser.dependencies();
		this.buildRepos = parser.importRepos().and(buildRepos);
		this.subProjects = parser.projects();
	}

	private void compile() {
		final LinkedHashSet<File> entries = new LinkedHashSet<File>();
		compile(new HashSet<File>(), entries);
		this.buildPath = JakePath.of(entries);
	}

	private void compile(Set<File> yetCompiledProjects, LinkedHashSet<File> path) {
		if (!this.resolver.hasBuildSource() || yetCompiledProjects.contains(this.projectBaseDir)) {
			return;
		}
		yetCompiledProjects.add(this.projectBaseDir);
		preCompile();
		JakeLog.startHeaded("Making build classes for project " + this.projectBaseDir.getName());
		path.addAll(resolveBuildPath().entries());
		path.addAll(compileDependentProjects(yetCompiledProjects, path).entries());
		path.addAll(localBuildPath().entries());
		this.compileBuild(JakePath.of(path));
		path.add(this.resolver.buildClassDir);
		JakeLog.done();
	}

	public JakeBuild getBuild() {
		if (resolver.needCompile()) {
			this.compile();
		}
		return resolver.resolve();
	}

	public <T extends JakeBuild> T getBuild(Class<T> baseClass) {
		if (resolver.needCompile()) {
			this.compile();
		}
		return resolver.resolve(baseClass);
	}

	/**
	 * Pre-compile and compile build classes (if needed) then execute the build of this project.
	 * 
	 * @param buildClassNameHint The full or simple class name of the build class to execute. It can be <code>null</code>
	 * or empty.
	 */
	public void execute(CommandLine commandLine, String buildClassNameHint) {
		compile();
		JakeLog.nextLine();
		final JakeClassLoader classLoader = JakeClassLoader.current();
		classLoader.addEntries(this.buildPath);
		JakeLog.info("Setting build execution classpath to : " + classLoader.childClasspath());
		final JakeBuild build = resolver.resolve(buildClassNameHint);
		if (build == null) {
			throw new JakeException("Can't find or guess any build class for project hosted in " +  this.projectBaseDir
					+ " .\nAre you sure this directory is a buildable project ?");
		}
		try {
			this.launch(build, commandLine);
		} catch(final RuntimeException e) {
			JakeLog.error("Project " + projectBaseDir.getAbsolutePath() + " failed");
			throw e;
		}
	}

	private	JakePath localBuildPath() {
		final List<File> extraLibs = new LinkedList<File>();
		final File localJakeBuild = new File(this.projectBaseDir,"build/libs/jake");
		if (localJakeBuild.exists()) {
			extraLibs.addAll(JakeDir.of(localJakeBuild).include("**/*.jar").files());
		}
		if (JakeLocator.libExtDir().exists()) {
			extraLibs.addAll(JakeDir.of(JakeLocator.libExtDir()).include("**/*.jar").files());
		}
		return JakePath.of(extraLibs).and(JakeLocator.jakeJarFile(), JakeLocator.ivyJarFile());
	}

	private JakePath resolveBuildPath() {
		final JakePath buildPath;
		if (buildDependencies.isEmpty()) {
			buildPath = JakePath.of();
		} else {
			JakeLog.startln("Resolving build dependencies");
			final JakeDependencies importedDependencies =  buildDependencies;
			JakePath extraPath  = JakePath.of(importedDependencies.fileDependencies(JAKE_SCOPE));
			if (importedDependencies.containsExternalModule()) {
				extraPath = extraPath.and(this.jakeCompilePath(buildRepos, importedDependencies));
			}
			buildPath = extraPath;
			JakeLog.done();
		}
		return buildPath;
	}

	private JakePath compileDependentProjects(Set<File> yetCompiledProjects, LinkedHashSet<File> pathEntries) {
		final JakePath jakePath = JakePath.of();
		for (final File file : this.subProjects) {
			final Project project = new Project(file);
			project.compile(yetCompiledProjects, pathEntries);
		}
		return jakePath;
	}

	private void compileBuild(JakePath buildPath) {
		baseBuildCompiler().withClasspath(buildPath).compile();
		JakeDir.of(this.resolver.buildSourceDir).exclude("**/*.java").copyTo(this.resolver.buildClassDir);
	}

	private void launch(JakeBuild build, CommandLine commandLine) {

		JakeOptions.populateFields(build, commandLine.getMasterBuildOptions());
		build.init();

		// setup plugins
		final Class<JakeBuildPlugin> baseClass = JakeClassLoader.of(build.getClass()).load(JakeBuildPlugin.class.getName());
		final PluginDictionnary<JakeBuildPlugin> dictionnary = PluginDictionnary.of(baseClass);

		if (!commandLine.getSubProjectMethods().isEmpty()
				&& !build.buildDependencies().transitiveBuilds().isEmpty()) {
			JakeLog.startHeaded("Executing dependent projects");
			for (final JakeBuild subBuild : build.buildDependencies().transitiveBuilds()) {
				configurePluginsAndRun(subBuild, commandLine.getSubProjectMethods(),
						commandLine.getSubProjectPluginSetups(), commandLine.getSubProjectBuildOptions(), dictionnary);

			}
			JakeLog.done();
		}
		configurePluginsAndRun(build, commandLine.getMasterMethods(),
				commandLine.getMasterPluginSetups(), commandLine.getMasterBuildOptions(), dictionnary);
	}

	private static void configurePluginsAndRun(JakeBuild build, List<MethodInvocation> invokes,
			Collection<JakePluginSetup> pluginSetups, Map<String, String> options,  PluginDictionnary<JakeBuildPlugin> dictionnary) {
		JakeLog.startHeaded("Executing building for project " + build.baseDir().root().getName());
		JakeLog.info("Using build class " + build.getClass().getName());
		JakeLog.info("With activated plugins : " + build.plugins.getActives());
		JakeOptions.populateFields(build, options);
		configureAndActivatePlugins(build, pluginSetups, dictionnary);
		JakeLog.info("Build options : " + JakeOptions.fieldOptionsToString(build));
		build.execute(toBuildMethods(invokes, dictionnary));
		JakeLog.done("Build " + build.baseDir().root().getName());
	}

	private static void configureAndActivatePlugins(JakeBuild build, Collection<JakePluginSetup> pluginSetups, PluginDictionnary<JakeBuildPlugin> dictionnary) {
		for (final JakePluginSetup pluginSetup : pluginSetups) {
			final Class<? extends JakeBuildPlugin> pluginClass =
					dictionnary.loadByNameOrFail(pluginSetup.pluginName).pluginClass();
			if (pluginSetup.activated) {
				JakeLog.startln("Activating plugin " + pluginClass.getName());
				final Object plugin = build.plugins.addActivated(pluginClass, pluginSetup.options);
				JakeLog.done("Activating plugin " + pluginClass.getName() + " with options "
						+ JakeOptions.fieldOptionsToString(plugin));
			} else {
				JakeLog.startln("Configuring plugin " + pluginClass.getName());
				final Object plugin = build.plugins.addConfigured(pluginClass, pluginSetup.options);
				JakeLog.done("Configuring plugin " + pluginClass.getName() + " with options "
						+ JakeOptions.fieldOptionsToString(plugin));
			}
		}
	}

	private static List<BuildMethod> toBuildMethods(Iterable<MethodInvocation> invocations, PluginDictionnary<JakeBuildPlugin> dictionnary) {
		final List<BuildMethod> buildMethods = new LinkedList<BuildMethod>();
		for (final MethodInvocation methodInvokation : invocations) {
			if (methodInvokation.isMethodPlugin()) {
				final Class<? extends JakeBuildPlugin> clazz = dictionnary.loadByNameOrFail(methodInvokation.pluginName).pluginClass();
				buildMethods.add(BuildMethod.pluginMethod(clazz, methodInvokation.methodName));
			} else {
				buildMethods.add(BuildMethod.normal(methodInvokation.methodName));
			}
		}
		return buildMethods;
	}

	private JakeJavaCompiler baseBuildCompiler() {
		final JakeDir buildSource = JakeDir.of(resolver.buildSourceDir).include("**/*.java").exclude("**/_*");
		if (!resolver.buildClassDir.exists()) {
			resolver.buildClassDir.mkdirs();
		}
		return JakeJavaCompiler.ofOutput(resolver.buildClassDir)
				.andSources(buildSource)
				.failOnError(true);
	}


	private JakePath jakeCompilePath(JakeRepos jakeRepos, JakeDependencies deps) {
		final JakeIvy ivy = JakeIvy.of(jakeRepos);
		final Set<JakeArtifact> artifacts = ivy.resolve(deps, JAKE_SCOPE);
		return JakePath.of(JakeArtifact.localFiles(artifacts));
	}

	@Override
	public String toString() {
		return this.projectBaseDir.getName();
	}

	private static JakeRepos repos() {
		final JakeBuild build = new JakeBuild(); // Create a fake build just to get the download repos.
		JakeOptions.populateFields(build);
		return build.downloadRepositories();
	}

}
