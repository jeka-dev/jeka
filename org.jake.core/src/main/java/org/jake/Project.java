package org.jake;

import java.io.File;
import java.lang.reflect.Modifier;
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
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.eclipse.JakeEclipseBuild;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

/**
 * Buildable project. This class has the responsability to compile the build classes along to run them.<br/>
 * Build class are expected to lie in [project base dir]/build/spec.<br/>
 * Classes having simple name starting by '_' are not compiled.
 */
class Project {

	static final JakeScope JAKE_SCOPE = JakeScope.of("jake");

	private static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String BUILD_BIN_DIR = "build/output/build-bin";

	private static final String BUILD_LIB_DIR = "build/libs/build";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	private final File projectBaseDir;

	private JakeDependencies buildDependencies;

	private final JakeRepos originalBuildRepos;

	private JakeRepos buildRepos;

	//private JakeClasspath buildClasspath = JakeClasspath.of();

	private List<File> subProjects = new LinkedList<File>();

	private JakePath buildPath;


	/**
	 * Constructs a module builder where is specified the base directory of the module along
	 * its relative path to the 'root' module.
	 * The Relative path is used to display the module under build
	 */
	public Project(File baseDir, JakeRepos downlodRepo) {
		super();
		this.projectBaseDir = JakeUtilsFile.canonicalFile(baseDir);
		buildRepos = downlodRepo;
		originalBuildRepos = downlodRepo;
		this.buildDependencies = JakeDependencies.on();
	}

	private void preCompile() {
		final JavaSourceParser parser = JavaSourceParser.of(this.projectBaseDir, JakeDir.of(buildSourceDir()).include("**/*.java"));
		this.buildDependencies = parser.dependencies();
		this.buildRepos = parser.importRepos().and(buildRepos);
		this.subProjects = parser.projects();
	}

	public void compile() {
		final LinkedHashSet<File> entries = new LinkedHashSet<File>();
		compile(new HashSet<File>(), entries);
		this.buildPath = JakePath.of(entries);

	}

	private void compile(Set<File> yetCompiledProjects, LinkedHashSet<File> path) {
		if (!this.hasBuildSource() || yetCompiledProjects.contains(this.projectBaseDir)) {
			return;
		}
		yetCompiledProjects.add(this.projectBaseDir);
		preCompile();
		JakeLog.startHeaded("Making build classes for project " + this.projectBaseDir.getName());
		path.addAll(resolveBuildPath().entries());
		path.addAll(compileDependentProjects(yetCompiledProjects, path).entries());
		path.addAll(localBuildPath().entries());
		this.compileBuild(JakePath.of(path));
		path.add(this.buildBinDir());
		JakeLog.done();
	}

	/**
	 * Precompile and compile build classes (if needed) then execute the build of this project.
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
		final Class<? extends JakeBuild> buildClass = this.findBuildClass(classLoader, buildClassNameHint);

		try {
			this.launch(buildClass, commandLine);
		} catch(final RuntimeException e) {
			JakeLog.error("Project " + projectBaseDir.getAbsolutePath() + " failed");
			throw e;
		}
	}

	private boolean hasBuildSource() {
		if (!this.buildSourceDir().exists()) {
			return false;
		}
		return JakeDir.of(buildSourceDir()).include("**/*.java").fileCount(false) > 0;
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
			final JakePath extraPath;
			if (importedDependencies.containsExternalModule()) {
				extraPath = this.jakeCompilePath(buildRepos, importedDependencies);
			} else {
				extraPath = JakePath.of(importedDependencies.fileDependencies(JAKE_SCOPE));
			}
			buildPath = extraPath;
			JakeLog.done();
		}
		return buildPath;
	}

	private JakePath compileDependentProjects(Set<File> yetCompiledProjects, LinkedHashSet<File> pathEntries) {
		final JakePath jakePath = JakePath.of();
		for (final File file : this.subProjects) {
			final Project project = new Project(file, originalBuildRepos);
			project.compile(yetCompiledProjects, pathEntries);
		}
		return jakePath;
	}

	private void compileBuild(JakePath buildPath) {
		baseBuildCompiler().withClasspath(buildPath).compile();
		JakeDir.of(this.buildSourceDir()).exclude("**/*.java").copyTo(this.buildBinDir());
	}


	@SuppressWarnings("unchecked")
	private Class<? extends JakeBuild> findBuildClass(JakeClassLoader classLoader, String classNameHint) {

		// If class name specified in options.
		if (!JakeUtilsString.isBlank(classNameHint)) {
			final Class<? extends JakeBuild> clazz = classLoader.loadFromNameOrSimpleName(classNameHint, JakeBuild.class);
			if (clazz == null) {
				throw new JakeException("No build class named " + classNameHint + " found.");
			}
			return clazz;
		}

		// If there is a build source
		if (this.hasBuildSource()) {
			final JakeDir dir = JakeDir.of(new File(projectBaseDir, BUILD_SOURCE_DIR));
			for (final String path : dir.relativePathes()) {
				if (path.endsWith(".java")) {
					final Class<?> clazz = classLoader.loadGivenClassSourcePath(path);
					if (JakeBuild.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
						return (Class<? extends JakeBuild>) clazz;
					}
				}

			}
		}

		// If nothing yet found use defaults
		if (new File(projectBaseDir, DEFAULT_JAVA_SOURCE).exists()
				&& new File(projectBaseDir, BUILD_LIB_DIR ).exists()) {
			return classLoader.load(JakeJavaBuild.class.getName());
		}
		if (JakeEclipseBuild.candidate(projectBaseDir)) {
			return classLoader.load(JakeEclipseBuild.class.getName());
		}
		return null;
	}

	private void launch(Class<? extends JakeBuild> buildClass, 	CommandLine commandLine) {

		final JakeBuild build = JakeUtilsReflect.newInstance(buildClass);
		JakeOptions.populateFields(build, commandLine.getMasterBuildOptions());
		build.init();

		// setup plugins
		final Class<JakeBuildPlugin> baseClass = JakeClassLoader.of(buildClass).load(JakeBuildPlugin.class.getName());
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
		final JakeDir buildSource = JakeDir.of(buildSourceDir()).include("**/*.java").exclude("**/_*");
		if (!buildBinDir().exists()) {
			buildBinDir().mkdirs();
		}
		return JakeJavaCompiler.ofOutput(buildBinDir())
				.andSources(buildSource)
				.failOnError(true);
	}

	private File buildSourceDir() {
		return new File(projectBaseDir, BUILD_SOURCE_DIR);
	}

	private File buildBinDir() {
		return new File(projectBaseDir, BUILD_BIN_DIR);
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

}
