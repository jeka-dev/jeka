package org.jake.java;

import java.io.File;

import org.jake.JakeBuildBase;
import org.jake.JakeDoc;
import org.jake.JakeLog;
import org.jake.JakeOption;
import org.jake.file.JakeDirSet;
import org.jake.file.JakeFileFilter;
import org.jake.file.utils.JakeUtilsFile;
import org.jake.java.test.junit.JakeUnit;
import org.jake.java.test.junit.JakeUnit.JunitReportDetail;

public class JakeBuildJava extends JakeBuildBase {

	/**
	 * Default path for the non managed dependencies. This path is relative to {@link #baseDir()}.
	 */
	protected static final String STD_LIB_PATH = "build/libs";

	/**
	 * Filter to excludes everything in a java source directory which are not resources.
	 */
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

	@JakeOption({
		"You can force the dependencyResolver to use by specifying a class name. This class must be in Jake classpath.",
	"You can either use a fully qulified class name or just its simple name." })
	protected String dependencyResolver;

	@JakeOption({"The more details the longer tests take to be processed.",
		"BASIC mention the total time elapsed along detail on failed tests.",
		"FULL detailed report displays additionally the time to run each tests.",
	"Example : -junitReportDetail=NONE"})
	protected JunitReportDetail junitReportDetail = JunitReportDetail.BASIC;

	// --------------------------- Callable Methods -----------------------

	@JakeDoc("Generate sources and resources, compile production sources and process production resources to the classes directory.")
	public void compile() {
		JakeLog.startAndNextLine("Processing production code and resources");
		generateSources();
		compiler(sourceDirs(), classDir(), this.deps().compile()).compileOrFail();;
		generateResources();
		processResources();
		JakeLog.done();
	}

	@JakeDoc("Compile and run all unit tests.")
	public void unitTest() {
		if (!checkProcessTests(testSourceDirs())) {
			return;
		}
		JakeLog.startAndNextLine("Process unit tests");
		compileUnitTests();
		processUnitTestResources();
		runUnitTests();
		JakeLog.done();
	}

	@JakeDoc("Produce the Javadoc.")
	public void javadoc() {
		JakeJavadoc.of(this.sourceDirs())
		.withClasspath(this.deps().compile())
		.processAndZip(ouputDir(projectName() + "-javadoc"), ouputDir(projectName() + "-javadoc.zip"));
	}

	@JakeDoc("Compile production code and resources, compile test code and resources then launch the unit tests.")
	@Override
	public void base() {
		super.base();
		compile();
		unitTest();
	}

	// ----------------------- Overridable sub-methods ---------------------

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
		return sourceDirs().withFilter(RESOURCE_FILTER).and(
				baseDir("src/main/resources"), generatedSourceDir());
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
		return ouputDir("ganerated-sources/java");
	}

	/**
	 * Returns location of generated resources.
	 */
	protected File generatedResourceDir() {
		return ouputDir("generated-resources");
	}

	/**
	 * Returns location of generated resources for tests.
	 */
	protected File generatedTestResourceDir() {
		return ouputDir("generated-test-resources");
	}

	/**
	 * Returns location where the java production classes are compiled.
	 */
	protected File classDir() {
		return ouputDir().sub("classes").createIfNotExist().root();
	}

	/**
	 * Returns location where the test reports are written.
	 */
	public File testReportDir() {
		return ouputDir("test-reports");
	}

	/**
	 * Returns location where the java production classes are compiled.
	 */
	protected File testClassDir() {
		return ouputDir().sub("testClasses").createIfNotExist().root();
	}

	private JakeJavaDependencyResolver cachedResolver;

	/**
	 * Returns the base dependency resolver.
	 * 
	 * @see #dependencyResolver().
	 */
	protected JakeJavaDependencyResolver baseDependencyResolver() {
		final File folder = baseDir(STD_LIB_PATH);
		final JakeJavaDependencyResolver resolver;
		if (folder.exists()) {
			resolver = JakeLocalDependencyResolver
					.standard(baseDir(STD_LIB_PATH));
		} else {
			resolver = JakeLocalDependencyResolver.empty();
		}
		return resolver;
	}

	/**
	 * Returns the resolver finally used in this build. Depending od the passed
	 * options, It is made of the {@link #baseDependencyResolver()} augmented
	 * with extra-libs mentioned in options <code>extraXxxxPath</code>.
	 */
	public final JakeJavaDependencyResolver deps() {
		if (cachedResolver == null) {
			JakeLog.startAndNextLine("Resolving Dependencies ");
			final JakeJavaDependencyResolver resolver = JakeJavaDependencyResolver
					.findByClassNameOrDfault(dependencyResolver, baseDependencyResolver());
			final JakeJavaDependencyResolver extraResolver = computeExtraPath();
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

	protected void generateSources() {
		// Do nothing by default
	}

	@JakeDoc("Generate files to be taken as resources.  Do nothing by default.")
	protected void generateResources() {
		// Do Nothing
	}

	protected JakeJavaCompiler compiler(JakeDirSet sources, File outputDir,
			Iterable<File> classpath) {
		return JakeJavaCompiler.ofOutput(outputDir)
				.addSourceFiles(sources)
				.setClasspath(classpath);
	}

	protected void processResources() {
		JakeResourceProcessor.of(resourceDirs()).andIfExist(generatedResourceDir()).generateTo(classDir());
	}

	protected JakeUnit jakeUnit() {
		final JakeClasspath classpath = JakeClasspath.of(this.testClassDir(), this.classDir()).and(this.deps().test());
		return JakeUnit.of(classpath).withReport(junitReportDetail, new File(testReportDir(),"junit"));
	}

	protected void compileUnitTests() {
		final Iterable<File> classpath =  this.deps().test().andAtHead(classDir());
		compiler(testSourceDirs(), testClassDir(), classpath).compileOrFail();
	}

	protected void processUnitTestResources() {
		JakeResourceProcessor.of(testResourceDirs()).andIfExist(generatedTestResourceDir()).generateTo(testClassDir());
	}

	protected void runUnitTests() {
		runJunitTests(this.testClassDir());
	}

	protected void runJunitTests(File testClassDir) {
		jakeUnit().launchAll(testClassDir);
	}

	protected boolean checkProcessTests(JakeDirSet testSourceDirs) {
		if (skipTests) {
			return false;
		}
		if (testSourceDirs == null || testSourceDirs.listJakeDirs().isEmpty()) {
			JakeLog.info("No test source declared. Skip tests.");
			return false;
		}
		if (!testResourceDirs().exist()) {
			JakeLog.info("No existing source folder declared in " + testSourceDirs +". Skip tests.");
			return false;
		}
		return true;
	}

	// ------------------------------------

	public static void main(String[] args) {
		new JakeBuildJava().base();
	}

	private JakeLocalDependencyResolver computeExtraPath() {
		return new JakeLocalDependencyResolver(
				toPath(extraCompilePath), toPath(extraRuntimePath), toPath(extraTestPath), toPath(extraProvidedPath));
	}

	private final JakeClasspath toPath(String pathAsString) {
		if (pathAsString == null) {
			return JakeClasspath.of();
		}
		return JakeClasspath.of(JakeUtilsFile.toPath(pathAsString, ";", baseDir().root()));
	}

}
