package org.jake;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.CommandLine.JakePluginSetup;
import org.jake.CommandLine.MethodInvocation;
import org.jake.PluginDictionnary.JakePluginDescription;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.ivy.JakeIvy;
import org.jake.java.build.JakeJavaBuild;
import org.jake.java.eclipse.JakeEclipseBuild;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;
import org.jake.utils.JakeUtilsTime;

/**
 * Single buildable project.
 */
class Project {

	static final JakeScope JAKE_SCOPE = JakeScope.of("jake");

	private static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String BUILD_BIN_DIR = "build/output/build-bin";

	private static final String BUILD_LIB_DIR = "build/libs/build";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	private final File projectBaseDir;

	private final String projectRelativePath;

	private JakeClasspath classpathForExecution;

	/**
	 * Constructs a module builder where is specified the base directory of the module along
	 * its relative path to the 'root' module.
	 * The Relative path is used to display the module under build
	 */
	public Project(File buildBaseParentDir, File moduleBaseDir) {
		super();
		this.projectBaseDir = moduleBaseDir;
		if (buildBaseParentDir.equals(moduleBaseDir)) {
			projectRelativePath = moduleBaseDir.getName();
		} else {
			projectRelativePath = JakeUtilsFile.getRelativePath(buildBaseParentDir, moduleBaseDir);
		}
	}

	public void compileBuildScriptIfNeeded(JakeRepos repos) {
		if (!this.hasBuildSource()) {
			this.classpathForExecution = JakeClasspath.of();
			return;
		}
		final JakePath extraPath = resolveBuildPath(repos);
		final File dir = compileBuild(extraPath.and(localBuildPath()));
		this.classpathForExecution = JakeClasspath.of(extraPath.and(dir));

	}

	/**
	 * @param buildClassNameHint The full or simple class name of the build class to execute. It can be <code>null</code>
	 * or empty.
	 */
	public boolean executeBuild(
			Iterable<MethodInvocation> methods, Iterable<CommandLine.JakePluginSetup> setups, String buildClassNameHint) {
		final long start = System.nanoTime();
		JakeLog.displayHead("Building project : " + projectRelativePath);
		final JakeClassLoader classLoader;
		if (hasBuildSource() && this.classpathForExecution == null) {
			throw new IllegalStateException("You need to compile build source prior executing the build.");
		}
		if (!hasBuildSource() || this.classpathForExecution.isEmpty()) {
			classLoader = JakeClassLoader.current();
		} else {
			classLoader = JakeClassLoader.current().createChild(this.classpathForExecution);
		}
		final Class<? extends JakeBuild> buildClass = this.findBuildClass(classLoader, buildClassNameHint);
		final boolean result = this.launch(buildClass, methods, setups, classLoader);
		final float duration = JakeUtilsTime.durationInSeconds(start);
		if (result) {
			JakeLog.info("--> Project " + projectBaseDir.getAbsolutePath() + " built with success in " + duration + " seconds.");
		} else {
			JakeLog.error("--> Project " + projectBaseDir.getAbsolutePath() + " failed after " + duration + " seconds.");
		}
		return result;
	}

	public boolean hasBuildSource() {
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

	private JakePath resolveBuildPath(JakeRepos repos) {
		JakeLog.displayHead("Compiling build classes for project : " + projectRelativePath);
		JakeLog.start("Parsing source code for gathering imports");
		final JavaSourceParser parser = JavaSourceParser.of(this.projectBaseDir,
				JakeDir.of(buildSourceDir()).include("**/*.java"));
		JakeLog.done();

		final JakePath buildPath;
		if (parser.dependencies().isEmpty()) {
			buildPath = JakePath.of();
		} else {
			JakeLog.startln("Resolving build dependencies");
			final JakeDependencies importedDependencies =  parser.dependencies();
			final JakePath extraPath;
			if (importedDependencies.containsExternalModule()) {
				extraPath = this.jakeCompilePath(repos, importedDependencies);
			} else {
				extraPath = JakePath.of(importedDependencies.fileDependencies(JAKE_SCOPE));
			}
			buildPath = extraPath;
			JakeLog.done();
		}
		return buildPath;
	}

	private File compileBuild(JakePath buildPath) {
		baseBuildCompiler().withClasspath(buildPath).compile();
		JakeDir.of(this.buildSourceDir()).exclude("**/*.java").copyTo(this.buildBinDir());
		return buildBinDir();
	}

	private JakeRepos jakeCompileRepos(List<String> importRepoUrls, JakeRepo repo) {
		final JakeRepos result = importRepos(importRepoUrls);
		if (repo != null) {
			return result.and(repo);
		}
		return result;

	}

	private JakeRepos importRepos(List<String> importRepoUrls) {
		JakeRepos result = JakeRepos.of();
		for (final String url : importRepoUrls) {
			result = result.and(JakeRepo.of(url));
		}
		return result;
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

	private boolean launch(Class<? extends JakeBuild> buildClass,
			Iterable<MethodInvocation> methods, Iterable<JakePluginSetup> setups, JakeClassLoader classLoader) {

		final JakeBuild build = JakeUtilsReflect.newInstance(buildClass);
		JakeOptions.populateFields(build);

		JakeLog.info("Use build class '" + buildClass.getCanonicalName()
				+ "' with methods : "
				+ JakeUtilsString.toString(methods, ", ") + ".");
		JakeLog.info("Using classpath : " + classLoader.fullClasspath());
		if (JakeOptions.hasFieldOptions(buildClass)) {
			JakeLog.info("With options : " + JakeOptions.fieldOptionsToString(build));
		}

		build.setBaseDir(this.projectBaseDir);
		final Map<String, Object> pluginMap = instantiatePlugins(build.pluginTemplateClasses(), setups);
		final List<Object> plugins = new LinkedList<Object>(pluginMap.values());
		build.setPlugins(plugins);
		for (final Object plugin : plugins) {
			JakeLog.info("Using plugin " + plugin.getClass().getName() + " with options " + JakeOptions.fieldOptionsToString(plugin));
		}
		JakeLog.nextLine();

		for (final MethodInvocation methodInvokation : methods) {

			final Method method;
			final String actionIntro = "Method : " + methodInvokation.toString();
			JakeLog.info(actionIntro);
			JakeLog.info(JakeUtilsString.repeat("-", actionIntro.length()));
			Class<?> targetClass = null;
			try {
				if (methodInvokation.isMethodPlugin()) {
					targetClass = pluginMap.get(methodInvokation.pluginName).getClass();
				} else {
					targetClass = buildClass;
				}
				method = targetClass.getMethod(methodInvokation.methodName);
			} catch (final NoSuchMethodException e) {
				JakeLog.warn("No zero-arg method '" + methodInvokation.methodName
						+ "' found in class '" + targetClass  + "'. Skip.");
				continue;
			}
			if (!Void.TYPE.equals(method.getReturnType())) {
				JakeLog.warn("A zero-arg method '" + methodInvokation
						+ "' found in class '" + targetClass + "' but was not returning a void result. Skip.");
				continue;
			}
			final long start = System.nanoTime();
			boolean success = false;
			try {
				if (methodInvokation.isMethodPlugin()) {
					method.invoke(pluginMap.get(methodInvokation.pluginName));
				} else {
					method.invoke(build);
				}
				success = true;
			} catch (final SecurityException e) {
				throw new RuntimeException(e);
			} catch (final IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (final InvocationTargetException e) {
				final Throwable target = e.getTargetException();
				if (target instanceof JakeException) {
					JakeLog.error(target.getMessage());
					return false;
				} else if (target instanceof RuntimeException) {
					throw (RuntimeException) target;
				} else {
					throw new RuntimeException(target);
				}
			} finally {
				JakeLog.nextLine();
				if (success) {
					JakeLog.info("-> Method " + methodInvokation + " executed in " + JakeUtilsTime.durationInSeconds(start) + " seconds.");
				} else {
					JakeLog.error("-> Method " + methodInvokation + " failed in " + JakeUtilsTime.durationInSeconds(start) + " seconds.");
				}
			}
			JakeLog.nextLine();
		}
		return true;
	}

	private static Map<String, Object> instantiatePlugins(Iterable<Class<Object>> templateClasses,
			Iterable<JakePluginSetup> setups) {
		final Map<String, Object> result = new LinkedHashMap<String, Object>();
		final Set<String> names = JakePluginSetup.names(setups);
		final Set<String> unmatchingNames = new HashSet<String>(names);
		for (final Class<Object> templateClass : templateClasses) {
			final PluginDictionnary<Object> plugins = PluginDictionnary.of(templateClass);
			final Map<String, JakePluginDescription<Object>> pluginDescriptions = plugins.loadAllByNames(names);
			unmatchingNames.removeAll(pluginDescriptions.keySet());
			for (final String name : pluginDescriptions.keySet()) {
				final JakePluginDescription<Object> desc = pluginDescriptions.get(name);
				final Object plugin = JakeUtilsReflect.newInstance(desc.pluginClass());
				final JakePluginSetup setup = JakePluginSetup.findOrFail(name, setups);
				JakeOptions.populateFields(plugin, setup.options);
				result.put(name, plugin);
			}
		}
		if (!unmatchingNames.isEmpty()) {
			JakeLog.warn("No plugins found with name : " + unmatchingNames);
		}
		return result;
	}

	private JakeJavaCompiler baseBuildCompiler() {
		final JakeDir buildSource = JakeDir.of(buildSourceDir());
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


}
