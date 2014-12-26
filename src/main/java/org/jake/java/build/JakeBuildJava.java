package org.jake.java.build;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jake.JakeBuildBase;
import org.jake.JakeClasspath;
import org.jake.JakeDir;
import org.jake.JakeDirSet;
import org.jake.JakeDoc;
import org.jake.JakeFileFilter;
import org.jake.JakeJavaCompiler;
import org.jake.JakeLog;
import org.jake.JakeOption;
import org.jake.depmanagement.JakeDependencies;
import org.jake.depmanagement.JakeDependency;
import org.jake.depmanagement.JakeDependencyResolver;
import org.jake.depmanagement.JakeScope;
import org.jake.depmanagement.JakeScopeMapping;
import org.jake.java.JakeJavadoc;
import org.jake.java.JakeResourceProcessor;
import org.jake.java.JakeUtilsJdk;
import org.jake.java.test.jacoco.Jakeoco;
import org.jake.java.test.junit.JakeUnit;
import org.jake.java.test.junit.JakeUnit.JunitReportDetail;
import org.jake.utils.JakeUtilsFile;
import org.jake.verify.sonar.JakeSonar;

public class JakeBuildJava extends JakeBuildBase {

	public static final JakeScope PROVIDED = JakeScope.of("provided").transitive(false)
			.descr("Dependencies to compile the project but that should not be embedded in produced artifacts.");

	public static final JakeScope COMPILE = JakeScope.of("compile")
			.descr("Dependencies to compile the project.");

	public static final JakeScope RUNTIME = JakeScope.of("runtime").extending(COMPILE)
			.descr("Dependencies to embed in produced artifacts (as war or fat jar * files).");

	public static final JakeScope TEST = JakeScope.of("test").extending(RUNTIME, PROVIDED)
			.descr("Dependencies necessary to compile and run tests.");

	public static final JakeScope SOURCES = JakeScope.of("sources")
			.descr("Contains the source artefacts");

	public static final JakeScope JAVADOC = JakeScope.of("javadoc")
			.descr("Contains the javadoc of this project");

	private static final JakeScopeMapping SCOPE_MAPPING = JakeScopeMapping
			.of(COMPILE).to("archive(master)", COMPILE.name())
			.and(PROVIDED).to("archive(master)")
			.and(RUNTIME).to("archive(master)", RUNTIME.name())
			.and(TEST).to("archive(master)", TEST.name());

	/**
	 * Default path for the non managed dependencies. This path is relative to {@link #baseDir()}.
	 */
	protected static final String STD_LIB_PATH = "build/libs";

	/**
	 * Filter to excludes everything in a java source directory which are not resources.
	 */
	protected static final JakeFileFilter RESOURCE_FILTER = JakeFileFilter
			.exclude("**/*.java").andExclude("**/package.html")
			.andExclude("**/doc-files");

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

	public boolean skipTests() {
		return skipTests;
	}

	@JakeOption({
		"You can force the dependencyResolver to use by specifying a class name. This class must be in Jake classpath.",
	"You can either use a fully qulified class name or just its simple name." })
	protected String dependencyResolver;

	@JakeOption({"The more details the longer tests take to be processed.",
		"BASIC mention the total time elapsed along detail on failed tests.",
		"FULL detailed report displays additionally the time to run each tests.",
	"Example : -junitReportDetail=NONE"})
	protected JunitReportDetail junitReportDetail = JunitReportDetail.BASIC;

	// A cache for
	private JakeDependencyResolver cachedResolver;

	private final Map<JakeScope, JakeClasspath> cachedDeps = new HashMap<JakeScope, JakeClasspath>();

	// --------------------------- Project settings -----------------------

	public String sourceEncoding() {
		return "UTF-8";
	}

	public String sourceJavaVersion() {
		return JakeUtilsJdk.runningJavaVersion();
	}

	public String targetJavaVersion() {
		return sourceJavaVersion();
	}

	/**
	 * Returns the location of production source code that has not been edited manually (not generated).
	 */
	public JakeDirSet editedSourceDirs() {
		return JakeDirSet.of(baseDir("src/main/java"));
	}

	/**
	 * Returns location of production source code.
	 */
	public JakeDirSet sourceDirs() {
		return editedSourceDirs().and(generatedSourceDir());
	}

	/**
	 * Returns location of production resources.
	 */
	public JakeDirSet resourceDirs() {
		return sourceDirs().andFilter(RESOURCE_FILTER).and(
				baseDir("src/main/resources"), generatedSourceDir());
	}

	/**
	 * Returns location of test source code.
	 */
	public JakeDirSet testSourceDirs() {
		return JakeDirSet.of(baseDir().sub("src/test/java"));
	}

	/**
	 * Returns location of test resources.
	 */
	public JakeDirSet testResourceDirs() {
		return JakeDirSet.of(baseDir("src/test/resources")).and(
				testSourceDirs().andFilter(RESOURCE_FILTER));
	}

	/**
	 * Returns location of generated sources.
	 */
	public File generatedSourceDir() {
		return ouputDir("generated-sources/java");
	}

	/**
	 * Returns location of generated resources.
	 */
	public File generatedResourceDir() {
		return ouputDir("generated-resources");
	}

	/**
	 * Returns location of generated resources for tests.
	 */
	public File generatedTestResourceDir() {
		return ouputDir("generated-test-resources");
	}

	/**
	 * Returns location where the java production classes are compiled.
	 */
	public File classDir() {
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
	public File testClassDir() {
		return ouputDir().sub("testClasses").createIfNotExist().root();
	}

	// --------------------------- Configurer -----------------------------

	public JakeJavaCompiler productionCompiler() {
		return JakeJavaCompiler.ofOutput(classDir())
				.andSources(sourceDirs())
				.withClasspath(depsFor(COMPILE).and(depsFor(PROVIDED)))
				.withSourceVersion(this.sourceJavaVersion())
				.withTargetVersion(this.targetJavaVersion());
	}

	public JakeJavaCompiler unitTestCompiler() {
		return JakeJavaCompiler.ofOutput(testClassDir())
				.andSources(testSourceDirs())
				.withClasspath(this.depsFor(TEST).andHead(classDir()))
				.withSourceVersion(this.sourceJavaVersion())
				.withTargetVersion(this.targetJavaVersion());
	}

	public JakeUnit unitTester() {
		final JakeClasspath classpath = JakeClasspath.of(this.testClassDir(), this.classDir()).and(this.depsFor(TEST));
		final File junitReport = new File(this.testReportDir(), "junit");
		return JakeUnit.of(classpath)
				.withReportDir(junitReport)
				.withReport(this.junitReportDetail)
				.withClassesToTest(this.testClassDir());
	}

	public JakeJavadoc javadoc() {
		final File outputDir = ouputDir(projectName() + "-javadoc");
		final File zip =  ouputDir(projectName() + "-javadoc.zip");
		return JakeJavadoc.of(sourceDirs(), outputDir, zip)
				.withClasspath(depsFor(COMPILE).and(depsFor(PROVIDED)));
	}

	public JakeJarPacker jarPacker() {
		return JakeJarPacker.of(this);
	}

	public Jakeoco jacoco() {
		final File agent = baseDir("build/libs/jacoco-agent/jacocoagent.jar");
		final File agentFile = agent.exists() ? agent : Jakeoco.defaultAgentFile();
		if (!agentFile.exists()) {
			throw new IllegalStateException("No jacocoagent.jar found neither in "
					+ Jakeoco.defaultAgentFile().getAbsolutePath()
					+ " nor in " + agent.getAbsolutePath() );
		}
		return Jakeoco.of(new File(testReportDir(), "jacoco/jacoco.exec")).withAgent(agentFile);
	}

	public JakeSonar jakeSonar() {
		final File baseDir = baseDir().root();
		JakeLog.warnIf(this.junitReportDetail != JunitReportDetail.FULL,"*  You need to use junitReportDetail=FULL " +
				"to get complete sonar test report but you are currently using " + this.junitReportDetail.name() + ".");
		return JakeSonar.of(projectFullName(), projectName(), version())
				.withProjectBaseDir(baseDir)
				.withBinaries(classDir())
				.withLibraries(depsFor(COMPILE))
				.withSources(editedSourceDirs().roots())
				.withTest(testSourceDirs().roots())
				.withProperty(JakeSonar.JUNIT_REPORTS_PATH, JakeUtilsFile.getRelativePath(baseDir, new File(testReportDir(), "junit")))
				.withProperty(JakeSonar.SUREFIRE_REPORTS_PATH, JakeUtilsFile.getRelativePath(baseDir, new File(testReportDir(), "junit")))
				.withProperty(JakeSonar.DYNAMIC_ANALYSIS, "reuseReports")
				.withProperty(JakeSonar.JACOCO_REPORTS_PATH, JakeUtilsFile.getRelativePath(baseDir, new File(testReportDir(), "jacoco/jacoco.exec")));
	}

	// --------------------------- Callable Methods -----------------------

	@JakeDoc("Generate sources and resources, compile production sources and process production resources to the classes directory.")
	public void compile() {
		JakeLog.startAndNextLine("Processing production code and resources");
		generateSources();
		productionCompiler().compile();
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
		unitTestCompiler().compile();
		processUnitTestResources();
		unitTester().run();
		JakeLog.done();
	}

	@JakeDoc("Produce documents for this project (javadoc, Html site, ...)")
	public void doc() {
		javadoc().process();
	}

	@JakeDoc({	"Create many jar files containing respectively binaries, sources, test binaries and test sources.",
	"The jar containing the binary is the one that will be used as a depe,dence for other project."})
	public void pack() {
		jarPacker().pack();
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
	 * Returns the base dependency resolver.
	 * 
	 * @see #dependencyResolver().
	 */
	protected JakeDependencyResolver baseDependencyResolver() {
		final JakeDependencies dependencies = dependencies().and(extraCommandLineDeps());
		if (dependencies.containsExternalModule()) {
			return JakeDependencyResolver.managed(jakeIvy(), dependencies, module());
		}
		return JakeDependencyResolver.unmanaged(dependencies);
	}

	@Override
	protected JakeScopeMapping scopeMapping() {
		return SCOPE_MAPPING;
	}

	/**
	 * Returns the dependencies of this module. By default it uses unmanaged dependencies stored
	 * locally in the project as described by {@link #defaultUnmanagedDependencies()} method.
	 * If you want to use managed dependencies, you must override this method.
	 */
	protected JakeDependencies dependencies() {
		return defaultUnmanagedDependencies();
	}





	/**
	 * Returns the resolved dependencies for the given scope. Depending on the passed
	 * options, it may be augmented with extra-libs mentioned in options <code>extraXxxxPath</code>.
	 */
	public final JakeClasspath depsFor(JakeScope scope) {
		if (cachedResolver == null) {
			JakeLog.startAndNextLine("Setting dependency resolver ");
			cachedResolver = baseDependencyResolver();
			JakeLog.done("Resolver set " + cachedResolver);
		}
		JakeClasspath result = cachedDeps.get(scope);
		if (result == null) {
			result = JakeClasspath.of(cachedResolver.get(scope));
			JakeLog.info("Resolved scope : " + scope + "(" + result.entries().size() + " artifacts) " + result);
			cachedDeps.put(scope, result);
		}
		return result;
	}

	protected void generateSources() {
		// Do nothing by default
	}

	@JakeDoc("Generate files to be taken as resources.  Do nothing by default.")
	protected void generateResources() {
		// Do Nothing
	}

	protected void processResources() {
		JakeResourceProcessor.of(resourceDirs()).andIfExist(generatedResourceDir()).generateTo(classDir());
	}

	protected void processUnitTestResources() {
		JakeResourceProcessor.of(testResourceDirs()).andIfExist(generatedTestResourceDir()).generateTo(testClassDir());
	}

	protected boolean checkProcessTests(JakeDirSet testSourceDirs) {
		if (skipTests) {
			return false;
		}
		if (testSourceDirs == null || testSourceDirs.jakeDirs().isEmpty()) {
			JakeLog.info("No test source declared. Skip tests.");
			return false;
		}
		if (!testSourceDirs().allExists()) {
			JakeLog.info("No existing test source directory found : " + testSourceDirs +". Skip tests.");
			return false;
		}
		return true;
	}

	// ------------------------------------

	public static void main(String[] args) {
		new JakeBuildJava().base();
	}

	private JakeDependencies extraCommandLineDeps() {
		return JakeDependencies.builder()
				.forScopes(COMPILE).onFiles(toPath(extraCompilePath))
				.forScopes(RUNTIME).onFiles(toPath(extraRuntimePath))
				.forScopes(TEST).onFiles(toPath(extraTestPath))
				.forScopes(PROVIDED).onFiles(toPath(extraProvidedPath)).build();
	}

	private final JakeClasspath toPath(String pathAsString) {
		if (pathAsString == null) {
			return JakeClasspath.of();
		}
		return JakeClasspath.of(JakeUtilsFile.toPath(pathAsString, ";", baseDir().root()));
	}

	protected JakeDependencies defaultUnmanagedDependencies() {
		final JakeDir libDir = JakeDir.of(baseDir(STD_LIB_PATH));
		return JakeDependencies.builder()
				.forScopes(COMPILE).on(JakeDependency.of(libDir.include("*.jar", "compile/*.jar")))
				.forScopes(PROVIDED).on(JakeDependency.of(libDir.include("*.jar", "provided/*.jar")))
				.forScopes(RUNTIME).on(JakeDependency.of(libDir.include("*.jar", "runtime/*.jar")))
				.forScopes(TEST).on(JakeDependency.of(libDir.include("*.jar", "test/*.jar"))).build();
	}


}
