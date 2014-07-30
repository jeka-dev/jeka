package org.jake.java;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.jake.JakeBuildBase;
import org.jake.JakeDoc;
import org.jake.JakeLog;
import org.jake.JakeOption;
import org.jake.file.JakeDirSet;
import org.jake.file.JakeFileFilter;
import org.jake.file.JakeZip;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.eclipse.JakeEclipse;
import org.jake.utils.JakeUtilsIterable;

public class JakeBuildJava extends JakeBuildBase {

	protected static final JakeFileFilter JAVA_SOURCE_ONLY_FILTER = JakeFileFilter
			.include("**/*.java");

	protected static final String STD_LIB_PATH = "build/libs";

	protected static final JakeFileFilter RESOURCE_FILTER = JakeFileFilter
			.exclude("**/*.java").andExcludeAll("**/package.html")
			.andExcludeAll("**/doc-files");

	@JakeOption({
		"Mention if you want to add extra lib in your 'compile' scope but not in your 'runtime' scope. It can be absolute or relative to the project base dir.",
		"These libs will be added to the compile path but won't be embedded in war files or fat jars.",
	"Example : -extraProvidedPath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private String extraProvidedPath;

	@JakeOption({
		"Mention if you want to add extra lib in your 'runtime' scope path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the runtime path.",
	"Example : -extraRuntimePath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private String extraRuntimePath;

	@JakeOption({
		"Mention if you want to add extra lib in your 'compile' scope path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the compile and runtime path.",
	"Example : -extraCompilePath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private String extraCompilePath;

	@JakeOption({
		"Mention if you want to add extra lib in your 'test' scope path. It can be absolute or relative to the project base dir.",
		"These libs will be added to the compile and runtime path.",
	"Example : -extraTestPath=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
	private String extraTestPath;

	@JakeOption("Turn it on to skip tests.")
	protected boolean skipTests;

	/**
	 * Returns location of production source code.
	 */
	protected JakeDirSet sourceDirs() {
		return JakeDirSet.of(baseDir("src/main/java"), generatedSourceDir());
	}

	/**
	 * Returns location of production resources.
	 */
	protected JakeDirSet resourceDirs() {
		return sourceDirs().withFilter(RESOURCE_FILTER)
				.and(baseDir("src/main/resources"), generatedSourceDir());
	}

	/**
	 * Returns location of test source code.
	 */
	protected JakeDirSet testSourceDirs() {
		return JakeDirSet.of(baseDir().sub("src/test/java"));
	}

	/**
	 * Returns location of test resources.
	 */
	protected JakeDirSet testResourceDirs() {
		return JakeDirSet.of(baseDir("src/test/resources")).and(
				testSourceDirs().withFilter(RESOURCE_FILTER));
	}

	/**
	 * Returns location of generated sources.
	 */
	protected File generatedSourceDir() {
		return buildOuputDir("ganerated-sources/java");
	}

	/**
	 * Returns location of generated resources.
	 */
	protected File generatedResourceDir() {
		return buildOuputDir("generated-ressources");
	}


	/**
	 * Returns location where the java production classes are compiled.
	 */
	protected File classDir() {
		return buildOuputDir().sub("classes").createIfNotExist().root();
	}

	/**
	 * Returns location where the java production classes are compiled.
	 */
	protected File testClassDir() {
		return buildOuputDir().sub("testClasses").createIfNotExist().root();
	}

	private JakeJavaDependencyResolver cachedResolver;

	/**
	 * Returns the base dependency resolver.
	 * @see #dependencyResolver().
	 */
	protected JakeJavaDependencyResolver baseDependencyResolver() {
		final File folder = baseDir(STD_LIB_PATH);
		final JakeJavaDependencyResolver resolver;
		if (folder.exists()) {
			resolver = JakeLocalDependencyResolver
					.standard(baseDir(STD_LIB_PATH));
		} else if (JakeEclipse.isDotClasspathPresent(baseDir().root())) {
			resolver = JakeEclipse.dependencyResolver(baseDir().root());
		} else {
			resolver = JakeLocalDependencyResolver.empty();
		}
		return resolver;
	}

	/**
	 * Returns the resolver finally used in this build. It is made of the
	 * {@link #baseDependencyResolver()} augmented with extra-libs mentioned
	 * in options <code>extraXxxxPath</code>.
	 */
	public final JakeJavaDependencyResolver dependencyResolver() {
		if (cachedResolver == null) {
			JakeLog.startAndNextLine("Resolving Dependencies ");
			final JakeJavaDependencyResolver resolver = baseDependencyResolver();
			final JakeJavaDependencyResolver extraResolver = computeExtraPath(baseDir().root());
			if (!extraResolver.isEmpty()) {
				JakeLog.info("Using extra libs : ", extraResolver.toStrings());
				cachedResolver = resolver.merge(extraResolver, null, null);
			} else {
				cachedResolver = resolver;
			}
			JakeLog.info("Effective resolver : ", cachedResolver.toStrings());
			JakeLog.done();
		}
		return cachedResolver;
	}

	// ------------ Operations ------------

	@JakeDoc("Generate java sources to be compiled along the others. Do nothing by default.")
	public void generateSources() {
		// Do nothing
	}

	@JakeDoc("Generate files to be taken as resources.  Do nothing by default.")
	public void generateResources() {
		// Do Nothing
	}


	protected void compile(JakeDirSet sources, File destination,
			Iterable<File> classpath) {
		final JakeJavaCompiler compilation = new JakeJavaCompiler();
		final JakeDirSet javaSources = sources
				.withFilter(JAVA_SOURCE_ONLY_FILTER);
		JakeLog.start("Compiling " + javaSources.countFiles(false)
				+ " source files to " + destination.getPath());
		JakeLog.nextLine();
		compilation.addSourceFiles(javaSources.listFiles());
		compilation.setClasspath(classpath);
		compilation.setOutputDirectory(destination);
		compilation.compileOrFail();
		JakeLog.done();
	}

	@JakeDoc("Compile all production code to the classes directory.")
	public void compile() {
		compile(sourceDirs(), classDir(), this.dependencyResolver().compile());
	}

	@JakeDoc("Compile all test code to the test classes directory.")
	@SuppressWarnings("unchecked")
	public void compileTest() {
		if (skipTests) {
			return;
		}
		compile(testSourceDirs(), testClassDir(),
				JakeUtilsIterable.concatToList(this.classDir(), this
						.dependencyResolver().test()));
	}



	@JakeDoc("Copy all production resources to the classes directory.")
	public void copyResources() {
		JakeLog.start("Coping resource files to " + classDir().getPath());
		final int count = resourceDirs().copyTo(classDir());
		JakeLog.done(count + " file(s) copied.");
	}

	@JakeDoc("Copy all test resources to the test classes directory.")
	public void copyTestResources() {
		if (skipTests) {
			return;
		}
		JakeLog.start("Coping test resource files to "
				+ testClassDir().getPath());
		final int count = testResourceDirs().copyTo(testClassDir());
		JakeLog.done(count + " file(s) copied.");
	}

	protected JakeJUnit juniter() {
		return JakeJUnit.classpath(this.classDir(), this.dependencyResolver()
				.test());
	}

	@JakeDoc("Run all unit tests.")
	public void runUnitTests() {
		if (skipTests) {
			return;
		}
		JakeLog.start("Launching JUnit Tests");
		juniter().launchAll(this.testClassDir()).printToNotifier();
		JakeLog.done();
	}

	@JakeDoc("Produce the Javadoc.")
	public void javadoc() {
		JakeLog.start("Generating Javadoc");
		final File dir = buildOuputDir(projectName() + "-javadoc");
		JakeJavadoc.of(this.sourceDirs())
		.withClasspath(this.dependencyResolver().compile()).process(dir);
		if (dir.exists()) {
			JakeZip.of(dir).create(
					buildOuputDir(projectName() + "-javadoc.zip"));
		}
		JakeLog.done();
	}

	@JakeDoc("Compile production code and resources, copy test code and resources then launch the unit tests.")
	@Override
	public void base() {
		super.base();
		compile();
		copyResources();
		compileTest();
		copyTestResources();
		runUnitTests();
	}

	public static void main(String[] args) {
		new JakeBuildJava().base();
	}

	private JakeLocalDependencyResolver computeExtraPath(File baseDir) {
		final List<File> extraProvidedPathList = toPath(baseDir, extraProvidedPath);
		final List<File> extraCompilePathList = toPath(baseDir, extraCompilePath);
		final List<File> extraRuntimePathList = toPath(baseDir, extraRuntimePath);
		final List<File> extraTestPathList = toPath(baseDir, extraTestPath);
		return new JakeLocalDependencyResolver(extraCompilePathList,
				extraRuntimePathList, extraTestPathList, extraProvidedPathList);
	}

	private static final List<File> toPath(File workingDir, String pathAsString) {
		if (pathAsString == null) {
			return Collections.emptyList();
		}
		return JakeUtilsFile.toPath(pathAsString, ";", workingDir);
	}

}
