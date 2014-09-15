package org.jake;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jake.file.JakeDir;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJar;
import org.jake.java.JakeClassLoader;
import org.jake.java.JakeClasspath;
import org.jake.java.JakeJavaCompiler;
import org.jake.java.eclipse.JakeBuildEclipseProject;
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
			throw new IllegalStateException(
					BUILD_SOURCE_DIR
					+ " directory has not been found in this project. "
					+ " This directory is supposed to contains build scripts (as form of java source");
		}
		final File buildBinDir = new File(projectBaseDir, BUILD_BIN_DIR);
		if (!buildBinDir.exists()) {
			buildBinDir.mkdirs();
		}
		displayHead("Compiling build classes for project : " + projectRelativePath);
		final long start = System.nanoTime();
		JakeJavaCompiler.ofOutput(buildBinDir)
		.addSourceFiles(buildSource)
		.setClasspath(classpath)
		.compileOrFail();
		JakeLog.info("Done in " + JakeUtilsTime.durationInSeconds(start) + " seconds.", "");
		return buildBinDir;
	}

	public boolean executeBuild( JakeClassLoader classLoader, Iterable<String> methods) {
		final long start = System.nanoTime();
		displayHead("Building project : " + projectRelativePath);
		final Class<? extends JakeBuildBase> buildClass = this.findBuildClass(classLoader);
		final boolean result = this.launch(buildClass, methods, classLoader);

		final float duration = JakeUtilsTime.durationInSeconds(start);
		if (result) {
			JakeLog.info("--> Project " + projectBaseDir.getAbsolutePath() + " built with success in " + duration + " seconds.");
		} else {
			JakeLog.info("--> Project " + projectBaseDir.getAbsolutePath() + " failed after " + duration + " seconds.");
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
	private Class<? extends JakeBuildBase> findBuildClass(JakeClassLoader classLoader) {

		// If class name specified in options.
		if (!JakeUtilsString.isBlank(JakeOptions.buildClass())) {
			final Class<? extends JakeBuildBase> clazz = classLoader.loadFromNameOrSimpleName(JakeOptions.buildClass(), JakeBuildBase.class);
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
					final String className = JakeClasspath.javaSourcePathToClassName(path);
					final Class<?> clazz = classLoader.load(className);
					if (JakeBuildBase.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
						return (Class<? extends JakeBuildBase>) clazz;
					}
				}

			}
		}

		// If nothing yet found use defaults
		if (new File(projectBaseDir, DEFAULT_JAVA_SOURCE).exists()
				&& new File(projectBaseDir, BUILD_LIB_DIR ).exists()) {
			return classLoader.load(JakeBuildJar.class.getName());
		}
		if (JakeBuildEclipseProject.candidate(projectBaseDir)) {
			return classLoader.load(JakeBuildEclipseProject.class.getName());
		}
		return null;
	}

	private boolean launch(Class<? extends JakeBuildBase> buildClass, Iterable<String> methods, JakeClassLoader classLoader) {

		final JakeBuildBase build = JakeUtilsReflect.newInstance(buildClass);

		JakeLog.info("Use build class '" + buildClass.getCanonicalName()
				+ "' with methods : "
				+ JakeUtilsString.toString(methods, ", ") + ".");
		JakeLog.info("Using classpath : " + classLoader.fullClasspath());
		if (JakeOptions.hasFieldOptions(buildClass)) {
			JakeLog.info("With options : " + JakeOptions.fieldOptionsToString(build));
		}
		JakeLog.nextLine();

		for (final String methodName : methods) {
			final Method method;
			final String actionIntro = "Method : " + methodName;
			JakeLog.info(actionIntro);
			JakeLog.info(JakeUtilsString.repeat("-", actionIntro.length()));
			try {
				method = build.getClass().getMethod(methodName);
			} catch (final NoSuchMethodException e) {
				JakeLog.warn("No zero-arg method '" + methodName
						+ "' found in class '" + buildClass.getCanonicalName() + "'. Skip.");
				continue;
			}
			final long start = System.nanoTime();
			try {
				method.invoke(build);
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
				JakeLog.info("-> Method " + methodName + " executed in " + JakeUtilsTime.durationInSeconds(start) + " seconds.");
			}
			JakeLog.flush();
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
