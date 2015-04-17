package org.jerkar;

import java.io.File;
import java.lang.reflect.Modifier;

import org.jerkar.java.build.JkJavaBuild;
import org.jerkar.utils.JkUtilsReflect;
import org.jerkar.utils.JkUtilsString;

/**
 * A resolver for the {@link JkBuild} to use for a given project.
 * 
 * @author Jerome Angibaud
 */
public final class JkBuildResolver {

	public static final String BUILD_SOURCE_DIR = "build/spec";

	private static final String DEFAULT_JAVA_SOURCE = "src/main/java";

	public static final String BUILD_LIB_DIR = "build/libs/build";

	public static final String BUILD_BIN_DIR_NAME = "build-bin";

	public static final String BUILD_OUTPUT_PATH = "build/output";

	private static final String BUILD_BIN_DIR = BUILD_OUTPUT_PATH + "/" + BUILD_BIN_DIR_NAME;

	private final File baseDir;

	final File buildSourceDir;

	final File buildClassDir;

	final File buildlibDir;

	final File defaultJavaSource;

	JkBuildResolver(File baseDir) {
		super();
		this.baseDir = baseDir;
		this.buildSourceDir = new File(baseDir, BUILD_SOURCE_DIR);
		this.buildClassDir = new File(baseDir, BUILD_BIN_DIR);
		this.buildlibDir = new File(baseDir, BUILD_LIB_DIR);
		this.defaultJavaSource = new File(baseDir, DEFAULT_JAVA_SOURCE);
	}

	/**
	 * Resolves the {@link JkBuild} instance to use on this project.
	 */
	JkBuild resolve() {
		return resolve(null, JkBuild.class);
	}

	/**
	 * Resolves the {@link JkBuild} instance to use on this project.
	 */
	JkBuild resolve(String classNameHint) {
		return resolve(classNameHint, JkBuild.class);
	}

	/**
	 * Resolves the {@link JkBuild} instance to use on this project.
	 */
	@SuppressWarnings("unchecked")
	<T extends JkBuild> T resolve(Class<T> baseClass) {
		return (T) resolve(null, baseClass);
	}

	boolean hasBuildSource() {
		if (!buildSourceDir.exists()) {
			return false;
		}
		return JkDir.of(buildSourceDir).include("**/*.java").fileCount(false) > 0;
	}

	boolean needCompile() {
		if (!this.hasBuildSource()) {
			return false;
		}
		final JkDir dir = JkDir.of(buildSourceDir);
		for (final String path : dir.relativePathes()) {
			if (path.endsWith(".java") ) {
				final String simpleName;
				if (path.contains(File.pathSeparator)) {
					simpleName = JkUtilsString.substringAfterLast(path, File.separator);
				} else {
					simpleName = path;
				}
				if (simpleName.startsWith("_")) {
					continue;
				}
				final Class<?> clazz = JkClassLoader.current().loadGivenClassSourcePathIfExist(path);
				if (clazz == null) {
					return true;
				}
			}
		}
		return false;
	}

	private JkBuild resolve(String classNameHint, Class<? extends JkBuild> baseClass) {

		final JkClassLoader classLoader = JkClassLoader.current();

		// If class name specified in options.
		if (!JkUtilsString.isBlank(classNameHint)) {
			final Class<? extends JkBuild> clazz = classLoader.loadFromNameOrSimpleName(classNameHint, JkBuild.class);
			if (clazz == null) {
				throw new JkException("No build class named " + classNameHint + " found.");
			}
			final JkBuild build = JkUtilsReflect.newInstance(clazz);
			build.setBaseDir(this.baseDir);
			return build;
		}

		// If there is a build source
		if (this.hasBuildSource()) {
			final JkDir dir = JkDir.of(buildSourceDir);
			for (final String path : dir.relativePathes()) {
				if (path.endsWith(".java")) {
					final Class<?> clazz = classLoader.loadGivenClassSourcePath(path);
					if (baseClass.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
						final JkBuild build = (JkBuild) JkUtilsReflect.newInstance(clazz);
						build.setBaseDir(baseDir);
						return build;
					}
				}

			}
		}

		// If nothing yet found use defaults
		final JkBuild result = new JkJavaBuild();
		result.setBaseDir(baseDir);
		return result;
	}



}
