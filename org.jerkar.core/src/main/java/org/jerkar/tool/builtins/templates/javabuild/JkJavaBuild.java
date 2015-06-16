package org.jerkar.tool.builtins.templates.javabuild;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.depmanagement.JkDependency;
import org.jerkar.api.depmanagement.JkScope;
import org.jerkar.api.depmanagement.JkScopeMapping;
import org.jerkar.api.file.JkFileTree;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.file.JkPathFilter;
import org.jerkar.api.java.JkClasspath;
import org.jerkar.api.java.JkJavaCompiler;
import org.jerkar.api.java.JkJavadocMaker;
import org.jerkar.api.java.JkResourceProcessor;
import org.jerkar.api.java.junit.JkUnit;
import org.jerkar.api.java.junit.JkUnit.JunitReportDetail;
import org.jerkar.api.publishing.JkIvyPublication;
import org.jerkar.api.publishing.JkMavenPublication;
import org.jerkar.api.system.JkLog;
import org.jerkar.api.utils.JkUtilsIterable;
import org.jerkar.api.utils.JkUtilsJdk;
import org.jerkar.api.utils.JkUtilsString;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkScaffolder;
import org.jerkar.tool.builtins.templates.dependencysupport.JkBuildDependencySupport;

/**
 * Template class to define build on Java project.
 * This template is flexible enough to handle exotic project structure as it proposes
 * to override default setting at different level of granularity.<b/>
 * Beside this template define a set of "standard" scope to define dependencies.
 * You are not forced to use it strictly but it can simplify dependency management to follow a given standard.
 * 
 * @author Jerome Angibaud
 */
public class JkJavaBuild extends JkBuildDependencySupport {

	public static final JkScope PROVIDED = JkScope.of("provided").transitive(false)
			.descr("Dependencies to compile the project but that should not be embedded in produced artifacts.");

	public static final JkScope COMPILE = JkScope.of("compile")
			.descr("Dependencies to compile the project.");

	public static final JkScope RUNTIME = JkScope.of("runtime").extending(COMPILE)
			.descr("Dependencies to embed in produced artifacts (as war or fat jar * files).");

	public static final JkScope TEST = JkScope.of("test").extending(RUNTIME, PROVIDED)
			.descr("Dependencies necessary to compile and run tests.");

	public static final JkScope SOURCES = JkScope.of("sources").transitive(false)
			.descr("Contains the source artefacts");

	public static final JkScope JAVADOC = JkScope.of("javadoc").transitive(false)
			.descr("Contains the javadoc of this project");

	private static final JkScopeMapping SCOPE_MAPPING = JkScopeMapping
			.of(COMPILE).to("archives(master)", COMPILE.name())
			.and(PROVIDED).to("archives(master)", COMPILE.name())
			.and(RUNTIME).to("archives(master)", RUNTIME.name())
			.and(TEST).to("archives(master)", RUNTIME.name(), "test(master)");

	/**
	 * Filter to excludes everything in a java source directory which are not resources.
	 */
	public static final JkPathFilter RESOURCE_FILTER = JkPathFilter
			.exclude("**/*.java").andExclude("**/package.html")
			.andExclude("**/doc-files");



	@JkDoc("Tests")
	public JkOptionTest tests = new JkOptionTest();

	@JkDoc("Packaging")
	public JkOptionPack pack = new JkOptionPack();

	@JkDoc({"Inject extra dependencies to the desired scope.",
	"It can be absolute or relative to the project base dir."})
	public JkOptionExtaPath extraPath = new JkOptionExtaPath();


	@Override
	protected List<Class<Object>> pluginTemplateClasses() {
		return JkUtilsIterable.listOfGeneric(JkJavaBuildPlugin.class);
	}

	// --------------------------- Project settings -----------------------

	/**
	 * Returns the encoding of source files for the compiler.
	 */
	public String sourceEncoding() {
		return "UTF-8";
	}



	/**
	 * Returns the Java source version for the compiler (as "1.4", 1.6", "7", ...).
	 */
	public String sourceJavaVersion() {
		return JkUtilsJdk.runningJavaVersion();
	}

	/**
	 * Returns the Java target version for the compiler (as "1.4", 1.6", "7", ...).
	 */
	public String targetJavaVersion() {
		return sourceJavaVersion();
	}

	/**
	 * Returns the location of production source code that has been edited manually (not generated).
	 */
	public JkFileTreeSet editedSources() {
		return JkFileTreeSet.of(baseDir("src/main/java"));
	}

	/**
	 * Returns the location of unit test source code that has been edited manually (not generated).
	 */
	public JkFileTreeSet unitTestEditedSources() {
		return JkFileTreeSet.of(baseDir("src/test/java"));
	}

	/**
	 * Returns location of production source code (containing edited + generated sources).
	 */
	public JkFileTreeSet sources() {
		return JkJavaBuildPlugin.applySourceDirs(this.plugins.getActives(),
				editedSources().and(generatedSourceDir()));
	}

	/**
	 * Returns the location of production resources that has been edited manually (not generated).
	 */
	public JkFileTreeSet editedResources() {
		return JkFileTreeSet.of(baseDir("src/main/resources"));
	}

	/**
	 * Returns location of production resources.
	 */
	public JkFileTreeSet resources() {
		final JkFileTreeSet original = sources().andFilter(RESOURCE_FILTER).and(
				editedResources()).and(generatedResourceDir());
		return JkJavaBuildPlugin.applyResourceDirs(this.plugins.getActives(), original);
	}

	/**
	 * Returns location of test source code.
	 */
	public JkFileTreeSet unitTestSources() {
		return JkJavaBuildPlugin.applyTestSourceDirs(this.plugins.getActives(),
				unitTestEditedSources().and(unitTestGeneratedSourceDir()));
	}

	/**
	 * Returns location of test resources.
	 */
	public JkFileTreeSet unitTestResources() {
		final JkFileTreeSet original = JkFileTreeSet.of(unitTestGeneratedSourceDir()).and(
				unitTestSources().andFilter(RESOURCE_FILTER));
		return JkJavaBuildPlugin.applyTestResourceDirs(this.plugins.getActives(), original);
	}

	/**
	 * Returns location of generated sources.
	 */
	public File generatedSourceDir() {
		return ouputDir("generated-sources/java");
	}

	/**
	 * Returns location of generated unit test sources.
	 */
	public File unitTestGeneratedSourceDir() {
		return ouputDir("generated-unitTest-sources/java");
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
		return ouputDir("generated-unitTest-resources");
	}

	/**
	 * Returns location where the java production classes are compiled.
	 */
	public File classDir() {
		return ouputDir().from("classes").createIfNotExist().root();
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
		return ouputDir().from("testClasses").createIfNotExist().root();
	}

	// --------------------------- Configurer -----------------------------

	public JkJavaCompiler productionCompiler() {
		return JkJavaCompiler.ofOutput(classDir())
				.andSources(sources())
				.withClasspath(depsFor(COMPILE, PROVIDED))
				.withSourceVersion(this.sourceJavaVersion())
				.withTargetVersion(this.targetJavaVersion());
	}

	public JkJavaCompiler unitTestCompiler() {
		return JkJavaCompiler.ofOutput(testClassDir())
				.andSources(unitTestSources())
				.withClasspath(this.depsFor(TEST, PROVIDED).andHead(classDir()))
				.withSourceVersion(this.sourceJavaVersion())
				.withTargetVersion(this.targetJavaVersion());
	}

	public final JkUnit unitTester() {
		return JkJavaBuildPlugin.applyUnitTester(plugins.getActives(), createUnitTester());
	}

	protected JkUnit createUnitTester() {
		final JkClasspath classpath = JkClasspath.of(this.testClassDir(), this.classDir())
				.and(this.depsFor(TEST, PROVIDED));
		final File junitReport = new File(this.testReportDir(), "junit");
		final JkUnit result = JkUnit.of(classpath)
				.withReportDir(junitReport)
				.withReport(this.tests.report)
				.withClassesToTest(this.testClassDir())
				.forked(this.tests.fork);
		return result;
	}

	public JkJavadocMaker javadocMaker() {
		return javadocMaker(this,  true, false);
	}

	public final JkJavaPacker packer() {
		return JkJavaBuildPlugin.applyPacker(plugins.getActives(), createPacker());
	}

	protected JkJavaPacker createPacker() {
		return JkJavaPacker.of(this);
	}



	protected JkResourceProcessor resourceProcessor() {
		return JkResourceProcessor.of(resources());
	}

	// --------------------------- Callable Methods -----------------------



	@Override
	protected JkScaffolder scaffolder() {
		final Runnable action = new Runnable() {

			@Override
			public void run() {
				for (final JkFileTree dir : editedSources().fileTrees()) {
					dir.root().mkdirs();
				}
				for (final JkFileTree dir : unitTestEditedSources().fileTrees()) {
					dir.root().mkdirs();
				}
			}
		};
		return super.scaffolder().withExtraAction(action)
				.withExtendedClass(JkJavaBuild.class);
	}

	@JkDoc("Generate sources and resources, compile production sources and process production resources to the classes directory.")
	public void compile() {
		JkLog.startln("Processing production code and resources");
		generateSources();
		productionCompiler().compile();
		generateResources();
		processResources();
		JkLog.done();
	}

	@JkDoc("Compile and run all unit tests.")
	public void unitTest() {
		this.generateUnitTestSources();
		if (!checkProcessTests(unitTestSources())) {
			return;
		}
		JkLog.startln("Process unit tests");
		unitTestCompiler().compile();
		generateUnitTestResources();
		processUnitTestResources();
		unitTester().run();
		JkLog.done();
	}

	@JkDoc("Produce documents for this project (javadoc, Html site, ...)")
	public void javadoc() {
		javadocMaker().process();
		signIfNeeded(javadocMaker().zipFile());
	}

	/**
	 * Signs the specified files with PGP if the option <code>pack.signWithPgp</code> is <code>true</code>.
	 * The signature will be detached in the same folder than the signed file and will have the same name
	 * but with the <i>.asc</i> suffix.
	 */
	protected final void signIfNeeded(File ...files) {
		if (pack.signWithPgp) {
			pgp().sign(files);
		}
	}

	@JkDoc({"Create many jar files containing respectively binaries, sources, test binaries and test sources.",
	"The jar containing the binary is the one that will be used as a depe,dence for other project."})
	public void pack() {
		packer().pack();
	}

	@JkDoc("Method executed by default when none is specified. By default this method equals to #clean + #doPack")
	@Override
	public void doDefault() {
		doPack();
	}


	@JkDoc({"Publish the produced artifact to the defined repositories. ",
	"This can work only if a 'publishable' repository has been defined and the artifact has been generated (pack method)."})
	public void publish() {
		final Date date = this.buildTime();
		if (this.publisher().hasMavenPublishRepo()) {
			final JkMavenPublication publication = mavenPublication();
			this.publisher().publishMaven(versionedModule(), publication, dependencyResolver().declaredDependencies(), date);
		}
		if (this.publisher().hasIvyPublishRepo()) {
			this.publisher().publishIvy(versionedModule(), ivyPublication(), dependencyResolver().declaredDependencies(), COMPILE, SCOPE_MAPPING, date);
		}
	}


	// ----------------------- Overridable sub-methods ---------------------

	/**
	 * Override this method if you need to generate some production sources
	 */
	protected void generateSources() {
		// Do nothing by default
	}

	/**
	 * Override this method if you need to generate some unit test sources.
	 */
	protected void generateUnitTestSources() {
		// Do nothing by default
	}

	/**
	 * Override this method if you need to generate some resources.
	 */
	protected void generateResources() {
		// Do nothing by default
	}

	/**
	 * Override this method if you need to generate some resources for running unit tests.
	 */
	protected void generateUnitTestResources() {
		// Do nothing by default
	}

	/**
	 * Copies the generated sources for production into the class dir. If you want to do special
	 * processing (as interpolating) you should override this method.
	 */
	protected void processResources() {
		this.resourceProcessor().generateTo(classDir());
	}

	/**
	 * Copies the generated resources for test into the test class dir. If you want to do special
	 * processing (as interpolating) you should override this method.
	 */
	protected void processUnitTestResources() {
		JkResourceProcessor.of(unitTestResources()).andIfExist(generatedTestResourceDir()).generateTo(testClassDir());
	}

	protected boolean checkProcessTests(JkFileTreeSet testSourceDirs) {
		if (this.tests.skip) {
			return false;
		}
		if (testSourceDirs == null || testSourceDirs.fileTrees().isEmpty()) {
			JkLog.info("No test source declared. Skip tests.");
			return false;
		}
		if (!unitTestSources().allExists()) {
			JkLog.info("No existing test source directory found : " + testSourceDirs +". Skip tests.");
			return false;
		}
		return true;
	}

	@Override
	protected JkScopeMapping scopeMapping() {
		return SCOPE_MAPPING;
	}

	@Override
	protected JkScope defaultScope() {
		return COMPILE;
	}

	protected JkMavenPublication mavenPublication(boolean includeTests, boolean includeSources) {
		final JkJavaPacker packer = packer();
		return JkMavenPublication.of(artifactName(packer.jarFile()) ,packer.jarFile())
				.andIf(includeSources, packer.jarSourceFile(), "sources")
				.andOptional(javadocMaker().zipFile(), "javadoc")
				.andOptionalIf(includeTests, packer.jarTestFile(), "test")
				.andOptionalIf(includeTests && includeSources, packer.jarTestSourceFile(), "testSources");
	}

	private static String artifactName(File file) {
		final String name = file.getName();
		return name.contains(".") ? JkUtilsString.substringBeforeLast(name, ".") : name;
	}

	protected JkIvyPublication ivyPublication(boolean includeTests, boolean includeSources) {
		final JkJavaPacker packer = packer();
		return JkIvyPublication.of(packer.jarFile(), COMPILE)
				.andIf(includeSources, packer.jarSourceFile(), "source", SOURCES)
				.andOptional(javadocMaker().zipFile(), "javadoc", JAVADOC)
				.andOptionalIf(includeTests, packer.jarTestFile(), "jar", TEST)
				.andOptionalIf(includeTests, packer.jarTestSourceFile(), "source", SOURCES);
	}

	protected JkIvyPublication ivyPublication() {
		return ivyPublication(includeTestsInPublication(), includeSourcesInPublication());
	}

	protected JkMavenPublication mavenPublication() {
		return mavenPublication(includeTestsInPublication(), includeSourcesInPublication());
	}

	protected boolean includeTestsInPublication() {
		return false;
	}

	protected boolean includeSourcesInPublication() {
		return true;
	}



	// ------------------------------------

	public static void main(String[] args) {
		new JkJavaBuild().doDefault();
	}

	@Override
	protected JkDependencies extraCommandLineDeps() {
		return JkDependencies.builder()
				.usingDefaultScopes(COMPILE).onFiles(toPath(extraPath.compile))
				.usingDefaultScopes(RUNTIME).onFiles(toPath(extraPath.runtime))
				.usingDefaultScopes(TEST).onFiles(toPath(extraPath.test))
				.usingDefaultScopes(PROVIDED).onFiles(toPath(extraPath.provided)).build();
	}

	@Override
	protected JkDependencies implicitDependencies() {
		final JkFileTree libDir = JkFileTree.of(baseDir(STD_LIB_PATH));
		if (!libDir.root().exists()) {
			return super.implicitDependencies();
		}
		return JkDependencies.builder()
				.usingDefaultScopes(COMPILE).on(JkDependency.of(libDir.include("*.jar", "compile/*.jar")))
				.usingDefaultScopes(PROVIDED).on(JkDependency.of(libDir.include("provided/*.jar")))
				.usingDefaultScopes(RUNTIME).on(JkDependency.of(libDir.include("runtime/*.jar")))
				.usingDefaultScopes(TEST).on(JkDependency.of(libDir.include("test/*.jar"))).build();
	}

	// Lifecycle methods

	@JkDoc("Lifecycle method :#compile. As doCompile is the first stage, this is equals to #compile")
	public void doCompile() {
		this.clean();
		this.compile();
	}

	@JkDoc("Lifecycle method : #doCompile + #unitTest")
	public void doUnitTest() {
		this.doCompile();
		this.unitTest();
	}

	@JkDoc("Lifecycle method : #doUnitTest + #pack")
	public void doPack() {
		doUnitTest();
		pack();
	}

	@JkDoc("Lifecycle method : #doUnitTest + #pack")
	public void doVerify() {
		doPack();
		verify();
	}

	@JkDoc("Lifecycle method : #doVerify + #publish")
	public void doPublish() {
		doVerify();
		publish();
	}

	/**
	 * Options about extra path
	 */
	public static class JkOptionExtaPath {

		@JkDoc({
			"provided scope : these libs will be added to the compile path but won't be embedded in war files or fat jars.",
		"Example : -extraPath.provided=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
		private String provided;

		@JkDoc({
			"runtime scope : these libs will be added to the runtime path.",
		"Example : -extraPath.runtime=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
		private String runtime;

		@JkDoc({
			"compile scope : these libs will be added to the compile and runtime path.",
		"Example : -extraPath.compile=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
		private String compile;

		@JkDoc({
			"test scope : these libs will be added to the compile and runtime path.",
		"Example : -extraPath.test=C:\\libs\\mylib.jar;libs/others/**/*.jar" })
		private String test;

		public String provided() {
			return provided;
		}

		public String runtime() {
			return runtime;
		}

		public String compile() {
			return compile;
		}

		public String test() {
			return test;
		}

	}

	/**
	 * Options about tests
	 */
	public final static class JkOptionTest {

		@JkDoc("Turn it on to skip tests.")
		public boolean skip;

		@JkDoc("turn it on to run tests in a forked process.")
		public boolean fork;

		@JkDoc({"The more details the longer tests take to be processed.",
			"BASIC mention the total time elapsed along detail on failed tests.",
			"FULL detailed report displays additionally the time to run each tests.",
		"Example : -report=NONE"})
		public JunitReportDetail report = JunitReportDetail.BASIC;


	}

	public static final class JkOptionPack {

		@JkDoc("When true, produce a fat-jar, meaning a jar embedding all the dependencies.")
		public boolean fatJar;

		@JkDoc("When true, the produced artifacts are signed with PGP.")
		public boolean signWithPgp;

		@JkDoc("When true, tests classes and sources are packed in jars")
		public boolean tests;

	}

	private static JkJavadocMaker javadocMaker(JkJavaBuild javaBuild, boolean fullName, boolean includeVersion) {
		String name = fullName ? javaBuild.moduleId().toString(): javaBuild.moduleId().toString();
		if (includeVersion) {
			name = name + "-" + javaBuild.version().name();
		}
		name = name + "-javadoc";
		return JkJavadocMaker.of(javaBuild.sources(), javaBuild.ouputDir(name), javaBuild.ouputDir(name + ".jar"))
				.withClasspath(javaBuild.depsFor(JkJavaBuild.COMPILE, JkJavaBuild.PROVIDED));
	}



}
