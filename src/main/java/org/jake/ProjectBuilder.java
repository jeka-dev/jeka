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
import org.jake.utils.JakeUtilsTime;

class ProjectBuilder {

	private static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String BUILD_LIB_DIR = "build/libs/build";

	private static final String BUILD_BIN_DIR = "build/output/build-bin";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	private final File moduleBaseDir;

	private final String moduleRelativePath;

	/**
	 * Create a module builder where is specified the base directory of the module along
	 * its relative path to the 'root' module.
	 * The Relative path is used to display the module under build
	 */
	public ProjectBuilder(File buildBaseParentDir, File moduleBaseDir) {
		super();
		this.moduleBaseDir = moduleBaseDir;
		this.moduleRelativePath = JakeUtilsFile.getRelativePath(buildBaseParentDir, moduleBaseDir);
	}

	public ProjectBuilder(File moduleBaseDir) {
		super();
		this.moduleBaseDir = moduleBaseDir;
		this.moduleRelativePath = moduleBaseDir.getName();
	}



	public boolean build(Iterable<String> methods) {
		final long start = System.nanoTime();

		final String pattern = "-";
		final String intro = "Building module : " + moduleRelativePath;
		JakeLog.info(JakeUtilsString.repeat(pattern, intro.length() ));
		JakeLog.info(intro);
		JakeLog.info(JakeUtilsString.repeat(pattern, intro.length() ));
		JakeLog.nextLine();

		final Iterable<File> buildClasspath = this
				.resolveBuildCompileClasspath();

		if (this.hasBuildSource()) {
			this.compileBuild(buildClasspath);
		} else {
			JakeLog.info("No specific build class provided, will use default one (Specific build Java sources are supposed to be in [Project dir]/build/spec directory)." );
		}
		JakeLog.nextLine();
		final boolean result = this.launch(buildClasspath, methods);

		final float duration = JakeUtilsTime.durationInSeconds(start);
		if (result) {
			JakeLog.info("Module " + moduleRelativePath + " processed with success in " + duration + " seconds.");
		} else {
			JakeLog.info("Module " + moduleRelativePath + " failed after " + duration + " seconds.");
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

	private void compileBuild(Iterable<File> classpath) {
		final JakeDirView buildSource = JakeDirView.of(new File(moduleBaseDir,
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
		final File buildBinDir = new File(moduleBaseDir, BUILD_BIN_DIR);
		if (!buildBinDir.exists()) {
			buildBinDir.mkdirs();
		}
		javaCompilation.setOutputDirectory(buildBinDir);
		JakeLog.info("Compiling build sources to "
				+ buildBinDir.getAbsolutePath() + "...");
		JakeLog.info("using classpath " + System.getProperty("java.class.path"));
		final boolean result = javaCompilation.compile();
		JakeLog.info("Done");
		if (result == false) {
			JakeLog.error("Build script can't be compiled.");
		}
	}

	@SuppressWarnings({ "unchecked" })
	private boolean launch(Iterable<File> compileClasspath,
			Iterable<String> actions) {
		final File buildBin = new File(moduleBaseDir, BUILD_BIN_DIR);
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
		final List<String> optionDesc = JakeUtilsReflect.newInstance(
				build.optionClass()).toStrings();
		JakeLog.info("", optionDesc);

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
		if (!new File(moduleBaseDir, DEFAULT_JAVA_SOURCE).exists()) {
			return null;
		}
		return JakeBuildJar.class.getName();
	}

}
