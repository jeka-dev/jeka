package org.jake;

import java.io.File;
import java.lang.reflect.Modifier;

import org.jake.java.build.JakeJavaBuild;
import org.jake.java.eclipse.JakeEclipseBuild;
import org.jake.utils.JakeUtilsFile;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

/**
 * A resolver for the {@link JakeBuild} to use for a given project.
 * 
 * @author Jerome Angibaud
 */
class BuildResolver {

	private static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	private static final String BUILD_LIB_DIR = "build/libs/build";

	private static final String BUILD_BIN_DIR = "build/output/build-bin";

	private final File baseDir;

	final File buildSourceDir;

	final File buildClassDir;

	final File buildlibDir;

	final File defaultJavaSource;

	public BuildResolver(File baseDir) {
		super();
		this.baseDir = baseDir;
		this.buildSourceDir = new File(baseDir, BUILD_SOURCE_DIR);
		this.buildClassDir = new File(baseDir, BUILD_BIN_DIR);
		this.buildlibDir = new File(baseDir, BUILD_LIB_DIR);
		this.defaultJavaSource = new File(baseDir, DEFAULT_JAVA_SOURCE);
	}

	/**
	 * Resolves the {@link JakeBuild} instance to use on this project.
	 */
	public JakeBuild resolve() {
		return resolve(null, JakeBuild.class);
	}

	/**
	 * Resolves the {@link JakeBuild} instance to use on this project.
	 */
	public JakeBuild resolve(String classNameHint) {
		return resolve(classNameHint, JakeBuild.class);
	}

	/**
	 * Resolves the {@link JakeBuild} instance to use on this project.
	 */
	@SuppressWarnings("unchecked")
	public <T extends JakeBuild> T resolve(Class<T> baseClass) {
		return (T) resolve(null, baseClass);
	}

	/**
	 * Resolves the {@link JakeBuild} instance to use on this project. A class name hint
	 * can be provided in order to choose a certain build class.
	 */
	private JakeBuild resolve(String classNameHint, Class<? extends JakeBuild> baseClass) {
		final Class<? extends JakeBuild> clazz = resolveClass(classNameHint, baseClass);
		if (clazz == null) {
			return null;
		}
		final JakeBuild build = JakeUtilsReflect.newInstance(clazz);
		build.setBaseDir(baseDir);
		if (this.isClassDefinedInProject(clazz)) {
			enrichWithPlugins(build);
		}
		return build;
	}

	boolean hasBuildSource() {
		if (!buildSourceDir.exists()) {
			return false;
		}
		return JakeDir.of(buildSourceDir).include("**/*.java").fileCount(false) > 0;
	}

	boolean needCompile() {
		if (this.hasBuildSource()) {
			return false;
		}
		final JakeDir dir = JakeDir.of(buildSourceDir);
		for (final String path : dir.relativePathes()) {
			if (path.endsWith(".java")) {
				final Class<?> clazz = JakeClassLoader.current().loadGivenClassSourcePathIfExist(path);
				if (clazz == null) {
					return true;
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private Class<? extends JakeBuild> resolveClass(String classNameHint, Class<? extends JakeBuild> baseClass) {

		final JakeClassLoader classLoader = JakeClassLoader.current();

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
			final JakeDir dir = JakeDir.of(buildSourceDir);
			for (final String path : dir.relativePathes()) {
				if (path.endsWith(".java")) {
					final Class<?> clazz = classLoader.loadGivenClassSourcePath(path);
					if (baseClass.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
						return (Class<? extends JakeBuild>) clazz;
					}
				}

			}
		}

		// If nothing yet found use defaults
		if (new File(baseDir, DEFAULT_JAVA_SOURCE).exists() && buildlibDir.exists()) {
			return classLoader.load(JakeJavaBuild.class.getName());
		}
		if (JakeEclipseBuild.candidate(baseDir)) {
			return classLoader.load(JakeEclipseBuild.class.getName());
		}
		return null;
	}

	private boolean isClassDefinedInProject(Class<?> clazz) {
		final File entry = JakeClassLoader.of(clazz).fullClasspath().getEntryContainingClass(clazz.getName());
		return JakeUtilsFile.equals(entry, buildClassDir);
	}

	private void enrichWithPlugins(JakeBuild build) {

	}


}
