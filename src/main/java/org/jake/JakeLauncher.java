package org.jake;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jake.file.JakeDirView;
import org.jake.file.utils.FileUtils;
import org.jake.java.JakeJarBuild;
import org.jake.java.JakeJavaCompiler;
import org.jake.java.utils.JakeUtilsClassloader;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;

public class JakeLauncher {

	private static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String BUILD_LIB_DIR = "build/libs/build";

	private static final String BUILD_BIN_DIR = "build/output/build-bin";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	public static void main(String[] args) {
		final JakeLauncher launcher = new JakeLauncher();
		final Iterable<File> compileClasspath = launcher
				.resolveBuildCompileClasspath();
		final JakeDirView buildSourceDir = JakeDirView.of(new File(BUILD_SOURCE_DIR));

		if (launcher.hasBuildSource(buildSourceDir)) {
			launcher.compileBuild(compileClasspath);
		}
		final boolean result = launcher.launch(compileClasspath);
		if (result) {
			JakeLogger.info("Build success.");
		} else {
			JakeLogger.info("Build failed.");
			System.exit(1);
		}

	}

	private boolean hasBuildSource(JakeDirView sourceDir) {
		final JakeDirView buildSource = JakeDirView.of(new File(BUILD_SOURCE_DIR));
		if (!sourceDir.exists()) {
			JakeLogger.info(buildSource.root().getAbsolutePath()
					+ " directory not exists.");
			return false;
		}
		return true;
	}

	private void compileBuild(Iterable<File> classpath) {
		final JakeDirView buildSource = JakeDirView.of(new File(BUILD_SOURCE_DIR));
		if (!buildSource.exists()) {
			throw new IllegalStateException(
					BUILD_SOURCE_DIR
					+ " directory has not been found in this project. "
					+ " This directory is supposed to contains build scripts (as form of java source");
		}
		final JakeJavaCompiler javaCompilation = new JakeJavaCompiler();
		javaCompilation.addSourceFiles(buildSource.include("**/*.java"));
		javaCompilation.setClasspath(classpath);
		final File buildBinDir = new File(BUILD_BIN_DIR);
		if (!buildBinDir.exists()) {
			buildBinDir.mkdirs();
		}
		javaCompilation.setOutputDirectory(buildBinDir);
		JakeLogger.start("Compiling build sources to "
				+ buildBinDir.getAbsolutePath());
		final boolean result = javaCompilation.compile();
		JakeLogger.done();
		if (result == false) {
			JakeLogger.error("Build script can't be compiled.");
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean launch(Iterable<File> compileClasspath) {
		final File buildBin = new File(BUILD_BIN_DIR);
		final Iterable<File> runtimeClassPath = JakeUtilsIterable.concatToList(
				buildBin, compileClasspath);
		final URLClassLoader classLoader = JakeUtilsClassloader.createFrom(
				runtimeClassPath, JakeLauncher.class.getClassLoader());

		// Find the Build class
		final List<String> buildClassNames = getBuildClassNames(JakeUtilsIterable
				.single(buildBin));
		final String buildClassName;
		if (!buildClassNames.isEmpty()) {
			buildClassName = buildClassNames.get(0);
		} else {
			buildClassName = defaultBuildClassName();
			if (buildClassName == null) {
				throw new IllegalStateException(
						"No build class found for this project and cannot find default build class that fits.");
			}
		}
		final Class buildClass = JakeUtilsClassloader.loadClass(classLoader,
				buildClassName);

		final Object build = JakeUtilsReflect.newInstance(buildClass);
		final String command = "doDefault";
		JakeLogger.info("Use Build class " + buildClass.getCanonicalName()
				+ " with method " + command);

		try {
			final Method method = build.getClass().getMethod(command);
			method.invoke(build);
			return true;
		} catch (final SecurityException e) {
			throw new RuntimeException(e);
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException("No zero-arg method '" + command
					+ "' found in class '" + buildClassName);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (final InvocationTargetException e) {
			final Throwable target = e.getTargetException();
			if (target instanceof JakeBuildException) {
				JakeLogger.error(target.getMessage());
				return false;
			} else if (target instanceof RuntimeException) {
				throw (RuntimeException) target;
			} else {
				throw new RuntimeException(target);
			}

		}

	}

	private List<File> resolveBuildCompileClasspath() {
		final URL[] urls = JakeUtilsClassloader.current().getURLs();
		final List<File> result = FileUtils.toFiles(urls);
		final File buildLibDir = new File(BUILD_LIB_DIR);
		if (buildLibDir.exists() && buildLibDir.isDirectory()) {
			final List<File> libs = FileUtils.filesOf(buildLibDir,
					FileUtils.endingBy(".jar"), true);
			for (final File file : libs) {
				result.add(file);
			}
		}
		final File jakeJarFile = JakeLocator.getJakeJarFile();
		final File extLibDir = new File(jakeJarFile.getParentFile(), "ext");
		if (extLibDir.exists() && extLibDir.isDirectory()) {
			result.addAll(FileUtils.filesOf(extLibDir,
					FileUtils.endingBy(".jar"), false));
		}
		return result;
	}



	private static List<String> getBuildClassNames(Iterable<File> classpath) {

		final URLClassLoader classLoader = JakeUtilsClassloader.createFrom(classpath,
				JakeLauncher.class.getClassLoader());

		// Find the Build class
		final Class<?> jakeBaseBuildClass = JakeUtilsClassloader.loadClass(classLoader,
				JakeBaseBuild.class.getName());
		final Set<Class> classes = JakeUtilsClassloader.getAllTopLevelClasses(
				classLoader, FileUtils.acceptAll(), true);
		final List<String> buildClasses = new LinkedList<String>();
		for (final Class clazz : classes) {
			final boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());
			if (!isAbstract && jakeBaseBuildClass.isAssignableFrom(clazz)) {
				buildClasses.add(clazz.getName());
			}
		}
		return buildClasses;
	}

	private static String defaultBuildClassName() {
		if (!new File(DEFAULT_JAVA_SOURCE).exists()) {
			return null;
		}
		return JakeJarBuild.class.getName();

	}

}
