package org.jake;

import java.io.File;
import java.lang.reflect.Modifier;

import org.jake.java.build.JakeJavaBuild;
import org.jake.java.eclipse.JakeBuildPluginEclipse;
import org.jake.utils.JakeUtilsReflect;
import org.jake.utils.JakeUtilsString;

/**
 * A resolver for the {@link JakeBuild} to use for a given project.
 * 
 * @author Jerome Angibaud
 */
public final class JakeBuildResolver {

	public static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	private static final String BUILD_LIB_DIR = "build/libs/build";

	public static final String BUILD_BIN_DIR_NAME = "build-bin";

	public static final String BUILD_OUTPUT_PATH = "build/output";

	private static final String BUILD_BIN_DIR = BUILD_OUTPUT_PATH + "/" + BUILD_BIN_DIR_NAME;

	private final File baseDir;

	final File buildSourceDir;

	final File buildClassDir;

	final File buildlibDir;

	final File defaultJavaSource;

	JakeBuildResolver(File baseDir) {
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
	JakeBuild resolve() {
		return resolve(null, JakeBuild.class);
	}

	/**
	 * Resolves the {@link JakeBuild} instance to use on this project.
	 */
	JakeBuild resolve(String classNameHint) {
		return resolve(classNameHint, JakeBuild.class);
	}

	/**
	 * Resolves the {@link JakeBuild} instance to use on this project.
	 */
	@SuppressWarnings("unchecked")
	<T extends JakeBuild> T resolve(Class<T> baseClass) {
		return (T) resolve(null, baseClass);
	}

	boolean hasBuildSource() {
		if (!buildSourceDir.exists()) {
			return false;
		}
		return JakeDir.of(buildSourceDir).include("**/*.java").fileCount(false) > 0;
	}

	boolean needCompile() {
		if (!this.hasBuildSource()) {
			return false;
		}
		final JakeDir dir = JakeDir.of(buildSourceDir);
		for (final String path : dir.relativePathes()) {
			if (path.endsWith(".java") ) {
				final String simpleName;
				if (path.contains(File.pathSeparator)) {
					simpleName = JakeUtilsString.substringAfterLast(path, File.separator);
				} else {
					simpleName = path;
				}
				if (simpleName.startsWith("_")) {
					continue;
				}
				final Class<?> clazz = JakeClassLoader.current().loadGivenClassSourcePathIfExist(path);
				if (clazz == null) {
					return true;
				}
			}
		}
		return false;
	}

	private JakeBuild resolve(String classNameHint, Class<? extends JakeBuild> baseClass) {

		final JakeClassLoader classLoader = JakeClassLoader.current();

		// If class name specified in options.
		if (!JakeUtilsString.isBlank(classNameHint)) {
			final Class<? extends JakeBuild> clazz = classLoader.loadFromNameOrSimpleName(classNameHint, JakeBuild.class);
			if (clazz == null) {
				throw new JakeException("No build class named " + classNameHint + " found.");
			}
			final JakeBuild build = JakeUtilsReflect.newInstance(clazz);
			build.setBaseDir(this.baseDir);
			return build;
		}

		// If there is a build source
		if (this.hasBuildSource()) {
			final JakeDir dir = JakeDir.of(buildSourceDir);
			for (final String path : dir.relativePathes()) {
				if (path.endsWith(".java")) {
					final Class<?> clazz = classLoader.loadGivenClassSourcePath(path);
					if (baseClass.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
						final JakeBuild build = (JakeBuild) JakeUtilsReflect.newInstance(clazz);
						build.setBaseDir(baseDir);
						return build;
					}
				}

			}
		}

		// If nothing yet found use defaults
		final JakeBuild result = new JakeJavaBuild();
		result.setBaseDir(baseDir);
		if (new File(baseDir, DEFAULT_JAVA_SOURCE).exists() && buildlibDir.exists()) {
			return result;
		}
		if (JakeBuildPluginEclipse.candidate(baseDir)) {
			final JakeBuildPluginEclipse pluginEclipse = new JakeBuildPluginEclipse();
			JakeOptions.populateFields(pluginEclipse);
			result.plugins.addActivated(pluginEclipse);
			return result;
		}

		return null;
	}

}
