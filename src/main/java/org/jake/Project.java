package org.jake;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jake.CommandLine.MethodInvocation;
import org.jake.JakeImportAnnotationProcessor.ImportResult;
import org.jake.JakePlugins.JakePluginSetup;
import org.jake.depmanagement.JakeArtifact;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeDependency;
import org.jake.depmanagement.JakeRepo;
import org.jake.depmanagement.JakeRepos;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.depmanagement.JakeScopedDependency;
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

	private static final JakeScope JAKE_SCOPE = JakeScope.of("jake");

	private static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String BUILD_BIN_DIR = "build/output/build-bin";

	private static final String BUILD_LIB_DIR = "build/libs/build";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	private final File projectBaseDir;

	private final String projectRelativePath;


	/**
	 * Create a module builder where is specified the base directory of the module along
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

	private	JakePath localBuildPath() {
		final List<File> extraLibs = new LinkedList<File>();
		final File localJakeBuild = new File(this.projectBaseDir,"build/libs/jake");
		if (localJakeBuild.exists()) {
			extraLibs.addAll(JakeDir.of(localJakeBuild).include("**/*.jar").files());
		}
		if (JakeLocator.libExtDir().exists()) {
			extraLibs.addAll(JakeDir.of(JakeLocator.libExtDir()).include("**/*.jar").files());
		}
		return JakePath.of(extraLibs).and(JakeLocator.jakeJarFile());
	}


	public File compileBuild(BootstrapOptions bootstrapOptions) {
		displayHead("Compiling build classes for project : " + projectRelativePath);
		final long start = System.nanoTime();

		final ImportResult importsResult = this.annotatedImports();
		final JakePath buildPath;
		if (importsResult.imports.isEmpty()) {
			buildPath = this.localBuildPath();
		} else {
			final JakeDependencies importedDependencies = this.importDependencies(importsResult.imports);
			if (importedDependencies.containsExternalModule()) {
				final JakeRepos repos = jakeCompileRepos(importsResult.repos, bootstrapOptions);
				buildPath = this.jakeCompilePath(repos, importedDependencies);
			} else {
				buildPath = JakePath.of(importedDependencies.fileDependencies(JAKE_SCOPE));
			}
		}

		baseBuildCompiler().withClasspath(buildPath).compile();
		JakeLog.info("Done in " + JakeUtilsTime.durationInSeconds(start) + " seconds.", "");
		return buildBinDir();
	}


	private JakeRepos jakeCompileRepos(List<String> importRepoUrls, BootstrapOptions bootstrapOptions) {
		final JakeRepos result = importRepos(importRepoUrls);
		if (bootstrapOptions.downloadRepo() != null) {
			return result.and(bootstrapOptions.downloadRepo());
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



	public boolean executeBuild(File projectFolder, JakeClassLoader classLoader,
			Iterable<MethodInvocation> methods, Iterable<JakePluginSetup> setups) {
		final long start = System.nanoTime();
		displayHead("Building project : " + projectRelativePath);
		final Class<? extends JakeBuild> buildClass = this.findBuildClass(classLoader);
		final boolean result = this.launch(projectFolder, buildClass, methods, setups, classLoader);

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

	@SuppressWarnings("unchecked")
	private Class<? extends JakeBuild> findBuildClass(JakeClassLoader classLoader) {

		// If class name specified in options.
		if (!JakeUtilsString.isBlank(JakeOptions.buildClass())) {
			final Class<? extends JakeBuild> clazz = classLoader.loadFromNameOrSimpleName(JakeOptions.buildClass(), JakeBuild.class);
			if (clazz == null) {
				throw new JakeException("No build class named " + JakeOptions.buildClass() + " found.");
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

	private boolean launch(File projectFolder, Class<? extends JakeBuild> buildClass,
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

		build.setBaseDir(projectFolder);
		final Map<String, Object> pluginMap = JakePlugins.instantiatePlugins(build.pluginTemplateClasses(), setups);
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


	protected static void displayHead(String intro) {
		final String pattern = "-";
		JakeLog.info(JakeUtilsString.repeat(pattern, intro.length() ));
		JakeLog.info(intro);
		JakeLog.info(JakeUtilsString.repeat(pattern, intro.length() ));
		JakeLog.nextLine();
	}

	private ImportResult annotatedImports() {
		JakeLog.startln("Processing annotation on build sources to get imports information");
		baseBuildCompiler()
		.withClasspath(JakePath.of(JakeLocator.jakeJarFile()))
		.withAnnotationProcessingOnly()
		.withAnnotationProcessors(JakeImportAnnotationProcessor.class.getName())
		.withSourceVersion(JakeJavaCompiler.V6)
		.withTargetVersion(JakeJavaCompiler.V6)
		.withOptions("-nowarn")
		.compile();
		JakeLog.done();
		return JakeImportAnnotationProcessor.getResult(buildBinDir());
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

	private JakeDependencies importDependencies(List<String> dependencies) {
		final JakeDependencies.Builder builder = JakeDependencies.builder();
		for (final String dependency : dependencies) {
			if (dependency.contains(">")) {
				final String scope = JakeUtilsString.substringBeforeFirst(dependency, ">").trim();
				final String rest = JakeUtilsString.substringAfterLast(dependency, ">").trim();
				final JakeDependency dep = JakeDependency.of(rest);
				final JakeScopedDependency scopedDependency =
						JakeScopedDependency.of(dep, JakeScopeMapping.of(JAKE_SCOPE).to(JakeScope.of(scope)));
				builder.on(scopedDependency);
			}
		}
		return builder.build();
	}




}
