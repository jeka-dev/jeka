package org.jake;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jake.CommandLine.MethodInvocation;
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

	public File compileBuild(Iterable<File> classpath) {
		final JakeDir buildSource = JakeDir.of(new File(projectBaseDir,
				BUILD_SOURCE_DIR));
		if (!buildSource.exists()) {
			JakeLog.info("No source build found at " + BUILD_SOURCE_DIR +".");
			return null;
		}
		final File buildBinDir = new File(projectBaseDir, BUILD_BIN_DIR);
		if (!buildBinDir.exists()) {
			buildBinDir.mkdirs();
		}
		displayHead("Compiling build classes for project : " + projectRelativePath);
		final long start = System.nanoTime();
		JakeJavaCompiler.ofOutput(buildBinDir)
		.andSources(buildSource)
		.withClasspath(classpath)
		.failOnError(true)
		.compile();
		JakeLog.info("Done in " + JakeUtilsTime.durationInSeconds(start) + " seconds.", "");
		return buildBinDir;
	}

	public boolean executeBuild(File projectFolder, JakeClassLoader classLoader, Iterable<MethodInvocation> methods) {
		final long start = System.nanoTime();
		displayHead("Building project : " + projectRelativePath);
		final Class<? extends JakeBuild> buildClass = this.findBuildClass(classLoader);
		final boolean result = this.launch(projectFolder, buildClass, methods, classLoader);

		final float duration = JakeUtilsTime.durationInSeconds(start);
		if (result) {
			JakeLog.info("--> Project " + projectBaseDir.getAbsolutePath() + " built with success in " + duration + " seconds.");
		} else {
			JakeLog.error("--> Project " + projectBaseDir.getAbsolutePath() + " failed after " + duration + " seconds.");
		}
		return result;
	}

	private boolean hasBuildSource() {
		final File buildSource = new File(projectBaseDir, BUILD_SOURCE_DIR);
		if (!buildSource.exists()) {
			return false;
		}
		return true;
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

	private boolean launch(File projectFolder, Class<? extends JakeBuild> buildClass, Iterable<MethodInvocation> methods, JakeClassLoader classLoader) {

		final JakeBuild build = JakeUtilsReflect.newInstance(buildClass);
		JakeOptions.populateFields(build);
		build.setBaseDir(projectFolder);


		JakeLog.info("Use build class '" + buildClass.getCanonicalName()
				+ "' with methods : "
				+ JakeUtilsString.toString(methods, ", ") + ".");
		JakeLog.info("Using classpath : " + classLoader.fullClasspath());
		if (JakeOptions.hasFieldOptions(buildClass)) {
			JakeLog.info("With options : " + JakeOptions.fieldOptionsToString(build));
		}
		JakeLog.nextLine();

		for (final MethodInvocation methodInvokation : methods) {
			if (methodInvokation.isMethodPlugin()) {
				continue;
			}

			final Method method;
			final String actionIntro = "Method : " + methodInvokation.toString();
			JakeLog.info(actionIntro);
			JakeLog.info(JakeUtilsString.repeat("-", actionIntro.length()));
			try {
				method = build.getClass().getMethod(methodInvokation.methodName);
			} catch (final NoSuchMethodException e) {
				JakeLog.warn("No zero-arg method '" + methodInvokation.methodName
						+ "' found in class '" + buildClass.getCanonicalName() + "'. Skip.");
				continue;
			}
			if (!Void.TYPE.equals(method.getReturnType())) {
				JakeLog.warn("A zero-arg method '" + methodInvokation
						+ "' found in class '" + buildClass.getCanonicalName() + "' but was not returning a void result. Skip.");
				continue;
			}
			final long start = System.nanoTime();
			boolean success = false;
			try {
				method.invoke(build);
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


}
