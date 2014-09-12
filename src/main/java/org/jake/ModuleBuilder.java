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
import org.jake.java.eclipse.JakeBuildEclipseProject;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;
import org.jake.utils.JakeUtilsTime;

/**
 * Builder for a single module. The injected class loader is supposed to include yet the build binaries.
 */
class ModuleBuilder {


	private static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String BUILD_LIB_DIR = "build/libs/build";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	private final File moduleBaseDir;

	private final File parentBaseDir;

	private final JakeClassLoader classLoader;


	/**
	 * Create a module builder where is specified the base directory of the module along
	 * its relative path to the 'root' module.
	 * The Relative path is used to display the module under build
	 */
	public ModuleBuilder(File buildBaseParentDir, File moduleBaseDir,JakeClassLoader classLoader) {
		super();
		this.moduleBaseDir = moduleBaseDir;
		this.parentBaseDir = buildBaseParentDir;
		this.classLoader = classLoader;
	}

	public boolean build(Iterable<String> methods) {
		final long start = System.nanoTime();

		final String pattern = "-";
		final String intro = "Building module : " + JakeUtilsFile.getRelativePath(parentBaseDir, moduleBaseDir);
		JakeLog.info(JakeUtilsString.repeat(pattern, intro.length() ));
		JakeLog.info(intro);
		JakeLog.info(JakeUtilsString.repeat(pattern, intro.length() ));
		JakeLog.nextLine();
		final Class<? extends JakeBuildBase> buildClass = this.findBuildClass();
		final boolean result = this.launch(buildClass, methods);

		final float duration = JakeUtilsTime.durationInSeconds(start);
		if (result) {
			JakeLog.info("--> Module " + moduleBaseDir.getAbsolutePath() + " processed with success in " + duration + " seconds.");
		} else {
			JakeLog.info("--> Module " + moduleBaseDir.getAbsolutePath() + " failed after " + duration + " seconds.");
		}
		return result;
	}

	private boolean hasBuildSource() {
		final File buildSource = new File(moduleBaseDir, BUILD_SOURCE_DIR);
		if (!buildSource.exists()) {
			return false;
		}
		return true;
	}



	@SuppressWarnings("unchecked")
	private Class<? extends JakeBuildBase> findBuildClass() {

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
			final JakeDir dir = JakeDir.of(new File(moduleBaseDir, BUILD_SOURCE_DIR));
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
		if (new File(moduleBaseDir, DEFAULT_JAVA_SOURCE).exists()
				&& new File(moduleBaseDir, BUILD_LIB_DIR ).exists()) {
			return classLoader.load(JakeBuildJar.class.getName());
		}
		if (JakeBuildEclipseProject.candidate(moduleBaseDir)) {
			return classLoader.load(JakeBuildEclipseProject.class.getName());
		}
		return null;
	}

	private boolean launch(Class<? extends JakeBuildBase> buildClass, Iterable<String> methods) {

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


}
