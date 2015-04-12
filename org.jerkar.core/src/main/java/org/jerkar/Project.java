package org.jerkar;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jerkar.CommandLine.JakePluginSetup;
import org.jerkar.CommandLine.MethodInvocation;
import org.jerkar.depmanagement.JkArtifact;
import org.jerkar.depmanagement.JkDependencies;
import org.jerkar.depmanagement.JkRepos;
import org.jerkar.depmanagement.JkScope;
import org.jerkar.depmanagement.ivy.JkIvy;
import org.jerkar.utils.JkUtilsFile;

/**
 * Buildable project. This class has the responsability to compile the build classes along to run them.<br/>
 * Build class are expected to lie in [project base dir]/build/spec.<br/>
 * Classes having simple name starting by '_' are not compiled.
 */
class Project {

	static final JkScope BUILD_SCOPE = JkScope.of("build");

	private final File projectBaseDir;

	private JkDependencies buildDependencies;

	private JkRepos buildRepos;

	private List<File> subProjects = new LinkedList<File>();

	private JkPath buildPath;

	private final JkBuildResolver resolver;


	/**
	 * Constructs a project from its base directory and the download repo.
	 * Download repo is used in case the build classes need some dependencies
	 * in order to be compiled/run.
	 */
	public Project(File baseDir) {
		super();
		this.projectBaseDir = JkUtilsFile.canonicalFile(baseDir);
		buildRepos = repos();
		this.buildDependencies = JkDependencies.on();
		this.resolver = new JkBuildResolver(baseDir);
	}

	private void preCompile() {
		final JavaSourceParser parser = JavaSourceParser.of(this.projectBaseDir, JkDir.of(resolver.buildSourceDir).include("**/*.java"));
		this.buildDependencies = parser.dependencies();
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
		path.addAll(resolveBuildPath().entries());
		path.addAll(compileDependentProjects(yetCompiledProjects, path).entries());
		path.addAll(localBuildPath().entries());
		this.compileBuild(JkPath.of(path));
		path.add(this.resolver.buildClassDir);
		JkLog.done();
	}

	public JkBuild getBuild() {
		if (resolver.needCompile()) {
			this.compile();
		}
		return resolver.resolve();
	}

	public <T extends JkBuild> T getBuild(Class<T> baseClass) {
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
		JkLog.nextLine();
		final JkClassLoader classLoader = JkClassLoader.current();
		classLoader.addEntries(this.buildPath);
		JkLog.info("Setting build execution classpath to : " + classLoader.childClasspath());
		final JkBuild build = resolver.resolve(buildClassNameHint);
		if (build == null) {
			throw new JkException("Can't find or guess any build class for project hosted in " +  this.projectBaseDir
					+ " .\nAre you sure this directory is a buildable project ?");
		}
		try {
			this.launch(build, commandLine);
		} catch(final RuntimeException e) {
			JkLog.error("Project " + projectBaseDir.getAbsolutePath() + " failed");
			throw e;
		}
	}

	private	JkPath localBuildPath() {
		final List<File> extraLibs = new LinkedList<File>();
		final File localJakeBuild = new File(this.projectBaseDir,"build/libs/jake");
		if (localJakeBuild.exists()) {
			extraLibs.addAll(JkDir.of(localJakeBuild).include("**/*.jar").files());
		}
		if (JkLocator.libExtDir().exists()) {
			extraLibs.addAll(JkDir.of(JkLocator.libExtDir()).include("**/*.jar").files());
		}
		return JkPath.of(extraLibs).and(JkLocator.jakeJarFile(), JkLocator.ivyJarFile());
	}

	private JkPath resolveBuildPath() {
		final JkPath buildPath;
		if (buildDependencies.isEmpty()) {
			buildPath = JkPath.of();
		} else {
			JkLog.startln("Resolving build dependencies");
			final JkDependencies importedDependencies =  buildDependencies;
			JkPath extraPath  = JkPath.of(importedDependencies.fileDependencies(BUILD_SCOPE));
			if (importedDependencies.containsExternalModule()) {
				extraPath = extraPath.and(this.jakeCompilePath(buildRepos, importedDependencies));
			}
			buildPath = extraPath;
			JkLog.done();
		}
		return buildPath;
	}

	private JkPath compileDependentProjects(Set<File> yetCompiledProjects, LinkedHashSet<File> pathEntries) {
		final JkPath jkPath = JkPath.of();
		for (final File file : this.subProjects) {
			final Project project = new Project(file);
			project.compile(yetCompiledProjects, pathEntries);
		}
		return jkPath;
	}

	private void compileBuild(JkPath buildPath) {
		baseBuildCompiler().withClasspath(buildPath).compile();
		JkDir.of(this.resolver.buildSourceDir).exclude("**/*.java").copyTo(this.resolver.buildClassDir);
	}

	private void launch(JkBuild build, CommandLine commandLine) {

		JkOptions.populateFields(build, commandLine.getMasterBuildOptions());
		build.init();

		// setup plugins
		final Class<JkBuildPlugin> baseClass = JkClassLoader.of(build.getClass()).load(JkBuildPlugin.class.getName());
		final PluginDictionnary<JkBuildPlugin> dictionnary = PluginDictionnary.of(baseClass);

		if (!commandLine.getSubProjectMethods().isEmpty()
				&& !build.buildDependencies().transitiveBuilds().isEmpty()) {
			JkLog.startHeaded("Executing dependent projects");
			for (final JkBuild subBuild : build.buildDependencies().transitiveBuilds()) {
				configurePluginsAndRun(subBuild, commandLine.getSubProjectMethods(),
						commandLine.getSubProjectPluginSetups(), commandLine.getSubProjectBuildOptions(), dictionnary);

			}
			JkLog.done();
		}
		configurePluginsAndRun(build, commandLine.getMasterMethods(),
				commandLine.getMasterPluginSetups(), commandLine.getMasterBuildOptions(), dictionnary);
	}

	private static void configurePluginsAndRun(JkBuild build, List<MethodInvocation> invokes,
			Collection<JakePluginSetup> pluginSetups, Map<String, String> options,  PluginDictionnary<JkBuildPlugin> dictionnary) {
		JkLog.startHeaded("Executing building for project " + build.baseDir().root().getName());
		JkLog.info("Using build class " + build.getClass().getName());
		JkLog.info("With activated plugins : " + build.plugins.getActives());
		JkOptions.populateFields(build, options);
		configureAndActivatePlugins(build, pluginSetups, dictionnary);
		JkLog.info("Build options : " + JkOptions.fieldOptionsToString(build));
		build.execute(toBuildMethods(invokes, dictionnary));
		JkLog.done("Build " + build.baseDir().root().getName());
	}

	private static void configureAndActivatePlugins(JkBuild build, Collection<JakePluginSetup> pluginSetups, PluginDictionnary<JkBuildPlugin> dictionnary) {
		for (final JakePluginSetup pluginSetup : pluginSetups) {
			final Class<? extends JkBuildPlugin> pluginClass =
					dictionnary.loadByNameOrFail(pluginSetup.pluginName).pluginClass();
			if (pluginSetup.activated) {
				JkLog.startln("Activating plugin " + pluginClass.getName());
				final Object plugin = build.plugins.addActivated(pluginClass, pluginSetup.options);
				JkLog.done("Activating plugin " + pluginClass.getName() + " with options "
						+ JkOptions.fieldOptionsToString(plugin));
			} else {
				JkLog.startln("Configuring plugin " + pluginClass.getName());
				final Object plugin = build.plugins.addConfigured(pluginClass, pluginSetup.options);
				JkLog.done("Configuring plugin " + pluginClass.getName() + " with options "
						+ JkOptions.fieldOptionsToString(plugin));
			}
		}
	}

	private static List<BuildMethod> toBuildMethods(Iterable<MethodInvocation> invocations, PluginDictionnary<JkBuildPlugin> dictionnary) {
		final List<BuildMethod> buildMethods = new LinkedList<BuildMethod>();
		for (final MethodInvocation methodInvokation : invocations) {
			if (methodInvokation.isMethodPlugin()) {
				final Class<? extends JkBuildPlugin> clazz = dictionnary.loadByNameOrFail(methodInvokation.pluginName).pluginClass();
				buildMethods.add(BuildMethod.pluginMethod(clazz, methodInvokation.methodName));
			} else {
				buildMethods.add(BuildMethod.normal(methodInvokation.methodName));
			}
		}
		return buildMethods;
	}

	private JkJavaCompiler baseBuildCompiler() {
		final JkDir buildSource = JkDir.of(resolver.buildSourceDir).include("**/*.java").exclude("**/_*");
		if (!resolver.buildClassDir.exists()) {
			resolver.buildClassDir.mkdirs();
		}
		return JkJavaCompiler.ofOutput(resolver.buildClassDir)
				.andSources(buildSource)
				.failOnError(true);
	}


	private JkPath jakeCompilePath(JkRepos jakeRepos, JkDependencies deps) {
		final JkIvy ivy = JkIvy.of(jakeRepos);
		final Set<JkArtifact> artifacts = ivy.resolve(deps, BUILD_SCOPE);
		return JkPath.of(JkArtifact.localFiles(artifacts));
	}

	@Override
	public String toString() {
		return this.projectBaseDir.getName();
	}

	private static JkRepos repos() {
		final JkBuild build = new JkBuild(); // Create a fake build just to get the download repos.
		JkOptions.populateFields(build);
		return build.downloadRepositories();
	}

}
