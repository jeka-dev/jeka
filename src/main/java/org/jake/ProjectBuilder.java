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
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.JakeBuildJar;
import org.jake.java.JakeJavaCompiler;
import org.jake.java.utils.JakeUtilsClassloader;
import org.jake.utils.JakeUtilsIterable;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

class ProjectBuilder {

	private static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String BUILD_LIB_DIR = "build/libs/build";

	private static final String BUILD_BIN_DIR = "build/output/build-bin";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	private final File root;

	public ProjectBuilder(File root) {
		super();
		this.root = root;
	}

	public boolean build(Iterable<String> actions) {
		final Iterable<File> compileClasspath = this
				.resolveBuildCompileClasspath();

		if (this.hasBuildSource()) {
			this.compileBuild(compileClasspath);
		}
		final boolean result = this.launch(compileClasspath, actions);
		if (result) {
			JakeLog.info("Build success.");
		} else {
			JakeLog.info("Build failed.");
		}
		return result;

	}

	private boolean hasBuildSource() {
		final File buildSource = new File(root, BUILD_SOURCE_DIR);
		if (!buildSource.exists()) {
			JakeLog.info(buildSource.getAbsolutePath()
					+ " directory not exists.");
			return false;
		}
		return true;
	}

	private void compileBuild(Iterable<File> classpath) {
		final JakeDirView buildSource = JakeDirView.of(new File(root,
				BUILD_SOURCE_DIR));
		if (!buildSource.exists()) {
			throw new IllegalStateException(
					BUILD_SOURCE_DIR
					+ " directory has not been found in this project. "
					+ " This directory is supposed to contains build scripts (as form of java source");
		}
		final JakeJavaCompiler javaCompilation = new JakeJavaCompiler();
		javaCompilation.addSourceFiles(buildSource.include("**/*.java"));
		javaCompilation.setClasspath(classpath);
		final File buildBinDir = new File(root, BUILD_BIN_DIR);
		if (!buildBinDir.exists()) {
			buildBinDir.mkdirs();
		}
		javaCompilation.setOutputDirectory(buildBinDir);
		JakeLog.start("Compiling build sources to "
				+ buildBinDir.getAbsolutePath());
		final boolean result = javaCompilation.compile();
		JakeLog.done();
		if (result == false) {
			JakeLog.error("Build script can't be compiled.");
		}
	}

	@SuppressWarnings({ "unchecked" })
	private boolean launch(Iterable<File> compileClasspath,
			Iterable<String> actions) {
		final File buildBin = new File(root, BUILD_BIN_DIR);
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
		final Class<JakeBuildBase> buildClass = (Class<JakeBuildBase>) JakeUtilsClassloader
				.loadClass(classLoader, buildClassName);

		final JakeBuildBase build = JakeUtilsReflect.newInstance(buildClass);
		final List<String> methods = new LinkedList<String>();
		for (final String action : actions) {
			methods.add(action.equals("default") ? "doDefault" : action);
		}
		if (methods.isEmpty()) {
			methods.add("doDefault");
		}

		JakeLog.info("Use build class '" + buildClass.getCanonicalName()
				+ "' with methods : "
				+ JakeUtilsIterable.toString(methods, ", ") + ".");
		JakeLog.info("Using options :");
		final String optionDesc = JakeUtilsReflect.newInstance(
				build.optionClass()).toString();
		JakeLog.info(optionDesc);

		JakeLog.nextLine();

		for (final String methodName : methods) {
			final Method method;
			final String actionIntro = "Action : " + methodName;
			JakeLog.info(actionIntro);
			JakeLog.info(JakeUtilsString.repeat("-", actionIntro.length()));
			try {
				method = build.getClass().getMethod(methodName);
			} catch (final NoSuchMethodException e) {
				JakeLog.warn("No zero-arg method '" + methodName
						+ "' found in class '" + buildClassName + "'. Skip.");
				continue;
			}
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

			}
			JakeLog.flush();
			JakeLog.nextLine();
		}
		JakeLog.nextLine();
		return true;
	}

	private List<File> resolveBuildCompileClasspath() {
		final URL[] urls = JakeUtilsClassloader.current().getURLs();
		final List<File> result = JakeUtilsFile.toFiles(urls);
		final File buildLibDir = new File(BUILD_LIB_DIR);
		if (buildLibDir.exists() && buildLibDir.isDirectory()) {
			final List<File> libs = JakeUtilsFile.filesOf(buildLibDir,
					JakeUtilsFile.endingBy(".jar"), true);
			for (final File file : libs) {
				result.add(file);
			}
		}
		final File jakeJarFile = JakeLocator.getJakeJarFile();
		final File extLibDir = new File(jakeJarFile.getParentFile(), "ext");
		if (extLibDir.exists() && extLibDir.isDirectory()) {
			result.addAll(JakeUtilsFile.filesOf(extLibDir,
					JakeUtilsFile.endingBy(".jar"), false));
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	private static List<String> getBuildClassNames(Iterable<File> classpath) {

		final URLClassLoader classLoader = JakeUtilsClassloader.createFrom(
				classpath, JakeLauncher.class.getClassLoader());

		// Find the Build class
		final Class<?> jakeBaseBuildClass = JakeUtilsClassloader.loadClass(
				classLoader, JakeBuildBase.class.getName());
		final Set<Class> classes = JakeUtilsClassloader.getAllTopLevelClasses(
				classLoader, JakeUtilsFile.acceptAll(), true);
		final List<String> buildClasses = new LinkedList<String>();
		for (final Class clazz : classes) {
			final boolean isAbstract = Modifier
					.isAbstract(clazz.getModifiers());
			if (!isAbstract && jakeBaseBuildClass.isAssignableFrom(clazz)) {
				buildClasses.add(clazz.getName());
			}
		}
		return buildClasses;
	}

	private String defaultBuildClassName() {
		if (!new File(root, DEFAULT_JAVA_SOURCE).exists()) {
			return null;
		}
		return JakeBuildJar.class.getName();
	}

}
